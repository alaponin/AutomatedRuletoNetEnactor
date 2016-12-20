package converter;

import net.sf.tweety.logics.pl.syntax.Proposition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by arnelaponin on 02/11/2016.
 */
public class UseCase6DataResolver implements ParameterResolver {
    @Override
    public boolean supports(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(TestData.class);
    }

    @Override
    public Object resolve(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {

        List<Proposition> propositions = new ArrayList<>();

        propositions.add(new Proposition("A"));
        propositions.add(new Proposition("B"));
        propositions.add(new Proposition("C"));
        propositions.add(new Proposition("D"));
        propositions.add(new Proposition("E"));

        List<List<String>> allSourcePlaces = new ArrayList<>();
        List<List<String>> allTargetPlaces = new ArrayList<>();

        List<String> sourcePlaces1 = new ArrayList<>();
        sourcePlaces1.add("p2");
        sourcePlaces1.add("p3");
        sourcePlaces1.add("p4");
        List<String> sourcePlaces2 = new ArrayList<>();
        sourcePlaces2.add("p3");
        sourcePlaces2.add("p4");
        sourcePlaces2.add("p5");
        allSourcePlaces.add(sourcePlaces1);
        allSourcePlaces.add(sourcePlaces2);

        List<String> targetPlaces1 = new ArrayList<>();
        targetPlaces1.add("p2");
        targetPlaces1.add("p4");
        targetPlaces1.add("p6");
        List<String> targetPlaces2 = new ArrayList<>();
        targetPlaces2.add("p4");
        targetPlaces2.add("p5");
        targetPlaces2.add("p6");
        allTargetPlaces.add(targetPlaces1);
        allTargetPlaces.add(targetPlaces2);

        List<String> toRemovePlaces = new ArrayList<>();
        toRemovePlaces.add("p3");

        TestData useCase6TestData = new TestData("usecase6.pnml", "(G (D -> (F C)))", propositions, allSourcePlaces, allTargetPlaces, toRemovePlaces);


        return useCase6TestData;
    }
}
