package converter.utils;

import net.sf.tweety.logics.pl.syntax.Proposition;
import org.processmining.contexts.cli.CLIContext;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.plugins.pnml.exporting.PnmlExportNetToPNML;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by arnelaponin on 05/09/16.
 */
public class PetrinetUtils {

    public static Place getStartPlace(PetrinetGraph petrinet) {
        Place startPlace = null;
        Collection<Place> places = petrinet.getPlaces();

        for (Place p: places) {
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgesInP = petrinet.getInEdges(p);

            //Finding the start of the petri net
            if (edgesInP.isEmpty()) {
                startPlace = p;
            }
        }
        return startPlace;
    }

    public static Place getFinalPlace(PetrinetGraph petrinet) {
        Place finalPlace = null;
        Collection<Place> places = petrinet.getPlaces();
        for (Place p: places) {
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edgesOutP = petrinet.getOutEdges(p);

            //Finding the end places of the petri net
            if (edgesOutP.isEmpty()) {
                finalPlace = p;
            }
        }
        return finalPlace;
    }

    public static List<Proposition> getAllTransitionLabels(PetrinetGraph petrinet) {
        List<Proposition> transitionLabels = new ArrayList<Proposition>();
        for (Transition t : petrinet.getTransitions()) {
            Proposition p = new Proposition(t.getLabel());
            transitionLabels.add(p);
        }
        return transitionLabels;
    }

    public static void exportPetriNetToPNML(String fileName, PetrinetGraph petriNet) {
        PetrinetImpl net = (PetrinetImpl) petriNet;
        CLIContext dl = new CLIContext();
        CLIPluginContext context = new CLIPluginContext(dl, "g");

        PnmlExportNetToPNML ex = new PnmlExportNetToPNML();
        File file = new File(fileName);

        try {
            ex.exportPetriNetToPNMLFile(context, net, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
