package converter.epsilon;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * Created by arnelaponin on 13/03/2017.
 */
public class MyHashValue<T> {

    public final int hash;

    public final Set<T> s;

    public MyHashValue(Set<T> s) {
        this.s = s;
        Random random = new Random();
        if (s.size() > 1) {
            Iterator<T> iterator = s.iterator();
            int count = 0;
            int finalHash = 0;
            while (iterator.hasNext()) {
                count++;
                finalHash += (count*1000) * iterator.next().hashCode();
            }
            this.hash = finalHash;
        } else {
            this.hash = s.hashCode();
        }

    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MyHashValue)) return false;
        return ((MyHashValue<?>) obj).hash == hash;
    }

    public int hashCode() {
        return hash;
    }

    public String toString() {
        return s.toString();
    }
}
