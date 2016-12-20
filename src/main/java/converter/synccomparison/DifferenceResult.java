package converter.synccomparison;

import rationals.Transition;
import rationals.State;

/**
 * Created by arnelaponin on 26/09/2016.
 * Class used in Marlon's paper, not going to Github.
 */
public class DifferenceResult {

    State ss1;
    Transition t1;
    State st1;
    State ss2;
    Transition t2;
    State st2;
    String status;

    public DifferenceResult(State ss1, Transition t1, State st1, State ss2, Transition t2, State st2, String status) {
        this.ss1 = ss1;
        this.t1 = t1;
        this.st1 = st1;
        this.ss2 = ss2;
        this.t2 = t2;
        this.st2 = st2;
        this.status = status;
    }

    public State getSs1() {
        return ss1;
    }

    public Transition getT1() {
        return t1;
    }

    public State getSt1() {
        return st1;
    }

    public State getSs2() {
        return ss2;
    }

    public Transition getT2() {
        return t2;
    }

    public State getSt2() {
        return st2;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "DifferenceResult{" +
                "ss1=" + ss1 +
                ", t1=" + t1 +
                ", st1=" + st1 +
                ", ss2=" + ss2 +
                ", t2=" + t2 +
                ", st2=" + st2 +
                ", status='" + status + '\'' +
                '}';
    }
}
