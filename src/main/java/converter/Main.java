package converter;

import converter.utils.Extractor;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import rationals.Automaton;

/**
 * Created by arnelaponin on 02/09/16.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        String petriNetFile = "test.pnml";

        PetrinetGraph net = Extractor.extractPetriNet(petriNetFile);

        PNAutomatonConverter converter = new PNAutomatonConverter(net);
        Automaton automaton = converter.convertToAutomaton();
        System.out.println("Automaton: "+automaton);
        utils.AutomatonUtils.printAutomaton(automaton, "automaton.gv");

    }


}
