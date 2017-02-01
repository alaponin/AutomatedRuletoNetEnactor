package converter.utils;

import automaton.PossibleWorldWrap;
import converter.ModelRepairer;
import converter.PNAutomatonConverter;
import converter.automaton.*;
import converter.petrinet.NumberOfStatesDoesNotMatchException;
import main.LTLfAutomatonResultWrapper;
import net.sf.tweety.logics.pl.syntax.Proposition;
import net.sf.tweety.logics.pl.syntax.PropositionalSignature;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

import java.util.*;

/**
 * Created by arnelaponin on 06/10/2016.
 */
public class AutomatonUtils {

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
                            troubledTransitions.stream().filter(t -> t.label().equals(intersectionTransition.label())).forEach(t -> {

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


                            });
                        }
                    }
                }
            }

        }

        System.out.println("Sorted markings: " + sortedMarkings);

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
                    markingsContainingPlace.stream().filter(state -> !toBeVisited.contains(state) && !visited.contains(state)).forEach(toBeVisited::add);
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
            List<Place> placesToRemove = entry.getValue().getDifferenceSourceAndTarget();
            placesToRemove.stream().filter(p -> !toRemove.contains(p)).forEach(toRemove::add);
        }
        return toRemove;
    }

    public static void getSyncPoints(InformationWrapper informationWrapper) {
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
                    if (!checkXorness(net, procedural, (PossibleWorldWrap) outGoingTransition.label(), transitionLabel)) {
                        repairSourceTargetPair.put((PossibleWorldWrap) outGoingTransition.label(), transitionLabel);
                    }
                }
            }
        }

        ModelRepairer.addSyncPointsToParallelBranches(informationWrapper, repairSourceTargetPair);
    }

    private static boolean checkXorness(PetrinetGraph net, MyAutomaton procedural, PossibleWorldWrap label1, PossibleWorldWrap label2) {
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

    private static boolean setContainsSubList(Set<State> s1, List<State> l) {
        for (State state : l) {
            if (s1.contains(state)) {
                return true;
            }
        }
        return false;
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

    public static void checkLanguage(MyAutomaton originalNetAutomaton, Automaton declareAutomaton, Petrinet repairedNet) throws Exception {
        PNAutomatonConverter converter = new PNAutomatonConverter(repairedNet);
        MyAutomaton repairedNetAutomaton = converter.convertToAutomaton();

        Automaton originalIntersectionAutomaton = AutomatonOperationUtils.getIntersection(originalNetAutomaton, declareAutomaton);

        Automaton negatedRepairedIntersection = AutomatonOperationUtils.getNegated(repairedNetAutomaton);

        Automaton repairedIntersectionAutomaton = AutomatonOperationUtils.getIntersection(originalIntersectionAutomaton, negatedRepairedIntersection);
        utils.AutomatonUtils.printAutomaton(repairedIntersectionAutomaton, "automaton_neg_intersection_check.gv");
    }
}
