package converter.utils;

import automaton.PossibleWorldWrap;
import converter.automaton.*;
import converter.epsilon.MyEpsilonTransitionRemover;
import converter.petrinet.NumberOfStatesDoesNotMatchException;
import main.LTLfAutomatonResultWrapper;
import net.sf.tweety.logics.pl.syntax.Proposition;
import net.sf.tweety.logics.pl.syntax.PropositionalSignature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;
import rationals.transformations.SinkComplete;

import java.util.*;

/**
 * Created by arnelaponin on 06/10/2016.
 */
public class AutomatonUtils {

    private static Logger logger = LogManager.getLogger(AutomatonUtils.class);

    public static Stack<StatePair> getInitialStatePairInStack(Automaton a1, Automaton a2) throws NumberOfStatesDoesNotMatchException {
        Stack<StatePair> stack = new Stack<>();
        Set<State> a1InitialStates = a1.initials();
        Set<State> a2InitialStates = a2.initials();
        if (a1InitialStates.size() == a2InitialStates.size()) {
            Iterator<State> it1 = a1InitialStates.iterator();
            Iterator<State> it2 = a2InitialStates.iterator();
            while (it1.hasNext() && it2.hasNext()) {
                StatePair statePair = new StatePair(it1.next(), it2.next());
                stack.push(statePair);
            }
        } else {
            logger.error("The number of states does not match! The intersection might be empty!");
            throw new NumberOfStatesDoesNotMatchException("The number of states does not match! The intersection might be empty!");
        }
        return stack;
    }

