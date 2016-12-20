package converter.automaton;

import org.processmining.models.graphbased.directed.petrinet.elements.Place;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by arnelaponin on 13/10/2016.
 */
public class TransitionMarkingPair {

    List<Place> sourceMarking;
    List<Place> targetMarking;

    public TransitionMarkingPair(List<Place> sourceMarking, List<Place> targetMarking) {
        this.sourceMarking = sourceMarking;
        this.targetMarking = targetMarking;
    }

    public List<Place> getSourceMarking() {
        return sourceMarking;
    }

    public List<Place> getTargetMarking() {
        return targetMarking;
    }

    public List<Place> getDifferenceSourceAndTarget() {
        Set<Place> difference = new HashSet<>(sourceMarking);
        difference.removeAll(targetMarking);
        return new ArrayList<>(difference);
    }

    public List<Place> getIntersectionSourceAndTarget() {
        List<Place> intersection = new ArrayList<>(sourceMarking);
        intersection.retainAll(targetMarking);
        return intersection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransitionMarkingPair)) return false;

        TransitionMarkingPair that = (TransitionMarkingPair) o;

        if (sourceMarking != null ? !sourceMarking.equals(that.sourceMarking) : that.sourceMarking != null)
            return false;
        return targetMarking != null ? targetMarking.equals(that.targetMarking) : that.targetMarking == null;

    }

    @Override
    public int hashCode() {
        int result = sourceMarking != null ? sourceMarking.hashCode() : 0;
        result = 31 * result + (targetMarking != null ? targetMarking.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TransitionMarkingPair{" +
                "sourceMarking=" + sourceMarking +
                ", targetMarking=" + targetMarking +
                '}';
    }
}
