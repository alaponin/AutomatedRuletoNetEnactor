package converter.utils;

import converter.automaton.MyAutomaton;
import converter.epsilon.MyEpsilonTransitionRemover;
import converter.petrinet.CanNotConvertPNToAutomatonException;
import main.LTLfAutomatonResultWrapper;
import net.sf.tweety.logics.pl.syntax.Proposition;
import net.sf.tweety.logics.pl.syntax.PropositionalSignature;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import rationals.Automaton;
import rationals.State;
import rationals.Transition;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * Created by arnelaponin on 12/01/2017.
 */
public class TSFileConverter {

    /**
     * Method creates a Transition System file from an automaton. Invisible transitions are removed.
     * @param net Petri Net model.
     * @param formula Declare formula.
     * @param fileName file name where the transition system will be saved.
     * @throws CanNotConvertPNToAutomatonException
     */
    public static String TS2File(Petrinet net, String formula, String fileName) throws CanNotConvertPNToAutomatonException {
        net = PetrinetUtils.setLabellessTransitionsInvisible(net);
        MyAutomaton netAutomatonWithoutHiddenTransitions = TSFactory.getAutomatonWithoutHiddenTransitions(net);
        MyEpsilonTransitionRemover orgEpsilonTransitionRemover = new MyEpsilonTransitionRemover();
        Automaton netAutomaton = orgEpsilonTransitionRemover.transform(netAutomatonWithoutHiddenTransitions);

        PropositionalSignature signature = new PropositionalSignature();
        List<Proposition> netPropositions = PetrinetUtils.getAllTransitionLabels(net);
        signature.addAll(netPropositions);

        LTLfAutomatonResultWrapper ltlfARW = AutomatonOperationUtils.createDefaultLtlAutomaton(signature, formula);
        Automaton intersection = AutomatonOperationUtils.getIntersection(netAutomaton, ltlfARW.getAutomaton());
        TS2File(AutomatonOperationUtils.getTrimmed(intersection), fileName);
        return fileName;
    }

    /**
     * Method creates a Transition System file from an automaton. Invisible transitions (transitions without a label)
     * are given labels starting with 'A' and a number e.g. 'A1'.
     * @param trimmedIntersection Automaton, which is the trimmed intersection of the procedural and declarative models.
     * @param fileName Name of the saved file.
     */
    public static void TS2File(Automaton trimmedIntersection, String fileName) {
        String alphabet = trimmedIntersection.alphabet().toString().replace("[","").replace("]","").replace(",","");

        StringBuilder graphString = new StringBuilder(".dummy ");
        graphString.append(alphabet);
        Set<Transition> allTransitions = trimmedIntersection.delta();
        int transitionCount = 0;
        for (Transition t : allTransitions) {
            String reducedLabel = t.label().toString().replace("[","").replace("]","");
            if (reducedLabel.equals("")) {
                transitionCount++;
                String invisibleTransition = " A" + transitionCount;
                graphString.append(invisibleTransition);
            }
        }
        graphString.append("\n");
        graphString.append(".state graph\n");

        String initialState = null;

        Set<State> states = trimmedIntersection.states();
        int invisibleTransitionCount = 0;
        for (State state : states) {
            if (state.isInitial()) {
                initialState = state.toString();
            }

            Set<Transition> transitions = trimmedIntersection.delta(state);
            for (Transition t : transitions) {
                graphString.append("s").append((t.start()).toString());
                State arrivalState = t.end();
                String reducedLabel = t.label().toString().replace("[","").replace("]","");
                if (reducedLabel.equals("")) {
                    invisibleTransitionCount++;
                    reducedLabel = "A" + invisibleTransitionCount;
                }
                String transitionLabel = " " + reducedLabel + " ";
                graphString.append(transitionLabel).append("s").append(arrivalState.toString());
                graphString.append("\n");
            }
        }
        graphString.append(".marking {s").append(initialState).append("}\n");
        graphString.append(".end").append("\n");

        String content = graphString.toString();

        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        PrintStream ps = new PrintStream(fos);
        ps.println(content);
        ps.flush();
        ps.close();

    }
}
