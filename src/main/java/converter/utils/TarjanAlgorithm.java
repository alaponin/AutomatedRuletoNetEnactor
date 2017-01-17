package converter.utils;

import rationals.Automaton;
import rationals.State;
import rationals.Transition;

import java.util.*;

/**
 * Created by arnelaponin on 17/01/2017.
 */
public class TarjanAlgorithm {

    private Set<State> states;
    private Map<State, Integer> indexMap;
    private Map<State, Integer> lowLinkMap;
    private Map<State, Integer> id;
    private List<State> visited;
    private Stack<State> stack;
    private Integer pre;
    private Integer count;

    public TarjanAlgorithm(Automaton automaton) {
        states = automaton.states();
        indexMap = new HashMap<>();
        lowLinkMap = new HashMap<>();
        id = new HashMap<>();
        visited = new ArrayList<>();
        stack = new Stack<>();
        pre = 0;
        count = 0;
        for (State state : states) {
            indexMap.put(state,0);
            lowLinkMap.put(state,0);
        }
        for (State state : states) {
            if (!visited.contains(state)) {
                dfs(automaton, state);
            }
        }
    }

    private void dfs(Automaton automaton, State state) {
        visited.add(state);
        lowLinkMap.put(state, pre++);
        int min = lowLinkMap.get(state);
        stack.push(state);
        Set<Transition> outGoingEdges = automaton.delta(state);
        for (Transition edge : outGoingEdges) {
            State targetState = edge.end();
            if (!visited.contains(targetState)) {
                dfs(automaton, targetState);
            }
            if (lowLinkMap.get(targetState) < min) {
                min = lowLinkMap.get(targetState);
            }
        }
        if (min < lowLinkMap.get(state)) {
            lowLinkMap.put(state, min);
            return;
        }
        State anotherState;
        do {
            anotherState = stack.pop();
            id.put(anotherState, count);
            lowLinkMap.put(anotherState, Integer.MAX_VALUE);

        } while (!anotherState.equals(state));
        count++;
    }

    public Map<State, Integer> getIdMap() {
        return id;
    }

    public Integer getNumberOfComponents() {
        return count;
    }
}
