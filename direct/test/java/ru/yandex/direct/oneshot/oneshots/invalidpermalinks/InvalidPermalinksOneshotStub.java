package ru.yandex.direct.oneshot.oneshots.invalidpermalinks;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.ytwrapper.client.YtProvider;

public class InvalidPermalinksOneshotStub extends InvalidPermalinksOneshot {
    private static final Logger logger = LoggerFactory.getLogger(InvalidPermalinksOneshotStub.class);

    protected List<Pair<Integer, List<Pair<ClientId, Long>>>> invokes = new ArrayList<>();

    protected InvalidPermalinksOneshotStub(YtProvider ytProvider, int chunkSize) {
        super(ytProvider, chunkSize);
    }

    @Override
    protected void processItems(int shard, List<Pair<ClientId, Long>> items) {
        invokes.add(Pair.of(shard, items));
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
