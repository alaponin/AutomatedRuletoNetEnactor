package converter.automaton;

import automaton.PossibleWorldWrap;
import converter.petrinet.CanNotConvertPNToAutomatonException;
import converter.petrinet.NoLabelInPetriNetException;
import converter.petrinet.NumberOfStatesDoesNotMatchException;
import converter.utils.*;
import main.LTLfAutomatonResultWrapper;
import net.sf.tweety.logics.pl.syntax.Proposition;
import net.sf.tweety.logics.pl.syntax.PropositionalSignature;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.IllegalTransitionException;
import rationals.Automaton;
import rationals.NoSuchStateException;
import rationals.State;
import rationals.Transition;

import java.util.*;

/**
 * Created by arnelaponin on 19/12/2016.
 */
public class InformationWrapper {

    private static Logger logger = LogManager.getLogger(InformationWrapper.class);

    private String formula;
    PropositionalSignature signature;
    LTLfAutomatonResultWrapper ltlfARW;
    private Automaton declarative;
    private PetrinetGraph net;
    private MyAutomaton procedural;
    private Automaton reducedIntersection;
    private MyAutomaton trimmedIntersectionWithMarkings;
    private List<State> badStates;
    private List<State> goodStates;
    private Map<State, List<Transition>> semiBadStates;
    private Map<State, List<Transition>> badStatesWithTransitions;
    private Stack<SemiBadFront> fronts;
    private Boolean hasCycle;

    public InformationWrapper(String formula, PetrinetGraph net) throws Exception {
        this.formula = formula;
        signature = new PropositionalSignature();
        List<Proposition> netPropositions = PetrinetUtils.getAllTransitionLabels(net);
        signature.addAll(netPropositions);

        //Automaton of the declarative model.
        ltlfARW = AutomatonOperationUtils.createDefaultLtlAutomaton(signature, formula);

        this.declarative = ltlfARW.getAutomaton();
        utils.AutomatonUtils.printAutomaton(declarative, "automatons/automaton_declarative.gv");
        this.updateInformation(net);
    }

    private void updateInformation(PetrinetGraph updatedNet) throws NoSuchStateException, NoLabelInPetriNetException, CanNotConvertPNToAutomatonException, NumberOfStatesDoesNotMatchException, IllegalTransitionException {
        this.net = updatedNet;

        logger.info("Starting to create procedural: ");
        this.procedural = createProceduralAutomaton(updatedNet);
        hasCycle = automatonHasCycle(this.procedural);

        logger.info("Automaton has cycles: " + hasCycle);
        String proceduralFileName = "automatons/automaton_procedural_" + procedural.hashCode() + "_.gv";
        utils.AutomatonUtils.printAutomaton(procedural, proceduralFileName);
        this.reducedIntersection = createReducedIntersection(this.procedural, this.declarative);
        String intersectionFileName = "automatons/automaton_intersection_" + reducedIntersection.hashCode() + "_.gv";
        utils.AutomatonUtils.printAutomaton(reducedIntersection, intersectionFileName);

        logger.info("Reduced done ...");
        this.badStates = findBadStates(this.reducedIntersection);

        logger.info("Bad states done ...");
        this.goodStates = findGoodStates(this.reducedIntersection);

        logger.info("Good states done ...");
        this.semiBadStates = findSemiBadStates(this.reducedIntersection);

        logger.info("Semi-bad states done ...");


        if (!reducedIntersection.terminals().isEmpty()) {
            this.badStatesWithTransitions = getBadStatesWithTransitions(this.reducedIntersection);
        }

        logger.info("Bad states with transitions done ...");
        this.trimmedIntersectionWithMarkings = findTrimmedIntersectionWithMarkings();

        logger.info("Trimmed done ...");
        fronts = new Stack<>();

        logger.info("Fronts done ...");
    }

    public String getFormula() {
        return formula;
    }

    public PropositionalSignature getSignature() {
        return signature;
    }

    public LTLfAutomatonResultWrapper getLtlfARW() {
        return ltlfARW;
    }

    public PetrinetGraph getNet() {
        return net;
    }

    public MyAutomaton getProceduralAutomaton() {
        return procedural;
    }

    public Automaton getDeclarativeAutomaton() {
        return declarative;
    }

    public Automaton getReducedIntersection() {
        return reducedIntersection;
    }

    public List<State> getBadStates() {
        return this.badStates;
    }

    public Map<State, List<Transition>> getSemiBadStates() {
        return semiBadStates;
    }

    public Map<State, List<Transition>> getBadStatesWithTransitions() {
        return badStatesWithTransitions;
    }

    public MyAutomaton getTrimmedIntersectionWithMarkings() {
        return trimmedIntersectionWithMarkings;
    }

