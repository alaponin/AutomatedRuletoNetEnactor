package converter.automaton;

import org.processmining.models.semantics.petrinet.Marking;
import rationals.Automaton;
import rationals.State;
import rationals.StateFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created by arnelaponin on 05/09/16.
 * Code snippets taken from: https://github.com/RiccardoDeMasellis/FLLOAT
 */
public class MarkingStateFactory implements StateFactory, Cloneable {

    private int id = 0;
    Automaton automaton;

    public MarkingStateFactory(Automaton automaton) {
        this.automaton = automaton;
    }

    public MarkingStateFactory() {
        this(null);
    }

    public State create(boolean initial, boolean terminal) {
        return new MarkingState(id++, initial, terminal);
    }

    public State create(boolean initial, boolean terminal, Marking marking) {
        Optional<MarkingState> state = checkIfStateExists(marking);
        Optional<MarkingState> anotherState = containsAnotherMarking(marking);
        if (state.isPresent()) {
            return state.get();
        }

        if (anotherState.isPresent()) {
            return anotherState.get();
        }

        MarkingState newState = new MarkingState(id++, initial, terminal, marking);
        automaton.states().add(newState);


        if (initial) {
            automaton.initials().add(newState);
        }
        if (terminal) {
            automaton.terminals().add(newState);
        }
        return newState;
    }

    private Optional<MarkingState> checkIfStateExists(Marking marking) {
        Set<MarkingState> states = automaton.states();
        for (MarkingState state : states) {
            if (state.getMarking().equals(marking)) {
                return Optional.of(state);
            }
        }
        return Optional.empty();
    }

    private Optional<MarkingState> containsAnotherMarking(Marking marking) {
        Set<MarkingState> states = automaton.states();
        for (MarkingState state : states) {
            if (state.getMarking().baseSet().containsAll(marking.baseSet())) {
                return Optional.of(state);
            }
        }
        return Optional.empty();
    }

    public State create(Marking marking) {
        return create(false, false, marking);
    }

    public Set<State> stateSet() {
        return new HashSet<State>();
    }

    public Object clone() {
        return null;
    }

    public void setAutomaton(Automaton automaton) {
        this.automaton = automaton;
    }

    public State create(boolean initial, boolean terminal, Object o) {
        if (o instanceof Marking) {
            return create(initial, terminal, (Marking) o);
        }
        return null;
    }

    public Set<State> stateSet(Set set) {
        return new HashSet<State>(set);
    }

    public class MarkingState implements State {

        public final int i;

        boolean initial;

        boolean terminal;

        Automaton a;

        Marking m;

        public MarkingState(int i, boolean initial, boolean terminal) {
            this.a = automaton;
            this.i = i;
            this.initial = initial;
            this.terminal = terminal;
        }

        public MarkingState(int i, boolean initial, boolean terminal, Marking m) {
            this.i = i;
            this.initial = initial;
            this.terminal = terminal;
            this.a = automaton;
            this.m = m;
        }

        public boolean isInitial() {
            return initial;
        }

        public State setInitial(boolean initial) {
            this.initial = initial;
            if (initial) {
                a.initials().add(this);
            }
            else {
                a.initials().remove(this);
            }
            return this;
        }

        public boolean isTerminal() {
            return terminal;
        }

        public State setTerminal(boolean terminal) {
            this.terminal = terminal;
            if (terminal)
                a.terminals().add(this);
            else
                a.terminals().remove(this);
            return this;
        }

        public Marking getMarking() {
            return m;
        }

        @Override
        public boolean equals(Object o) {
            try {
                MarkingState ms = (MarkingState) o;
                return (ms.m.equals(m) && (ms.a == a) && ms.i == i);
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return i;
        }

        @Override
        public String toString() {
            return Integer.toString(i);
        }
    }
}
