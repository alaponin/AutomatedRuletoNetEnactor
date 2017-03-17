package converter;

import converter.automaton.InformationWrapper;
import converter.automaton.StatePair;
import converter.utils.*;
import main.LTLfAutomatonResultWrapper;
import net.sf.tweety.logics.pl.syntax.Proposition;
import net.sf.tweety.logics.pl.syntax.PropositionalSignature;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import rationals.Automaton;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by arnelaponin on 08/02/2017.
 */
public class ProceduralRepairer {

    private static Logger logger = LogManager.getLogger(ProceduralRepairer.class);

    /**
     * Method returns a repaired Petri Net, if the repair is possible. If the repair is not possible
     * then the method returns the original net.
     * @param net Petrinet model to be repaired.
     * @param ltlFormula LTL formula to be accepted by the tool created by Riccardo De Masellis.
     * @return Repaired Petrinet.
     * @throws Exception
     */
    public static Petrinet repair(Petrinet net, String ltlFormula) throws Exception {
        logger.info("Starting repair on: " + net.getLabel() + " with rule: " + ltlFormula);
        net = PetrinetUtils.setLabellessTransitionsInvisible(net);
        InformationWrapper informationWrapper = new InformationWrapper(ltlFormula, net);

        createTsFile(informationWrapper);
        InformationWrapper loopFreeWrapper = removeAllLoopsFromPN(informationWrapper);
        if (loopFreeWrapper.getSemiBadStates().isEmpty()) {
            return (Petrinet) loopFreeWrapper.getNet();
        }

        AutomatonOperationUtils.colorAutomatonStates(loopFreeWrapper, "automatons/automaton_coloured.gv");

        Petrinet repairedNet = repairXorSyncFlattening(loopFreeWrapper);
        String repairedFileName = "final_nets/"+ repairedNet.getLabel() + "_final_" + repairedNet.hashCode() + ".pnml";
        PetrinetUtils.exportPetriNetToPNML(repairedFileName, repairedNet);
        logger.info("Rule: " + ltlFormula + " has been repaired.");
        logger.info("Net " + repairedNet.hashCode() + "has been produced.");
        repairedNet = PetrinetUtils.setLabellessTransitionsInvisible(repairedNet);
        return repairedNet;
    }

    public static Object[] repairWithMarking(Petrinet net, String ltlFormula) throws Exception {
        Petrinet repairedNet = repair(net, ltlFormula);
        Set<Place> places = new HashSet<>();
        places.add(PetrinetUtils.getStartPlace(repairedNet));
        Marking marking = new Marking(places);

        return new Object[] {repairedNet, marking};
    }

    private static InformationWrapper removeAllLoopsFromPN(InformationWrapper informationWrapper) throws Exception {
        PetrinetGraph net = informationWrapper.getNet();
        Automaton declareAutomaton = informationWrapper.getDeclarativeAutomaton();
        String formula = informationWrapper.getFormula();
        String updatedFormula = formula;
        if (!informationWrapper.getSemiBadStates().isEmpty()) {

            logger.info("LTL formula signature: " + informationWrapper.getLtlfARW().getLtlfFormula().getSignature());

            updatedFormula = checkIfActivitiesAreInSeparateXorBranches(informationWrapper.getFormula(), informationWrapper.getSignature(), informationWrapper.getLtlfARW(), declareAutomaton, informationWrapper);

            if (updatedFormula != null) {
                logger.info("The Declare formula has been updated: " + updatedFormula);

                informationWrapper = new InformationWrapper(updatedFormula, net);
            }
        }

        return removeLoops(updatedFormula, net, informationWrapper.getLtlfARW(), informationWrapper);
    }

    private static void createTsFile(InformationWrapper informationWrapper) {
        String label = informationWrapper.getNet().getLabel();
        String tsFileName = "sg_files/"+label + "_" + informationWrapper.getNet().hashCode() + ".sg";
        logger.info("Writing sg file into: " + tsFileName);

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
                logger.info("At least one transition is in the loop");
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

        return new InformationWrapper(ltlFormula, updatedNet);
    }

    private static Petrinet removeLoops(Petrinet net, InformationWrapper informationWrapper) {
        LoopSearch loopSearch = new LoopSearch(informationWrapper.getTrimmedIntersectionWithMarkings());
        logger.info("Starting to remove loops.....");
        logger.info("Is there a loop: "+loopSearch.getLoop());

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

    private static Petrinet removeTransitionBetween2Places(Petrinet net, Place p1, Place p2) {
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
                    logger.info("Removing transition: " + t);
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
                        logger.info("Additionally removing place: " + p);
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
                    logger.info("Additionally removing transition: " + t);
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
        logger.info("Checking if activities are in separate XOR branches...");
        Proposition left;
        Proposition right;
        String newFormula = ltlFormula;
        String regex = "[(]*G[\\ ]*[(]*[a-zA-Z0-9_]*[)]*[\\ ]*->[\\ ]*[(]*F[\\ ]*[(]*[a-zA-Z0-9_]+[)]+";
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

        logger.info("Updated formula: " + newFormula);

        return newFormula;
    }

    private static String makeActivityObligatory(String ltlFormula, PropositionalSignature signature, InformationWrapper informationWrapper, Proposition left, Proposition right) {
        String newFormula = null;

        String xorCheck = "(!(F " + left + ")) && (!(F " + right + "))";


        LTLfAutomatonResultWrapper xorWrapper = AutomatonOperationUtils.createDefaultLtlAutomaton(signature, xorCheck);
        Automaton xorIA = AutomatonOperationUtils.getIntersection(informationWrapper.getReducedIntersection(), xorWrapper.getAutomaton());
        logger.info("xorIA.terminals(): " + xorIA.terminals());
        if (!xorIA.terminals().isEmpty()) {
            logger.info("WE SHOULD DYNAMICALLY ADD STUFF");
            newFormula = ltlFormula + " && (F " + left + ")";
            LTLfAutomatonResultWrapper newFormulaWrapper = AutomatonOperationUtils.createDefaultLtlAutomaton(signature, newFormula);
            Automaton intersection = AutomatonOperationUtils.getIntersection(informationWrapper.getProceduralAutomaton(), newFormulaWrapper.getAutomaton());
            if (intersection.terminals().isEmpty()) {
                newFormula = ltlFormula;
            }

        }

        return newFormula;
    }

    private static Petrinet repairXorSyncFlattening(InformationWrapper informationWrapper) throws Exception {
        informationWrapper.colourAutomaton();
        Petrinet netWithRemovedBranches = ModelRepairer.repairXorBranch(informationWrapper);

        InformationWrapper updatedWrapperOptional = ModelRepairer.wrapCandidate(
                informationWrapper.getFormula(), netWithRemovedBranches, "after_removing_unused_xor");

        if (updatedWrapperOptional != null && !updatedWrapperOptional.getSemiBadStates().isEmpty()) {

            AutomatonOperationUtils.colorAutomatonStates(informationWrapper, "automatons/automaton_coloured_after_xor.gv");

            return AutomatonUtils.getSyncPoints(updatedWrapperOptional);
        } else {
            String repairedFileName = "test_nets/"+netWithRemovedBranches.getLabel() + "_repaired_fully_3.pnml";
            PetrinetUtils.exportPetriNetToPNML(repairedFileName, netWithRemovedBranches);

            return netWithRemovedBranches;
        }
    }
}
