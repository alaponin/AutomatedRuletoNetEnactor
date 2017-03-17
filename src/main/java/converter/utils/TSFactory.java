package converter.utils;

import converter.automaton.MyAutomaton;
import converter.petrinet.CanNotConvertPNToAutomatonException;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;

/**
 * Created by arnelaponin on 10/03/2017.
 */
public class TSFactory {

    public static MyAutomaton getRegularAutomaton(Petrinet net) throws CanNotConvertPNToAutomatonException {
        TSGenerator generator = new TSGenerator();
        return generator.createAutomaton(net);
    }

    public static MyAutomaton getAutomatonWithoutHiddenTransitions(Petrinet net) throws CanNotConvertPNToAutomatonException {
        TSGenerator generator = new TSGenerator();
        return generator.createAutomaton(net, true);
    }
}
