package converter.automaton;

import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import rationals.Automaton;
import rationals.State;
import rationals.StateFactory;
import rationals.Transition;

import java.util.*;

/**
 * Created by arnelaponin on 06/10/2016.
 */
public class MyAutomaton extends Automaton {

    private Map<State, List<Place>> markingMap;

    public MyAutomaton(StateFactory sf) {
        super(sf);
        markingMap = new HashMap<>();
    }

    public MyAutomaton() {
        markingMap = new HashMap<>();
    }

    public Map<State, List<Place>> getMarkingMap() {
        return markingMap;
    }

    public void addMarkingList(State state, List<Place> placeList) {
        markingMap.putIfAbsent(state, placeList);
    }

    public List<State> getAdjacentStates(State state) {
        List<State> adjacentStates = new ArrayList<>();
        Set<Transition> outGoingEdges = this.delta(state);
        for (Transition edge : outGoingEdges) {
            adjacentStates.add(edge.end());
        }
        return adjacentStates;
    }
}
