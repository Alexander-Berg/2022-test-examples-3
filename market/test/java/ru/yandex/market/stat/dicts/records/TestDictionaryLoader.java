package ru.yandex.market.stat.dicts.records;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoadIterator;
import ru.yandex.market.stat.dicts.loaders.DictionaryLoader;
import ru.yandex.market.stat.dicts.loaders.LoaderScale;
import ru.yandex.market.stat.dicts.services.YtDictionaryStorage;

@AllArgsConstructor
public class TestDictionaryLoader implements DictionaryLoader<TestDictionary> {

    @Getter
    private final Class<TestDictionary> recordClass = TestDictionary.class;

    private YtDictionaryStorage dictionaryStorage;
    private List<String> ids;
    private LoaderScale scale;
    private boolean isHeavy;
    private boolean isSla;

    private static long ttlSeconds = 3600;

    public TestDictionaryLoader(YtDictionaryStorage dictionaryStorage,
                                List<String> ids,
                                LoaderScale scale) {
        this.dictionaryStorage = dictionaryStorage;
        this.ids = ids;
        this.scale = scale;
        this.isHeavy = false;
        this.isSla = false;
    }

    @Override
    public DictionaryLoadIterator<TestDictionary> iterator(LocalDateTime day) {
        return DictionaryLoadIterator.from(TestDictionary.makeDataWithIds(ids));
    }

    @Override
    public long load(String cluster, LocalDateTime day) throws IOException {
        return dictionaryStorage.save(cluster, getDictionary(), day, iterator(cluster, day));
    }

    @Override
    public Dictionary<TestDictionary> getDictionary() {
        Dictionary<TestDictionary> testDictionaryDictionary = Dictionary.fromClass(getRecordClass(), scale);
        testDictionaryDictionary.setSla(isSla);
        return testDictionaryDictionary;
    }

    @Override
    public String getSystemSource() {
        return "test";
    }

}
