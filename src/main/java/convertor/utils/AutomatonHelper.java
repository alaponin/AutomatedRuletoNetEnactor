package convertor.utils;

import automaton.PossibleWorldWrap;
import automaton.TransitionLabel;
import convertor.automaton.MarkingStateFactory;
import convertor.automaton.MarkingStateFactory.MarkingState;
import net.sf.tweety.logics.pl.semantics.PossibleWorld;
import net.sf.tweety.logics.pl.syntax.Proposition;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;
import rationals.Automaton;
import rationals.Transition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by arnelaponin on 05/09/16.
 */
public class AutomatonHelper {

    private Automaton automaton;
    private MarkingStateFactory stateFactory;
    private PetrinetGraph petrinet;
    private Set<TransitionLabel> transitionLabels;

    public AutomatonHelper(PetrinetGraph petrinet) {
        this.stateFactory = new MarkingStateFactory();
        this.automaton = new Automaton(stateFactory);
        stateFactory.setAutomaton(automaton);
        this.petrinet = petrinet;

        Collection<Place> initialPlaces = new ArrayList<Place>();
        initialPlaces.add(PetrinetUtils.getStartPlace(petrinet));

        Marking initialMarking = new Marking(initialPlaces);
        Marking finalMarking = new Marking(PetrinetUtils.getFinalPlace(petrinet));

        //Have to be created
        MarkingState initialState = (MarkingState) stateFactory.create(true, false, initialMarking);
        MarkingState finalState = (MarkingState) stateFactory.create(false, true, finalMarking);

        transitionLabels = new HashSet<TransitionLabel>();
        for (Proposition p : PetrinetUtils.getAllTransitionLabels(petrinet)) {
            Set<Proposition> pr = new HashSet<Proposition>();
            pr.add(p);
            transitionLabels.add(new PossibleWorldWrap(new PossibleWorld(pr)));
        }
    }

    public void addTransition(Marking source, String transitionLabel, Marking target) throws Exception {

        MarkingState sourceState = (MarkingState) stateFactory.create(source);
        MarkingState targetState = (MarkingState) stateFactory.create(target);

        Transition<TransitionLabel> t = new Transition<TransitionLabel>(sourceState, getTransitionLabel(transitionLabel), targetState);
        automaton.addTransition(t);
    }

    private TransitionLabel getTransitionLabel(String label) throws Exception {
        TransitionLabel transitionLabel = null;
        Proposition proposition = new Proposition(label);
        Set<Proposition> pr = new HashSet<Proposition>();
        pr.add(proposition);
        for (TransitionLabel tl : transitionLabels) {
            if (tl.equals(new PossibleWorldWrap(new PossibleWorld(pr)))) {
                transitionLabel = tl;
            }
        }
        if (transitionLabel == null) {
            throw new Exception("No such label in the petri net.");
        }
        return transitionLabel;
    }

    public Automaton getAutomaton() {
        return automaton;
    }

    public boolean checkForFinalMarking(Marking marking) {
        Marking finalMarking = new Marking(PetrinetUtils.getFinalPlace(petrinet));
        return finalMarking.baseSet().containsAll(marking.baseSet());
    }

}
