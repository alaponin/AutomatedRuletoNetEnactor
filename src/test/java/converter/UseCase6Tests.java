package converter;

import converter.automaton.MyAutomaton;
import converter.automaton.TransitionMarkingPair;
import converter.utils.AutomatonUtils;
import converter.utils.Extractor;
import main.LTLfAutomatonResultWrapper;
import net.sf.tweety.logics.pl.syntax.PropositionalSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import rationals.Automaton;
import rationals.State;
import rationals.Synchronization;
import rationals.Transition;
import rationals.transformations.Mix;
import rationals.transformations.Pruner;
import rationals.transformations.Reducer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Created by arnelaponin on 29/10/2016.
 */

@ExtendWith(MockitoExtension.class)
@ExtendWith(UseCase6DataResolver.class)
public class UseCase6Tests {

    String petriNetFile;
    String ltlFormula;
    PetrinetGraph net;

    //LTL variables
    PropositionalSignature signature;
    boolean declare = true;
    boolean minimize = true;
    boolean trim = false;
    boolean noEmptyTrace = true;
    boolean printing = false;

    //Automatons
    MyAutomaton petriNetAutomaton;
    Automaton ltlAutomaton;
    Automaton intersectionAutomaton;

    List<List<String>> allSourcePlaces;
    List<List<String>> allTargetPlaces;
    List<String> toRemovePlaces;

