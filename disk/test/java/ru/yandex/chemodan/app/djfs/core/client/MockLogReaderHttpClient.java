package ru.yandex.chemodan.app.djfs.core.client;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;

public class MockLogReaderHttpClient extends LogReaderHttpClient {
    private MapF<String, Long> counters;

    public MockLogReaderHttpClient() {
        super(null, null);
        counters = Cf.hashMap();
    }

    public void putValue(String publicHash, Long value) {
        counters.put(publicHash, value);
    }

    public void reset() {
        counters.clear();
    }

    @Override
    public Option<Long> getCounter(String publicHash) {
        return counters.getO(publicHash);
    }
}
