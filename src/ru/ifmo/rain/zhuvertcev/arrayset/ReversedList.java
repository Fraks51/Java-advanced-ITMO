package ru.ifmo.rain.zhuvertcev.arrayset;

import java.util.AbstractList;
import java.util.List;

public class ReversedList<T> extends AbstractList<T> {
    private final List<T> list;
    private boolean reversed;

    ReversedList(List<T> list) {
        this.list = list;
        reversed = false;
    }

    @Override
    public int size() {
        return list.size();
    }

    public void reverse() {
        reversed = !reversed;
    }

    @Override
    public T get(final int index) {
        return list.get(reversed ? size() - index - 1 : index);
    }
}
