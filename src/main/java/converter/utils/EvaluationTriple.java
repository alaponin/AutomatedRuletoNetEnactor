package converter.utils;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;

/**
 * Created by arnelaponin on 01/03/2017.
 */
public class EvaluationTriple {

    private Petrinet originalModel;
    private String formula;
    private Petrinet repairedModel;

    public EvaluationTriple(Petrinet originalModel, String formula, Petrinet repairedModel) {
        this.originalModel = originalModel;
        this.formula = formula;
        this.repairedModel = repairedModel;
    }

    public Petrinet getOriginalModel() {
        return originalModel;
    }

    public String getFormula() {
        return formula;
    }

    public Petrinet getRepairedModel() {
        return repairedModel;
    }
}
