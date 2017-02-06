package converter;

import automaton.PossibleWorldWrap;
import converter.automaton.*;
import converter.petrinet.CanNotConvertPNToAutomatonException;
import converter.petrinet.NumberOfStatesDoesNotMatchException;
import converter.utils.*;
import net.sf.tweety.logics.pl.syntax.Proposition;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

import java.util.*;

/**
 * Created by arnelaponin on 08/11/2016.
 */
public class ModelRepairer {

    public static Petrinet repairXorBranch(InformationWrapper informationWrapper) throws Exception {
        PetrinetGraph net = informationWrapper.getNet();
        MyAutomaton procedural = informationWrapper.getProceduralAutomaton();
        Automaton reducedIntersection = informationWrapper.getReducedIntersection();
        utils.AutomatonUtils.printAutomaton(reducedIntersection, "automaton_before_xor.gv");
        MyAutomaton trimmedIntersectionWithMarkings = informationWrapper.getTrimmedIntersectionWithMarkings();
        List<Place> unusedPlaces = MarkingAnalyser.getUnusedStates(procedural, trimmedIntersectionWithMarkings);

        System.out.println("Unused places: " + unusedPlaces);

        if (!unusedPlaces.isEmpty()) {
            //Branches with unused places are removed.
            net = removeBranch((Petrinet) net, unusedPlaces);
        } else {
            //Unused transitions are found and removed.
            List<PossibleWorldWrap> unusedTransitionLabels = MarkingAnalyser.getUnusedTransitionLabels(procedural, trimmedIntersectionWithMarkings);
            System.out.println("Unused transitions: " + unusedTransitionLabels);
            net = removeTransitions(net, unusedTransitionLabels);
        }


        return (Petrinet) net;

    }

    private static PetrinetGraph removeTransitions(PetrinetGraph net, List<PossibleWorldWrap> unusedTransitionLabels) {
        List<org.processmining.models.graphbased.directed.petrinet.elements.Transition> transitionsToRemove = new ArrayList<>();
        for (PossibleWorldWrap transitionLabel : unusedTransitionLabels) {
            for (org.processmining.models.graphbased.directed.petrinet.elements.Transition transition : net.getTransitions()) {
                Proposition prop = new Proposition(transition.getLabel());
                List<Proposition> props = new ArrayList<>();
                props.add(prop);
                PossibleWorldWrap pw = new PossibleWorldWrap(props);
                if (pw.equals(transitionLabel)) {
                    transitionsToRemove.add(transition);
                }
            }
        }
        transitionsToRemove.forEach(net::removeTransition);
        return net;
    }

