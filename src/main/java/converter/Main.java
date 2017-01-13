package converter;

import converter.automaton.*;
import converter.utils.*;
import main.LTLfAutomatonResultWrapper;
import net.sf.tweety.logics.pl.syntax.Proposition;
import net.sf.tweety.logics.pl.syntax.PropositionalSignature;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import rationals.*;

import java.util.*;

/**
 * Created by arnelaponin on 02/09/16.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        String petriNetFile = "usecase7.pnml";
        //(G (E -> (F f))) && (G(E -> F J)) (G (C -> (X (!D))))
        //String ltlFormula = "G (t1 -> (F B))";
        String ltlFormula = "(G ((K) -> (F (N)))) && (G ((S) -> (F (z))))";
        String optionalSource = "(F S) -> (G (!J))";
        // (G ((K) -> (F (N)))) && (G ((Y) -> (F (z)))) && (G ((S) -> (F (g)))) usecase 13
        // (G ((B) -> (X (C)))) && (G ((C) -> (X (g)))) && (G ((E) -> (X (f))))
        // (G (A -> (F B)))
        // (G (A -> (X B))) chain response
        // G (A -> (X ((!A) U B))) alt response
        // (((F A) || (F B)) && (!((F A) && (F B)))) exclusive choice
        // ((!B) U A) || (G (!B)) precedence
        // (G (A -> (F B))) && (((!B) U A) || (G (!B))) succession
        // (!((F A) && (F B))) not-coexistence
        // ((F I) -> (F g)) && ((F g) -> (F I)) coexistence
        // (G (C -> (F D))) && (((!D) U C) || (G (!D))) && (F C) succession and existence
        // (G (X D -> C)) chain precedence

        PropositionalSignature signature = new PropositionalSignature();

        boolean declare = true;
        boolean minimize = true;
        boolean trim = false;
        boolean noEmptyTrace = true;
        boolean printing = false;

        PetrinetGraph net = Extractor.extractPetriNet(petriNetFile);
        List<Proposition> netPropositions = PetrinetUtils.getAllTransitionLabels(net);
        signature.addAll(netPropositions);

        //Automaton of the procedural model.
        PNAutomatonConverter converter = new PNAutomatonConverter(net);
        MyAutomaton automaton = converter.convertToAutomaton();

        //Automaton of the declarative model.
        LTLfAutomatonResultWrapper ltlfARW = main.Main.ltlfString2Aut(ltlFormula, signature, declare, minimize, trim, noEmptyTrace, printing);
        Automaton declareAutomaton = ltlfARW.getAutomaton();

        LTLfAutomatonResultWrapper optionalSourceARW = main.Main.ltlfString2Aut(optionalSource, signature, declare, minimize, trim, noEmptyTrace, printing);
        Automaton optionalSourceAutomaton = optionalSourceARW.getAutomaton();

        utils.AutomatonUtils.printAutomaton(automaton, "automaton_procedural.gv");
        utils.AutomatonUtils.printAutomaton(declareAutomaton, "automaton_declarative.gv");
        utils.AutomatonUtils.printAutomaton(optionalSourceAutomaton, "automaton_opt_source.gv");

        /*//Taking the intersection between 2 automatons.
        Automaton intersectionAutomaton = AutomatonOperationUtils.getIntersection(automaton, declareAutomaton);
        //utils.AutomatonUtils.printAutomaton(intersectionAutomaton, "automaton_mix.gv");

        Automaton optionalIntersectionAutomaton = AutomatonOperationUtils.getIntersection(automaton, optionalSourceAutomaton);
        //utils.AutomatonUtils.printAutomaton(optionalIntersectionAutomaton, "automaton_mix_optional_source.gv");
        //System.out.println("K is mandatory: " + optionalIntersectionAutomaton.terminals().isEmpty());

        //Reducing the intersection automaton, while keeping the behaviour.
        Automaton reducedIntersection = AutomatonOperationUtils.getReduced(intersectionAutomaton);
        //TODO: Check if you can transform everything into MyAutomaton
        //utils.AutomatonUtils.printAutomaton(reducedIntersection, "automaton_red.gv");

        //Trimming the bad behaviour.
        Automaton trimIntersection = AutomatonOperationUtils.getTrimmed(reducedIntersection);
        //utils.AutomatonUtils.printAutomaton(trimIntersection, "automaton_trim.gv");

        //Getting the difference.
        Automaton relComp = AutomatonOperationUtils.getTrimmed(AutomatonOperationUtils.getRelativeComplement(automaton,declareAutomaton));
        //utils.AutomatonUtils.printAutomaton(relComp, "automaton_comp.gv");

        Explorer explorer = new Explorer(automaton, trimIntersection);
        MyAutomaton trimIntersectionMarkings = explorer.addMarkingsFromOriginal();*/


        //Map<State, List<Transition>> semiBadStates = AutomatonUtils.getSemiBadStates(reducedIntersection);



        InformationWrapper informationWrapper = new InformationWrapper(declareAutomaton, net);

        AutomatonOperationUtils.colorAutomatonStates(informationWrapper, "automaton_coloured.gv");

        repairXorSyncFlattening(informationWrapper);



    }

    //private static void repairXorSyncFlattening(PetrinetGraph net, MyAutomaton automaton, Automaton declareAutomaton) throws Exception {
    private static void repairXorSyncFlattening(InformationWrapper informationWrapper) throws Exception {
        informationWrapper.colourAutomaton();
        Petrinet netWithRemovedBranches = ModelRepairer.repairXorBranch(informationWrapper);

        InformationWrapper updatedWrapper = ModelRepairer.checkIfCandidateFullyConformsToRules(informationWrapper.getDeclarativeAutomaton(), netWithRemovedBranches, "after_removing_unused_xor");

        if (!updatedWrapper.getSemiBadStates().isEmpty()) {
            //PNAutomatonConverter converter1 = new PNAutomatonConverter(netWithRemovedBranches);
            //MyAutomaton partiallyRepairedAutomaton = converter1.convertToAutomaton();
            //Automaton intersection = AutomatonOperationUtils.getIntersection(partiallyRepairedAutomaton, declareAutomaton);
            //Automaton reducedIntersection = AutomatonOperationUtils.getReduced(intersection);

            //Map<State, List<Transition>> semiBadStates = AutomatonUtils.getSemiBadStates(reducedIntersection);

            AutomatonOperationUtils.colorAutomatonStates(informationWrapper, "automaton_coloured_after_xor.gv");

            AutomatonUtils.getSyncPoints(informationWrapper);
        } else {
            String repairedFileName = netWithRemovedBranches.getLabel() + "_repaired_fully.pnml";
            PetrinetUtils.exportPetriNetToPNML(repairedFileName, netWithRemovedBranches);
        }
    }

    private static void checkLanguage(MyAutomaton originalNetAutomaton, Automaton declareAutomaton, Petrinet repairedNet) throws Exception {
        PNAutomatonConverter converter = new PNAutomatonConverter(repairedNet);
        MyAutomaton repairedNetAutomaton = converter.convertToAutomaton();

        Automaton originalIntersectionAutomaton = AutomatonOperationUtils.getIntersection(originalNetAutomaton, declareAutomaton);
        Automaton negatedOriginalIntersection = AutomatonOperationUtils.getNegated(originalIntersectionAutomaton);

        Automaton repairedIntersectionAutomaton = AutomatonOperationUtils.getIntersection(repairedNetAutomaton, negatedOriginalIntersection);

        utils.AutomatonUtils.printAutomaton(repairedIntersectionAutomaton, "automaton_neg_intersection.gv");

        Automaton negatedRepairedIntersection = AutomatonOperationUtils.getNegated(repairedNetAutomaton);

        Automaton repairedIntersectionAutomaton2 = AutomatonOperationUtils.getIntersection(originalIntersectionAutomaton, negatedRepairedIntersection);
        utils.AutomatonUtils.printAutomaton(repairedIntersectionAutomaton2, "automaton_neg_intersection_2.gv");
    }


}
