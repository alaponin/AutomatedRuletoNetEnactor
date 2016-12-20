package converter.automaton;

import automaton.PossibleWorldWrap;
import converter.utils.AutomatonBuilder;
import converter.utils.AutomatonUtils;
import rationals.Automaton;
import rationals.Transition;

import java.util.Set;
import java.util.Stack;

/**
 * Created by arnelaponin on 05/10/2016.
 */
public class Explorer {

    private final Automaton original;
    private final Automaton product;
    private Stack<StatePair> visited;
    private Stack<StatePair> toBeVisited;

    public Explorer(Automaton original, Automaton product) {
        this.original = original;
        this.product = product;
        visited = new Stack<>();
        toBeVisited = new Stack<>();
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws Exception {
        toBeVisited = AutomatonUtils.getInitialStatePairInStack(original, product);
    }

    public MyAutomaton addMarkingsFromOriginal() throws Exception {
        AutomatonBuilder automatonBuilder = new AutomatonBuilder(original);
        while (!toBeVisited.isEmpty()) {
            StatePair statePair = toBeVisited.pop();
            visited.push(statePair);
            Set<Transition> originalOutgoingTransitions = original.delta(statePair.getS1());
            Set<Transition> productOutgoingTransitions = product.delta(statePair.getS2());

            for (Transition productTransition : productOutgoingTransitions) {
                for (Transition originalTransition : originalOutgoingTransitions) {
                    if (productTransition.label().equals(originalTransition.label())) {
                        toBeVisited.push(new StatePair(originalTransition.end(), productTransition.end()));
                        MarkingStateFactory.MarkingState markingStateOriginal = (MarkingStateFactory.MarkingState) statePair.getS1();
                        MarkingStateFactory.MarkingState markingStateTarget = (MarkingStateFactory.MarkingState) originalTransition.end();
                        String label = getPossibleWorldWrapLabel((PossibleWorldWrap) productTransition.label());
                        automatonBuilder.addTransition(markingStateOriginal.getMarking(),
                                label,
                                markingStateTarget.getMarking());
                    }
                }
            }
        }
        return automatonBuilder.getAutomaton();
    }

    private static String getPossibleWorldWrapLabel(PossibleWorldWrap psw) {
        String pswString = psw.toString();
        return pswString.substring(1, pswString.length()-1);
    }
}
