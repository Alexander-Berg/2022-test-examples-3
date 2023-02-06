package ru.yandex.market.stat.dicts.loaders;

import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.records.TestDictionary;

import java.time.LocalDateTime;

import static ru.yandex.market.stat.dicts.loaders.DefaultLoaderParams.DEFAULT_LOAD_TIMEOUT_MINUTES;

public class TestLoader implements DictionaryLoader<TestDictionary> {

    private final Long duration;
    private Dictionary<TestDictionary> dictionary;

    public TestLoader(LoaderScale scale) {
        this("test_dictionary", scale, DEFAULT_LOAD_TIMEOUT_MINUTES);
    }

    public TestLoader(String dictName, LoaderScale scale, Long duration) {
        this.dictionary = Dictionary.from(dictName, TestDictionary.class, scale, LoaderScale.DEFAULT_NO_TTL);
        this.duration = duration;
    }

    @Override
    public Class<TestDictionary> getRecordClass() {
        return TestDictionary.class;
    }

    @Override
    public long load(String cluster, LocalDateTime day) throws Exception {
        return 0;
    }

    @Override
    public DictionaryLoadIterator<TestDictionary> iterator(LocalDateTime day) {
        return null;
    }

    @Override
    public Dictionary getDictionary() {
        return dictionary;
    }

    @Override
    public String getSystemSource() {
        return "test";
    }

    @Override
    public Long getMaxDurationInMinutes() {
        return duration;
    }
}