    private MyAutomaton createProceduralAutomaton(PetrinetGraph net) throws NoSuchStateException, NoLabelInPetriNetException, CanNotConvertPNToAutomatonException, IllegalTransitionException {
        TSGenerator tsGenerator = new TSGenerator();
        return tsGenerator.createAutomaton((Petrinet) net);
    }

    private Automaton createReducedIntersection(MyAutomaton procedural, Automaton declarative) {
        Automaton intersection = AutomatonOperationUtils.getIntersection(procedural, declarative);
        return AutomatonOperationUtils.getReduced(intersection);
    }

    public SemiBadFront getFirstSemiBadFront() {
        SemiBadFront firstSemiBadFront = new SemiBadFront(semiBadStates, this.getSemiBadMarkingsFromIntersection());
        fronts.add(firstSemiBadFront);
        return firstSemiBadFront;
    }

    private List<State> findBadStates(Automaton reducedIntersection) {
        Queue<State> toBeVisited = new LinkedList<>();
        List<State> visited = new ArrayList<>();
        toBeVisited.addAll(reducedIntersection.initials());
        List<State> badStates = new ArrayList<>();
        while (!toBeVisited.isEmpty()) {
            State currentState = toBeVisited.poll();
            visited.add(currentState);
            Set<Transition> outGoingTransitions = reducedIntersection.delta(currentState);
            for (Transition currentTransition : outGoingTransitions) {
                State targetState = currentTransition.end();
                Set<State> accessibleStates = reducedIntersection.accessibleStates(targetState);
                if (!accessibleStates.containsAll(reducedIntersection.terminals())) {
                    if (!badStates.contains(targetState)) {
                        badStates.add(targetState);
                    }
                    for (State accessibleState : accessibleStates) {
                        if (!badStates.contains(accessibleState)) {
                            badStates.add(accessibleState);
                        }
                    }
                    for (State badState : badStates) {
                        if (!visited.contains(badState)) {
                            visited.add(badState);
                        }
                    }
                } else {
                    if (!toBeVisited.contains(targetState) && !badStates.contains(targetState) && !visited.contains(targetState)) {
                        toBeVisited.add(targetState);
                    }

                }
            }
        }

        return badStates;
    }

    private List<State> findGoodStates(Automaton reducedIntersection) {
        Queue<State> toBeVisited = new LinkedList<>();
        List<State> visited = new ArrayList<>();
        toBeVisited.addAll(reducedIntersection.initials());
        List<State> goodStates = new ArrayList<>();
        while (!toBeVisited.isEmpty()) {
            State currentState = toBeVisited.poll();
            visited.add(currentState);
            Set<Transition> outGoingTransitions = reducedIntersection.delta(currentState);
            for (Transition currentTransition : outGoingTransitions) {
                State targetState = currentTransition.end();
                Set<State> accessibleStates = reducedIntersection.accessibleStates(targetState);
                if (accessibleStates.containsAll(reducedIntersection.terminals()) && !setContainsSubList(accessibleStates, this.badStates)) {
                    if (!goodStates.contains(targetState)) {
                        goodStates.add(targetState);
                    }
                    for (State s : accessibleStates) {
                        if (!goodStates.contains(s)) {
                            goodStates.add(s);
                        }
                    }
                    for (State goodState : goodStates) {
                        if (!visited.contains(goodState)) {
                            visited.add(goodState);
                        }
                    }
                }  else {
                    if (!toBeVisited.contains(targetState) && !goodStates.contains(targetState) && !visited.contains(targetState)) {
                        toBeVisited.add(targetState);
                    }
                }

            }
        }

        return goodStates;
    }

    private Map<State, List<Transition>> findSemiBadStates(Automaton reducedIntersection) {
        Map<State, List<Transition>> semiBadStates = new HashMap<>();

        Set<State> allStates = reducedIntersection.states();

        List<State> potentialSemiBadStates = new ArrayList<>();
        for (State s : allStates) {
            if (!goodStates.contains(s) && !badStates.contains(s)) {
                potentialSemiBadStates.add(s);
            }
         }
        for (State potential : potentialSemiBadStates) {
            Set<Transition> transitions = reducedIntersection.delta(potential);

            for (Transition transition : transitions) {
                if (badStates.contains(transition.end())) {
                    insertTransitionToMapList(semiBadStates, potential, transition);
                }
            }
        }

        logger.info("SEMI-BAD STATES: " + semiBadStates);
        return semiBadStates;
    }

    public void colourAutomaton() {
        AutomatonOperationUtils.colorAutomatonStates(this, "automatons/automaton_coloured.gv");
    }

    private MyAutomaton findTrimmedIntersectionWithMarkings() throws NumberOfStatesDoesNotMatchException, NoLabelInPetriNetException, NoSuchStateException {
        Automaton trimIntersection = AutomatonOperationUtils.getTrimmed(reducedIntersection);
        Explorer explorer = new Explorer(procedural, trimIntersection);
        MyAutomaton trimmedWithMarkings = explorer.addMarkingsFromOriginal();
        utils.AutomatonUtils.printAutomaton(trimmedWithMarkings, "automatons/automaton_trimmed_markings.gv");
        return trimmedWithMarkings;
    }

