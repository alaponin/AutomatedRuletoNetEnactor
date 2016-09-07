package convertor;

import convertor.utils.AutomatonHelper;
import convertor.utils.ExecutionResult;
import convertor.utils.PetrinetUtils;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetExecutionInformation;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import rationals.Automaton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by arnelaponin on 07/09/16.
 */
public class PNAutomatonConverter {

    PetrinetGraph net;
    Queue<Place> placesToVisit;
    Queue<Marking> markingHistory;
    PetrinetSemantics semantics;


    public PNAutomatonConverter(PetrinetGraph net) {
        this.net = net;
        placesToVisit = new LinkedList<Place>();
        markingHistory = new LinkedList<Marking>();
        semantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
        init();
    }

    private void init() {
        Place startPlace = PetrinetUtils.getStartPlace(net);
        placesToVisit.add(startPlace);
        Collection<Place> initialPlaces = new ArrayList<Place>();
        initialPlaces.add(startPlace);

        Marking initialMarking = new Marking(initialPlaces);

        markingHistory.add(initialMarking);

        semantics.initialize(net.getTransitions(), initialMarking);
    }

    public Automaton convertToAutomaton() throws Exception {
        AutomatonHelper automatonUtils = new AutomatonHelper(net);

        while (!placesToVisit.isEmpty()) {
            Place place = placesToVisit.poll();
            Marking prevMarking = markingHistory.poll();
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgesOutP = net.getOutEdges(place);

            for (PetrinetEdge edgeOut : edgesOutP) {
                Transition transition = (Transition) edgeOut.getTarget();
                //Setting historic marking in order to visit transitions that have the same preceding place
                semantics.setCurrentState(prevMarking);

                ExecutionResult executionResult = new ExecutionResult(transition, semantics);
                boolean successfulExecution = executionResult.execute();
                //If the execution of the of the transition fails, then it is placed
                //at the end queue, because the problem could have been the lack of tokens in the preceding places
                if (successfulExecution) {
                    PetrinetExecutionInformation executionInformation = executionResult.getExecutionInformation();

                    automatonUtils.addTransition(prevMarking, transition.getLabel(), semantics.getCurrentState());
                    Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgesOutT = net.getOutEdges(transition);
                    //If there are branches then place and the context are added to the queues so that they could be visited later
                    for (PetrinetEdge e : edgesOutT) {
                        placesToVisit.add((Place) e.getTarget());
                        markingHistory.add(semantics.getCurrentState());
                    }
                    //Queues are cleared so that there would not be indefinite loops, because of the places added, while transition firing failed
                    if (automatonUtils.checkForFinalMarking(semantics.getCurrentState())) {
                        placesToVisit.clear();
                        markingHistory.clear();
                    }
                } else {
                    placesToVisit.add(place);
                    markingHistory.add(semantics.getCurrentState());
                }

            }
        }

        return automatonUtils.getAutomaton();
    }
}
