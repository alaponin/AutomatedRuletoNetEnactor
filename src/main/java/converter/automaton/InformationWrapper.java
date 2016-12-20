package converter.automaton;

import automaton.PossibleWorldWrap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import converter.PNAutomatonConverter;
import converter.utils.AutomatonOperationUtils;
import converter.utils.AutomatonSource;
import converter.utils.AutomatonUtils;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by arnelaponin on 19/12/2016.
 */
public class InformationWrapper {

    private Automaton declarative;
    private PetrinetGraph net;
    private MyAutomaton procedural;
    private Automaton reducedIntersection;
    private MyAutomaton trimmedIntersectionWithMarkings;
    private List<State> badStates;
    private List<State> goodStates;
    private Map<State, List<Transition>> semiBadStates;
    private Map<State, List<Transition>> badStatesWithTransitions;

    public InformationWrapper(Automaton declarative, PetrinetGraph net) throws Exception {
        this.declarative = declarative;
        this.updateInformation(net);
    }

    public void updateInformation(PetrinetGraph updatedNet) throws Exception {
        this.net = updatedNet;
        this.procedural = createProceduralAutomaton(updatedNet);
        this.reducedIntersection = createReducedIntersection(this.procedural, this.declarative);
        this.badStates = findBadStates(this.reducedIntersection);
        this.goodStates = findGoodStates(this.reducedIntersection);
        this.semiBadStates = findSemiBadStates(this.reducedIntersection);
        this.badStatesWithTransitions = getBadStatesWithTransitions(this.reducedIntersection);
        this.trimmedIntersectionWithMarkings = findTrimmedIntersectionWithMarkings();
    }


    public PetrinetGraph getNet() {
        return net;
    }

    public MyAutomaton getProceduralAutomaton() {
        return procedural;
    }

    public Automaton getDeclarativeAutomaton() {
        return declarative;
    }

    public Automaton getReducedIntersection() {
        return reducedIntersection;
    }

    public List<State> getBadStates() {
        return this.badStates;
    }

    public Map<State, List<Transition>> getSemiBadStates() {
        return semiBadStates;
    }

    public Map<State, List<Transition>> getBadStatesWithTransitions() {
        return badStatesWithTransitions;
    }

    public MyAutomaton getTrimmedIntersectionWithMarkings() {
        return trimmedIntersectionWithMarkings;
    }

    private MyAutomaton createProceduralAutomaton(PetrinetGraph net) throws Exception {
        PNAutomatonConverter converter = new PNAutomatonConverter(net);
        return converter.convertToAutomaton();
    }

    private Automaton createReducedIntersection(MyAutomaton procedural, Automaton declarative) {
        Automaton intersection = AutomatonOperationUtils.getIntersection(procedural, declarative);
        return AutomatonOperationUtils.getReduced(intersection);
    }

    private List<State> findBadStates(Automaton reducedIntersection) {
        Queue<State> toBeVisited = new LinkedList<>();
        List<State> visited = new ArrayList<>();
        toBeVisited.addAll(reducedIntersection.initials());
        List<State> badStates = new ArrayList<>();
        while (!toBeVisited.isEmpty()) {
            State currentState = toBeVisited.poll();
            visited.add(currentState);
            Set<Transition> outGoingTransitions = reducedIntersection.delta(currentState);
            for (Transition currentTransition : outGoingTransitions) {
                State targetState = currentTransition.end();
                Set<State> accessibleStates = reducedIntersection.accessibleStates(targetState);
                if (!accessibleStates.containsAll(reducedIntersection.terminals())) {
                    if (!badStates.contains(targetState)) {
                        badStates.add(targetState);
                    }
                    accessibleStates.stream().filter(s -> !badStates.contains(s)).forEach(badStates::add);
                    badStates.stream().filter(s -> !visited.contains(s)).forEach(visited::add);
                } else {
                    if (!toBeVisited.contains(targetState) && !badStates.contains(targetState)) {
                        toBeVisited.add(targetState);
                    }

                }
            }
        }

        return badStates;
    }