    public static Petrinet repairProcedural(InformationWrapper informationWrapper) throws Exception {
        MyAutomaton procedural = informationWrapper.getProceduralAutomaton();
        Automaton reducedIntersection = informationWrapper.getReducedIntersection();
        MyAutomaton trimmedIntersectionWithMarkings = informationWrapper.getTrimmedIntersectionWithMarkings();
        PetrinetGraph net = informationWrapper.getNet();

        //List of semi-bad (gold) states are found.
        Map<State, List<Transition>> semiBadStates = informationWrapper.getSemiBadStates();
        //Markings are added to the semi-bad states.
        Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> semiBadStatesWithMarkings = informationWrapper.getSemiBadMarkingsFromIntersection();

        AutomatonOperationUtils.colorAutomatonStates(informationWrapper, "automaton_coloured_proc.gv");

        SemiBadFront firstSemiBadFront = new SemiBadFront(semiBadStates, semiBadStatesWithMarkings);

        Stack<SemiBadFront> fronts = new Stack<>();
        fronts.add(firstSemiBadFront);

        Petrinet repairedPetriNet = null;

        Stack<Petrinet> repairedCandidates = new Stack<>();
        repairedCandidates.push((Petrinet) net);

        while (!fronts.isEmpty()) {
            SemiBadFront semiBadFront = fronts.pop();

            Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> markingsForGroup = semiBadFront.getMarkingsFromIntersection();

            for (Map.Entry<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>>  entry : markingsForGroup.entrySet()) {
                PossibleWorldWrap transitionLabel = entry.getKey();
                Map<Transition, TransitionMarkingPair> transitionGroupMarkings = entry.getValue();

                //Difference between the semi-bad state marking and a bad state marking is taken.
                Queue<Place> problematicPlaces = AutomatonUtils.findPlacesToRemoveFromSemiBadMarkings(transitionGroupMarkings);
                System.out.println("Problematic places for group " + transitionLabel + ": " + problematicPlaces);

                while (!problematicPlaces.isEmpty()) {
                    Place problematicPlace = problematicPlaces.poll();

                    //Last marking, before the token from toRemove place is removed.
                    List<Place> markingFromWhereToRepair = AutomatonUtils.getLastPlacesBeforeTokenMoved(trimmedIntersectionWithMarkings, problematicPlace);

                    if (markingFromWhereToRepair != null) {
                        //Places from where the net could be repaired.
                        markingFromWhereToRepair.remove(problematicPlace);
                        System.out.println("Marking from where to repair: " + markingFromWhereToRepair);
                        if (!markingFromWhereToRepair.isEmpty()) {
                            for (Place troubledPlace : markingFromWhereToRepair) {
                                System.out.println("Troubled: " + troubledPlace);
                                Petrinet currentNet = repairedCandidates.peek();

                                //TODO: Check if cloning is necessary.
                                //Petri net is cloned, so that the changes can figuratively be rolled back.
                                Petrinet cloneNet = PetrinetFactory.clonePetrinet((Petrinet) currentNet);
                                Petrinet originalCloneNet = PetrinetFactory.clonePetrinet((Petrinet) net);


                                //To be safe, that cloned places correspond to real ones, a check is needed.
                                Place problematicPlaceClone = getPlaceFromCloneNet(cloneNet, problematicPlace);

                                Place troubledPlaceClone = getPlaceFromCloneNet(cloneNet, troubledPlace);
                                System.out.println("Troubled clone: " + troubledPlaceClone);

                                //TODO: The order with which we take troubled places might matter.

                                if (troubledPlaceClone != null && problematicPlaceClone != null) {
                                    Petrinet repairedCandidate = (Petrinet) Repairer.repair(cloneNet, getPlaceFromCloneNet(cloneNet, troubledPlace), problematicPlaceClone);
                                    Petrinet repairedCandidateNoFlat = (Petrinet) Repairer.repair(originalCloneNet, getPlaceFromCloneNet(originalCloneNet, troubledPlace), getPlaceFromCloneNet(originalCloneNet, problematicPlace));


                                    String explanation = "removing_" + problematicPlace.getLabel();
                                    String explanation2 = "no_flat_" + problematicPlace.getLabel();
                                    System.out.println("Repair has been finished...");
                                    if (checkIfGroupHasBeenRepaired(informationWrapper.getDeclarativeAutomaton(), transitionLabel, repairedCandidate, explanation)) {
                                        System.out.println("Checking repair results.....");
                                        repairedCandidates.push(repairedCandidate);
                                        problematicPlaces.clear();
                                        //TODO: For some markings, the execution of the program gets stuck, that should be fixed.
                                        //markingFromWhereToRepair.clear();
                                        System.out.println("Fronts might not be empty... " + fronts);
                                        System.out.println("Markings might not be empty... " + markingsForGroup.keySet());
                                    } else {
                                        Optional<InformationWrapper> finalWrapperOptional = checkIfCandidateFullyConformsToRules(informationWrapper.getDeclarativeAutomaton(), repairedCandidateNoFlat, "_sync_no_flat");
                                        if (finalWrapperOptional.isPresent() && finalWrapperOptional.get().getSemiBadStates().isEmpty()) {
                                            System.out.println("MODEL HAS BEEN REPAIRED WITHOUT FLATTENING!!!");
                                            return repairedCandidateNoFlat;
                                        }
                                    }
                                }


                            }
                        }

                    } else {
                        System.out.println("Group with transition label " + transitionLabel + " can not be repaired.");
                    }

                }
                System.out.println("REPAIRED candidates: " + repairedCandidates);


            }

            Petrinet finalCandidate = repairedCandidates.peek();
            Optional<InformationWrapper> finalWrapperOptional = checkIfCandidateFullyConformsToRules(informationWrapper.getDeclarativeAutomaton(), finalCandidate, "_after_final_removal");
            if (finalWrapperOptional.isPresent() && finalWrapperOptional.get().getSemiBadStates().isEmpty()) {
                System.out.println("Found a repaired model!!!!");
                repairedPetriNet = finalCandidate;
            }



            //If the model has not been repaired a new semi-bad state front is created and added to the stack.
            if (repairedPetriNet == null) {
                SemiBadFront nextFront = AutomatonUtils.getNextSemiBadFront(procedural, reducedIntersection, semiBadFront);
                if (!nextFront.getStates().isEmpty()) {
                    fronts.add(nextFront);
                }

            } else {
                System.out.println("MODEL HAS BEEN REPAIRED!!!");
            }
        }

        return repairedPetriNet;
    }

