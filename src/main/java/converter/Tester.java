package converter;

import converter.petrinet.CanNotConvertPNToAutomatonException;
import converter.utils.AutomatonUtils;
import converter.utils.EvaluationTriple;
import converter.utils.Extractor;
import converter.utils.TSFileConverter;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by arnelaponin on 16/03/2017.
 */
public class Tester {
    private static String ltlFormula1_1 = "(G (activity_g -> (X (! activity_g U activity_h)))) && ((!activity_r) WU activity_s)";
    private static String ltlFormula1_2 = "((!activity_g) WU activity_h) && (G (activity_g -> X (!activity_g WU activity_h))) && (G ((activity_ag) -> (F (activity_ah))))";
    private static String ltlFormula1_3 = "((!activity_ae) WU activity_c) && (G (activity_ae -> X ((!activity_ae) WU activity_c))) && (G (activity_h -> (F activity_g))) && (!(F (activity_y)))";
    private static String ltlFormula1_4 = "((!activity_ae) WU activity_c) && (G (activity_h -> (F activity_g))) && ((G (activity_r -> (F activity_s))))";

    private static String ltlFormula2_1 = "(G (activity_d -> (F activity_o))) && (G (activity_k -> (F activity_s)))";
    private static String ltlFormula2_2 = "(G (activity_g -> (F activity_q))) && ((! activity_t) WU activity_m) && (!(F (activity_l)))";
    private static String ltlFormula2_3 = "(G (activity_g -> (F activity_o))) && (!(F (activity_e))) && ((! activity_m) WU activity_v)";
    private static String ltlFormula2_4 = "((G (activity_k -> (F activity_s))) && (G (activity_d -> (X (! activity_d U activity_r)))) && ((! activity_g) WU activity_o))";

    private static String ltlFormula3_1 = "((!activity_f) WU activity_h)";
    private static String ltlFormula3_2 = "(G (activity_k -> (F activity_j)))";
    private static String ltlFormula3_3 = "(G (activity_g -> (X (! activity_g U activity_b))))";

    private static String ltlFormula5_1 = "((!activity_p) WU activity_o) && (G (activity_ai -> (F activity_ah)))";
    private static String ltlFormula5_2 = "(G (activity_bh -> (F activity_bk)))";
    private static String ltlFormula5_3 = "((!activity_bp) WU activity_ay) && (G (activity_bp -> X ((! activity_bp) WU activity_ay))) && (!(F (activity_az))) && (G (activity_bo -> (F activity_bm)))";
    private static String ltlFormula5_4 = "(G (activity_ex -> (X (! activity_ex U activity_ev))))";

    private static String ltlFormula8_1 = "((!check_medical_history) WU ask_for_major_updates) && ((!predict_risk_behaviour) WU ask_for_major_updates) && (!(F ask_applicant_to_recheck))";


    private static Petrinet model1;
    private static Petrinet model1_r1_Alg;
    private static Petrinet model1_r1_petrify;
    private static Petrinet model1_r2_Alg;
    private static Petrinet model1_r2_petrify;
    private static Petrinet model1_r3_Alg;
    private static Petrinet model1_r3_petrify;
    private static Petrinet model1_r4_Alg;
    private static Petrinet model1_r4_petrify;

    private static Petrinet model1_r1_log;
    private static Petrinet model1_r2_log;
    private static Petrinet model1_r3_log;
    private static Petrinet model1_r4_log;

    private static Petrinet model2;
    private static Petrinet model2_r1_Alg;
    private static Petrinet model2_r1_petrify;
    private static Petrinet model2_r2_Alg;
    private static Petrinet model2_r2_petrify;
    private static Petrinet model2_r3_Alg;
    private static Petrinet model2_r3_petrify;
    private static Petrinet model2_r4_Alg;
    private static Petrinet model2_r4_petrify;

    private static Petrinet model2_r1_log;
    private static Petrinet model2_r2_log;
    private static Petrinet model2_r3_log;
    private static Petrinet model2_r4_log;

    private static Petrinet model3;
    private static Petrinet model3_r1_petrify;
    private static Petrinet model3_r2_petrify;
    private static Petrinet model3_r3_petrify;

    private static Petrinet model3_r1_log;
    private static Petrinet model3_r2_log;
    private static Petrinet model3_r3_log;

    private static Petrinet model5;
    private static Petrinet model5_r1_petrify;
    private static Petrinet model5_r2_petrify;
    private static Petrinet model5_r3_petrify;
    private static Petrinet model5_r4_petrify;

