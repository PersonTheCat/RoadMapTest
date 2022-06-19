package personthecat.roadmap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DemoWeightedList<E> implements Collection<E> {

    private final NavigableMap<Double, E> entries = new TreeMap<>();
    private final double defaultWeight;
    private double total = 1.0;

    public DemoWeightedList() {
        this(1.0);
    }

    public DemoWeightedList(final double defaultWeight) {
        this.defaultWeight = defaultWeight;
    }

    public void add(final double weight, final E entry) {
        this.entries.put(weight, entry);
        this.total += weight;
    }

    @Nullable
    public E sample(final Random rand) {
        final Map.Entry<Double, E> entry =
            this.entries.higherEntry(rand.nextDouble() * this.total);
        return entry != null ? entry.getValue() : null;
    }

    @Override
    public int size() {
        return this.entries.size();
    }

    @Override
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return this.entries.containsValue(o);
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return this.entries.values().iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return this.entries.values().toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(final @NotNull T[] a) {
        return this.entries.values().toArray(a);
    }

    @Override
    public boolean add(final E e) {
        this.add(this.defaultWeight, e);
        return true;
    }

    @Override
    public boolean remove(final Object o) {
        return this.entries.values().remove(o);
    }

    @Override
    public boolean containsAll(final @NotNull Collection<?> c) {
        return this.entries.values().containsAll(c);
    }

    @Override
    public boolean addAll(final @NotNull Collection<? extends E> c) {
        c.forEach(this::add);
        return true;
    }

    @Override
    public boolean removeAll(final @NotNull Collection<?> c) {
        return this.entries.values().removeAll(c);
    }

    @Override
    public boolean retainAll(final @NotNull Collection<?> c) {
        return this.entries.values().retainAll(c);
    }

    @Override
    public void clear() {
        this.entries.clear();
    }
}
