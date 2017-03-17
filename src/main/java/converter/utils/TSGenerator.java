package converter.utils;

import converter.automaton.MyAutomaton;
import converter.petrinet.CanNotConvertPNToAutomatonException;
import converter.petrinet.NoLabelInPetriNetException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.transitionsystem.ReachabilityGraph;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetExecutionInformation;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import rationals.NoSuchStateException;

import java.util.*;

/**
 * Created by arnelaponin on 07/02/2017.
 * Code taken from ProM plugin PNAnalysis written by A. Adriansyah
 * Available: https://svn.win.tue.nl/repos/prom/Packages/PNAnalysis/Trunk/src/org/processmining/plugins/petrinet/behavioralanalysis/TSGenerator.java
 */
public class TSGenerator {
    private ReachabilityGraph ts;
    private boolean transitionsHidden = false;
    private static Logger logger = LogManager.getLogger(TSGenerator.class.getName());

    private static final int MAXSTATES = 250000;

    public MyAutomaton createAutomaton(Petrinet net) throws CanNotConvertPNToAutomatonException {
        Place startPlace = PetrinetUtils.getStartPlace(net);
        Collection<Place> initialPlaces = new ArrayList<Place>();
        initialPlaces.add(startPlace);

        Marking initialMarking = new Marking(initialPlaces);
        return createAutomaton(net, initialMarking, PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class));
    }

    public MyAutomaton createAutomaton(Petrinet net, boolean transitionsHidden) throws CanNotConvertPNToAutomatonException {
        this.transitionsHidden = transitionsHidden;
        Place startPlace = PetrinetUtils.getStartPlace(net);
        Collection<Place> initialPlaces = new ArrayList<Place>();
        initialPlaces.add(startPlace);

        Marking initialMarking = new Marking(initialPlaces);
        return createAutomaton(net, initialMarking, PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class));
    }

    private MyAutomaton createAutomaton(Petrinet net, Marking state, PetrinetSemantics semantics) throws CanNotConvertPNToAutomatonException {
        semantics.initialize(net.getTransitions(), new Marking(state));
        return buildAndConnect(net, state, semantics);
    }

    private MyAutomaton buildAndConnect(PetrinetGraph net, Marking initial,
                                     Semantics<Marking, Transition> semantics) throws CanNotConvertPNToAutomatonException {
        AutomatonBuilder automatonBuilder = new AutomatonBuilder(net);
        ts = new ReachabilityGraph("StateSpace of " + net.getLabel());

        return doBreadthFirst(initial, semantics, MAXSTATES, automatonBuilder);
    }

    private MyAutomaton doBreadthFirst(Marking state, Semantics<Marking, Transition> semantics, int max, AutomatonBuilder automatonBuilder) throws CanNotConvertPNToAutomatonException {

        Queue<Marking> newStates = new LinkedList<>();
        ts.addState(state);
        newStates.add(state);
        do {
            newStates.addAll(extend(newStates.poll(), semantics, automatonBuilder));
        } while (!newStates.isEmpty() && (automatonBuilder.getAutomaton().states().size() < max));
        if (!newStates.isEmpty()) {
            // This net has been shows to be unbounded on this marking
            logger.error("The behaviour of the given net is has over " + max + " states. Aborting...");
            return null;
        }
        logger.info("Final state-space size: " + automatonBuilder.getAutomaton().states().size() + " states.");
        return automatonBuilder.getAutomaton();
    }

    private Set<Marking> extend(Marking state, Semantics<Marking, Transition> semantics, AutomatonBuilder automatonBuilder) throws CanNotConvertPNToAutomatonException {
        Set<Marking> newStates = new HashSet<>();
        semantics.setCurrentState(state);
        for (Transition t : semantics.getExecutableTransitions()) {
            semantics.setCurrentState(state);
            PetrinetExecutionInformation executionInformation = null;
            try {
                executionInformation = (PetrinetExecutionInformation) semantics.executeExecutableTransition(t);
            } catch (IllegalTransitionException e) {
                assert (false);
            }

            if (executionInformation != null && executionInformation.getTokensConsumed().isEmpty()) {
                logger.error("Net is not sound!");
                throw new CanNotConvertPNToAutomatonException("Net is not sound!");
            }
            Marking newState = semantics.getCurrentState();


            if (ts.addState(newState)) {
                newStates.add(newState);
                int size = ts.getEdges().size();
                if (size % 1000 == 0) {
                    logger.info("Statespace size: " + ts.getStates().size() + " states and " + ts.getEdges().size()
                            + " transitions.");
                }
            }
            ts.addTransition(state, newState, t);

            try {
                if (transitionsHidden) {
                    if (t.isInvisible()) {
                        automatonBuilder.addTransition(state, null, newState);
                    } else {
                        automatonBuilder.addTransition(state, t.getLabel(), newState);
                    }

                } else {
                    automatonBuilder.addTransition(state, t.getLabel(), newState);
                }

            } catch (NoLabelInPetriNetException e) {
                e.printStackTrace();
            } catch (NoSuchStateException e) {
                e.printStackTrace();
            }
            semantics.setCurrentState(state);
        }
        return newStates;

    }
}
