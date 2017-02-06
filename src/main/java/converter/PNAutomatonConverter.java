package converter;

import converter.automaton.MyAutomaton;
import converter.petrinet.CanNotConvertPNToAutomatonException;
import converter.petrinet.NoLabelInPetriNetException;
import converter.utils.AutomatonBuilder;
import converter.utils.PetrinetUtils;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetExecutionInformation;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import rationals.NoSuchStateException;

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
        markingsToVisit = new LinkedList<>();
        visitedMarkings = new ArrayList<>();
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

    public MyAutomaton convertToAutomaton() throws NoLabelInPetriNetException, NoSuchStateException, CanNotConvertPNToAutomatonException, IllegalTransitionException {
        AutomatonBuilder automatonBuilder = new AutomatonBuilder(net);

        //Algorithm taken from: http://cpntools.org/_media/book/covgraph.pdf (page 33)
        while(!markingsToVisit.isEmpty()) {
            Marking marking = markingsToVisit.poll();
            visitedMarkings.add(marking);
            semantics.setCurrentState(marking);
            List<Transition> executableTransitions = (List<Transition>) semantics.getExecutableTransitions();
            for (Transition transition : executableTransitions) {
                PetrinetExecutionInformation executionInformation = (PetrinetExecutionInformation) semantics.executeExecutableTransition(transition);
                Marking currentState = semantics.getCurrentState();

                Iterator<Place> placeIterator = currentState.iterator();
                List<Place> markingPlace = new ArrayList<>();
                while (placeIterator.hasNext()) {
                    Place place = placeIterator.next();
                    markingPlace.add(place);
                }
                Set<Place> markingSet = new HashSet<>(markingPlace);
                if (markingSet.size() < markingPlace.size()) {
                    throw new CanNotConvertPNToAutomatonException("There are deadlocks in the Petri net.");
                }
                if (!visitedMarkings.contains(semantics.getCurrentState())) {
                    markingsToVisit.add(semantics.getCurrentState());
                }
                automatonBuilder.addTransition(marking, transition.getLabel(), semantics.getCurrentState());
                semantics.setCurrentState(marking);
            }
        }

        return automatonBuilder.getAutomaton();
    }
}
