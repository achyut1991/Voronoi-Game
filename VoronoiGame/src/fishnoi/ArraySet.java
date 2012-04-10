package fishnoi;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class ArraySet<E> extends AbstractSet<E> {

    private ArrayList<E> items;        

    public ArraySet () {
        this(3);
    }

    public ArraySet (int initialCapacity) {
        items  = new ArrayList<E>(initialCapacity);
    }

    public ArraySet (Collection<? extends E> collection) {
        items = new ArrayList<E>(collection.size());
        for (E item: collection)
            if (!items.contains(item)) items.add(item);
    }

    public E get (int index) throws IndexOutOfBoundsException {
        return items.get(index);
    }

    public boolean containsAny (Collection<?> collection) {
        for (Object item: collection)
            if (this.contains(item)) return true;
        return false;
    }

    @Override
    public boolean add(E item) {
        if (items.contains(item)) return false;
        return items.add(item);
    }

    @Override
    public Iterator<E> iterator() {
        return items.iterator();
    }

    @Override
    public int size() {
        return items.size();
    }

}