    public static Queue<StatePair> getInitialStatePairInQueue(Automaton a1, Automaton a2) {
        Queue<StatePair> queue = new LinkedList<>();
        Set<State> a1InitialStates = a1.initials();
        Set<State> a2InitialStates = a2.initials();
        if (a1InitialStates.size() == a2InitialStates.size()) {
            Iterator<State> it1 = a1InitialStates.iterator();
            Iterator<State> it2 = a2InitialStates.iterator();
            while (it1.hasNext() && it2.hasNext()) {
                StatePair statePair = new StatePair(it1.next(), it2.next());
                queue.add(statePair);
            }
        } else {
            try {
                logger.error("The number of initial states is not equal!");
                throw new Exception("The number of initial states is not equal!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return queue;
    }

    private static Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> getMarkingsBasedOnStates(MyAutomaton original, Automaton intersection, Map<State, List<Transition>> originStates, AutomatonSource source) {
        Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> sortedMarkings = new HashMap<>();
        Queue<StatePair> toBeVisited = getInitialStatePairInQueue(original, intersection);
        Map<State, List<Place>> originalMarkings = original.getMarkingMap();
        while (!toBeVisited.isEmpty()) {
            StatePair statePair = toBeVisited.poll();
            Set<Transition> originalOutgoingTransitions = original.delta(statePair.getS1());
            Set<Transition> intersectionOutgoingTransitions = intersection.delta(statePair.getS2());

            for (Transition intersectionTransition : intersectionOutgoingTransitions) {
                for (Transition originalTransition : originalOutgoingTransitions) {
                    if (intersectionTransition.label().equals(originalTransition.label())) {
                        toBeVisited.add(new StatePair(originalTransition.end(), intersectionTransition.end()));
                        if (originStates.containsKey(intersectionTransition.start())) {
                            List<Transition> troubledTransitions = originStates.get(intersectionTransition.start());
                            for (Transition t : troubledTransitions) {
                                if (t.label().equals(intersectionTransition.label())) {
                                    MarkingStateFactory.MarkingState start = (MarkingStateFactory.MarkingState) originalTransition.start();
                                    MarkingStateFactory.MarkingState end = (MarkingStateFactory.MarkingState) originalTransition.end();

                                    TransitionMarkingPair markingPair = new TransitionMarkingPair(originalMarkings.get(start), originalMarkings.get(end));

                                    Transition transition = null;

                                    switch(source) {
                                        case fromOriginal:
                                            transition = originalTransition;
                                            break;
                                        case fromIntersection:
                                            transition = intersectionTransition;
                                            break;
                                    }

                                    if (sortedMarkings.containsKey(transition.label())) {
                                        Map<Transition, TransitionMarkingPair> transitionMarkingPairMap = sortedMarkings.get(transition.label());
                                        transitionMarkingPairMap.put(transition, markingPair);
                                        sortedMarkings.put((PossibleWorldWrap) transition.label(), transitionMarkingPairMap);

                                    } else {
                                        Map<Transition, TransitionMarkingPair> transitionMarkingPairMap = new HashMap<>();
                                        transitionMarkingPairMap.put(transition, markingPair);
                                        sortedMarkings.put((PossibleWorldWrap) transition.label(), transitionMarkingPairMap);
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }

        return sortedMarkings;
    }

    public static Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> getSemiBadMarkingsFromOriginal(MyAutomaton original, Automaton intersection, Map<State, List<Transition>> troubledStates) {
        return getMarkingsBasedOnStates(original, intersection, troubledStates, AutomatonSource.fromOriginal);
    }

    public static Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> getSemiBadMarkingsFromIntersection(MyAutomaton original, Automaton intersection, Map<State, List<Transition>> troubledStates) {
        return getMarkingsBasedOnStates(original, intersection, troubledStates, AutomatonSource.fromIntersection);
    }

    public static SemiBadFront getNextSemiBadFront(MyAutomaton original, Automaton intersection, SemiBadFront currentFront) {
        Map<State, List<Transition>> previousFrontWithTransitions = currentFront.getStates();
        List<State> previousFrontStates = new ArrayList<>(previousFrontWithTransitions.keySet());
        Queue<State> toBeVisited = new LinkedList<>();
        List<State> visited = new ArrayList<>();
        toBeVisited.addAll(intersection.initials());
        List<State> nextFrontStates = new ArrayList<>();
        Map<State, List<Transition>> nextFrontStatesMap = new HashMap<>();
        while (!toBeVisited.isEmpty()) {
            State currentState = toBeVisited.poll();
            visited.add(currentState);
            Set<Transition> outGoingTransitions = intersection.delta(currentState);
            for (Transition currentTransition : outGoingTransitions) {
                State targetState = currentTransition.end();
                if (previousFrontStates.contains(targetState)) {
                    nextFrontStates.add(currentTransition.start());
                    insertTransitionToMapList(nextFrontStatesMap, currentTransition.start(), currentTransition);
                } else if (!toBeVisited.contains(targetState) && !visited.contains(targetState)) {
                    toBeVisited.add(targetState);
                }
            }

        }
        Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> markingsFromIntersection = getSemiBadMarkingsFromIntersection(original, intersection, nextFrontStatesMap);
        SemiBadFront nextFront = new SemiBadFront(nextFrontStatesMap, markingsFromIntersection);
        return nextFront;
    }

    public static List<Place> getLastPlacesBeforeTokenMoved(MyAutomaton automaton, Place place) {
        Map<State, List<Place>> markings = automaton.getMarkingMap();
        Queue<State> toBeVisited = new LinkedList<>();
        List<State> visited = new ArrayList<>();
        toBeVisited.addAll(automaton.initials());

        State searchedState = null;

        while (!toBeVisited.isEmpty()) {
            State currentState = toBeVisited.poll();
            visited.add(currentState);
            Set<Transition> outGoingTransitions = automaton.delta(currentState);
            for (Transition currentTransition : outGoingTransitions) {
                State targetState = currentTransition.end();
                Set<State> accessibleStates = automaton.accessibleStates(targetState);
                List<Place> marking = markings.get(targetState);
                List<State> markingsContainingPlace = accessibleMarkingsContainingPlace(markings, accessibleStates, place);

                searchedState = getLastTokenState(markings, searchedState, targetState, marking, markingsContainingPlace);
                if (!markingsContainingPlace.isEmpty()) {
                    for (State state : markingsContainingPlace) {
                        if (!toBeVisited.contains(state) && !visited.contains(state)) {
                            toBeVisited.add(state);
                        }
                    }
                }
            }
        }
        return automaton.getMarkingMap().get(searchedState);
    }

    private static State getLastTokenState(Map<State, List<Place>> markings, State searchedState, State targetState, List<Place> marking, List<State> markingsContainingPlace) {
        if (markingsContainingPlace.size() == 1) {
            if (markings.get(markingsContainingPlace.get(0)).equals(marking)) {
                searchedState = targetState;
            }
        }
        return searchedState;
    }

    public static Queue<Place> findPlacesToRemoveFromSemiBadMarkings(Map<Transition, TransitionMarkingPair> semiBadStatesWithMarkings) {
        Queue<Place> toRemove = new LinkedList<>();
        for (Map.Entry<Transition, TransitionMarkingPair> entry : semiBadStatesWithMarkings.entrySet()) {
            logger.info("Semi-bad and bad state pair: " + entry);
            List<Place> placesToRemove = entry.getValue().getDifferenceSourceAndTarget();
            for (Place p : placesToRemove) {
                if (!toRemove.contains(p)) {
                    toRemove.add(p);
                }
            }
        }
        return toRemove;
    }

    public static boolean checkXorness(PetrinetGraph net, MyAutomaton procedural, PossibleWorldWrap label1, PossibleWorldWrap label2) {
        //TODO: This transition label conversion might cause problems.
        String firstLabel = label1.toString().replace("[","").replace("]","");
        String secondLabel = label2.toString().replace("[","").replace("]","");
        System.out.println(firstLabel + ", " + secondLabel);
        String rule = "(F " + firstLabel + ") -> (G (!" + secondLabel + "))";
        System.out.println(rule);
        PropositionalSignature signature = new PropositionalSignature();

        boolean declare = true;
        boolean minimize = true;
        boolean trim = false;
        boolean noEmptyTrace = true;
        boolean printing = false;

        List<Proposition> netPropositions = PetrinetUtils.getAllTransitionLabels(net);
        signature.addAll(netPropositions);
        LTLfAutomatonResultWrapper ltlfARW = main.Main.ltlfString2Aut(rule, signature, declare, minimize, trim, noEmptyTrace, printing);
        Automaton ruleAutomaton = ltlfARW.getAutomaton();
        Automaton negatedRuleAutomaton = AutomatonOperationUtils.getNegated(ruleAutomaton);
        Automaton intersection = AutomatonOperationUtils.getIntersection(procedural, negatedRuleAutomaton);

        return intersection.terminals().isEmpty();

    }

    private static List<State> accessibleMarkingsContainingPlace(Map<State, List<Place>> markings, Set<State> accessibleStates, Place place) {
        List<State> markingsContainingPlace = new ArrayList<>();
        for (State state : accessibleStates) {
            List<Place> marking = markings.get(state);
            if (marking.contains(place)) {
                markingsContainingPlace.add(state);
            }
        }
        return markingsContainingPlace;
    }

    public static Map<State, List<Transition>> insertTransitionToMapList(Map<State, List<Transition>> stateTransitionMap, State state, Transition transition) {
        if (stateTransitionMap.containsKey(state)) {
            List<Transition> t = stateTransitionMap.get(state);
            t.add(transition);
            stateTransitionMap.put(state, t);
        } else {
            List<Transition> t = new ArrayList<>();
            t.add(transition);
            stateTransitionMap.put(state, t);
        }
        return stateTransitionMap;
    }

    public static void checkLanguage(Petrinet originalNet, String formula, Petrinet repairedNet) throws Exception {
        logger.info("Calculating behaviour for: " + originalNet.getLabel() + " and " + repairedNet.getLabel());
        originalNet = PetrinetUtils.setLabellessTransitionsInvisible(originalNet);
        repairedNet = PetrinetUtils.setLabellessTransitionsInvisible(repairedNet);


        MyAutomaton repairedAutomaton = TSFactory.getRegularAutomaton(repairedNet);
        AutomatonOperationUtils.drawAutomaton(repairedAutomaton, "automatons/automaton_rep_hid.gv");

        MyAutomaton originalAutomatonWithoutHiddenTransitions = TSFactory.getAutomatonWithoutHiddenTransitions(originalNet);
        AutomatonOperationUtils.drawAutomaton(originalAutomatonWithoutHiddenTransitions, "automatons/automaton_org_no_hid"+originalAutomatonWithoutHiddenTransitions.hashCode()+".gv");
        MyEpsilonTransitionRemover orgEpsilonTransitionRemover = new MyEpsilonTransitionRemover();
        Automaton originalNetAutomaton = orgEpsilonTransitionRemover.transform(originalAutomatonWithoutHiddenTransitions);
        AutomatonOperationUtils.drawAutomaton(originalNetAutomaton, "automatons/automaton_org"+originalNetAutomaton.hashCode()+".gv");

        MyAutomaton repairedAutomatonWithoutHiddenTransitions = TSFactory.getAutomatonWithoutHiddenTransitions(repairedNet);
        AutomatonOperationUtils.drawAutomaton(repairedAutomatonWithoutHiddenTransitions, "automatons/automaton_rep_no_hid"+ repairedAutomatonWithoutHiddenTransitions.hashCode() +".gv");
        MyEpsilonTransitionRemover epsilonTransitionRemover = new MyEpsilonTransitionRemover();
        Automaton repairedNetAutomaton = epsilonTransitionRemover.transform(repairedAutomatonWithoutHiddenTransitions);
        AutomatonOperationUtils.drawAutomaton(repairedNetAutomaton, "automatons/automaton_rep"+ repairedNetAutomaton.hashCode() +".gv");

        PropositionalSignature signature = new PropositionalSignature();
        List<Proposition> netPropositions = PetrinetUtils.getAllTransitionLabels(originalNet);
        signature.addAll(netPropositions);

        LTLfAutomatonResultWrapper ltlfARW = AutomatonOperationUtils.createDefaultLtlAutomaton(signature, formula);


        create1234(originalNetAutomaton, ltlfARW.getAutomaton(), repairedNetAutomaton);
    }

    private static void create1234(Automaton procedural, Automaton declarative, Automaton repaired) {
        AutomatonOperationUtils.drawAutomaton(procedural, "automatons/automaton_procedural.gv");
        AutomatonOperationUtils.drawAutomaton(repaired, "automatons/automaton_repaired.gv");
        AutomatonOperationUtils.drawAutomaton(declarative, "automatons/automaton_declarative.gv");

        final Set idealAlphabet = procedural.alphabet();
        SinkComplete sinkComplete = new SinkComplete(idealAlphabet);

        Automaton intersectionProcAndDec = AutomatonOperationUtils.getIntersection(procedural, declarative);
        AutomatonOperationUtils.drawAutomaton(AutomatonOperationUtils.getTrimmed(intersectionProcAndDec), "automatons/automaton_int_dec_proc.gv");
        PathFinder pf = new PathFinder(AutomatonOperationUtils.getTrimmed(intersectionProcAndDec));

        int iCount = pf.getAllPaths();
        Automaton negatedDeclarative = AutomatonOperationUtils.getNegated(declarative);
        Automaton negatedProcedural = AutomatonOperationUtils.getNegated(procedural);

        Automaton transformRepaired = sinkComplete.transform(repaired);
        Automaton trimmedTransformedRepaired = AutomatonOperationUtils.getTrimmed(transformRepaired);
        Automaton intersectionProcAndRepaired = AutomatonOperationUtils.getIntersection(procedural, trimmedTransformedRepaired);

        AutomatonOperationUtils.drawAutomaton(intersectionProcAndRepaired, "automatons/automaton_prc_rep_" + intersectionProcAndRepaired.hashCode()+ ".gv");

        Automaton intersectionDecAndRepaired = AutomatonOperationUtils.getIntersection(declarative, sinkComplete.transform(trimmedTransformedRepaired));
        Automaton intersectionNegProcAndNegDec = AutomatonOperationUtils.getIntersection(negatedDeclarative, negatedProcedural);

        logger.info("-----------------");
        logger.info("");
        Automaton nonCompliantToP1 = AutomatonOperationUtils.getIntersection(sinkComplete.transform(intersectionProcAndRepaired), sinkComplete.transform(negatedDeclarative));
        logger.info("1: (should be empty (true))" + nonCompliantToP1.terminals().isEmpty());
        Automaton intersection2 = AutomatonOperationUtils.getIntersection(sinkComplete.transform(intersectionProcAndRepaired), declarative);
        AutomatonOperationUtils.drawAutomaton(AutomatonOperationUtils.getTrimmed(intersection2), "automatons/automaton_2_" + intersection2.hashCode()+ ".gv");
        PathFinder pathFinder = new PathFinder(AutomatonOperationUtils.getTrimmed(intersection2));

        int twoCount = pathFinder.getAllPaths();
        double result = (double) twoCount / iCount;
        logger.info("twoCount: " + twoCount);
        logger.info("iCount: " + iCount);
        logger.info("2: (paths have to be counted #2/#I) " + result);
        Automaton nonCompliantToD3 = AutomatonOperationUtils.getIntersection(sinkComplete.transform(intersectionDecAndRepaired), negatedProcedural);
        logger.info("3: (should be empty (true)) " + nonCompliantToD3.terminals().isEmpty());
        AutomatonOperationUtils.drawAutomaton(AutomatonOperationUtils.getTrimmed(nonCompliantToD3), "automatons/automaton_3_" + nonCompliantToD3.hashCode()+ ".gv");
        Automaton nonCompliantToEverything4 = AutomatonOperationUtils.getIntersection(sinkComplete.transform(intersectionNegProcAndNegDec), sinkComplete.transform(trimmedTransformedRepaired));
        logger.info("4: (should be empty (true)) " + nonCompliantToEverything4.terminals().isEmpty());
        logger.info("");
        logger.info("-----------------");
    }
}
