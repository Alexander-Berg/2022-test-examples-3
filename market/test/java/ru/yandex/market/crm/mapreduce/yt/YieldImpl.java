package ru.yandex.market.crm.mapreduce.yt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.yandex.inside.yt.kosher.operations.Yield;

/**
 * Created by vdorogin on 24.07.17.
 */
public class YieldImpl<T> implements Yield<T> {

    private List<T> array;

    public YieldImpl() {
        array = new ArrayList<>();
    }

    @Override
    public void yield(int index, T value) {
        array.add(index, value);
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    public T get(int i) {
        return array.get(i);
    }

    public List<T> get() {
        return array;
    }
}
