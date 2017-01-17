package converter.utils;

import automaton.PossibleWorldWrap;
import net.sf.tweety.logics.pl.syntax.Proposition;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.*;

/**
 * Created by arnelaponin on 17/01/2017.
 */
public class TarjanAlgorithmPN {

    private Collection<Transition> transitions;
    private Map<Transition, Integer> lowLinkMap;
    private Map<Transition, Integer> ssc;
    private List<Transition> visited;
    private Stack<Transition> stack;
    private Integer pre;
    private Integer count;

    public TarjanAlgorithmPN(PetrinetGraph net) {
        transitions = net.getTransitions();
        lowLinkMap = new HashMap<>();
        ssc = new HashMap<>();
        visited = new ArrayList<>();
        stack = new Stack<>();
        pre = 0;
        count = 0;
        for (Transition transition : transitions) {
            lowLinkMap.put(transition,0);
        }
        for (Transition transition : transitions) {
            if (!visited.contains(transition)) {
                dfs(net, transition);
            }
        }
    }

    private void dfs(PetrinetGraph net, Transition transition) {
        visited.add(transition);
        lowLinkMap.put(transition, pre++);
        int min = lowLinkMap.get(transition);
        stack.push(transition);
        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = net.getOutEdges(transition);
        for (PetrinetEdge edge : outEdges) {
            Place place = (Place) edge.getTarget();
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdgesFromPlace = net.getOutEdges(place);
            for (PetrinetEdge edgeFromPlace : outEdgesFromPlace) {
                Transition targetTransition = (Transition) edgeFromPlace.getTarget();
                if (!visited.contains(targetTransition)) {
                    dfs(net, targetTransition);
                }
                if (lowLinkMap.get(targetTransition) < min) {
                    min = lowLinkMap.get(targetTransition);
                }
            }

        }
        if (min < lowLinkMap.get(transition)) {
            lowLinkMap.put(transition, min);
            return;
        }

        Transition anotherTransition;
        do {
            anotherTransition = stack.pop();
            ssc.put(anotherTransition, count);
            lowLinkMap.put(anotherTransition, Integer.MAX_VALUE);

        } while (!anotherTransition.equals(transition));
        count++;
    }

    public Map<Transition, Integer> getIdMap() {
        return ssc;
    }

    public Integer getNumberOfComponents() {
        return count;
    }

    private Integer whichCycleIsTransitionPartOf(String transitionLabel) {
        Integer sscNumber = null;
        for (Map.Entry<Transition, Integer> entry : ssc.entrySet()) {
            Transition transition = entry.getKey();
            if (transitionLabel.equals(transition.getLabel())) {
                sscNumber = entry.getValue();
            }
        }
        System.out.println(sscNumber);
        return sscNumber;
    }

    public boolean areTwoTransitionsPartOfSameCycle(String transitionLabel1, String transitionLabel2) {
        return whichCycleIsTransitionPartOf(transitionLabel1).equals(whichCycleIsTransitionPartOf(transitionLabel2));
    }
}
