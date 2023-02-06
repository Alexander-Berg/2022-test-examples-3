package ru.yandex.market.stat.dicts;

import ru.yandex.market.stat.dicts.common.DictionaryBuilder;
import ru.yandex.market.stat.dicts.records.DictionaryRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
public class TestDictionaryBuilder<T extends DictionaryRecord> implements DictionaryBuilder<T> {

    public List<T> records = new ArrayList<>();

    @Override
    public void add(T r) throws IOException {
        records.add(r);
    }

    @Override
    public long publish() throws IOException {
        throw new IllegalStateException();
    }


    @Override
    public void close() throws IOException {
        throw new IllegalStateException();
    }
}
