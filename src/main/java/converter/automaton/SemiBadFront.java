package converter.automaton;

import automaton.PossibleWorldWrap;
import rationals.State;
import rationals.Transition;

import java.util.List;
import java.util.Map;

/**
 * Created by arnelaponin on 27/10/2016.
 */
public class SemiBadFront {

    Map<State, List<Transition>> states;
    Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> markingsFromIntersection;

    public SemiBadFront(Map<State, List<Transition>> states, Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> markingsFromIntersection) {
        this.states = states;
        this.markingsFromIntersection = markingsFromIntersection;
    }

    public Map<State, List<Transition>> getStates() {
        return states;
    }

    public Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> getMarkingsFromIntersection() {
        return markingsFromIntersection;
    }

    @Override
    public String toString() {
        return "SemiBadFront{" +
                "states=" + states +
                ", markingsFromIntersection=" + markingsFromIntersection +
                '}';
    }
}
