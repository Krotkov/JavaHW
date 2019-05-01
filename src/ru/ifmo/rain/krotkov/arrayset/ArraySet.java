package ru.ifmo.rain.krotkov.arrayset;

import java.util.*;

import static java.util.Objects.isNull;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> data;
    private final Comparator<? super T> comparator;


    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(Comparator<? super T> comparator) {
        this(Collections.emptyList(), comparator);
    }

    public ArraySet(Collection<? extends T> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends T> other, Comparator<? super T> comparator) {
        Set<T> buffer = new TreeSet<>(comparator);
        buffer.addAll(other);
        data = new ArrayList<>(buffer);
        this.comparator = comparator;
    }

    private ArraySet(List<T> other, Comparator<? super T> comparator) {
        data = other;
        this.comparator = comparator;
    }

    private int getBinarySearchIndex(int existsShift, int notExistsShift, T elem) {
        int index = Collections.binarySearch(data, elem, comparator);
        if (index >= 0) {
            return index + existsShift;
        } else {
            return -index - 1 + notExistsShift;
        }
    }

    private boolean checkIndex(int index) {
        return (index >= 0 && index < data.size());
    }

    private T getElem(int index) {
        return checkIndex(index) ? data.get(index) : null;
    }

    private T getBinarySearchElem(int existsShift, int notExistsShift, T elem) {
        return getElem(getBinarySearchIndex(existsShift, notExistsShift, elem));
    }

    @Override
    public T lower(T t) {
        return getBinarySearchElem(-1, -1, t);
    }

    @Override
    public T floor(T t) {
        return getBinarySearchElem(0, -1, t);
    }

    @Override
    public T ceiling(T t) {
        return getBinarySearchElem(0, 0, t);
    }

    @Override
    public T higher(T t) {
        return getBinarySearchElem(1, 0, t);
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

    private static class DescendingList<T> extends AbstractList<T> {
        private final List<T> data;
        private boolean isReversed;

        DescendingList(List<T> list) {
            if (list instanceof DescendingList) {
                data = ((DescendingList<T>) list).data;
                isReversed = !((DescendingList<T>) list).isReversed;
            } else {
                data = list;
                isReversed = true;
            }
        }

        @Override
        public T get(int index) {
            return data.get(isReversed ? size() - 1 - index : index);
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new DescendingList<>(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (!validateOrder(fromElement, toElement)) {
            throw new IllegalArgumentException();
        }
        return cutSet(fromElement, fromInclusive, toElement, toInclusive);
    }

    private NavigableSet<T> cutSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int left = getBinarySearchIndex(fromInclusive ? 0 : 1, 0, fromElement);
        int right = getBinarySearchIndex(toInclusive ? 0 : -1, -1, toElement) + 1;
        return (left >= right || left == -1 || right > data.size() || right == -1) ? new ArraySet<>(comparator)
                : new ArraySet<>(data.subList(left, right), comparator);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        return isEmpty() ? new ArraySet<>(comparator) : cutSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        return isEmpty() ? new ArraySet<>(comparator) : cutSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        if (!validateOrder(fromElement, toElement)) {
            throw new IllegalArgumentException();
        }
        return cutSet(fromElement, true, toElement, false);
    }

    private boolean validateOrder(T from, T to) {
        if (isNull(comparator)) {
            return isNull(from) || isNull(to) || ((Comparable) from).compareTo(to) <= 0;
        }
        return comparator.compare(from, to) <= 0;
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    private void checkEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public T first() {
        checkEmpty();
        return data.get(0);
    }

    @Override
    public T last() {
        checkEmpty();
        return data.get(data.size() - 1);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(data, (T) o, comparator) >= 0;
    }
}
