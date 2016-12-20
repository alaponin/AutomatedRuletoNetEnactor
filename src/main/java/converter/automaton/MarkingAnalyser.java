package converter.automaton;

import automaton.PossibleWorldWrap;
import converter.utils.AutomatonUtils;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by arnelaponin on 06/10/2016.
 */
public class MarkingAnalyser {

    public static List<Place> getUnusedStates(MyAutomaton original, MyAutomaton good) throws Exception {

        List<Place> flattenedOriginalPlaces = original.getMarkingMap().values().stream()
                .flatMap(List::stream).distinct()
                .collect(Collectors.toList());
        List<Place> flattenedGoodPlaces = good.getMarkingMap().values().stream()
                .flatMap(List::stream).distinct()
                .collect(Collectors.toList());

        Set<Place> placesNotInOriginal = new HashSet<>(flattenedOriginalPlaces);
        placesNotInOriginal.removeAll(flattenedGoodPlaces);

        return new ArrayList<>(placesNotInOriginal);
    }

    public static List<PossibleWorldWrap> getUnusedTransitionLabels(MyAutomaton original, MyAutomaton good) {
        Set<PossibleWorldWrap> originalAlphabet = original.alphabet();
        Set<PossibleWorldWrap> goodAlphabet = good.alphabet();
        List<PossibleWorldWrap> diffOfAlphabet = new ArrayList<>(originalAlphabet);
        diffOfAlphabet.removeAll(goodAlphabet);
        return diffOfAlphabet;
    }
}
