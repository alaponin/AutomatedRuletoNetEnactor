package converter.petrinet;

import org.processmining.models.graphbased.directed.AbstractDirectedGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.ExpandableSubNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

/**
 * Created by arnelaponin on 04/11/2016.
 */
public class TransitionClone extends Transition {

    public TransitionClone(String label, AbstractDirectedGraph<PetrinetNode, PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> net) {
        super(label, net);
    }

    public TransitionClone(String label, AbstractDirectedGraph<PetrinetNode, PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> net, ExpandableSubNet parent) {
        super(label, net, parent);
    }
}