    private static Petrinet model5_r1_Alg;
    private static Petrinet model5_r2_Alg;
    private static Petrinet model5_r3_Alg;
    private static Petrinet model5_r4_Alg;


    private static Petrinet model1_r1_F;
    private static Petrinet model1_r2_F;
    private static Petrinet model1_r3_F;
    private static Petrinet model1_r4_F;

    private static Petrinet model2_r1_F;
    private static Petrinet model2_r2_F;
    private static Petrinet model2_r3_F;
    private static Petrinet model2_r4_F;

    private static Petrinet model3_r1_F;
    private static Petrinet model3_r2_F;
    private static Petrinet model3_r3_F;

    private static EvaluationTriple t1;
    private static EvaluationTriple t2;
    private static EvaluationTriple t3;
    private static EvaluationTriple t4;
    private static EvaluationTriple t5;
    private static EvaluationTriple t6;
    private static EvaluationTriple t7;
    private static EvaluationTriple t8;

    private static EvaluationTriple t9;
    private static EvaluationTriple t10;
    private static EvaluationTriple t11;
    private static EvaluationTriple t12;
    private static EvaluationTriple t13;
    private static EvaluationTriple t14;
    private static EvaluationTriple t15;
    private static EvaluationTriple t16;

    private static EvaluationTriple t17;
    private static EvaluationTriple t18;
    private static EvaluationTriple t19;

    private static EvaluationTriple t20;
    private static EvaluationTriple t21;
    private static EvaluationTriple t22;
    private static EvaluationTriple t23;

    private static EvaluationTriple t24;
    private static EvaluationTriple t25;
    private static EvaluationTriple t26;
    private static EvaluationTriple t27;

    private static EvaluationTriple t28;
    private static EvaluationTriple t29;
    private static EvaluationTriple t30;

    private static EvaluationTriple t31;
    private static EvaluationTriple t32;
    private static EvaluationTriple t33;
    private static EvaluationTriple t34;

    //private static EvaluationTriple t35;
    //private static EvaluationTriple t36;

    private static EvaluationTriple t37;
    private static EvaluationTriple t38;
    private static EvaluationTriple t39;
    private static EvaluationTriple t40;

    private static EvaluationTriple t41;
    private static EvaluationTriple t42;
    private static EvaluationTriple t43;
    private static EvaluationTriple t44;

    private static EvaluationTriple t45;
    private static EvaluationTriple t46;
    private static EvaluationTriple t47;

