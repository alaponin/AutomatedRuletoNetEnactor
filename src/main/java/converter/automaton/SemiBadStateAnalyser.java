package converter.automaton;

import automaton.PossibleWorldWrap;
import converter.ProceduralRepairer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

import java.util.*;

/**
 * Created by arnelaponin on 07/12/2016.
 */
public class SemiBadStateAnalyser {

    private static Logger logger = LogManager.getLogger(SemiBadStateAnalyser.class);

    private final Automaton reducedIntersection;
    PetrinetGraph net;
    Map<State, List<Transition>> semiBadStates;

    public SemiBadStateAnalyser(PetrinetGraph net, Map<State, List<Transition>> semiBadStatesWithTransitions, Automaton reducedIntersection) {
        this.net = net;
        this.semiBadStates = semiBadStatesWithTransitions;
        this.reducedIntersection = reducedIntersection;
    }

    public Map<PossibleWorldWrap, State> getLastSemiBadState() {
        Map<PossibleWorldWrap, List<Transition>> statesSortedByLabels = new HashMap<>();
        Map<PossibleWorldWrap, State> lastStates = new HashMap<>();

        reSortTransitionsByLabel(statesSortedByLabels);
        logger.info("Sorted semi-bad states: " + statesSortedByLabels);

        for (Map.Entry<PossibleWorldWrap, List<Transition>> entry : statesSortedByLabels.entrySet()) {
            PossibleWorldWrap transitionLabel = entry.getKey();
            List<Transition> transitions = entry.getValue();
            List<State> semiBadStates = new ArrayList<>();
            for (Transition transition : transitions) {
                semiBadStates.add(transition.start());
            }
            for (State semiBadState : semiBadStates) {
                Set accessibleFromSemiBadState = reducedIntersection.accessibleStates(semiBadState);
                accessibleFromSemiBadState.remove(semiBadState);

                State lastState = accessibleStatesContainsSemiBadState(accessibleFromSemiBadState, semiBadStates, semiBadState);
                if (lastState != null) {
                    lastStates.put(transitionLabel, lastState);
                }
            }

        }
        return lastStates;
    }

    private State accessibleStatesContainsSemiBadState(Set<State> accessibleStates, List<State> semiBadStates, State currentState) {
        List<State> accessibleSemiBadStates = new ArrayList<>();
        for (State semiBadState : semiBadStates) {
            if (accessibleStates.contains(semiBadState)) {
                accessibleSemiBadStates.add(semiBadState);
            }
        }
        if (accessibleSemiBadStates.isEmpty()) {
            return currentState;
        } else {
            return null;
        }
    }

    private void reSortTransitionsByLabel(Map<PossibleWorldWrap, List<Transition>> statesSortedByLabels) {
        for (Map.Entry entry : semiBadStates.entrySet()) {
            List<Transition> transitionList = (List<Transition>) entry.getValue();
            for (Transition transition : transitionList) {
                PossibleWorldWrap transitionLabel = (PossibleWorldWrap) transition.label();
                if (statesSortedByLabels.containsKey(transitionLabel)) {
                    List<Transition> transitionsFromMap = statesSortedByLabels.get(transitionLabel);
                    transitionsFromMap.add(transition);
                    statesSortedByLabels.put(transitionLabel, transitionsFromMap);
                } else {
                    List<Transition> transitionsToMap = new ArrayList<>();
                    transitionsToMap.add(transition);
                    statesSortedByLabels.put(transitionLabel, transitionsToMap);
                }
            }
        }
    }
}
