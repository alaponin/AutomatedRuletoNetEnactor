package convertor.utils;

import net.sf.tweety.logics.pl.syntax.Proposition;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by arnelaponin on 05/09/16.
 */
public class PetrinetUtils {

    public static Place getStartPlace(PetrinetGraph petrinet) {
        Place startPlace = null;
        Collection<Place> places = petrinet.getPlaces();

        for (Place p: places) {
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgesInP = petrinet.getInEdges(p);

            //Finding the start of the petri net
            if (edgesInP.isEmpty()) {
                startPlace = p;
            }
        }
        return startPlace;
    }

    //I have made an assumption, that there might be multiple end places
    public static List<Place> getFinalPlace(PetrinetGraph petrinet) {
        List<Place> finalPlaces = new ArrayList<Place>();
        Collection<Place> places = petrinet.getPlaces();
        for (Place p: places) {
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgesOutP = petrinet.getOutEdges(p);

            //Finding the end places of the petri net
            if (edgesOutP.isEmpty()) {
                finalPlaces.add(p);
            }
        }
        return finalPlaces;
    }

    public static List<Proposition> getAllTransitionLabels(PetrinetGraph petrinet) {
        List<Proposition> transitionLabels = new ArrayList<Proposition>();
        for (Transition t : petrinet.getTransitions()) {
            Proposition p = new Proposition(t.getLabel());
            transitionLabels.add(p);
        }
        return transitionLabels;
    }
}
