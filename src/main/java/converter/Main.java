package converter;

import converter.utils.*;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by arnelaponin on 02/09/16.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        //ltlFormula1_1 - ltlFormula1_4 use model1.pnml as petriNetFile
        String ltlFormula1_1 = "(G (activity_g -> (X (! activity_g U activity_h)))) && ((!activity_r) WU activity_s)";
        String ltlFormula1_2 = "((!activity_g) WU activity_h) && (G (activity_g -> X (!activity_g WU activity_h))) && (G ((activity_ag) -> (F (activity_ah))))";
        String ltlFormula1_3 = "((!activity_ae) WU activity_c) && (G (activity_ae -> X ((!activity_ae) WU activity_c))) && (G (activity_h -> (F activity_g))) && (!(F (activity_y)))";
        String ltlFormula1_4 = "((!activity_ae) WU activity_c) && (G (activity_h -> (F activity_g))) && ((G (activity_r -> (F activity_s))))";

        //ltlFormula2_1 - ltlFormula2_4 use model2.pnml as petriNetFile
        String ltlFormula2_1 = "(G (activity_d -> (F activity_o))) && (G (activity_k -> (F activity_s)))";
        String ltlFormula2_2 = "(G (activity_g -> (F activity_q))) && ((! activity_t) WU activity_m) && (!(F (activity_l)))";
        String ltlFormula2_3 = "(G (activity_g -> (F activity_o))) && (!(F (activity_e))) && ((! activity_m) WU activity_v)";
        String ltlFormula2_4 = "((G (activity_k -> (F activity_s))) && (G (activity_d -> (X (! activity_d U activity_r)))) && ((! activity_g) WU activity_o))";

        //ltlFormula3_1 - ltlFormula3_4 use model3.pnml as petriNetFile
        String ltlFormula3_1 = "((!activity_f) WU activity_h)";
        String ltlFormula3_2 = "(G (activity_k -> (F activity_j)))";
        String ltlFormula3_3 = "(G (activity_g -> (X (! activity_g U activity_b))))";

        //ltlFormula_practical_example use practical_running.pnml as petriNetFile
        String ltlFormula_practical_example = "((!check_medical_history) WU ask_for_updates) && ((!predict_risk) WU ask_for_updates)  && (!(F ask_to_recheck))";
        String ltlFormula_prac_short = "((!cmh) WU afu) && ((!pr) WU afu)  && (!(F atr))";

        String ltlFormulaX = "((!C) WU B)";

        String petriNetFile = "data/model2.pnml";
        List<String> modelRules = new ArrayList<>();
        modelRules.add(ltlFormula2_4);


        int count = 0;
        File dir = new File("final_nets");
        dir.mkdir();
        File dir2 = new File("automatons");
        dir2.mkdir();
        File dir3 = new File("test_nets");
        dir3.mkdir();
        File dir4 = new File("sg_files");
        dir4.mkdir();
        File dir5 = new File("logs");
        dir5.mkdir();
        for (String rule : modelRules) {
            count++;
            Petrinet net = (Petrinet) Extractor.extractPetriNet(petriNetFile);
            Petrinet repairedNet = ProceduralRepairer.repair(net, rule);
        }
        /*Petrinet net = (Petrinet) Extractor.extractPetriNet(petriNetFile);
        Petrinet net_v1 = (Petrinet) Extractor.extractPetriNet("data/sync_v1.pnml");
        Petrinet net_v2 = (Petrinet) Extractor.extractPetriNet("data/sync_v2.pnml");
        AutomatonUtils.checkLanguage(net, ltlFormula_practical_example, net_v1);
        AutomatonUtils.checkLanguage(net, ltlFormula_practical_example, net_v2);*/
        //Tester.runModel3LanguageCheck();

    }






}
