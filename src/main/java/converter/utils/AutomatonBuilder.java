package converter.utils;

import automaton.PossibleWorldWrap;
import automaton.TransitionLabel;
import converter.automaton.MarkingStateFactory;
import converter.automaton.MarkingStateFactory.MarkingState;
import converter.automaton.MyAutomaton;
import converter.petrinet.NoLabelInPetriNetException;
import net.sf.tweety.logics.pl.semantics.PossibleWorld;
import net.sf.tweety.logics.pl.syntax.Proposition;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;

import rationals.Automaton;
import rationals.NoSuchStateException;
import rationals.Transition;

import java.util.*;
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
        List<PossibleWorldWrap> pwwTransitions = new ArrayList<>();
        for (Transition t : transitions) {
            pwwTransitions.add((PossibleWorldWrap) t.label());
        }
        transitionLabels.addAll(pwwTransitions);

    }

    public void addTransition(Marking source, String transitionLabel, Marking target) throws NoLabelInPetriNetException, NoSuchStateException {

        MarkingState sourceState = (MarkingState) stateFactory.create(source);
        MarkingState targetState = (MarkingState) stateFactory.create(target);

        addTransitionToAutomaton(transitionLabel, sourceState, targetState);
    }

    private void addTransitionToAutomaton(String transitionLabel, MarkingState sourceState, MarkingState targetState) throws NoLabelInPetriNetException, NoSuchStateException {
        Transition<TransitionLabel> t = new Transition<>(sourceState, getTransitionLabel(transitionLabel), targetState);
        automaton.addTransition(t);
        automaton.addMarkingList(sourceState, new ArrayList<>(sourceState.getMarking().baseSet()));
        automaton.addMarkingList(targetState, new ArrayList<>(targetState.getMarking().baseSet()));
    }

    private TransitionLabel getTransitionLabel(String label) throws NoLabelInPetriNetException {
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

        return transitionLabel;
    }

    public MyAutomaton getAutomaton() {
        return automaton;
    }

}
