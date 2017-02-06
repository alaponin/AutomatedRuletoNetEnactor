package converter.utils;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;

import java.util.*;

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

            //Necessary because of the loops
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdgesToNextPlace = net.getInEdges(nextPlaceAfterSource);
            List<Transition> transitionsThatHaveToBeConnectedToNewPlace = new ArrayList<>();
            if (inEdgesToNextPlace.size() > 1) {
                for (PetrinetEdge edge : inEdgesToNextPlace) {
                    Transition t = (Transition) edge.getSource();
                    if (!t.equals(source)) {
                        transitionsThatHaveToBeConnectedToNewPlace.add(t);
                    }
                }
            }

            String label = nextPlaceAfterSource.getLabel();
            net.removePlace(nextPlaceAfterSource);
            System.out.println("Removing place: " + label);
            Place place = net.addPlace(label);
            net.addArc(source, place);
            net.addArc(place, target);
            if (transitionsAfterSource.size() == 1) {
                Transition t = transitionsAfterSource.iterator().next();
                if (!areTransitionsConnected(net, target, t)) {
                    addNewPlace(net, target, transitionsAfterSource, transitionsThatHaveToBeConnectedToNewPlace);
                }
            } else {
                addNewPlace(net, target, transitionsAfterSource, transitionsThatHaveToBeConnectedToNewPlace);
            }
        }

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

    private static void addNewPlace(PetrinetGraph net, Transition target, Set<Transition> transitionsAfterSource, List<Transition> transitionsThatHaveToBeConnectedToNewPlace) {
        Place place1 = net.addPlace("p");
        net.addArc(target, place1);
        for (Transition t : transitionsAfterSource) {
            net.addArc(place1, t);
        }
        if (!transitionsThatHaveToBeConnectedToNewPlace.isEmpty()) {
            for (Transition t : transitionsThatHaveToBeConnectedToNewPlace) {
                net.addArc(t, place1);
            }
        }
    }

    private static Place getPlaceFromCloneNet(Petrinet net, Place p) {
        for (Place place : net.getPlaces()) {
            if (place.getLabel().equalsIgnoreCase(p.getLabel())) {
                return place;
            }
        }
        return null;
    }

    public static PetrinetGraph addSyncPointInsteadOfFlattening(PetrinetGraph originalNet, Place troubledPlace, Place problematicPlace) {

        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgesInPlace = originalNet.getInEdges(troubledPlace);
        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgesOutPlace = originalNet.getOutEdges(troubledPlace);
        Transition transitionConnectedPlace = null;
        if (edgesInPlace.size() == 1) {
            for (PetrinetEdge edge : edgesInPlace) {
                transitionConnectedPlace = (Transition) edge.getSource();
            }
        } else if (edgesInPlace.size() > 1) {
            if (edgesOutPlace.size() == 1) {
                for (PetrinetEdge edge : edgesOutPlace) {
                    transitionConnectedPlace = (Transition) edge.getTarget();
                }
            }
        }
        Transition dangling = null;
        if (problematicPlace != null) {
            dangling = precedingTransOfToRemoveHasOneOutgoingArc(originalNet, problematicPlace);
        }

        Map<Transition, Transition> netRepairPair = new HashMap<>();
        PetrinetGraph petrinetGraph = null;
        if (transitionConnectedPlace != null && dangling != null) {
            netRepairPair.put(transitionConnectedPlace, dangling);
            System.out.println("NET REPAIR PAIR: " + netRepairPair);
            petrinetGraph = putSyncPoints(originalNet, netRepairPair);
            String syncFileName = "test_sync_in_" + troubledPlace + "-" + problematicPlace + ".pnml";
            PetrinetUtils.exportPetriNetToPNML(syncFileName, petrinetGraph);
        }
        return petrinetGraph;
    }

    public static PetrinetGraph repair(PetrinetGraph net, Place troubledPlace, Place problematicPlace) throws Exception {
        System.out.println("Place: " + troubledPlace);
        System.out.println("Problematic place to remove: " + problematicPlace);

        String fileName = "test_repair_" + troubledPlace.getLabel() + "_" + problematicPlace + ".pnml";
        System.out.println("File name: " + fileName);

        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges = net.getOutEdges(troubledPlace);

        Transition dangling = precedingTransOfToRemoveHasOneOutgoingArc(net, problematicPlace);

        System.out.println("Dangling: " + dangling);

        for (PetrinetEdge edge : edges) {
            PetrinetNode source = (PetrinetNode) edge.getSource();
            PetrinetNode target = (PetrinetNode) edge.getTarget();
            System.out.println("Removing arc between: " + source + " and " + target);
            net.removeArc(source, target);
        }

        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = net.getOutEdges(problematicPlace);



        if (dangling != null) {
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdgesToPlace = net.getInEdges(troubledPlace);
            for (PetrinetEdge edgeIn : inEdgesToPlace) {
                Transition tBeforePlace = (Transition) edgeIn.getSource();
                String placeName = problematicPlace.getLabel();
                net.removePlace(problematicPlace);
                Place newlyAddedPlace = net.addPlace(placeName);
                net.addArc(dangling, newlyAddedPlace);
                System.out.println("1. ADDING arc between: " + dangling + " and " + newlyAddedPlace);
                net.addArc(newlyAddedPlace, tBeforePlace);
                System.out.println("2. ADDING arc between: " + newlyAddedPlace + " and " + tBeforePlace);
            }

            for (PetrinetEdge edgeOut : outEdges) {
                Transition transition = (Transition) edgeOut.getTarget();
                System.out.println("Will be adding to this transition: " + transition);
                net.addArc(troubledPlace,transition);
                System.out.println("3. ADDING arc between: " + troubledPlace + " and " + transition);
            }

        } else {
            for (PetrinetEdge edgeOut : outEdges) {

                Transition transition = (Transition) edgeOut.getTarget();

                System.out.println("4. ADDING arc between: " + troubledPlace + " and " + transition);
                net.addArc(troubledPlace, transition);
            }
            System.out.println("Removing: " + problematicPlace);
            net.removePlace(problematicPlace);
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
