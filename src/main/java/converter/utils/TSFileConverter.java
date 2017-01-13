package converter.utils;

import rationals.Automaton;
import rationals.State;
import rationals.Transition;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Set;

/**
 * Created by arnelaponin on 12/01/2017.
 */
public class TSFileConverter {

    public static void TS2File(Automaton trimmedIntersection, String fileName) {
        String alphabet = trimmedIntersection.alphabet().toString().replace("[","").replace("]","").replace(",","");

        StringBuilder graphString = new StringBuilder(".dummy ");
        graphString.append(alphabet).append("\n");
        graphString.append(".state graph\n");

        String initialState = null;

        Set<State> states = trimmedIntersection.states();
        for (State state : states) {

            if (state.isInitial()) {
                initialState = state.toString();
            }

            Set<Transition> transitions = trimmedIntersection.delta(state);
            for (Transition t : transitions) {
                graphString.append("s").append((t.start()).toString());
                State arrivalState = t.end();
                String reducedLabel = t.label().toString().replace("[","").replace("]","");
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
