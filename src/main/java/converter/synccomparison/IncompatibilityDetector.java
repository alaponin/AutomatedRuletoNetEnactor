package converter.synccomparison;

import converter.automaton.StatePair;
import rationals.Automaton;

import java.util.*;
import java.util.stream.Collectors;

import rationals.State;
import rationals.Transition;

/**
 * Created by arnelaponin on 26/09/2016.
 * Good examples from Marlon's paper, NOT going to Github.
 */
public class IncompatibilityDetector {

    private final Automaton a1;
    private final Automaton a2;
    private Stack<StatePair> visited;
    private Stack<StatePair> toBeVisited;
    private List<DifferenceResult> differenceResults;

    public IncompatibilityDetector(Automaton a1, Automaton a2) {
        this.a1 = a1;
        this.a2 = a2;
        visited = new Stack<>();
        toBeVisited = new Stack<>();
        differenceResults = new ArrayList<>();
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws Exception {
        Set<State> a1InitialStates = a1.initials();
        Set<State> a2InitialStates = a2.initials();
        if (a1InitialStates.size() == a2InitialStates.size()) {
            Iterator<State> it1 = a1InitialStates.iterator();
            Iterator<State> it2 = a2InitialStates.iterator();
            while (it1.hasNext() && it2.hasNext()) {
                StatePair statePair = new StatePair(it1.next(), it2.next());
                toBeVisited.push(statePair);
            }
        } else {
            throw new Exception("The number of initial states is not equal!");
        }
    }

    public void detectIncompatibilities() {

        while (!toBeVisited.empty()) {
            System.out.println("ToBeVisited at the beginning: " + toBeVisited);
            StatePair statePair = toBeVisited.pop();
            visited.push(statePair);
            Set<TransitionPair> transitionPairs = findSamePairs(statePair.getS1(), statePair.getS2());
            System.out.println("State pair: "+statePair);

            List<Transition> diffPiPj = getSetDiff(a1.delta(statePair.getS1()), a2.delta(statePair.getS2()));
            System.out.println("diffPiPj: " + diffPiPj);
            List<Transition> diffPjPi = getSetDiff(a2.delta(statePair.getS2()), a1.delta(statePair.getS1()));
            System.out.println("diffPjPi: " + diffPjPi);


            if (diffPiPj.size() >= 1 && diffPjPi.size() == 0) {

                for (Transition tp : diffPiPj) {
                    differenceResults.add(new DifferenceResult(statePair.getS1(), tp, null, statePair.getS2(),null,null, "DEL"));

                    addToVisitedAndToBeVisitedStack(tp.end(), statePair.getS2());
                }
            }
            if (diffPjPi.size() >= 1 && diffPiPj.size() == 0) {
                for (Transition tp : diffPjPi) {
                    differenceResults.add(new DifferenceResult(statePair.getS1(), null, null, statePair.getS2(),tp,null, "ADD"));

                    addToVisitedAndToBeVisitedStack(statePair.getS1(), tp.end());
                }
            }
            List<TransitionPair> combDiff = combineIntoPairs(diffPiPj,diffPjPi);
            for (TransitionPair tp : combDiff) {
                Transition ti = tp.getT1();
                Transition tj = tp.getT2();
                Set<Transition> nextTransitionsJ = a2.delta(tj.end());
                Set<Transition> nextTransitionsI = a1.delta(ti.end());
                if (setContainsTransition(nextTransitionsJ,ti)) {
                    differenceResults.add(new DifferenceResult(statePair.getS1(), null, null, statePair.getS2(),tp.getT2(),null, "ADD"));

                    addToVisitedAndToBeVisitedStack(statePair.getS1(), tj.end());
                }
                if (setContainsTransition(nextTransitionsI,tj)) {
                    differenceResults.add(new DifferenceResult(statePair.getS1(), tp.getT1(), null, statePair.getS2(),null,null, "DEL"));

                    addToVisitedAndToBeVisitedStack(ti.end(), statePair.getS2());
                }
                if (modificationCondition(a2,nextTransitionsJ,ti) && modificationCondition(a1,nextTransitionsI,tj)) {
                    differenceResults.add(new DifferenceResult(statePair.getS1(), ti, null, statePair.getS2(),tj,null, "MOD"));

                    addToVisitedAndToBeVisitedStack(ti.end(), tj.end());
                }

            }

            for (TransitionPair tp : transitionPairs) {
                addToVisitedAndToBeVisitedStack(tp.getT1().end(), tp.getT2().end());
            }

        }

        System.out.println("------------");
        differenceResults.forEach(System.out::println);
        System.out.println("Size: " + differenceResults.size());
    }

    private void addToVisitedAndToBeVisitedStack(State s1, State s2) {
        if (!s1.isTerminal() && !s2.isTerminal()) {
            StatePair toBeVisitedStatePair = new StatePair(s1, s2);
            if(!visited.contains(toBeVisitedStatePair)) {
                toBeVisited.push(toBeVisitedStatePair);
            }
        }
    }

    private static boolean modificationCondition(Automaton a, Set<Transition> nextTransitionsOfOtherA, Transition currentTransition) {
        for (Transition t : nextTransitionsOfOtherA) {
            if (t.label().equals(currentTransition.label())) {
                return false;
            }
            State state = t.end();
            Set<Transition> outGoingFromState = a.delta(state);
            for (Transition transition : outGoingFromState) {
                if (transition.label().equals(currentTransition.label())) {
                    return false;
                }
            }

        }
        return true;
    }

    private Set<TransitionPair> findSamePairs(State s1, State s2) {
        Set<Transition> accessibleTransitions1 = a1.delta(s1);
        Set<Transition> accessibleTransitions2 = a2.delta(s2);
        Set<TransitionPair> transitionPairs = new HashSet<>();

        for (Transition t1 : accessibleTransitions1) {
            transitionPairs.addAll(accessibleTransitions2.stream()
                    .filter(t2 -> t1.label().equals(t2.label()))
                    .map(t2 -> new TransitionPair(t1, t2))
                    .collect(Collectors.toList()));
        }
        return transitionPairs;
    }

    private static List<Transition> getSetDiff(Set<Transition> s1, Set<Transition> s2) {
        Set<Transition> s = new HashSet<>(s1);
        s.removeAll(s2);
        for (Transition t : s) {
            s2.stream().filter(t2 -> t2.label().equals(t.label())).forEach(t2 -> {
                s.remove(t);
            });
        }
        return (List<Transition>) new ArrayList(s);
    }

    private static boolean setContainsTransition(Set<Transition> set, Transition transition) {
        for (Transition t : set) {
            if (t.label().equals(transition.label())) {
                return true;
            }
        }
        return false;
    }

    private static List<TransitionPair> combineIntoPairs(List<Transition> l1, List<Transition> l2) {
        List<TransitionPair> pairs = new ArrayList<>();
        for (Transition t1 : l1) {
            pairs.addAll(l2.stream().map(t2 -> new TransitionPair(t1, t2)).collect(Collectors.toList()));
        }
        return pairs;
    }
}