    private Map<State, List<Transition>> getBadStatesWithTransitions(Automaton reducedIntersection) {
        Map<State, List<Transition>> badStatesTransitions = new HashMap<>();
        for (State badState : badStates) {
            Set<Transition> transitions = reducedIntersection.delta(badState);
            int count = 0;
            if (!badState.isTerminal() && transitions.isEmpty()) {
                badStatesTransitions.put(badState, new ArrayList<Transition>());
            }
            for (Transition transition : transitions) {
                if (badStates.contains(transition.end())) {
                    count++;
                }
            }
            if (count == transitions.size()) {
                for (Transition transition : transitions) {
                    insertTransitionToMapList(badStatesTransitions, badState, transition);
                }
            }
        }
        return badStatesTransitions;
    }

    public Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> getSemiBadMarkingsFromOriginal() {
        return getMarkingsBasedOnStates(AutomatonSource.fromOriginal);
    }

    public Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> getSemiBadMarkingsFromIntersection() {
        return getMarkingsBasedOnStates(AutomatonSource.fromIntersection);
    }

    private Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> getMarkingsBasedOnStates(AutomatonSource source) {
        Map<PossibleWorldWrap, Map<Transition, TransitionMarkingPair>> sortedMarkings = new HashMap<>();
        Queue<StatePair> toBeVisited = AutomatonUtils.getInitialStatePairInQueue(procedural, reducedIntersection);
        Map<State, List<Place>> originalMarkings = procedural.getMarkingMap();
        while (!toBeVisited.isEmpty()) {
            StatePair statePair = toBeVisited.poll();
            Set<Transition> originalOutgoingTransitions = procedural.delta(statePair.getS1());
            Set<Transition> intersectionOutgoingTransitions = reducedIntersection.delta(statePair.getS2());
            for (Transition intersectionTransition : intersectionOutgoingTransitions) {
                for (Transition originalTransition : originalOutgoingTransitions) {
                    if (intersectionTransition.label().equals(originalTransition.label())) {
                        toBeVisited.add(new StatePair(originalTransition.end(), intersectionTransition.end()));
                        if (semiBadStates.containsKey(intersectionTransition.start())) {
                            List<Transition> troubledTransitions = semiBadStates.get(intersectionTransition.start());
                            for (Transition t : troubledTransitions) {
                                if (t.label().equals(intersectionTransition.label())) {
                                    MarkingStateFactory.MarkingState start = (MarkingStateFactory.MarkingState) originalTransition.start();
                                    MarkingStateFactory.MarkingState end = (MarkingStateFactory.MarkingState) originalTransition.end();

                                    TransitionMarkingPair markingPair = new TransitionMarkingPair(originalMarkings.get(start), originalMarkings.get(end));

                                    Transition transition = null;

                                    switch(source) {
                                        case fromOriginal:
                                            transition = originalTransition;
                                            break;
                                        case fromIntersection:
                                            transition = intersectionTransition;
                                            break;
                                    }

                                    if (sortedMarkings.containsKey(transition.label())) {
                                        Map<Transition, TransitionMarkingPair> transitionMarkingPairMap = sortedMarkings.get(transition.label());
                                        transitionMarkingPairMap.put(transition, markingPair);
                                        sortedMarkings.put((PossibleWorldWrap) transition.label(), transitionMarkingPairMap);

                                    } else {
                                        Map<Transition, TransitionMarkingPair> transitionMarkingPairMap = new HashMap<>();
                                        transitionMarkingPairMap.put(transition, markingPair);
                                        sortedMarkings.put((PossibleWorldWrap) transition.label(), transitionMarkingPairMap);
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        return sortedMarkings;
    }

    private static Map<State, List<Transition>> insertTransitionToMapList(Map<State, List<Transition>> stateTransitionMap, State state, Transition transition) {
        if (stateTransitionMap.containsKey(state)) {
            List<Transition> t = stateTransitionMap.get(state);
            t.add(transition);
            stateTransitionMap.put(state, t);
        } else {
            List<Transition> t = new ArrayList<>();
            t.add(transition);
            stateTransitionMap.put(state, t);
        }
        return stateTransitionMap;
    }

    private static boolean setContainsSubList(Set<State> s1, List<State> l) {
        for (State state : l) {
            if (s1.contains(state)) {
                return true;
            }
        }
        return false;
    }

    private boolean automatonHasCycle(MyAutomaton automaton) {
        TarjanAlgorithm tarjanAlgorithm = new TarjanAlgorithm(automaton);
        return tarjanAlgorithm.getNumberOfComponents() != automaton.states().size();
    }

    public boolean hasCycles() {
        return hasCycle;
    }
}
