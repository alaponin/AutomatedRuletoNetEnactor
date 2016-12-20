package converter.synccomparison;

import rationals.Transition;

/**
 * Created by arnelaponin on 26/09/2016.
 */
public class TransitionPair {

    private final Transition t1;
    private final Transition t2;

    public TransitionPair(Transition t1, Transition t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public Transition getT1() {
        return t1;
    }

    public Transition getT2() {
        return t2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransitionPair)) return false;

        TransitionPair that = (TransitionPair) o;

        if (t1 != null ? !t1.equals(that.t1) : that.t1 != null) return false;
        return t2 != null ? t2.equals(that.t2) : that.t2 == null;

    }

    @Override
    public int hashCode() {
        int result = t1 != null ? t1.hashCode() : 0;
        result = 31 * result + (t2 != null ? t2.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TransitionPair{" +
                "t1=" + t1 +
                ", t2=" + t2 +
                '}';
    }
}
