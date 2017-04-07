package converter.utils;

import converter.petrinet.PetrinetNodeType;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.*;

/**
 * Created by arnelaponin on 03/11/2016.
 */
public class Repairer {

    private static Logger logger = LogManager.getLogger(Repairer.class);

    public static PetrinetGraph putSyncPoints(PetrinetGraph net, Map<Transition, Transition> repairSourceTargetPair) {

        for (Map.Entry<Transition, Transition> entry : repairSourceTargetPair.entrySet()) {
            Transition source = entry.getKey();
            Transition target = entry.getValue();

            logger.info("Original source and target: " + source + " " + target);

            Stack<PetrinetNode> prevSourceBloc = getPrevOfBloc(net, source);
            logger.info("Prev source bloc: " + prevSourceBloc);
            Stack<PetrinetNode> nextSourceBloc = getNextOfBloc(net, source);

            Stack<PetrinetNode> prevTargetBloc = getPrevOfBloc(net, target);
            logger.info("Prev target bloc: "+prevTargetBloc);
            Stack<PetrinetNode> nextTargetBloc = getNextOfBloc(net, target);

            Transition anotherSource = getNewSourceTransition(net, nextSourceBloc, nextTargetBloc);
            Transition anotherTarget = getNewTargetTransition(net, prevSourceBloc, prevTargetBloc);

            if (anotherSource != null) {
                source = anotherSource;
            }
            if (anotherTarget != null) {
                target = anotherTarget;
            }

            logger.info("Source and target: " + source + " -> " + target);

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

            logger.info("Transition after source: " + transitionsAfterSource);

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
            logger.info("Removing place: " + label);
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

    private static Transition getNewSourceTransition(PetrinetGraph net, Stack<PetrinetNode> nextSourceBloc, Stack<PetrinetNode> nextTargetBloc) {
        Transition anotherSource = null;

        if (!nextSourceBloc.isEmpty()) {
            if (nextSourceBloc.size() >= nextTargetBloc.size()) {
                nextSourceBloc.removeAll(nextTargetBloc);
                if (!nextSourceBloc.isEmpty()) {
                    PetrinetNode node = nextSourceBloc.peek();
                    if (node.getClass().isAssignableFrom(Transition.class)) {
                        Transition t = (Transition) node;
                        anotherSource = (Transition) node;
                    } else {
                        Place p = (Place) node;
                        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = net.getOutEdges(p);
                        if (outEdges.size() == 1) {
                            Iterator it = outEdges.iterator();
                            PetrinetEdge edge = (PetrinetEdge) it.next();
                            Transition transition = (Transition) edge.getTarget();
                            if (!nextTargetBloc.contains(transition)) {
                                anotherSource = transition;
                            }
                        }
                    }
                }

            }
        }
        return anotherSource;
    }

    private static Transition getNewTargetTransition(PetrinetGraph net, Stack<PetrinetNode> prevSourceBloc, Stack<PetrinetNode> prevTargetBloc) {
        Transition anotherTarget = null;
        //if (!prevSourceBloc.isEmpty() && !prevTargetBloc.isEmpty()) {
        if (!prevTargetBloc.isEmpty()) {
            if (prevTargetBloc.size() >= prevSourceBloc.size()) {
                prevTargetBloc.removeAll(prevSourceBloc);
                System.out.println("All possible prevs for target: " + prevTargetBloc);
                logger.info("All possible prevs for target: " + prevTargetBloc);
                if (!prevTargetBloc.isEmpty()) {
                    //TODO:Order of taking nodes might be wrong!!!!
                    PetrinetNode node = prevTargetBloc.peek();
                    logger.info(node);
                    logger.info(node.getClass().isAssignableFrom(Transition.class));
                    if (node.getClass().isAssignableFrom(Transition.class)) {
                        Transition t = (Transition) node;
                        logger.info(getNodeType(net, t));
                        if (getNodeType(net, t).equals(PetrinetNodeType.ANDSPLIT)) {
                            logger.info("Dealing with an and split...");
                            Transition transition = replaceWithHiddenTransition(net, t);
                            anotherTarget = addHiddenTransitionForSync(net, transition);
                        } else if (getNodeType(net, t).equals(PetrinetNodeType.ANDJOINSPLIT)) {
                            Transition hiddenTransitionForSync = addHiddenTransitionForSync(net, t);
                            addHiddenTransitionForSync(net, hiddenTransitionForSync);
                            anotherTarget = hiddenTransitionForSync;
                        } else {
                            anotherTarget = (Transition) node;
                        }
                    } else {
                        Place p = (Place) node;
                        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = net.getInEdges(p);
                        if (inEdges.size() == 1) {
                            Iterator it = inEdges.iterator();
                            PetrinetEdge edge = (PetrinetEdge) it.next();
                            Transition transition = (Transition) edge.getSource();
                            if (!prevSourceBloc.contains(transition)) {
                                anotherTarget = transition;
                            }
                        }
                    }
                }

            }
        }
        return anotherTarget;
    }

    private static Transition replaceWithHiddenTransition(PetrinetGraph net, Transition t) {
        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = net.getOutEdges(t);
        List<Place> outgoingPlaces = new ArrayList<>();
        for (PetrinetEdge e : outEdges) {
            outgoingPlaces.add((Place) e.getTarget());
        }
        for (Place p : outgoingPlaces) {
            net.removeArc(t,p);
        }
        Transition hiddenTransition = net.addTransition("");
        hiddenTransition.setInvisible(true);
        Place placeBeforeHiddenTransition = net.addPlace("p");
        net.addArc(t,placeBeforeHiddenTransition);
        net.addArc(placeBeforeHiddenTransition, hiddenTransition);
        for (Place p : outgoingPlaces) {
            net.addArc(hiddenTransition,p);
        }
        return hiddenTransition;
    }

    private static Transition addHiddenTransitionForSync(PetrinetGraph net, Transition t) {
        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = net.getInEdges(t);
        List<Place> incomingPlaces = new ArrayList<>();
        for (PetrinetEdge e : inEdges) {
            incomingPlaces.add((Place) e.getSource());
        }
        for (Place p : incomingPlaces) {
            net.removeArc(p,t);
        }
        Transition hiddenTransition = net.addTransition("");
        hiddenTransition.setInvisible(true);
        Place placeAfterHiddenTransition = net.addPlace("p");
        net.addArc(placeAfterHiddenTransition,t);
        net.addArc(hiddenTransition, placeAfterHiddenTransition);
        for (Place p : incomingPlaces) {
            net.addArc(p,hiddenTransition);
        }
        return hiddenTransition;
    }

    private static Stack<PetrinetNode> getPrevOfBloc(PetrinetGraph net, Transition t) {
        Stack<PetrinetNode> stack = new Stack<>();
        List<PetrinetNode> visited = new ArrayList<>();
        Stack<Integer> levelStack = new Stack<>();
        Integer level = 0;
        return getPrevOfBloc(net, t, visited, stack, levelStack, level);
    }

    private static Stack<PetrinetNode> getNextOfBloc(PetrinetGraph net, Transition t) {
        Stack<PetrinetNode> stack = new Stack<>();
        List<PetrinetNode> visited = new ArrayList<>();
        Stack<Integer> levelStack = new Stack<>();
        Integer level = 0;
        return getNextOfBloc(net, t, visited, stack, levelStack, level);
    }

    private static Stack<PetrinetNode> getNextOfBloc(PetrinetGraph net, PetrinetNode t, List<PetrinetNode> visited, Stack<PetrinetNode> stack, Stack<Integer> levelStack, Integer level) {
        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = net.getOutEdges(t);
        visited.add(t);
        if (outEdges.isEmpty()) {
            return stack;
        } else {
            Iterator it = outEdges.iterator();
            while (it.hasNext()) {
                PetrinetEdge edge = (PetrinetEdge) it.next();
                PetrinetNode node = (PetrinetNode) edge.getTarget();

                if (!visited.contains(node)) {
                    makeDecisionAboutKeepingNode(net, stack, levelStack, level, node, true);
                    getNextOfBloc(net, node, visited, stack, levelStack, level);
                }

            }

        }
        return stack;
    }

    private static Stack<PetrinetNode> getPrevOfBloc(PetrinetGraph net, PetrinetNode t, List<PetrinetNode> visited, Stack<PetrinetNode> stack, Stack<Integer> levelStack, Integer level) {
        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = net.getInEdges(t);
        visited.add(t);
        if (inEdges.isEmpty()) {
            return stack;
        } else {
            Iterator it = inEdges.iterator();
            while (it.hasNext()) {//if in the stack there is a XOR join and I am trying to add a split then I can throw away both of them.
                PetrinetEdge edge = (PetrinetEdge) it.next();
                PetrinetNode node = (PetrinetNode) edge.getSource();

                if (!visited.contains(node)) {
                    makeDecisionAboutKeepingNode(net, stack, levelStack, level, node, false);
                    getPrevOfBloc(net, node, visited, stack, levelStack, level);
                }


            }

        }
        return stack;
    }

    private static void makeDecisionAboutKeepingNode(PetrinetGraph net, Stack<PetrinetNode> stack, Stack<Integer> levelStack, Integer level, PetrinetNode node, boolean forward) {
        PetrinetNodeType nodeType = getNodeType(net, node);
        if (!nodeType.equals(PetrinetNodeType.NODE)) {
            if (nodeType.equals(PetrinetNodeType.XORSPLIT)) {
                if (stack.size() >= 1) {
                    PetrinetNode lastNode = stack.peek();
                    Integer lastLevel = levelStack.peek();
                    PetrinetNodeType lastNodeType = getNodeType(net, lastNode);
                    if (!forward) {
                        level++;
                        if (lastNodeType.equals(PetrinetNodeType.XORJOIN)) {
                            stack.pop();
                            levelStack.pop();
                        } else if (lastNodeType.equals(PetrinetNodeType.XORJOINSPLIT)) {
                            popFromStacksIfLevelIsSame(stack, levelStack, level, lastLevel);
                        } else {
                            pushToStacks(stack, levelStack, level, node);
                        }
                    } else {
                        pushToStacks(stack, levelStack, level, node);
                    }
                } else {
                    pushToStacks(stack, levelStack, level, node);
                }

            } else if (nodeType.equals(PetrinetNodeType.XORJOIN)) {
                if (stack.size() >= 1) {
                    PetrinetNode lastNode = stack.peek();
                    Integer lastLevel = levelStack.peek();
                    PetrinetNodeType lastNodeType = getNodeType(net, lastNode);
                    if (forward) {
                        level++;
                        if (lastNodeType.equals(PetrinetNodeType.XORSPLIT)) {
                            stack.pop();
                            levelStack.pop();
                        } else if (lastNodeType.equals(PetrinetNodeType.XORJOINSPLIT)) {
                            popFromStacksIfLevelIsSame(stack, levelStack, level, lastLevel);
                        } else {
                            pushToStacks(stack, levelStack, level, node);
                        }
                    } else {
                        pushToStacks(stack, levelStack, level, node);
                    }
                } else {
                    pushToStacks(stack, levelStack, level, node);
                }
            } else if (nodeType.equals(PetrinetNodeType.ANDSPLIT)) {
                if (stack.size() >= 1) {
                    PetrinetNode lastNode = stack.peek();
                    Integer lastLevel = levelStack.peek();
                    PetrinetNodeType lastNodeType = getNodeType(net, lastNode);
                    if (!forward) {
                        level++;
                        if (lastNodeType.equals(PetrinetNodeType.ANDJOIN)) {
                            stack.pop();
                            levelStack.pop();
                        } else if (lastNodeType.equals(PetrinetNodeType.ANDJOINSPLIT)) {
                            popFromStacksIfLevelIsSame(stack, levelStack, level, lastLevel);
                        } else {
                            pushToStacks(stack, levelStack, level, node);
                        }
                    } else {
                        pushToStacks(stack, levelStack, level, node);
                    }
                } else {
                    pushToStacks(stack, levelStack, level, node);
                }
            } else if (nodeType.equals(PetrinetNodeType.ANDJOIN)) {
                if (stack.size() >= 1) {
                    PetrinetNode lastNode = stack.peek();
                    Integer lastLevel = levelStack.peek();
                    PetrinetNodeType lastNodeType = getNodeType(net, lastNode);
                    //System.out.println("last: " + lastNode + " type: " + lastNodeType);
                    if (forward) {
                        level++;
                        if (lastNodeType.equals(PetrinetNodeType.ANDSPLIT)) {
                            stack.pop();
                            levelStack.pop();
                        } else if (lastNodeType.equals(PetrinetNodeType.ANDJOINSPLIT)) {
                            popFromStacksIfLevelIsSame(stack, levelStack, level, lastLevel);
                        } else {
                            pushToStacks(stack, levelStack, level, node);
                        }
                    } else {
                        pushToStacks(stack, levelStack, level, node);
                    }
                } else {
                    pushToStacks(stack, levelStack, level, node);
                }
            } else if (nodeType.equals(PetrinetNodeType.ANDJOINSPLIT)) {
                stack.push(node);
                pushToStacks(stack, levelStack, level, node);
                levelStack.push(level);
            } else {
                if (!stack.contains(node)) {
                    pushToStacks(stack, levelStack, level, node);
                }

            }
        }
    }

    private static void pushToStacks(Stack<PetrinetNode> stack, Stack<Integer> levelStack, Integer level, PetrinetNode node) {
        stack.push(node);
        levelStack.push(level);
    }

    private static void popFromStacksIfLevelIsSame(Stack<PetrinetNode> stack, Stack<Integer> levelStack, Integer level, Integer lastLevel) {
        if (lastLevel.equals(level)) {
            stack.pop();
            levelStack.pop();
        }
    }

    private static PetrinetNodeType getNodeType(PetrinetGraph net, PetrinetNode node) {
        if (node.getClass().isAssignableFrom(Transition.class)) {
            Transition transition = (Transition) node;
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdgesToTransition = net.getInEdges(transition);
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdgesFromTransition = net.getOutEdges(transition);
            if (inEdgesToTransition.size() > 1 && outEdgesFromTransition.size() > 1) {
                return PetrinetNodeType.ANDJOINSPLIT;
            } else if (inEdgesToTransition.size() > 1) {
                return PetrinetNodeType.ANDJOIN;
            } else if (outEdgesFromTransition.size() > 1) return PetrinetNodeType.ANDSPLIT;
        } else if (node.getClass().isAssignableFrom(Place.class)) {
            Place place = (Place) node;
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdgesToPlace = net.getInEdges(place);
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdgesFromPlace = net.getOutEdges(place);
            if (inEdgesToPlace.size() > 1 && outEdgesFromPlace.size() > 1) {
                return PetrinetNodeType.XORJOINSPLIT;
            } else if (inEdgesToPlace.size() > 1) {
                return PetrinetNodeType.XORJOIN;
            } else if (outEdgesFromPlace.size() > 1) return PetrinetNodeType.XORSPLIT;
        }

        return PetrinetNodeType.NODE;
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
        List<Transition> danglingTransitions = new ArrayList<>();
        if (problematicPlace != null) {
            danglingTransitions = precedingTransOfToRemoveHasOneOutgoingArc(originalNet, problematicPlace);
        }

        Map<Transition, Transition> netRepairPair = new HashMap<>();
        PetrinetGraph petrinetGraph = null;
        if (transitionConnectedPlace != null && !danglingTransitions.isEmpty()) {
            for (Transition dangling : danglingTransitions) {
                netRepairPair.put(transitionConnectedPlace, dangling);
                System.out.println("NET REPAIR PAIR: " + netRepairPair);
                logger.info("NET REPAIR PAIR: " + netRepairPair);
            }
            petrinetGraph = putSyncPoints(originalNet, netRepairPair);
            String syncFileName = "test_nets/test_sync_in_" + troubledPlace + "-" + problematicPlace + ".pnml";
            PetrinetUtils.exportPetriNetToPNML(syncFileName, petrinetGraph);
        }
        return petrinetGraph;
    }

    public static PetrinetGraph repair(PetrinetGraph net, Place troubledPlace, Place problematicPlace) throws Exception {
        System.out.println("Place: " + troubledPlace);
        logger.info("Place: " + troubledPlace);
        System.out.println("Problematic place to remove: " + problematicPlace);
        logger.info("Problematic place to remove: " + problematicPlace);

        String fileName = "test_nets/test_repair_" + net.hashCode()+ "_" + troubledPlace.getLabel() + "_" + problematicPlace + ".pnml";
        System.out.println("File name: " + fileName);
        logger.info("File name: " + fileName);

        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges = net.getOutEdges(troubledPlace);

        List<Transition> danglingTransitions = precedingTransOfToRemoveHasOneOutgoingArc(net, problematicPlace);

        System.out.println("Dangling transitions: " + danglingTransitions);
        logger.info("Dangling transitions: " + danglingTransitions);

        for (PetrinetEdge edge : edges) {
            PetrinetNode source = (PetrinetNode) edge.getSource();
            PetrinetNode target = (PetrinetNode) edge.getTarget();
            System.out.println("Removing arc between: " + source + " and " + target);
            logger.info("Removing arc between: " + source + " and " + target);
            net.removeArc(source, target);
        }

        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = net.getOutEdges(problematicPlace);



        if (!danglingTransitions.isEmpty()) {
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdgesToPlace = net.getInEdges(troubledPlace);
            for (PetrinetEdge edgeIn : inEdgesToPlace) {
                Transition tBeforePlace = (Transition) edgeIn.getSource();
                String placeName = problematicPlace.getLabel();
                net.removePlace(problematicPlace);
                Place newlyAddedPlace = net.addPlace(placeName);
                for (Transition dangling : danglingTransitions) {
                    net.addArc(dangling, newlyAddedPlace);
                    System.out.println("1. ADDING arc between: " + dangling + " and " + newlyAddedPlace);
                    logger.info("1. ADDING arc between: " + dangling + " and " + newlyAddedPlace);
                }
                net.addArc(newlyAddedPlace, tBeforePlace);
                System.out.println("2. ADDING arc between: " + newlyAddedPlace + " and " + tBeforePlace);
                logger.info("2. ADDING arc between: " + newlyAddedPlace + " and " + tBeforePlace);
            }

            for (PetrinetEdge edgeOut : outEdges) {
                Transition transition = (Transition) edgeOut.getTarget();
                System.out.println("Will be adding to this transition: " + transition);
                logger.info("Will be adding to this transition: " + transition);
                net.addArc(troubledPlace,transition);
                System.out.println("3. ADDING arc between: " + troubledPlace + " and " + transition);
                logger.info("3. ADDING arc between: " + troubledPlace + " and " + transition);
            }

        } else {
            for (PetrinetEdge edgeOut : outEdges) {

                Transition transition = (Transition) edgeOut.getTarget();

                System.out.println("4. ADDING arc between: " + troubledPlace + " and " + transition);
                logger.info("4. ADDING arc between: " + troubledPlace + " and " + transition);
                net.addArc(troubledPlace, transition);
            }
            System.out.println("Removing: " + problematicPlace);
            net.removePlace(problematicPlace);
        }

        PetrinetUtils.exportPetriNetToPNML(fileName, net);
        return net;

    }

    private static List<Transition> precedingTransOfToRemoveHasOneOutgoingArc(PetrinetGraph net, Place toRemove) {
        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = net.getInEdges(toRemove);
        List<Transition> precedingTransitions = new ArrayList<>();
        for (PetrinetEdge inEdge : inEdges) {
            Transition transition = (Transition) inEdge.getSource();
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdgesOfTransition = net.getOutEdges(transition);
            if (outEdgesOfTransition.size() == 1) {
                System.out.println("PRECEDING TRANSITION: " + transition);
                logger.info("PRECEDING TRANSITION: " + transition);
                if (!precedingTransitions.contains(transition)) {
                    precedingTransitions.add(transition);
                }
            }
        }
        return precedingTransitions;
    }

}
