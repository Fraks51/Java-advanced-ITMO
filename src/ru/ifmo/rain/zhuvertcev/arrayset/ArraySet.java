package ru.ifmo.rain.zhuvertcev.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> data;
    private Comparator<? super T> comparator;

    public ArraySet() {
        this(List.of());
    }

    public ArraySet(final Collection<? extends T> collection) {
        try {
            if (collectionsIsStrictSort(collection)) {
                data = List.copyOf(collection);
            } else {
                data = new ArrayList<>(new TreeSet<>(collection));
            }
        } catch (final ClassCastException e) {
            throw new ClassCastException("If your class, don't implement Comparable, please write your comparator for this class");
        }

    }

    public ArraySet(final Comparator<? super T> comparator) {
        this(List.of(), comparator);
    }

    public ArraySet(final Collection<? extends T> collection, final Comparator<? super T> comparator) {
        this.comparator = comparator;
        if (collectionsIsStrictSort(collection)) {
            data = List.copyOf(collection);
        } else {
            Set<T> set = new TreeSet<>(comparator);
            set.addAll(collection);
            data = new ArrayList<T>(set);
        }
    }

    @SuppressWarnings("unchecked")
    private int compare(final T a, final T b) {
        int compareResult = 0;
        if (a instanceof Comparable) {
            compareResult = ((Comparable) a).compareTo(b);
        }
        return comparator == null ? compareResult : comparator.compare(a, b);
    }

    private boolean collectionsIsStrictSort(final Collection<? extends T> collection)
    {
        T prev = null;
        for (T current : collection) {
            if (current == null)
                throw new NullPointerException("Collection contains null");
            if (prev != null) {
                if (compare(prev, current) >= 0) {
                    return false;
                }
            }
            prev = current;
        }
        return true;
    }

    private ArraySet(final List<T> list, final Comparator<? super T> comparator) {
        this.comparator = comparator;
        if (list instanceof ReversedList) {
            ((ReversedList) list).reverse();
        }
        data = list;
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(data).iterator();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public T first() {
        if (size() == 0)
            throw new NoSuchElementException();
        return data.get(0);
    }

    @Override
    public T last() {
        if (size() == 0)
            throw new NoSuchElementException();
        return data.get(size() - 1);
    }

    private int findIndex(final T element, final int wantFindLess, final int equal) {
        if (element == null)
            throw new NullPointerException();
        int res;
        int i = Collections.binarySearch(data, element, comparator);
        if (i >= 0) {
            res = (equal == 1) ? i : (i + 1 - 2 * wantFindLess);
        } else {
            i = -(i + 1);
            res = i - wantFindLess;
        }
        if (0 <= res && res < size()) return res;
        return -1;
    }

    private T getElement(final T element, final int wantFindLess, final int equal) {
        int index = findIndex(element, wantFindLess, equal);
        return index != -1 ? data.get(index) : null;
    }

    @Override
    public T lower(final T t) {
        return getElement(t, 1, 0);
    }

    @Override
    public T floor(final T t) {
        return getElement(t, 1, 1);
    }

    @Override
    public T ceiling(final T t) {
        return getElement(t, 0, 1);
    }

    @Override
    public T higher(final T t) {
        return getElement(t, 0, 0);
    }

    private NavigableSet<T> getSubSet(final T fromElement, final boolean fromInclusive, final T toElement, final boolean toInclusive) {
        final int indexFrom = findIndex(fromElement, 0, fromInclusive ? 1 : 0),
                indexTo = findIndex(toElement, 1, toInclusive ? 1 : 0);
        if (indexFrom == -1 || indexTo == -1 || indexFrom > indexTo)
            return new ArraySet<T>(comparator);
        return new ArraySet<>(data.subList(indexFrom, indexTo + 1), comparator);
    }

    @Override
    public NavigableSet<T> subSet(final T fromElement, final boolean fromInclusive, final T toElement, final boolean toInclusive) {
        return getSubSet(fromElement, fromInclusive, toElement, toInclusive);
    }

    @Override
    public NavigableSet<T> headSet(final T toElement, final boolean inclusive) {
        if (size() == 0)
            return new ArraySet<T>(comparator);
        return getSubSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(final T fromElement, final boolean inclusive) {
        if (size() == 0)
            return new ArraySet<T>(comparator);
        return getSubSet(fromElement, inclusive, last(), true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SortedSet<T> subSet(final T fromElement, final T toElement) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("Exception: fromElement is bigger, than toElement");
        }
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(final T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<T>(new ReversedList<>(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
        try {
            if (o == null)
                throw new ClassCastException("Contains was call with null!");
            return Collections.binarySearch(data, (T) o, comparator) >= 0;
        } catch (ClassCastException e) {
            System.err.println("Cast Exception");
            System.err.println(e.getMessage());
            return false;
        }
    }

}
