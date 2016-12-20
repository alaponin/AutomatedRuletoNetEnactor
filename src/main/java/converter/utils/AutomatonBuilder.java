package converter.utils;

import automaton.PossibleWorldWrap;
import automaton.TransitionLabel;
import converter.automaton.MarkingStateFactory;
import converter.automaton.MarkingStateFactory.MarkingState;
import converter.automaton.MyAutomaton;
import net.sf.tweety.logics.pl.semantics.PossibleWorld;
import net.sf.tweety.logics.pl.syntax.Proposition;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;

import rationals.Automaton;
import rationals.State;
import rationals.Transition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by arnelaponin on 05/09/16.
 */
public class AutomatonBuilder {

    private MyAutomaton automaton;
    private MarkingStateFactory stateFactory;
    private Set<TransitionLabel> transitionLabels;

    public AutomatonBuilder(PetrinetGraph petrinet) {
        this.stateFactory = new MarkingStateFactory();
        this.automaton = new MyAutomaton(stateFactory);
        stateFactory.setAutomaton(automaton);

        Collection<Place> initialPlaces = new ArrayList<Place>();
        Collection<Place> finalPlaces = new ArrayList<Place>();
        initialPlaces.add(PetrinetUtils.getStartPlace(petrinet));
        finalPlaces.add(PetrinetUtils.getFinalPlace(petrinet));

        Marking initialMarking = new Marking(initialPlaces);
        Marking finalMarking = new Marking(finalPlaces);

        //Have to be created
        MarkingState initialState = (MarkingState) stateFactory.create(true, false, initialMarking);
        MarkingState finalState = (MarkingState) stateFactory.create(false, true, finalMarking);

        transitionLabels = new HashSet<>();
        for (Proposition p : PetrinetUtils.getAllTransitionLabels(petrinet)) {
            Set<Proposition> pr = new HashSet<>();
            pr.add(p);
            transitionLabels.add(new PossibleWorldWrap(new PossibleWorld(pr)));
        }
    }

    public AutomatonBuilder(Automaton originalAutomaton) {
        this.stateFactory = new MarkingStateFactory();
        this.automaton = new MyAutomaton(stateFactory);
        stateFactory.setAutomaton(automaton);

        Marking initialMarking = null;
        Marking finalMarking = null;
        Set<MarkingState> initialStates = originalAutomaton.initials();
        for (MarkingState s : initialStates) {
            initialMarking = s.getMarking();
        }
        Set<MarkingState> finalStates = originalAutomaton.terminals();
        for (MarkingState s : finalStates) {
            finalMarking = s.getMarking();
        }

        //Have to be created
        MarkingState initialState = (MarkingState) stateFactory.create(true, false, initialMarking);
        MarkingState finalState = (MarkingState) stateFactory.create(false, true, finalMarking);

        transitionLabels = new HashSet<>();
        originalAutomaton.delta();
        Set<Transition> transitions = originalAutomaton.delta();
        transitionLabels.addAll(transitions.stream().map(t -> (PossibleWorldWrap) t.label()).collect(Collectors.toList()));

    }

    public void addTransition(Marking source, String transitionLabel, Marking target) throws Exception {

        MarkingState sourceState = (MarkingState) stateFactory.create(source);
        MarkingState targetState = (MarkingState) stateFactory.create(target);

        addTransitionToAutomaton(transitionLabel, sourceState, targetState);
    }

    private void addTransitionToAutomaton(String transitionLabel, MarkingState sourceState, MarkingState targetState) throws Exception {
        Transition<TransitionLabel> t = new Transition<>(sourceState, getTransitionLabel(transitionLabel), targetState);
        automaton.addTransition(t);
        automaton.addMarkingList(sourceState, new ArrayList<>(sourceState.getMarking().baseSet()));
        automaton.addMarkingList(targetState, new ArrayList<>(targetState.getMarking().baseSet()));
    }

    private TransitionLabel getTransitionLabel(String label) throws Exception {
        TransitionLabel transitionLabel = null;
        Proposition proposition = new Proposition(label);
        Set<Proposition> pr = new HashSet<Proposition>();
        pr.add(proposition);
        for (TransitionLabel tl : transitionLabels) {
            PossibleWorldWrap psw = new PossibleWorldWrap(new PossibleWorld(pr));
            if (tl.equals(psw)) {
                transitionLabel = tl;
            }
        }
        if (transitionLabel == null) {
            throw new Exception("No such label in the petri net.");
        }
        return transitionLabel;
    }

    public MyAutomaton getAutomaton() {
        return automaton;
    }

}