    public UseCase6Tests(TestData testData) {
        this.petriNetFile = testData.getPetriNetFile();
        this.ltlFormula = testData.getLtlFormula();
        this.signature = new PropositionalSignature();
        signature.addAll(testData.getPropositions());
        try {
            net = Extractor.extractPetriNet(petriNetFile);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        this.allSourcePlaces = testData.getAllSourcePlaces();
        this.allTargetPlaces = testData.getAllTargetPlaces();
        this.toRemovePlaces = testData.getToRemovePlaces();
    }

    @Test
    @DisplayName("creates an automaton from a Petri Net")
    void createPNAutomaton() {
        PNAutomatonConverter converter = new PNAutomatonConverter(net);
        try {
            petriNetAutomaton = converter.convertToAutomaton();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertAll("state count",
                () -> assertTrue(petriNetAutomaton.accessibleStates().size() > 1),
                () -> assertTrue(petriNetAutomaton.initials().size() == 1, () -> "There has to exist one initial state"),
                () -> assertTrue(petriNetAutomaton.terminals().size() == 1, () -> "There has to exist one final state")
        );
    }

    @Test
    @DisplayName("creates an automaton from a LTL formula")
    void createLTLAutomaton() {
        LTLfAutomatonResultWrapper ltlfARW = main.Main.ltlfString2Aut(ltlFormula, signature, declare, minimize, trim, noEmptyTrace, printing);
        ltlAutomaton = ltlfARW.getAutomaton();
        assertAll("state count",
                () -> assertTrue(ltlAutomaton.accessibleStates().size() > 1),
                () -> assertTrue(ltlAutomaton.initials().size() == 1, () -> "There has to exist one initial state"),
                () -> assertTrue(ltlAutomaton.terminals().size() >= 1, () -> "There has to exist at least one final state")
        );
    }

    @Nested
    @DisplayName("when automatons are created")
    class WhenAutomatonsCreated {

        @BeforeEach
        void createAutomatons() {
            PNAutomatonConverter converter = new PNAutomatonConverter(net);
            try {
                petriNetAutomaton = converter.convertToAutomaton();
            } catch (Exception e) {
                e.printStackTrace();
            }
            LTLfAutomatonResultWrapper ltlfARW = main.Main.ltlfString2Aut(ltlFormula, signature, declare, minimize, trim, noEmptyTrace, printing);
            ltlAutomaton = ltlfARW.getAutomaton();
        }

        @Test
        @DisplayName("takes an intersection between two automatons")
        void takeIntersection() {
            intersectionAutomaton = new Mix(new Synchronization() {
                //Taken from the default one.
                public Object synchronize(Object t1, Object t2) {
                    return t1 == null?null:(t1.equals(t2)?t1:null);
                }

                //Takes an union of alphabets, so that all of the members would be synchronized.
                public Set synchronizable(Set set, Set set1) {
                    Set<String> setUnion = new HashSet<String>(set);
                    setUnion.addAll(set1);
                    return setUnion;
                }

                public Set synchronizing(Collection collection) {
                    return null;
                }

                public boolean synchronizeWith(Object o, Set set) {
                    return false;
                }
            }).transform(petriNetAutomaton,ltlAutomaton);
            assertAll("state count",
                    () -> assertTrue(intersectionAutomaton.accessibleStates().size() > 1),
                    () -> assertTrue(intersectionAutomaton.initials().size() == 1, () -> "There has to exist one initial state"),
                    () -> assertTrue(intersectionAutomaton.terminals().size() >= 1, () -> "The intersection can not be empty")
            );
        }

        @Nested
        @DisplayName("when the intersection has been taken")
        @ExtendWith(MockitoExtension.class)
        class WhenIntersectionExists {

            Automaton reducedIntersection;
            Automaton trimIntersection;
            @Mock
            State firstState;
            @Mock
            State secondState;

            @BeforeEach
            void initIntersection() {
                PNAutomatonConverter converter = new PNAutomatonConverter(net);
                try {
                    petriNetAutomaton = converter.convertToAutomaton();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                LTLfAutomatonResultWrapper ltlfARW = main.Main.ltlfString2Aut(ltlFormula, signature, declare, minimize, trim, noEmptyTrace, printing);
                ltlAutomaton = ltlfARW.getAutomaton();

                takeIntersection();

                reducedIntersection = new Reducer().transform(intersectionAutomaton);
                trimIntersection = new Pruner().transform(reducedIntersection);


                when(firstState.toString()).thenReturn("1");
                when(secondState.toString()).thenReturn("11");
            }

            @Test
            @DisplayName("gets semi-bad markings")
            void getSemiBadMarkings() {

                /*Map<State, List<Transition>> semiBadStates = AutomatonUtils.getSemiBadStates(reducedIntersection);

                assertTrue(semiBadStates.size() == 2);

                Iterator it = semiBadStates.entrySet().iterator();

                Map.Entry<State, List<Transition>> entry1= (Map.Entry<State, List<Transition>>) it.next();
                String stateString1= entry1.getKey().toString();

                assertEquals(firstState.toString(), stateString1);

                Map.Entry<State, List<Transition>> entry2= (Map.Entry<State, List<Transition>>) it.next();
                String stateString2= entry2.getKey().toString();

                assertEquals(secondState.toString(), stateString2);

                Map<Transition, TransitionMarkingPair> semiBadStatesWithMarkings = AutomatonUtils.getSemiBadMarkingsFromOriginal(petriNetAutomaton, reducedIntersection, semiBadStates);

                Iterator itMarkings = semiBadStatesWithMarkings.entrySet().iterator();

                Iterator<List<String>> itAllSourcePlaces = allSourcePlaces.iterator();
                Iterator<List<String>> itAllTargetPlaces = allTargetPlaces.iterator();

                while (itMarkings.hasNext() && itAllSourcePlaces.hasNext() && itAllTargetPlaces.hasNext()) {
                    Map.Entry<Transition, TransitionMarkingPair> entry = (Map.Entry<Transition, TransitionMarkingPair>) itMarkings.next();
                    String sourceMarkingString = entry.getValue().getSourceMarking().toString();
                    String targetMarkingString = entry.getValue().getTargetMarking().toString();

                    List<String> sourcePlaces = itAllSourcePlaces.next();
                    for (String sourcePlace : sourcePlaces) {
                        assertTrue(sourceMarkingString.contains(sourcePlace));
                    }

                    List<String> targetPlaces = itAllTargetPlaces.next();
                    for (String targetPlace : targetPlaces) {
                        assertTrue(targetMarkingString.contains(targetPlace));
                    }
                }*/
            }

            @Test
            @DisplayName("gets places to remove")
            void getPlacesToRemove() {
                /*
                * Map<State, List<Transition>> semiBadStates = AutomatonUtils.getSemiBadStates(reducedIntersection);
                Map<Transition, TransitionMarkingPair> semiBadStatesWithMarkings = AutomatonUtils.getSemiBadMarkingsFromOriginal(petriNetAutomaton, reducedIntersection, semiBadStates);
                Queue<Place> toRemove = AutomatonUtils.findPlacesToRemoveFromSemiBadMarkings(semiBadStatesWithMarkings);


                for (String place : toRemovePlaces) {
                    assertTrue(toRemove.toString().contains(place));
                }*/
            }

        }


    }


}
