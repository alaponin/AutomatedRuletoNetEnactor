package converter.epsilon;

import rationals.*;
import rationals.transformations.TransformationsToolBox;
import rationals.transformations.UnaryTransformation;

import java.util.*;

/**
 * Created by arnelaponin on 13/03/2017.
 * Taken from jAutomata framework, since there was a bug in the original code.
 */
public class MyEpsilonTransitionRemover<L, Tr extends Transition<L>, T extends Builder<L, Tr, T>> implements UnaryTransformation<L, Tr, T> {

    @Override
    public Automaton<L, Tr, T> transform(Automaton<L, Tr, T> a) {
        Automaton<L, Tr, T> ret = new Automaton<>();
        Map<MyHashValue<State>, State> sm = new HashMap<>();
        Set<MyHashValue<State>> done = new HashSet<>();
        List<MyHashValue<State>> todo = new ArrayList<>();
        Set<State> cur = TransformationsToolBox.epsilonClosure(a.initials(), a);

        State is = ret.addState(true,TransformationsToolBox.containsATerminalState(cur));
        MyHashValue<State> hv = new MyHashValue<>(cur);
        sm.put(hv,is);
        todo.add(hv);
        do {
            MyHashValue<State> s = todo.remove(0);
            State ns =  sm.get(s);
            if(ns == null) {
                ns = ret.addState(false,TransformationsToolBox.containsATerminalState(s.s));
                sm.put(s,ns);
            }

            done.add(s);

            Map<L, Set<State>> trm = instructions(a.delta(s.s),a);
            Iterator<Map.Entry<L, Set<State>>> it = trm.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<L, Set<State>> e = it.next();
                L o = e.getKey();
                Set<State> ar = e.getValue();

                ar = TransformationsToolBox.epsilonClosure(ar, a);
                hv = new MyHashValue<>(ar);

                State ne = sm.get(hv);
                if(ne == null) {
                    ne = ret.addState(false,TransformationsToolBox.containsATerminalState(ar));
                    sm.put(hv,ne);
                }
                try {

                    ret.addTransition(new Transition<L>(ns,o,ne));
                } catch (NoSuchStateException e1) {
                }

                if(!done.contains(hv))
                    todo.add(hv);
            }
        } while (!todo.isEmpty());
        return ret;
    }

    private Map<L, Set<State>> instructions(Set<Transition<L>> s, Automaton<L, Tr, T> a) {
        Map<L, Set<State>> m = new HashMap<L, Set<State>>();
        Iterator<Transition<L>> it = s.iterator();
        while (it.hasNext()) {
            Transition<L> tr = it.next();
            L l = tr.label();
            if (l != null) {
                Set<State> st = m.get(l);
                if (st == null) {
                    st = a.getStateFactory().stateSet();
                    m.put(l,st);
                }

                st.add(tr.end());
            }
        }
        return m;
    }
}