    private static boolean checkIfGroupHasBeenRepaired(Automaton declarative, PossibleWorldWrap transitionLabel, Petrinet repairedCandidate, String explanation) throws Exception {
        try {
            InformationWrapper repairedWrapper = new InformationWrapper(declarative, repairedCandidate);
            Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> semiBadStatesWithMarkings = repairedWrapper.getSemiBadMarkingsFromOriginal();

            return !semiBadStatesWithMarkings.containsKey(transitionLabel);
        } catch (CanNotConvertPNToAutomatonException exception) {
            return false;
        }

    }

    //TODO: Name should be changed, this method only creates the wrapper.
    public static Optional<InformationWrapper> checkIfCandidateFullyConformsToRules(Automaton declarative, Petrinet repairedCandidate, String explanation) throws Exception {

        try {
            InformationWrapper candidateWrapper = new InformationWrapper(declarative, repairedCandidate);

            System.out.println("Starting to convert repaired candidate.");
            PetrinetUtils.exportPetriNetToPNML("partially_repaired.pnml", repairedCandidate);
            //PNAutomatonConverter converter = new PNAutomatonConverter(repairedCandidate);
            //MyAutomaton repairedCandidateAutomaton = converter.convertToAutomaton();
            System.out.println("Repaired candidate has been converted into an automaton.");

            //Automaton repairedCandidateReducedIntersection = candidateWrapper.getReducedIntersection();

            //Map<State, List<Transition>> semiBadStatesWithMarkings = candidateWrapper.getSemiBadStates();
            //Map<State, List<Transition>> badStatesWithMarkings = candidateWrapper.getBadStatesWithTransitions();

            String repairAutomatonFileName = "automaton_" + explanation + ".gv";

            if (candidateWrapper.getNet() == null) {
                AutomatonOperationUtils.colorAutomatonStates(candidateWrapper, repairAutomatonFileName);
            }

            return Optional.ofNullable(candidateWrapper);
        } catch (NumberOfStatesDoesNotMatchException e) {
            return null;
        }

    }

