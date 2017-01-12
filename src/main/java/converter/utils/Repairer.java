package converter.utils;

import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by arnelaponin on 03/11/2016.
 */
public class Repairer {

    public static PetrinetGraph putSyncPoints(PetrinetGraph net, Map<Transition, Transition> repairSourceTargetPair) {

        for (Map.Entry<Transition, Transition> entry : repairSourceTargetPair.entrySet()) {
            Transition source = entry.getKey();
            Transition target = entry.getValue();
            System.out.println("Source and target: " + source + " -> " + target);

            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdgesFromSource = net.getOutEdges(source);
            Place nextPlaceAfterSource = null;
            for (PetrinetEdge edge : outEdgesFromSource) {
                nextPlaceAfterSource = (Place) edge.getTarget();
            }
            Set<Transition> transitionsAfterSource = new HashSet<>();
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdgesFromNextPlace = net.getOutEdges(nextPlaceAfterSource);
            for (PetrinetEdge edge : outEdgesFromNextPlace) {
                transitionsAfterSource.add((Transition) edge.getTarget());
            }
            System.out.println("Transition after source: " + transitionsAfterSource);

            String label = nextPlaceAfterSource.getLabel();
            net.removePlace(nextPlaceAfterSource);
            System.out.println("Removing place: " + label);
            Place place = net.addPlace(label);
            net.addArc(source, place);
            net.addArc(place, target);
            if (transitionsAfterSource.size() == 1) {
                Transition t = transitionsAfterSource.iterator().next();
                if (!areTransitionsConnected(net, target, t)) {
                    addNewPlace(net, target, transitionsAfterSource);
                }
            } else {
                addNewPlace(net, target, transitionsAfterSource);
            }
        }
        PetrinetUtils.exportPetriNetToPNML("test_repair_OF_NEW_ALGORITHM_DEUX.pnml", net);
        return net;
    }

    private static boolean areTransitionsConnected(PetrinetGraph net, Transition t1, Transition t2) {
        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdgesFromT1 = net.getOutEdges(t1);
        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdgesToT2 = net.getInEdges(t2);
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : outEdgesFromT1) {
            Place place1 = (Place) outEdge.getTarget();
            for (PetrinetEdge inEdge : inEdgesToT2) {
                Place place2 = (Place) inEdge.getSource();
                if (place1.equals(place2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void addNewPlace(PetrinetGraph net, Transition target, Set<Transition> transitionsAfterSource) {
        Place place1 = net.addPlace("p");
        net.addArc(target, place1);
        for (Transition t : transitionsAfterSource) {
            net.addArc(place1, t);
        }
    }

    public static PetrinetGraph repair(PetrinetGraph net, Place place, Place placeToRemove) throws Exception {

        String fileName = "test_repair_" + place.getLabel() + ".pnml";
        System.out.println("File name: " + fileName);

        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges = net.getOutEdges(place);


        for (PetrinetEdge edge : edges) {
            PetrinetNode source = (PetrinetNode) edge.getSource();
            PetrinetNode target = (PetrinetNode) edge.getTarget();
            net.removeArc(source, target);
        }

        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = net.getOutEdges(placeToRemove);

        Transition dangling = precedingTransOfToRemoveHasOneOutgoingArc(net, placeToRemove);

        if (dangling != null) {
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdgesToPlace = net.getInEdges(place);
            for (PetrinetEdge edgeIn : inEdgesToPlace) {
                Transition tBeforePlace = (Transition) edgeIn.getSource();
                String placeName = placeToRemove.getLabel();
                net.removePlace(placeToRemove);
                Place newlyAddedPlace = net.addPlace(placeName);
                net.addArc(dangling, newlyAddedPlace);
                System.out.println("ADDING arc between: " + dangling + " and " + newlyAddedPlace);
                net.addArc(newlyAddedPlace, tBeforePlace);
                System.out.println("ADDING arc between: " + newlyAddedPlace + " and " + tBeforePlace);
            }

            for (PetrinetEdge edgeOut : outEdges) {
                Transition transition = (Transition) edgeOut.getTarget();
                System.out.println("Will be adding to this transition: " + transition);
                net.addArc(place,transition);
                System.out.println("ADDING arc between: " + place + " and " + transition);
            }

        } else {
            for (PetrinetEdge edgeOut : outEdges) {

                Transition transition = (Transition) edgeOut.getTarget();

                System.out.println("ADDING arc between: " + place + " and " + transition);
                net.addArc(place, transition);
            }
            System.out.println("Removing: " + placeToRemove);
            net.removePlace(placeToRemove);
        }

        PetrinetUtils.exportPetriNetToPNML(fileName, net);
        return net;

    }

    private static Transition precedingTransOfToRemoveHasOneOutgoingArc(PetrinetGraph net, Place toRemove) {
        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = net.getInEdges(toRemove);
        for (PetrinetEdge inEdge : inEdges) {
            Transition transition = (Transition) inEdge.getSource();
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdgesOfTransition = net.getOutEdges(transition);
            if (outEdgesOfTransition.size() == 1) {
                System.out.println("PRECEDING TRANSITION: " + transition);
                return transition;
            }
        }

        return null;
    }

}
