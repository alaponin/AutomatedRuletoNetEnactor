package converter.automaton;

import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import rationals.Automaton;
import rationals.State;
import rationals.StateFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}