    public static void addSyncPointsToParallelBranches(InformationWrapper informationWrapper, Map<PossibleWorldWrap, PossibleWorldWrap> repairSourceTargetPair) {
        Petrinet cloneNet = PetrinetFactory.clonePetrinet((Petrinet) informationWrapper.getNet());

        System.out.println("Repair: " + repairSourceTargetPair);
        Map<org.processmining.models.graphbased.directed.petrinet.elements.Transition, org.processmining.models.graphbased.directed.petrinet.elements.Transition> netRepairPair = new HashMap<>();
        for (Map.Entry<PossibleWorldWrap, PossibleWorldWrap> entry : repairSourceTargetPair.entrySet()) {
            PossibleWorldWrap source = entry.getKey();
            PossibleWorldWrap target = entry.getValue();
            org.processmining.models.graphbased.directed.petrinet.elements.Transition netSourceTransition = null;
            org.processmining.models.graphbased.directed.petrinet.elements.Transition netTargetTransition = null;

            for (org.processmining.models.graphbased.directed.petrinet.elements.Transition netTransition: cloneNet.getTransitions()) {
                if (netTransition.getLabel().equals(source.stream().findFirst().get().toString())) {
                    System.out.println("FOUND SOURCE: " + netTransition.getLabel());
                    netSourceTransition = netTransition;
                }
                if (netTransition.getLabel().equals(target.stream().findFirst().get().toString())) {
                    System.out.println("FOUND TARGET: " + netTransition.getLabel());
                    netTargetTransition = netTransition;
                }
            }
            if (netSourceTransition != null && netTargetTransition != null) {
                netRepairPair.put(netSourceTransition, netTargetTransition);
            }
        }

        PetrinetGraph syncedNet = Repairer.putSyncPoints(cloneNet, netRepairPair);

        try {
            Optional<InformationWrapper> syncedWrapperOptional = checkIfCandidateFullyConformsToRules(informationWrapper.getDeclarativeAutomaton(), (Petrinet) syncedNet, "after_sync");

            PetrinetUtils.exportPetriNetToPNML("test.pnml", syncedNet);
            if (syncedWrapperOptional.isPresent() && syncedWrapperOptional.get().getSemiBadStates().isEmpty()) {
                System.out.println("AND BRANCHES HAVE BEEN SUCCESSFULLY REPAIRED!");
                String repairedFileName = syncedNet.getLabel() + "_repaired_fully.pnml";
                PetrinetUtils.exportPetriNetToPNML(repairedFileName, syncedNet);
                AutomatonUtils.checkLanguage(informationWrapper.getProceduralAutomaton(), informationWrapper.getDeclarativeAutomaton(), (Petrinet) syncedNet);
            } else {
                //Let's try to put sync points one by one.
                Boolean oneGroupRepaired = false;

                for (Map.Entry<PossibleWorldWrap, PossibleWorldWrap> entry : repairSourceTargetPair.entrySet()) {
                    System.out.println(entry.getValue() + " repair group: ");
                    oneGroupRepaired = checkIfGroupHasBeenRepaired(informationWrapper.getDeclarativeAutomaton(), entry.getValue(), (Petrinet) syncedNet, "one_by_one");
                    if (oneGroupRepaired) {
                        InformationWrapper oneGroupRepairedWrapper = new InformationWrapper(informationWrapper.getDeclarativeAutomaton(), syncedNet);
                        AutomatonUtils.getSyncPoints(oneGroupRepairedWrapper);
                    }
                }

                if (!oneGroupRepaired) {
                    PetrinetUtils.exportPetriNetToPNML("test_repair_before_flattening.pnml", syncedNet);
                    System.out.println("Going into flattening area!");

                    Petrinet repairedPetriNet = ModelRepairer.repairProcedural(informationWrapper);
                    if (repairedPetriNet != null) {
                        String repairedFileName = repairedPetriNet.getLabel() + "_repaired_fully.pnml";
                        PetrinetUtils.exportPetriNetToPNML(repairedFileName, repairedPetriNet);
                        AutomatonUtils.checkLanguage(informationWrapper.getProceduralAutomaton(), informationWrapper.getDeclarativeAutomaton(), (Petrinet) syncedNet);
                    } else {
                        System.out.println("THIS MODEL CAN NOT BE REPAIRED!");
                    }
                }



            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static org.processmining.models.graphbased.directed.petrinet.elements.Transition getTransitionFromPN(Petrinet net, Transition automatonTransition) {
        PossibleWorldWrap psw = (PossibleWorldWrap) automatonTransition.label();
        String automatonTransitionLabel = psw.toString();
        Collection<org.processmining.models.graphbased.directed.petrinet.elements.Transition> pnTransitions = net.getTransitions();
        for (org.processmining.models.graphbased.directed.petrinet.elements.Transition pnTransition : pnTransitions) {
            String label = pnTransition.getLabel();
            Proposition proposition = new Proposition(label);
            List<Proposition> propositions = new ArrayList<>();
            propositions.add(proposition);
            PossibleWorldWrap pnPsw = new PossibleWorldWrap(propositions);
            if (psw.equals(pnPsw)) {
                return pnTransition;
            }
        }
        return null;
    }

    private static Place getPlaceFromCloneNet(Petrinet net, Place p) {
        for (Place place : net.getPlaces()) {
            if (place.getLabel().equalsIgnoreCase(p.getLabel())) {
                return place;
            }
        }
        return null;
    }

    private static Petrinet removeBranch(Petrinet net, List<Place> unusedPlaces) {
        for (Place place : unusedPlaces) {
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgesOutP = net.getOutEdges(place);
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgesInP = net.getInEdges(place);
            for (PetrinetEdge edge : edgesInP) {
                org.processmining.models.graphbased.directed.petrinet.elements.Transition incomingTransition = (org.processmining.models.graphbased.directed.petrinet.elements.Transition) edge.getSource();
                net.removeTransition(incomingTransition);
            }
            for (PetrinetEdge edge : edgesOutP) {
                org.processmining.models.graphbased.directed.petrinet.elements.Transition outgoingTransition = (org.processmining.models.graphbased.directed.petrinet.elements.Transition) edge.getTarget();
                net.removeTransition(outgoingTransition);
            }
            net.removePlace(place);
        }
        return net;
    }
}
