package converter.utils;

import org.processmining.framework.connections.Connection;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.connections.ConnectionManager;
import org.processmining.framework.plugin.*;
import org.processmining.framework.plugin.events.Logger;
import org.processmining.framework.plugin.events.PluginLifeCycleEventListener;
import org.processmining.framework.plugin.events.ProgressEventListener;
import org.processmining.framework.plugin.impl.AbstractPluginContext;
import org.processmining.framework.plugin.impl.FieldSetException;
import org.processmining.framework.providedobjects.ProvidedObjectManager;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.Pnml;
import org.processmining.plugins.pnml.exporting.PnmlExportNet;
import org.processmining.plugins.pnml.exporting.PnmlExportNetToPNML;
import org.processmining.plugins.pnml.importing.PnmlImportUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Created by arnelaponin on 02/09/16.
 */
public class Extractor {

    public static PetrinetGraph extractPetriNet(String pnmlFilename) throws Exception {
        PnmlImportUtils pnmlImportUtils = new PnmlImportUtils();
        File pnFile = new File (pnmlFilename);

        InputStream input = new FileInputStream(pnFile);
        Pnml pnml = pnmlImportUtils.importPnmlFromStream(null,input, pnFile.getName(), pnFile.length());
        String nameWithoutExtension = pnFile.getName().split("\\.")[0];
        PetrinetGraph net = PetrinetFactory.newPetrinet(nameWithoutExtension);
        Marking marking = new Marking();
        pnml.convertToNet(net, marking, new GraphLayoutConnection(net));
        return net;
    }
}
