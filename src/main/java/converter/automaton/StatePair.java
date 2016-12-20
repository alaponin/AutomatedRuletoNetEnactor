package converter.automaton;

import rationals.State;

/**
 * Created by arnelaponin on 26/09/2016.
 */
public class StatePair {

    private final State s1;
    private final State s2;

    public StatePair(State s1, State s2) {
        this.s1 = s1;
        this.s2 = s2;
    }

    public State getS1() {
        return s1;
    }

    public State getS2() {
        return s2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatePair)) return false;

        StatePair statePair = (StatePair) o;

        if (s1 != null ? !s1.equals(statePair.s1) : statePair.s1 != null) return false;
        return s2 != null ? s2.equals(statePair.s2) : statePair.s2 == null;

    }

    @Override
    public int hashCode() {
        int result = s1 != null ? s1.hashCode() : 0;
        result = 31 * result + (s2 != null ? s2.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StatePair{" +
                "s1=" + s1 +
                ", s2=" + s2 +
                '}';
    }
}
