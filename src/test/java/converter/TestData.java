package converter;

import net.sf.tweety.logics.pl.syntax.Proposition;

import java.util.List;

/**
 * Created by arnelaponin on 02/11/2016.
 */
public class TestData {

    private String petriNetFile;
    private String ltlFormula;
    private List<Proposition> propositions;
    private List<List<String>> allSourcePlaces;
    private List<List<String>> allTargetPlaces;
    private List<String> toRemovePlaces;

    public TestData(String petriNetFile, String ltlFormula, List<Proposition> propositions, List<List<String>> allSourcePlaces, List<List<String>> allTargetPlaces, List<String> toRemovePlaces) {
        this.petriNetFile = petriNetFile;
        this.ltlFormula = ltlFormula;
        this.propositions = propositions;
        this.allSourcePlaces = allSourcePlaces;
        this.allTargetPlaces = allTargetPlaces;
        this.toRemovePlaces = toRemovePlaces;
    }

    public String getPetriNetFile() {
        return petriNetFile;
    }

    public String getLtlFormula() {
        return ltlFormula;
    }

    public List<Proposition> getPropositions() {
        return propositions;
    }

    public List<List<String>> getAllSourcePlaces() {
        return allSourcePlaces;
    }

    public List<List<String>> getAllTargetPlaces() {
        return allTargetPlaces;
    }

    public List<String> getToRemovePlaces() {
        return toRemovePlaces;
    }
}
