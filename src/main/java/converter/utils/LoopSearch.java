package converter.utils;

import converter.automaton.MyAutomaton;
import converter.automaton.StatePair;
import rationals.State;

import java.util.*;

/**
 * Created by arnelaponin on 30/01/2017.
 */
public class LoopSearch {

    private MyAutomaton automaton;
    private List<State> visited;
    private Queue<State> queue;
    private Queue<StatePair> statePairs;
    private Integer[] edgeTo;
    private Integer[] distTo;

    public LoopSearch(MyAutomaton automaton) {
        this.automaton = automaton;
        visited = new ArrayList<>();
        queue = new LinkedList<>();
        statePairs = new LinkedList<>();
        edgeTo = new Integer[automaton.states().size()];
        distTo = new Integer[automaton.states().size()];
        for (int v = 0; v < automaton.states().size(); v++) {
            distTo[v] = Integer.MAX_VALUE;
        }
        bfs(this.automaton.initials());
    }

    private void bfs(Set<State> initialStates) {
        for (State s : initialStates) {
            Integer stateLabel = Integer.parseInt(s.toString());
            visited.add(s);
            distTo[stateLabel] = 0;
            queue.add(s);
        }
        while (!queue.isEmpty()) {
            State current = queue.remove();
            Integer currentStateLabel = Integer.parseInt(current.toString());
            for (State nextState : automaton.getAdjacentStates(current)) {
                Integer nextStateLabel = Integer.parseInt(nextState.toString());
                if (!visited.contains(nextState)) {
                    edgeTo[nextStateLabel] = currentStateLabel;
                    distTo[nextStateLabel] = distTo[currentStateLabel] + 1;
                    visited.add(nextState);
                    queue.add(nextState);
                } else {
                    if (distTo[nextStateLabel] < distTo[currentStateLabel]) {
                        StatePair statePair = new StatePair(current, nextState);
                        statePairs.add(statePair);
                    }

                }
            }
        }
    }

    public StatePair getLoop() {
        return statePairs.remove();
    }
}
