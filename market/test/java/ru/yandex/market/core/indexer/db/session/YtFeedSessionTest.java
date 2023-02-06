package ru.yandex.market.core.indexer.db.session;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.market.core.indexer.model.FeedSession;
import ru.yandex.market.core.indexer.model.ReturnCode;
import ru.yandex.market.yt.binding.YTBinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.yandex.market.core.indexer.db.session.DbFeedSessionServiceTest.PARSE_LOG;
import static ru.yandex.market.core.indexer.db.session.DbFeedSessionServiceTest.PARSE_LOG_PARSED;
import static ru.yandex.market.core.indexer.db.session.DbFeedSessionServiceTest.makeSession;

class YtFeedSessionTest {
    static Stream<Arguments> serializationData() {
        return Stream.of(
                arguments(makeSession(
                        1L,
                        null,
                        null,
                        null,
                        null
                )),
                arguments(makeSession(
                        1L,
                        ReturnCode.OK,
                        ReturnCode.WARNING,
                        PARSE_LOG,
                        PARSE_LOG_PARSED
                ))
        );
    }

    @ParameterizedTest
    @MethodSource("serializationData")
    void serialization(FeedSession session) {
        var binder = YTBinder.getBinder(YtFeedSession.class);
        assertThat(binder.getCypressSchema().toString()).startsWith("<\"unique_keys\"=%true;\"strict\"=%true>[" +
                // ключевые поля, порядок важен для быстрого поиска по нашим запросам
                "{\"name\"=\"feedId\";\"type\"=\"int64\";\"sort_order\"=\"ascending\";\"required\"=%true};" +
                "{\"name\"=\"startTime\";\"type\"=\"int64\";\"sort_order\"=\"ascending\";\"required\"=%true};" +
                "{\"name\"=\"sessionName\";\"type\"=\"string\";\"sort_order\"=\"ascending\";\"required\"=%true};" +
                "{\"name\"=\"indexerType\";\"type\"=\"int32\";\"sort_order\"=\"ascending\";\"required\"=%true};" +
                "{\"name\"=\"cluster\";\"type\"=\"string\";\"sort_order\"=\"ascending\";\"required\"=%true};" +
                // остальные поля...
                "{\"name\"=\"downloadReturnCode\";\"type\"=\"int32\";\"required\"=%false};");

        var ysonConsumer = new YTreeBuilder();
        binder.getSerializer().serialize(new YtFeedSession(session), ysonConsumer);
        var serialized = ysonConsumer.build();

        var deserialized = binder.getSerializer().deserialize(serialized).toFeedSession();
        assertThat(deserialized).isEqualTo(session);
    }
}
