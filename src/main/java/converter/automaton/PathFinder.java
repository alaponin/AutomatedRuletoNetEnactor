package converter.automaton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

import java.util.*;

/**
 * Created by arnelaponin on 25/02/2017.
 */
public class PathFinder {

    private static Logger logger = LogManager.getLogger(PathFinder.class);

    private final Automaton automaton;
    private HashMap<Transition, Integer> traversed;
    private Integer numberOfPaths;

    public PathFinder(Automaton automaton) {
        this.automaton = automaton;
        traversed = new HashMap<>();
        Set<Transition> transitions = automaton.delta();
        for (Transition t : transitions) {
            traversed.put(t, 0);
        }
    }

    public Integer getAllPaths() {
        logger.info("Starting all paths...");
        Set<State> sources = automaton.initials();
        Set<State> terminals = automaton.terminals();
        List<List<Transition>> paths = new ArrayList<>();
        numberOfPaths = 0;
        List<Transition> sourceTransitions = getSourceTransitions(sources);
        List<Transition> terminalTransitions = getTerminalTransitions(terminals);


        for (Transition s : sourceTransitions) {
            for (Transition t : terminalTransitions) {
                getPaths(s, t, new Stack<Transition>());
            }
        }

        return numberOfPaths;
    }

    private List<Transition> getSourceTransitions(Set<State> sources) {
        List<Transition> sourceTransitions = new ArrayList<>();
        for (State s : sources) {
            sourceTransitions.addAll(automaton.delta(s));
        }
        return sourceTransitions;
    }

    private List<Transition> getTerminalTransitions(Set<State> terminals) {
        List<Transition> upsideDownTransitions = new ArrayList<>();
        List<Transition> terminalTransitions = new ArrayList<>();
        for (State t : terminals) {
            upsideDownTransitions.addAll(automaton.deltaMinusOne(t));
            for (Transition upside : upsideDownTransitions) {
                State s = upside.end();
                Set<Transition> delta = automaton.delta(s);
                for (Transition t2 : delta) {
                    if (t2.end().equals(t)) {
                        terminalTransitions.add(t2);
                    }
                }
            }
        }
        return terminalTransitions;
    }

    private void getPaths(Transition current, Transition destination, List<Transition> path) {
        path.add(current);
        Integer currentCount = traversed.get(current);
        traversed.put(current, ++currentCount);

        if (current.equals(destination)) {
            numberOfPaths++;
            if (numberOfPaths % 100000 == 0) {
                System.out.println("Number of paths size: " + numberOfPaths);
            }
            path.remove(current);
            return;
        }

        State targetState = current.end();
        Set<Transition> outGoingTransitions = automaton.delta(targetState);

        for (Transition t : outGoingTransitions) {
            if (path.contains(t) && isSelfLoop(t)) {
                continue;
            }
            Integer count = traversed.get(t);
            if (!path.contains(t)) {
                getPaths(t, destination, path);
            } else {
                if (count < 2) {
                    getPaths(t, destination, path);
                }
            }
        }

        path.remove(current);

        currentCount = traversed.get(current);
        traversed.put(current, --currentCount);
    }

    private boolean isSelfLoop(Transition t) {
        return t.start().equals(t.end());
    }

}
