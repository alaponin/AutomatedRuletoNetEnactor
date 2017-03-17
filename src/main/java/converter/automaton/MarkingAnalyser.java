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

        List<Place> flattenedOriginalPlaces = new ArrayList<>();
        Collection<List<Place>> originalMarkingsCollection = original.getMarkingMap().values();
        for (Iterator iterator = originalMarkingsCollection.iterator(); iterator.hasNext();) {
            List<Place> originalMarking = (List<Place>) iterator.next();
            for (Place p : originalMarking) {
                if (!flattenedOriginalPlaces.contains(p)) {
                    flattenedOriginalPlaces.add(p);
                }
            }
        }

        List<Place> flattenedGoodPlaces = new ArrayList<>();
        Collection<List<Place>> goodMarkingsCollection = good.getMarkingMap().values();
        for (Iterator iterator = goodMarkingsCollection.iterator(); iterator.hasNext();) {
            List<Place> goodMarking = (List<Place>) iterator.next();
            for (Place p : goodMarking) {
                if (!flattenedGoodPlaces.contains(p)) {
                    flattenedGoodPlaces.add(p);
                }
            }
        }

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
