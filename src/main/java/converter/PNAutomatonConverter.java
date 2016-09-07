package converter;

import converter.utils.AutomatonHelper;
import converter.utils.PetrinetUtils;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetExecutionInformation;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import rationals.Automaton;

import java.util.*;

/**
 * Created by arnelaponin on 07/09/16.
 */
public class PNAutomatonConverter {

    PetrinetGraph net;
    PetrinetSemantics semantics;
    Queue<Marking> markingsToVisit;
    List<Marking> visitedMarkings;


    public PNAutomatonConverter(PetrinetGraph net) {
        this.net = net;
        markingsToVisit = new LinkedList<Marking>();
        visitedMarkings = new ArrayList<Marking>();
        semantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
        init();
    }

    private void init() {
        Place startPlace = PetrinetUtils.getStartPlace(net);
        Collection<Place> initialPlaces = new ArrayList<Place>();
        initialPlaces.add(startPlace);

        Marking initialMarking = new Marking(initialPlaces);

        markingsToVisit.add(initialMarking);

        semantics.initialize(net.getTransitions(), initialMarking);
    }

    public Automaton convertToAutomaton() throws Exception {
        AutomatonHelper automatonHelper = new AutomatonHelper(net);

        //Algorithm taken from: http://cpntools.org/_media/book/covgraph.pdf (page 33)
        while(!markingsToVisit.isEmpty()) {
            Marking marking = markingsToVisit.poll();
            visitedMarkings.add(marking);
            semantics.setCurrentState(marking);
            List<Transition> executableTransitions = (List<Transition>) semantics.getExecutableTransitions();
            for (Transition transition : executableTransitions) {
                PetrinetExecutionInformation executionInformation = (PetrinetExecutionInformation) semantics.executeExecutableTransition(transition);
                if (!visitedMarkings.contains(semantics.getCurrentState())) {
                    markingsToVisit.add(semantics.getCurrentState());
                }
                automatonHelper.addTransition(marking, transition.getLabel(), semantics.getCurrentState());
                semantics.setCurrentState(marking);
            }
        }

        return automatonHelper.getAutomaton();
    }
}