    private List<State> findGoodStates(Automaton reducedIntersection) {
        Queue<State> toBeVisited = new LinkedList<>();
        List<State> visited = new ArrayList<>();
        toBeVisited.addAll(reducedIntersection.initials());
        List<State> goodStates = new ArrayList<>();
        while (!toBeVisited.isEmpty()) {
            State currentState = toBeVisited.poll();
            visited.add(currentState);
            Set<Transition> outGoingTransitions = reducedIntersection.delta(currentState);
            for (Transition currentTransition : outGoingTransitions) {
                State targetState = currentTransition.end();
                Set<State> accessibleStates = reducedIntersection.accessibleStates(targetState);
                if (accessibleStates.containsAll(reducedIntersection.terminals()) && !setContainsSubList(accessibleStates, this.badStates)) {
                    if (!goodStates.contains(targetState)) {
                        goodStates.add(targetState);
                    }
                    accessibleStates.stream().filter(s -> !goodStates.contains(s)).forEach(goodStates::add);
                    goodStates.stream().filter(s -> !visited.contains(s)).forEach(visited::add);
                }  else {
                    if (!toBeVisited.contains(targetState) && !goodStates.contains(targetState)) {
                        toBeVisited.add(targetState);
                    }
                }

            }
        }

        return goodStates;
    }

    private Map<State, List<Transition>> findSemiBadStates(Automaton reducedIntersection) {
        Map<State, List<Transition>> semiBadStates = new HashMap<>();

        Set<State> allStates = reducedIntersection.states();

        List<State> potentialSemiBadStates = allStates.stream().filter(s -> !goodStates.contains(s) && !badStates.contains(s)).collect(Collectors.toList());
        for (State potential : potentialSemiBadStates) {
            Set<Transition> transitions = reducedIntersection.delta(potential);
            transitions.stream().filter(transition -> badStates.contains(transition.end())).forEach(transition -> {
                insertTransitionToMapList(semiBadStates, potential, transition);
            });
        }
        System.out.println("SEMI-BAD STATES: " + semiBadStates);
        return semiBadStates;
    }

    public void colourAutomaton() {
        AutomatonOperationUtils.colorAutomatonStates(this, "automaton_coloured.gv");
    }

    private MyAutomaton findTrimmedIntersectionWithMarkings() throws Exception {
        Automaton trimIntersection = AutomatonOperationUtils.getTrimmed(reducedIntersection);
        Explorer explorer = new Explorer(procedural, trimIntersection);
        return explorer.addMarkingsFromOriginal();
    }

    private Map<State, List<Transition>> getBadStatesWithTransitions(Automaton reducedIntersection) {
        Map<State, List<Transition>> badStatesTransitions = new HashMap<>();
        for (State badState : badStates) {
            Set<Transition> transitions = reducedIntersection.delta(badState);
            int count = 0;
            if (!badState.isTerminal() && transitions.isEmpty()) {
                badStatesTransitions.put(badState, new ArrayList<>());
            }
            for (Transition transition : transitions) {
                if (badStates.contains(transition.end())) {
                    count++;
                }
            }
            if (count == transitions.size()) {
                for (Transition transition : transitions) {
                    insertTransitionToMapList(badStatesTransitions, badState, transition);
                }
            }
        }
        return badStatesTransitions;
    }

    public Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> getSemiBadMarkingsFromOriginal() {
        return getMarkingsBasedOnStates(AutomatonSource.fromOriginal);
    }

    public Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> getSemiBadMarkingsFromIntersection() {
        return getMarkingsBasedOnStates(AutomatonSource.fromIntersection);
    }

    private Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> getMarkingsBasedOnStates(AutomatonSource source) {
        Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> sortedMarkings = new HashMap<>();
        Queue<StatePair> toBeVisited = AutomatonUtils.getInitialStatePairInQueue(procedural, reducedIntersection);
        Map<State, List<Place>> originalMarkings = procedural.getMarkingMap();
        while (!toBeVisited.isEmpty()) {
            StatePair statePair = toBeVisited.poll();
            Set<Transition> originalOutgoingTransitions = procedural.delta(statePair.getS1());
            Set<Transition> intersectionOutgoingTransitions = reducedIntersection.delta(statePair.getS2());
            for (Transition intersectionTransition : intersectionOutgoingTransitions) {
                for (Transition originalTransition : originalOutgoingTransitions) {
                    if (intersectionTransition.label().equals(originalTransition.label())) {
                        toBeVisited.add(new StatePair(originalTransition.end(), intersectionTransition.end()));
                        if (semiBadStates.containsKey(intersectionTransition.start())) {
                            List<Transition> troubledTransitions = semiBadStates.get(intersectionTransition.start());
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
        return sortedMarkings;
    }

    private static Map<State, List<Transition>> insertTransitionToMapList(Map<State, List<Transition>> stateTransitionMap, State state, Transition transition) {
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

    private static boolean setContainsSubList(Set<State> s1, List<State> l) {
        for (State state : l) {
            if (s1.contains(state)) {
                return true;
            }
        }
        return false;
    }
}