    static {

        try {
            model1 = (Petrinet) Extractor.extractPetriNet("data/model1.pnml");
            model1_r1_Alg = (Petrinet) Extractor.extractPetriNet("data/model1_r1_Alg.pnml");
            model1_r1_petrify = (Petrinet) Extractor.extractPetriNet("data/model1_r1_petrify.pnml");
            model1_r2_Alg = (Petrinet) Extractor.extractPetriNet("data/model1_r2_Alg.pnml");
            model1_r2_petrify = (Petrinet) Extractor.extractPetriNet("data/model1_r2_petrify.pnml");
            model1_r3_Alg = (Petrinet) Extractor.extractPetriNet("data/model1_r3_Alg.pnml");
            model1_r3_petrify = (Petrinet) Extractor.extractPetriNet("data/model1_r3_petrify.pnml");
            model1_r4_Alg = (Petrinet) Extractor.extractPetriNet("data/model1_r4_Alg.pnml");
            model1_r4_petrify = (Petrinet) Extractor.extractPetriNet("data/model1_r4_petrify.pnml");

            model1_r1_log = (Petrinet) Extractor.extractPetriNet("data/model1_log2000_f1.pnml");
            model1_r2_log = (Petrinet) Extractor.extractPetriNet("data/model1_log2000_f2.pnml");
            model1_r3_log = (Petrinet) Extractor.extractPetriNet("data/model1_log2000_f3.pnml");
            model1_r4_log = (Petrinet) Extractor.extractPetriNet("data/model1_log2000_f4.pnml");

            model2 = (Petrinet) Extractor.extractPetriNet("data/model2.pnml");
            model2_r1_Alg = (Petrinet) Extractor.extractPetriNet("data/model2_r1_Alg.pnml");
            model2_r1_petrify = (Petrinet) Extractor.extractPetriNet("data/model2_r1_petrify.pnml");
            model2_r2_Alg = (Petrinet) Extractor.extractPetriNet("data/model2_r2_Alg.pnml");
            model2_r2_petrify = (Petrinet) Extractor.extractPetriNet("data/model2_r2_petrify.pnml");
            model2_r3_Alg = (Petrinet) Extractor.extractPetriNet("data/model2_r3_Alg.pnml");
            model2_r3_petrify = (Petrinet) Extractor.extractPetriNet("data/model2_r3_petrify.pnml");
            model2_r4_Alg = (Petrinet) Extractor.extractPetriNet("data/model2_r4_Alg.pnml");
            model2_r4_petrify = (Petrinet) Extractor.extractPetriNet("data/model2_r4_petrify.pnml");

            model2_r1_log = (Petrinet) Extractor.extractPetriNet("data/model2_log2000_f1.pnml");
            model2_r2_log = (Petrinet) Extractor.extractPetriNet("data/model2_log2000_f2.pnml");
            model2_r3_log = (Petrinet) Extractor.extractPetriNet("data/model2_log2000_f3.pnml");
            model2_r4_log = (Petrinet) Extractor.extractPetriNet("data/model2_log2000_f4.pnml");

            model3 = (Petrinet) Extractor.extractPetriNet("data/model3.pnml");
            model3_r1_petrify = (Petrinet) Extractor.extractPetriNet("data/model3_r1_petrify.pnml");
            model3_r2_petrify = (Petrinet) Extractor.extractPetriNet("data/model3_r2_petrify.pnml");
            model3_r3_petrify = (Petrinet) Extractor.extractPetriNet("data/model3_r3_petrify.pnml");

            model3_r1_log = (Petrinet) Extractor.extractPetriNet("data/model3_log2000_f1.pnml");
            model3_r2_log = (Petrinet) Extractor.extractPetriNet("data/model3_log2000_f2.pnml");
            model3_r3_log = (Petrinet) Extractor.extractPetriNet("data/model3_log2000_f3.pnml");

            /*model5 = (Petrinet) Extractor.extractPetriNet("models/model5.pnml");
            model5_r1_petrify = (Petrinet) Extractor.extractPetriNet("models/model5_r1_petrify.pnml");
            model5_r2_petrify = (Petrinet) Extractor.extractPetriNet("models/model5_r2_petrify.pnml");
            model5_r3_petrify = (Petrinet) Extractor.extractPetriNet("models/model5_r3_petrify.pnml");
            model5_r4_petrify = (Petrinet) Extractor.extractPetriNet("models/model5_r4_petrify.pnml");

            model5_r1_Alg = (Petrinet) Extractor.extractPetriNet("models/model5_r1_Alg.pnml");
            model5_r2_Alg = (Petrinet) Extractor.extractPetriNet("models/model5_r2_Alg.pnml");
            model5_r3_Alg = (Petrinet) Extractor.extractPetriNet("models/model5_r3_Alg.pnml");
            model5_r4_Alg = (Petrinet) Extractor.extractPetriNet("models/model5_r4_Alg.pnml");*/

        /*model5_2 = (Petrinet) Extractor.extractPetriNet("models/model5_2.pnml");
        model5_3 = (Petrinet) Extractor.extractPetriNet("models/model5_3.pnml");
        model5_4 = (Petrinet) Extractor.extractPetriNet("models/model5_4.pnml");
        model5_3_r1_Alg = (Petrinet) Extractor.extractPetriNet("models/model5_3_r1_Alg.pnml");
        model5_2_r1_Alg = (Petrinet) Extractor.extractPetriNet("models/model5_2_r1_Alg.pnml");
        model5_4_r1_Alg = (Petrinet) Extractor.extractPetriNet("models/model5_4_r1_Alg.pnml");*/

            model1_r1_F = (Petrinet) Extractor.extractPetriNet("data/Repaired_1_1.pnml");
            model1_r2_F = (Petrinet) Extractor.extractPetriNet("data/Repaired_1_2.pnml");
            model1_r3_F = (Petrinet) Extractor.extractPetriNet("data/Repaired_1_3.pnml");
            model1_r4_F = (Petrinet) Extractor.extractPetriNet("data/Repaired_1_4.pnml");

            model2_r1_F = (Petrinet) Extractor.extractPetriNet("data/Repaired_2_1.pnml");
            model2_r2_F = (Petrinet) Extractor.extractPetriNet("data/Repaired_2_2.pnml");
            model2_r3_F = (Petrinet) Extractor.extractPetriNet("data/Repaired_2_3.pnml");
            model2_r4_F = (Petrinet) Extractor.extractPetriNet("data/Repaired_2_4.pnml");

            model3_r1_F = (Petrinet) Extractor.extractPetriNet("data/Repaired_3_1.pnml");
            model3_r2_F = (Petrinet) Extractor.extractPetriNet("data/Repaired_3_2.pnml");
            model3_r3_F = (Petrinet) Extractor.extractPetriNet("data/Repaired_3_3.pnml");

            t1 = new EvaluationTriple(model1, ltlFormula1_1, model1_r1_Alg);
            t2 = new EvaluationTriple(model1, ltlFormula1_1, model1_r1_petrify);
            t3 = new EvaluationTriple(model1, ltlFormula1_2, model1_r2_Alg);
            t4 = new EvaluationTriple(model1, ltlFormula1_2, model1_r2_petrify);
            t5 = new EvaluationTriple(model1, ltlFormula1_3, model1_r3_Alg);
            t6 = new EvaluationTriple(model1, ltlFormula1_3, model1_r3_petrify);
            t7 = new EvaluationTriple(model1, ltlFormula1_4, model1_r4_Alg);
            t8 = new EvaluationTriple(model1, ltlFormula1_4, model1_r4_petrify);

            t9 = new EvaluationTriple(model2, ltlFormula2_1, model2_r1_Alg);
            t10 = new EvaluationTriple(model2, ltlFormula2_1, model2_r1_petrify);
            t11 = new EvaluationTriple(model2, ltlFormula2_2, model2_r2_Alg);
            t12 = new EvaluationTriple(model2, ltlFormula2_2, model2_r2_petrify);
            t13 = new EvaluationTriple(model2, ltlFormula2_3, model2_r3_Alg);
            t14 = new EvaluationTriple(model2, ltlFormula2_3, model2_r3_petrify);
            t15 = new EvaluationTriple(model2, ltlFormula2_4, model2_r4_Alg);
            t16 = new EvaluationTriple(model2, ltlFormula2_4, model2_r4_petrify);

            t17 = new EvaluationTriple(model3, ltlFormula3_1, model3_r1_petrify);
            t18 = new EvaluationTriple(model3, ltlFormula3_2, model3_r2_petrify);
            t19 = new EvaluationTriple(model3, ltlFormula3_3, model3_r3_petrify);

            t20 = new EvaluationTriple(model1, ltlFormula1_1, model1_r1_log);
            t21 = new EvaluationTriple(model1, ltlFormula1_2, model1_r2_log);
            t22 = new EvaluationTriple(model1, ltlFormula1_3, model1_r3_log);
            t23 = new EvaluationTriple(model1, ltlFormula1_4, model1_r4_log);

            t24 = new EvaluationTriple(model2, ltlFormula2_1, model2_r1_log);
            t25 = new EvaluationTriple(model2, ltlFormula2_2, model2_r2_log);
            t26 = new EvaluationTriple(model2, ltlFormula2_3, model2_r3_log);
            t27 = new EvaluationTriple(model2, ltlFormula2_4, model2_r4_log);

            t28 = new EvaluationTriple(model3, ltlFormula3_1, model3_r1_log);
            t29 = new EvaluationTriple(model3, ltlFormula3_2, model3_r2_log);
            t30 = new EvaluationTriple(model3, ltlFormula3_3, model3_r3_log);

            /*t31 = new EvaluationTriple(model5, ltlFormula5_1, model5_r1_Alg);
            t32 = new EvaluationTriple(model5, ltlFormula5_2, model5_r2_Alg);
            t33 = new EvaluationTriple(model5, ltlFormula5_3, model5_r3_Alg);
            t34 = new EvaluationTriple(model5, ltlFormula5_4, model5_r4_Alg);*/

            //t35 = new EvaluationTriple(model5_2, ltlFormula5_2_1, model5_2_r1_Alg);
            //t36 = new EvaluationTriple(model5_4, ltlFormula5_4_1, model5_4_r1_Alg);

            t37 = new EvaluationTriple(model1, ltlFormula1_1, model1_r1_F);
            t38 = new EvaluationTriple(model1, ltlFormula1_2, model1_r2_F);
            t39 = new EvaluationTriple(model1, ltlFormula1_3, model1_r3_F);
            t40 = new EvaluationTriple(model1, ltlFormula1_4, model1_r4_F);

            t41 = new EvaluationTriple(model2, ltlFormula2_1, model2_r1_F);
            t42 = new EvaluationTriple(model2, ltlFormula2_2, model2_r2_F);
            t43 = new EvaluationTriple(model2, ltlFormula2_3, model2_r3_F);
            t44 = new EvaluationTriple(model2, ltlFormula2_4, model2_r4_F);

            t45 = new EvaluationTriple(model3, ltlFormula3_1, model3_r1_F);
            t46 = new EvaluationTriple(model3, ltlFormula3_2, model3_r2_F);
            t47 = new EvaluationTriple(model3, ltlFormula3_3, model3_r3_F);
        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    protected static void runAllLanguageChecks() throws Exception {
        List<EvaluationTriple> triples = new ArrayList<>();
        triples.add(t1);
        triples.add(t2);
        triples.add(t3);
        triples.add(t4);
        triples.add(t5);
        triples.add(t6);
        triples.add(t7);
        triples.add(t8);
        triples.add(t9);
        triples.add(t10);
        triples.add(t11);
        triples.add(t12);
        triples.add(t13);
        triples.add(t14);
        triples.add(t15);
        triples.add(t16);
        triples.add(t17);
        triples.add(t18);
        triples.add(t19);
        triples.add(t20);
        triples.add(t21);
        triples.add(t22);
        triples.add(t23);
        triples.add(t24);
        triples.add(t25);
        triples.add(t26);
        triples.add(t27);
        triples.add(t28);
        triples.add(t29);
        triples.add(t30);
        /*triples.add(t31);
        triples.add(t32);
        triples.add(t33);
        triples.add(t34);*/

        triples.add(t37);
        triples.add(t38);
        triples.add(t39);
        triples.add(t40);
        triples.add(t41);
        triples.add(t42);
        triples.add(t43);
        triples.add(t44);
        triples.add(t45);
        triples.add(t46);
        triples.add(t47);

        for (EvaluationTriple t : triples) {
            AutomatonUtils.checkLanguage(t.getOriginalModel(), t.getFormula(), t.getRepairedModel());
        }
    }

    protected static void runModel1LanguageChecksForAlgAndPetr() throws Exception {
        List<EvaluationTriple> triples = new ArrayList<>();
        triples.add(t1);
        triples.add(t2);
        triples.add(t3);
        triples.add(t4);
        triples.add(t5);
        triples.add(t6);
        triples.add(t7);
        triples.add(t8);
        triples.add(t9);
        for (EvaluationTriple t : triples) {
            AutomatonUtils.checkLanguage(t.getOriginalModel(), t.getFormula(), t.getRepairedModel());
        }
    }

    protected static void runModel2LanguageCheckPetrify() throws Exception {
        List<EvaluationTriple> triples = new ArrayList<>();
        triples.add(t10);
        triples.add(t12);
        triples.add(t14);
        triples.add(t16);
        for (EvaluationTriple t : triples) {
            AutomatonUtils.checkLanguage(t.getOriginalModel(), t.getFormula(), t.getRepairedModel());
        }
    }

    protected static void runModel2LanguageCheckArne() throws Exception {
        List<EvaluationTriple> triples = new ArrayList<>();
        triples.add(t9);
        triples.add(t11);
        triples.add(t13);
        triples.add(t15);
        for (EvaluationTriple t : triples) {
            AutomatonUtils.checkLanguage(t.getOriginalModel(), t.getFormula(), t.getRepairedModel());
        }
    }

    protected static void runModel3LanguageCheck() throws Exception {
        List<EvaluationTriple> triples = new ArrayList<>();
        triples.add(t20);
        triples.add(t21);
        triples.add(t22);
        triples.add(t23);
        triples.add(t24);
        triples.add(t25);
        triples.add(t26);
        triples.add(t27);
        triples.add(t28);
        triples.add(t29);
        triples.add(t30);
        for (EvaluationTriple t : triples) {
            AutomatonUtils.checkLanguage(t.getOriginalModel(), t.getFormula(), t.getRepairedModel());
        }
    }

    protected static void runModel2R2LanguageCheck() throws Exception {
        List<EvaluationTriple> triples = new ArrayList<>();
        triples.add(t26);
        for (EvaluationTriple t : triples) {
            AutomatonUtils.checkLanguage(t.getOriginalModel(), t.getFormula(), t.getRepairedModel());
        }
    }

    protected static void createTSFiles() throws CanNotConvertPNToAutomatonException {
        List<EvaluationTriple> tsTriples = new ArrayList<>();
        tsTriples.add(t1);

        for (EvaluationTriple t : tsTriples) {
            String fileName = "sg_files/"+ t.getRepairedModel().getLabel() + "_" + t.getRepairedModel().hashCode() + ".sg";
            TSFileConverter.TS2File(t.getOriginalModel(), t.getFormula(), fileName);
        }
    }


}
