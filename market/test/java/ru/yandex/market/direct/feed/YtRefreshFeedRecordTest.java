package ru.yandex.market.direct.feed;

import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.market.core.direct.feed.model.RefreshFeedRecord;
import ru.yandex.market.core.direct.feed.model.YtRefreshFeedRecord;
import ru.yandex.market.yt.binding.YTBinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class YtRefreshFeedRecordTest {

    static Stream<Arguments> serializationData() {
        return Stream.of(
                arguments(RefreshFeedRecord.builder()
                    .setFeedId(1)
                    .setEtag("etag1")
                    .setHash("hash1")
                    .setHttpCode(200)
                    .setUpdatedAt(Instant.EPOCH)
                    .build()
                ),
                arguments(RefreshFeedRecord.builder()
                    .setFeedId(1)
                    .setEtag("etag1")
                    .setHash("hash1")
                    .setHttpCode(200)
                    .setUpdatedAt(Instant.EPOCH)
                    .build()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("serializationData")
    void serialization(RefreshFeedRecord record) {
        var binder = YTBinder.getBinder(YtRefreshFeedRecord.class);
        assertThat(binder.getCypressSchema().toString()).startsWith("<\"unique_keys\"=%true;" +
                "\"strict\"=%true>[{\"name\"=\"feedId\";\"type\"=\"int64\";\"sort_order\"=\"ascending\";" +
                "\"required\"=%true};{\"name\"=\"etag\";\"type\"=\"string\";\"required\"=%false};{\"name\"=\"hash\";" +
                "\"type\"=\"string\";\"required\"=%false};{\"name\"=\"httpCode\";\"type\"=\"int32\";" +
                "\"required\"=%false};{\"name\"=\"errorMessage\";\"type\"=\"string\";\"required\"=%false};" +
                "{\"name\"=\"updatedAt\";\"type\"=\"int64\";\"required\"=%false}]");

        var ysonConsumer = new YTreeBuilder();
        binder.getSerializer().serialize(new YtRefreshFeedRecord(record), ysonConsumer);
        var serialized = ysonConsumer.build();

        var deserialized = binder.getSerializer().deserialize(serialized).toRefreshRecord();
        assertThat(deserialized).isEqualTo(record);
    }
}
