package converter;

import automaton.PossibleWorldWrap;
import converter.automaton.*;
import converter.petrinet.CanNotConvertPNToAutomatonException;
import converter.petrinet.NumberOfStatesDoesNotMatchException;
import converter.utils.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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

    private static Logger logger = LogManager.getLogger(ModelRepairer.class);

    public static Petrinet removeUnusedPlacesAndTransitions(InformationWrapper informationWrapper) throws Exception {
        PetrinetGraph net = informationWrapper.getNet();
        MyAutomaton procedural = informationWrapper.getProceduralAutomaton();
        MyAutomaton trimmedIntersectionWithMarkings = informationWrapper.getTrimmedIntersectionWithMarkings();
        List<Place> unusedPlaces = MarkingAnalyser.getUnusedStates(procedural, trimmedIntersectionWithMarkings);

        logger.info("Unused places: " + unusedPlaces);

        if (!unusedPlaces.isEmpty()) {
            //Branches with unused places are removed.
            net = removeBranch((Petrinet) net, unusedPlaces);
        }

        //The unused transitions are taken with respect to original procedural automaton, so the list is larger
        //than the alphabet of the current net.
        List<PossibleWorldWrap> unusedTransitionLabels = MarkingAnalyser.getUnusedTransitionLabels(procedural, trimmedIntersectionWithMarkings);
        logger.info("Unused transitions: " + unusedTransitionLabels);
        if (!unusedTransitionLabels.isEmpty()) {
            net = Repairer.removeTransitions(net, unusedTransitionLabels);
        }

        return (Petrinet) net;

    }

    public static Petrinet repairByFlattening(InformationWrapper informationWrapper) throws Exception {
        MyAutomaton procedural = informationWrapper.getProceduralAutomaton();
        Automaton reducedIntersection = informationWrapper.getReducedIntersection();
        MyAutomaton trimmedIntersectionWithMarkings = informationWrapper.getTrimmedIntersectionWithMarkings();
        PetrinetGraph net = informationWrapper.getNet();

        //List of semi-bad (gold) states are found.
        Map<State, List<Transition>> semiBadStates = informationWrapper.getSemiBadStates();
        //Markings are added to the semi-bad states.
        Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> semiBadStatesWithMarkings = informationWrapper.getSemiBadMarkingsFromIntersection();

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
                logger.info("Problematic places for group " + transitionLabel + ": " + problematicPlaces);

                while (!problematicPlaces.isEmpty()) {
                    Place problematicPlace = problematicPlaces.poll();

                    //Last marking, before the token from toRemove place is removed.
                    List<Place> markingFromWhereToRepair = AutomatonUtils.getLastPlacesBeforeTokenMoved(trimmedIntersectionWithMarkings, problematicPlace);
                    if (markingFromWhereToRepair != null) {
                        //Places from where the net could be repaired.
                        markingFromWhereToRepair.remove(problematicPlace);
                        if (!markingFromWhereToRepair.isEmpty()) {
                            for (Place troubledPlace : markingFromWhereToRepair) {
                                logger.info("Troubled: " + troubledPlace);
                                Petrinet currentNet = repairedCandidates.peek();

                                Petrinet currentNetClone = PetrinetFactory.clonePetrinet(currentNet);
                                Petrinet originalNetClone = PetrinetFactory.clonePetrinet((Petrinet) net);

                                Petrinet repairedCandidateFromOriginal = (Petrinet) Repairer.repair(originalNetClone, getPlaceFromCloneNet(originalNetClone, troubledPlace), getPlaceFromCloneNet(originalNetClone, problematicPlace));
                                logger.info("Checking whether candidate has been fully repaired. ");
                                InformationWrapper finalWrapperOptional =
                                        wrapCandidate(informationWrapper.getFormula(), repairedCandidateFromOriginal, "_sync_no_flat");
                                if (finalWrapperOptional != null && finalWrapperOptional.getSemiBadStates().isEmpty()) {
                                    logger.info("MODEL HAS BEEN REPAIRED WITHOUT FLATTENING!!!");
                                    return repairedCandidateFromOriginal;
                                }


                                //To be safe, that cloned places correspond to real ones, a check is needed.
                                Place problematicPlaceClone = getPlaceFromCloneNet(currentNetClone, problematicPlace);

                                Place troubledPlaceClone = getPlaceFromCloneNet(currentNetClone, troubledPlace);

                                //TODO: The order with which we take troubled places might matter.

                                if (troubledPlaceClone != null && problematicPlaceClone != null) {
                                    Petrinet repairedCandidate = (Petrinet) Repairer.repair(currentNetClone, getPlaceFromCloneNet(currentNetClone, troubledPlace), problematicPlaceClone);

                                    String explanation = "removing_" + problematicPlace.getLabel();
                                    logger.info("Repair has been finished...");
                                    if (checkIfGroupHasBeenRepaired(informationWrapper.getFormula(), transitionLabel, repairedCandidate, explanation)) {
                                        logger.info("Checking repair results.....");
                                        repairedCandidates.push(repairedCandidate);
                                        problematicPlaces.clear();
                                        logger.info("Fronts might not be empty... " + fronts);
                                        logger.info("Markings might not be empty... " + markingsForGroup.keySet());
                                    }
                                }
                            }
                        }

                    } else {
                        logger.info("Group with transition label " + transitionLabel + " can not be repaired.");
                    }

                }
                logger.info("REPAIRED candidates: " + repairedCandidates);


            }

            Petrinet finalCandidate = repairedCandidates.peek();
            InformationWrapper finalWrapperOptional = wrapCandidate(informationWrapper.getFormula(), finalCandidate, "_after_final_removal");
            if (finalWrapperOptional != null && finalWrapperOptional.getSemiBadStates().isEmpty()) {
                logger.info("Found a repaired model!!!!");
                repairedPetriNet = finalCandidate;
            }



            //If the model has not been repaired a new semi-bad state front is created and added to the stack.
            if (repairedPetriNet == null) {
                SemiBadFront nextFront = AutomatonUtils.getNextSemiBadFront(procedural, reducedIntersection, semiBadFront);
                if (!nextFront.getStates().isEmpty()) {
                    fronts.add(nextFront);
                }

            } else {
                logger.info("MODEL HAS BEEN REPAIRED!!!");
            }
        }

        return repairedPetriNet;
    }

    private static boolean checkIfGroupHasBeenRepaired(String formula, PossibleWorldWrap transitionLabel, Petrinet repairedCandidate, String explanation) throws Exception {
        try {
            logger.info("Checking the group " + transitionLabel);
            InformationWrapper repairedWrapper = new InformationWrapper(formula, repairedCandidate);
            Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> semiBadStatesWithMarkings = repairedWrapper.getSemiBadMarkingsFromOriginal();

            return !semiBadStatesWithMarkings.containsKey(transitionLabel);
        } catch (CanNotConvertPNToAutomatonException exception) {
            return false;
        }

    }

    public static InformationWrapper wrapCandidate(String formula, Petrinet repairedCandidate, String explanation) throws Exception {

        try {
            InformationWrapper candidateWrapper = new InformationWrapper(formula, repairedCandidate);

            String repairAutomatonFileName = "automatons/automaton_" + explanation + ".gv";

            if (candidateWrapper.getNet() == null) {
                AutomatonOperationUtils.colorAutomatonStates(candidateWrapper, repairAutomatonFileName);
            }

            return candidateWrapper;
        } catch (NumberOfStatesDoesNotMatchException | CanNotConvertPNToAutomatonException e) {
            return null;
        }

    }

    public static Petrinet getSyncPoints(InformationWrapper informationWrapper) {
        PetrinetGraph net = informationWrapper.getNet();
        MyAutomaton procedural = informationWrapper.getProceduralAutomaton();
        Automaton reducedIntersection = informationWrapper.getReducedIntersection();
        Map<PossibleWorldWrap, PossibleWorldWrap> repairSourceTargetPair = new HashMap<>();
        List<State> badStates = informationWrapper.getBadStates();
        Map<State, List<Transition>> semiBadStates = informationWrapper.getSemiBadStates();
        SemiBadStateAnalyser semiBadStateAnalyser = new SemiBadStateAnalyser(net, semiBadStates, reducedIntersection);
        Map<PossibleWorldWrap, State> lastSemiBadStateMap = semiBadStateAnalyser.getLastSemiBadState();
        for (Map.Entry<PossibleWorldWrap, State> entry : lastSemiBadStateMap.entrySet()) {
            PossibleWorldWrap transitionLabel = entry.getKey();
            State automatonState = entry.getValue();
            Set<Transition> outGoingTransitions = reducedIntersection.delta(automatonState);
            for (Transition outGoingTransition : outGoingTransitions) {
                State target = outGoingTransition.end();
                if (!badStates.contains(target) && !semiBadStates.containsKey(target)) {
                    if (!AutomatonUtils.checkXorness(net, procedural, (PossibleWorldWrap) outGoingTransition.label(), transitionLabel)) {
                        repairSourceTargetPair.put((PossibleWorldWrap) outGoingTransition.label(), transitionLabel);
                    }
                }
            }
        }

        return addSyncPointsToParallelBranches(informationWrapper, repairSourceTargetPair);
    }

    public static Petrinet addSyncPointsToParallelBranches(InformationWrapper informationWrapper, Map<PossibleWorldWrap, PossibleWorldWrap> repairSourceTargetPair) {
        Petrinet cloneNet = PetrinetFactory.clonePetrinet((Petrinet) informationWrapper.getNet());

        logger.info("Repair: " + repairSourceTargetPair);
        Map<org.processmining.models.graphbased.directed.petrinet.elements.Transition, org.processmining.models.graphbased.directed.petrinet.elements.Transition> netRepairPair = new HashMap<>();
        for (Map.Entry<PossibleWorldWrap, PossibleWorldWrap> entry : repairSourceTargetPair.entrySet()) {
            PossibleWorldWrap source = entry.getKey();
            PossibleWorldWrap target = entry.getValue();
            org.processmining.models.graphbased.directed.petrinet.elements.Transition netSourceTransition = null;
            org.processmining.models.graphbased.directed.petrinet.elements.Transition netTargetTransition = null;

            for (org.processmining.models.graphbased.directed.petrinet.elements.Transition netTransition: cloneNet.getTransitions()) {
                PossibleWorldWrap pWWTransition = Repairer.createPossibleWorldWrap(netTransition);
                if (source.equals(pWWTransition)) {
                    logger.info("FOUND SOURCE: " + netTransition.getLabel());
                    netSourceTransition = netTransition;
                }
                if (target.equals(pWWTransition)) {
                    logger.info("FOUND TARGET: " + netTransition.getLabel());
                    netTargetTransition = netTransition;
                }
            }
            if (netSourceTransition != null && netTargetTransition != null) {
                netRepairPair.put(netSourceTransition, netTargetTransition);
            }
        }

        PetrinetGraph syncedNet = Repairer.putSyncPoints(cloneNet, netRepairPair);

        try {
            InformationWrapper syncedWrapperOptional = wrapCandidate(informationWrapper.getFormula(), (Petrinet) syncedNet, "after_sync");

            if (syncedWrapperOptional != null && syncedWrapperOptional.getSemiBadStates().isEmpty()) {
                logger.info("AND BRANCHES HAVE BEEN SUCCESSFULLY REPAIRED!");
                String repairedFileName = "test_nets/"+syncedNet.getLabel() + "_repaired_fully_1.pnml";
                PetrinetUtils.exportPetriNetToPNML(repairedFileName, syncedNet);
                return (Petrinet) syncedNet;
            } else {
                //Let's try to put sync points one by one.
                Boolean oneGroupRepaired = false;

                for (Map.Entry<PossibleWorldWrap, PossibleWorldWrap> entry : repairSourceTargetPair.entrySet()) {
                    logger.info(entry.getValue() + " repair group: ");
                    oneGroupRepaired = checkIfGroupHasBeenRepaired(informationWrapper.getFormula(), entry.getValue(), (Petrinet) syncedNet, "one_by_one");
                    if (oneGroupRepaired) {
                        InformationWrapper oneGroupRepairedWrapper = new InformationWrapper(informationWrapper.getFormula(), syncedNet);
                        syncedNet = getSyncPoints(oneGroupRepairedWrapper);
                    }
                }

                if (!oneGroupRepaired) {
                    logger.info("Going into flattening area!");

                    Petrinet repairedPetriNet = ModelRepairer.repairByFlattening(informationWrapper);
                    if (repairedPetriNet != null) {
                        String repairedFileName = "test_nets/"+repairedPetriNet.getLabel() + "_repaired_fully_2.pnml";
                        PetrinetUtils.exportPetriNetToPNML(repairedFileName, repairedPetriNet);

                        return repairedPetriNet;
                    } else {
                        logger.info("THIS MODEL CAN NOT BE REPAIRED!");
                        return null;
                    }
                }



            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        return (Petrinet) syncedNet;
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
