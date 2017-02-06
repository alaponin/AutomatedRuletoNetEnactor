package converter;

import converter.automaton.*;
import converter.utils.*;
import main.LTLfAutomatonResultWrapper;
import net.sf.tweety.logics.pl.syntax.Proposition;
import net.sf.tweety.logics.pl.syntax.PropositionalSignature;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.*;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import rationals.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by arnelaponin on 02/09/16.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        String petriNetFile = "usecase8.pnml";
        //(G (E -> (F f))) && (G(E -> F J)) (G (C -> (X (!D))))
        //String ltlFormula = "G (t1 -> (F B))";
        String ltlFormula = "(G ((Y) -> (F (I)))) && (G ((g) -> (F (D)))) && (F Y) && (F g)";
        // (G ((K) -> (F (N)))) && (G ((Y) -> (F (z)))) && (G ((S) -> (F (g)))) usecase 13
        // (G ((B) -> (X (C)))) && (G ((C) -> (X (g)))) && (G ((E) -> (X (f))))
        // (G (A -> (F B)))
        // (G (A -> (X B))) chain response
        // G (A -> (X ((!A) U B))) alt response
        // (((F A) || (F B)) && (!((F A) && (F B)))) exclusive choice
        // ((!B) U A) || (G (!B)) precedence
        // (G (A -> (F B))) && (((!B) U A) || (G (!B))) succession
        // (!((F A) && (F B))) not-coexistence
        // ((F I) -> (F g)) && ((F g) -> (F I)) coexistence
        // (G (C -> (F D))) && (((!D) U C) || (G (!D))) && (F C) succession and existence
        // (G (X D -> C)) chain precedence

        PropositionalSignature signature = new PropositionalSignature();

        PetrinetGraph net = Extractor.extractPetriNet(petriNetFile);
        List<Proposition> netPropositions = PetrinetUtils.getAllTransitionLabels(net);
        signature.addAll(netPropositions);

        //Automaton of the declarative model.
        LTLfAutomatonResultWrapper ltlfARW = AutomatonOperationUtils.createDefaultLtlAutomaton(signature, ltlFormula);
        Automaton declareAutomaton = ltlfARW.getAutomaton();

        InformationWrapper informationWrapper = new InformationWrapper(declareAutomaton, net);

        createTsFile(informationWrapper);
        InformationWrapper loopFreeWrapper = removeAllLoopsFromPN(ltlFormula, signature, ltlfARW, informationWrapper);


        PetrinetUtils.exportPetriNetToPNML("pn_no_loops.pnml", loopFreeWrapper.getNet());

        AutomatonOperationUtils.colorAutomatonStates(loopFreeWrapper, "automaton_coloured.gv");

        repairXorSyncFlattening(loopFreeWrapper);



    }

    private static InformationWrapper removeAllLoopsFromPN(String ltlFormula, PropositionalSignature signature, LTLfAutomatonResultWrapper ltlfARW, InformationWrapper informationWrapper) throws Exception {
        PetrinetGraph net = informationWrapper.getNet();
        Automaton declareAutomaton = informationWrapper.getDeclarativeAutomaton();
        //TODO: This could be done after unused transitions check, see rule6 log.
        if (!informationWrapper.getSemiBadStates().isEmpty()) {

            System.out.println("LTL formula signature: " + ltlfARW.getLtlfFormula().getSignature());

            String updatedFormula = checkIfActivitiesAreInSeparateXorBranches(ltlFormula, signature, ltlfARW, declareAutomaton, informationWrapper);

            if (updatedFormula != null) {
                System.out.println("The Declare formula has been updated: " + updatedFormula);

                ltlfARW = AutomatonOperationUtils.createDefaultLtlAutomaton(signature, updatedFormula);
                declareAutomaton = ltlfARW.getAutomaton();

                informationWrapper = new InformationWrapper(declareAutomaton, net);
            }
        }

        return removeLoops(ltlFormula, net, ltlfARW, informationWrapper);
    }

    private static void createTsFile(InformationWrapper informationWrapper) {
        String label = informationWrapper.getNet().getLabel();
        String tsFileName = label + ".sg";
        TSFileConverter.TS2File(AutomatonOperationUtils.getTrimmed(informationWrapper.getReducedIntersection()), tsFileName);
    }

    private static InformationWrapper removeLoops(String ltlFormula, PetrinetGraph net, LTLfAutomatonResultWrapper ltlfARW, InformationWrapper informationWrapper) throws Exception {
        PropositionalSignature ltlSignature = ltlfARW.getLtlfFormula().getSignature();
        List<Integer> tarjanCyclesOfLtl = new ArrayList<>();
        TarjanAlgorithmPN sscPN = new TarjanAlgorithmPN(net);
        Map<Integer, List<Transition>> sscPNGroups = sscPN.getGroups();
        Boolean pnHasCycles = false;
        for (Proposition p : ltlSignature) {
            tarjanCyclesOfLtl.add(sscPN.whichCycleIsTransitionPartOf(p.getName()));
        }
        for (Integer groupNr : tarjanCyclesOfLtl) {
            if (sscPNGroups.containsKey(groupNr) && sscPNGroups.get(groupNr).size() > 1) {
                System.out.println("At least one transition is in the loop");
                pnHasCycles = true;
            }
        }

        Stack<InformationWrapper> loopRemovalNets = new Stack<>();
        loopRemovalNets.push(informationWrapper);

        InformationWrapper loopFreeWrapper;

        if (pnHasCycles) {
            while (loopRemovalNets.peek().hasCycles()) {
                InformationWrapper currentWrapper = loopRemovalNets.peek();
                InformationWrapper updatedWrapper = createWrapperWithoutLoops(ltlFormula, (Petrinet) currentWrapper.getNet(), currentWrapper);
                loopRemovalNets.push(updatedWrapper);
            }
        }
        loopFreeWrapper = loopRemovalNets.pop();
        return loopFreeWrapper;
    }

    private static InformationWrapper createWrapperWithoutLoops(String ltlFormula, Petrinet net, InformationWrapper informationWrapper) throws Exception {
        Petrinet updatedNet = removeLoops(net, informationWrapper);
        List<Proposition> netPropositions2 = PetrinetUtils.getAllTransitionLabels(updatedNet);
        PropositionalSignature signature2 = new PropositionalSignature();
        signature2.addAll(netPropositions2);
        LTLfAutomatonResultWrapper ltlfARW2 = AutomatonOperationUtils.createDefaultLtlAutomaton(signature2, ltlFormula);
        Automaton declareAutomaton2 = ltlfARW2.getAutomaton();
        return new InformationWrapper(declareAutomaton2, updatedNet);
    }

    private static Petrinet removeLoops(Petrinet net, InformationWrapper informationWrapper) {
        LoopSearch loopSearch = new LoopSearch(informationWrapper.getTrimmedIntersectionWithMarkings());
        System.out.println("Starting to remove loops.....");
        System.out.println(loopSearch.getLoop());

        StatePair loop = loopSearch.getLoop();

        List<Place> p1List = informationWrapper.getTrimmedIntersectionWithMarkings().getMarkingMap().get(loop.getS1());
        List<Place> p2List = informationWrapper.getTrimmedIntersectionWithMarkings().getMarkingMap().get(loop.getS2());
        List<Place> newP1List = new ArrayList<>(p1List);
        List<Place> newP2List = new ArrayList<>(p2List);
        newP1List.removeAll(p2List);
        newP2List.removeAll(p1List);
        Petrinet updatedNet = null;
        for (Place p1 : newP1List) {
            for (Place p2 : newP2List) {
                updatedNet = removeTransitionBetween2Places(net, p1, p2);
            }
        }
        return updatedNet;
    }

    //p2 should be with the back edge.
    private static Petrinet removeTransitionBetween2Places(Petrinet net, Place p1, Place p2) {
        //Petrinet net = PetrinetFactory.clonePetrinet(petrinet);

        System.out.println("p1: " + p1);
        System.out.println("p2: " + p2);

        Place initialPlace = PetrinetUtils.getStartPlace(net);
        Place finalPlace = PetrinetUtils.getFinalPlace(net);

        if (net.getPlaces().contains(p1) && net.getPlaces().contains(p2)) {
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = net.getOutEdges(p1);
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = net.getInEdges(p2);
            List<org.processmining.models.graphbased.directed.petrinet.elements.Transition> p2Transitions = new ArrayList<>();
            List<org.processmining.models.graphbased.directed.petrinet.elements.Transition> p1Transitions = new ArrayList<>();
            for (PetrinetEdge edge : outEdges) {
                p2Transitions.add((Transition) edge.getTarget());
            }

            for (PetrinetEdge edge : inEdges) {
                p1Transitions.add((Transition) edge.getSource());
            }
            List<org.processmining.models.graphbased.directed.petrinet.elements.Transition> transitions = new ArrayList<>(p2Transitions);
            transitions.retainAll(p1Transitions);
            if (!transitions.isEmpty()) {
                for (Transition t : transitions) {
                    System.out.println("Removing transition: " + t);
                    net.removeTransition(t);
                }

            }
        }
        List<Place> placesToRemove;
        List<Transition> transitionsToRemove;
        do {
            placesToRemove = new ArrayList<>();
            transitionsToRemove = new ArrayList<>();
            for (Place p : net.getPlaces()) {
                if (!p.equals(initialPlace) && !p.equals(finalPlace)) {
                    Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdgesFromP = net.getOutEdges(p);
                    Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdgesFromP = net.getInEdges(p);
                    if (outEdgesFromP.size() == 0 || inEdgesFromP.size() == 0) {
                        System.out.println("Additionally removing place: " + p);
                        placesToRemove.add(p);
                    }
                }
            }
            for (Place p : placesToRemove) {
                net.removePlace(p);
            }

            for (Transition t : net.getTransitions()) {
                Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdgesFromT = net.getOutEdges(t);
                Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdgesFromT = net.getInEdges(t);
                if (outEdgesFromT.size() == 0 || inEdgesFromT.size() == 0) {
                    System.out.println("Additionally removing transition: " + t);
                    transitionsToRemove.add(t);
                }
            }
            for (Transition t : transitionsToRemove) {
                net.removeTransition(t);
            }

        } while (!placesToRemove.isEmpty() || !transitionsToRemove.isEmpty());

        return net;
    }

    private static String checkIfActivitiesAreInSeparateXorBranches(String ltlFormula, PropositionalSignature signature, LTLfAutomatonResultWrapper ltlfARW, Automaton declareAutomaton, InformationWrapper informationWrapper) {
        Proposition left;
        Proposition right;
        String newFormula = ltlFormula;
        String regex = "([(]*G[\\ ,(]*[a-zA-Z0-9_]+[)]+[\\ ]*->[\\ ]*[(]*F[\\ ,(]*[a-zA-Z0-9_]+[)]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(ltlFormula);
        List<String> responseFormulas = new ArrayList<>();
        while (matcher.find()) {
            responseFormulas.add(matcher.group());
        }
        System.out.println(responseFormulas);
        for (String responseFormula : responseFormulas) {
            LTLfAutomatonResultWrapper responseFormulaARW = AutomatonOperationUtils.createDefaultLtlAutomaton(signature, responseFormula);
            PropositionalSignature formulaSignature = responseFormulaARW.getLtlfFormula().getSignature();
            Iterator<Proposition> propositionIterator = formulaSignature.iterator();
            if (formulaSignature.size() == 2) {
                left = propositionIterator.next();
                right = propositionIterator.next();

                newFormula = makeActivityObligatory(newFormula, signature, informationWrapper, left, right);
            }
        }

        System.out.println("Updated formula: " + newFormula);

        return newFormula;
    }

    private static String makeActivityObligatory(String ltlFormula, PropositionalSignature signature, InformationWrapper informationWrapper, Proposition left, Proposition right) {
        Automaton declareAutomaton = informationWrapper.getDeclarativeAutomaton();

        String newFormula = null;
        String precedenceFormula1 = "(!(" + right + ") W " + left + ") && (F " + right + ")";
        String precedenceFormula2 = "(!(" + left + ") W " + right + ") && (F " + left + ")";

        LTLfAutomatonResultWrapper precedenceWrapper1 = AutomatonOperationUtils.createDefaultLtlAutomaton(signature, precedenceFormula1);
        LTLfAutomatonResultWrapper precedenceWrapper2 = AutomatonOperationUtils.createDefaultLtlAutomaton(signature, precedenceFormula2);

        Automaton precedenceFormula1Automaton = precedenceWrapper1.getAutomaton();
        Automaton precedenceFormula2Automaton = precedenceWrapper2.getAutomaton();

        utils.AutomatonUtils.printAutomaton(declareAutomaton, "automaton_declarative.gv");

        //TODO: Check if both automatons are actually necessary.
        Automaton precedenceIA1 = AutomatonOperationUtils.getIntersection(AutomatonOperationUtils.getTrimmed(informationWrapper.getReducedIntersection()), precedenceFormula1Automaton);
        Automaton precedenceIA2 = AutomatonOperationUtils.getIntersection(AutomatonOperationUtils.getTrimmed(informationWrapper.getReducedIntersection()), precedenceFormula2Automaton);

        utils.AutomatonUtils.printAutomaton(precedenceIA1, "automaton_IA_1.gv");
        utils.AutomatonUtils.printAutomaton(precedenceIA2, "automaton_IA_2.gv");

        String xorCheck = null;
        if (!precedenceIA1.terminals().isEmpty()) {
            System.out.println("THis occurs first: " + left);
            xorCheck = "(!(F " + left + ")) && (!(F " + right + "))";
        } else if (!precedenceIA2.terminals().isEmpty()) {
            System.out.println("THIS occurs first: " + right);
            xorCheck = "(!(F " + right + ")) && (!(F " + left + "))";
        }

        if (xorCheck != null) {
            LTLfAutomatonResultWrapper xorWrapper = AutomatonOperationUtils.createDefaultLtlAutomaton(signature, xorCheck);
            Automaton xorIA = AutomatonOperationUtils.getIntersection(informationWrapper.getReducedIntersection(), xorWrapper.getAutomaton());
            if (!xorIA.terminals().isEmpty()) {
                System.out.println();
                System.out.println("WE SHOULD DYNAMICALLY ADD STUFF");
                newFormula = ltlFormula + " && (F " + left + ")";
                LTLfAutomatonResultWrapper newFormulaWrapper = AutomatonOperationUtils.createDefaultLtlAutomaton(signature, newFormula);
                Automaton intersection = AutomatonOperationUtils.getIntersection(informationWrapper.getProceduralAutomaton(), newFormulaWrapper.getAutomaton());
                if (intersection.terminals().isEmpty()) {
                    newFormula = ltlFormula;
                }

            }
        }
        return newFormula;
    }

    private static void repairXorSyncFlattening(InformationWrapper informationWrapper) throws Exception {
        informationWrapper.colourAutomaton();
        Petrinet netWithRemovedBranches = ModelRepairer.repairXorBranch(informationWrapper);

        Optional<InformationWrapper> updatedWrapperOptional = ModelRepairer.checkIfCandidateFullyConformsToRules(
                informationWrapper.getDeclarativeAutomaton(), netWithRemovedBranches, "after_removing_unused_xor");

        if (updatedWrapperOptional.isPresent() && !updatedWrapperOptional.get().getSemiBadStates().isEmpty()) {

            AutomatonOperationUtils.colorAutomatonStates(informationWrapper, "automaton_coloured_after_xor.gv");

            AutomatonUtils.getSyncPoints(informationWrapper);
        } else {
            String repairedFileName = netWithRemovedBranches.getLabel() + "_repaired_fully.pnml";
            PetrinetUtils.exportPetriNetToPNML(repairedFileName, netWithRemovedBranches);
            AutomatonUtils.checkLanguage(informationWrapper.getProceduralAutomaton(), informationWrapper.getDeclarativeAutomaton(), netWithRemovedBranches);
        }
    }




}
