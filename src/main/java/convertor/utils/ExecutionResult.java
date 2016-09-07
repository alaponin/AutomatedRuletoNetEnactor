package convertor.utils;

import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.PetrinetExecutionInformation;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;

/**
 * Created by arnelaponin on 06/09/16.
 */
public class ExecutionResult {

    boolean success;
    Transition transition;
    PetrinetSemantics semantics;
    PetrinetExecutionInformation executionInformation;

    public ExecutionResult(Transition transition, PetrinetSemantics semantics) {
        this.transition = transition;
        this.semantics = semantics;
        this.executionInformation = null;
    }

    public PetrinetExecutionInformation getExecutionInformation() {
        return executionInformation;
    }

    public boolean execute() {
        try {
            executionInformation = (PetrinetExecutionInformation) semantics.executeExecutableTransition(transition);
            success = true;
        } catch (IllegalTransitionException e) {
            success = false;
        }
        return success;
    }
}
