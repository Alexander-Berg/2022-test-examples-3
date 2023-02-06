package ru.yandex.market.core.periodic_survey.yt;

import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.market.core.periodic_survey.model.SurveyId;
import ru.yandex.market.core.periodic_survey.model.SurveyRecord;
import ru.yandex.market.core.periodic_survey.model.SurveyStatus;
import ru.yandex.market.core.periodic_survey.model.SurveyType;
import ru.yandex.market.yt.binding.YTBinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class YtSurveyRecordTest {

    static Stream<Arguments> serializationData() {
        return Stream.of(
                arguments(createSurveyRecord(101L, 1001L, "{\"input\":\"answer\"}",
                        "{\"viewDetails\":\"someDetails\"}")),
                arguments(createSurveyRecord(102L, 1002L, "", "")
                ));
    }

    @ParameterizedTest
    @MethodSource("serializationData")
    void serialization(SurveyRecord record) {
        var binder = YTBinder.getBinder(YtSurveyRecord.class);
        assertThat(binder.getCypressSchema().toString()).startsWith("<\"unique_keys\"=%true;\"strict\"=%true>[" +
                // ключевые поля, порядок важен для быстрого поиска по нашим запросам
                "{\"name\"=\"partnerId\";\"type\"=\"int64\";\"sort_order\"=\"ascending\";\"required\"=%true};" +
                "{\"name\"=\"userId\";\"type\"=\"int64\";\"sort_order\"=\"ascending\";\"required\"=%true};" +
                "{\"name\"=\"surveyType\";\"type\"=\"int32\";\"sort_order\"=\"ascending\";\"required\"=%true};" +
                "{\"name\"=\"createdAt\";\"type\"=\"int64\";\"sort_order\"=\"ascending\";\"required\"=%true};" +
                // остальные поля...
                "{\"name\"=\"status\";\"type\"=\"int32\";\"required\"=%false};" +
                "{\"name\"=\"answeredAt\";\"type\"=\"int64\";\"required\"=%false};" +
                "{\"name\"=\"answer\";\"type\"=\"string\";\"required\"=%false};" +
                "{\"name\"=\"viewDetails\";\"type\"=\"string\";\"required\"=%false};" +
                "{\"name\"=\"meta\";\"type\"=\"string\";\"required\"=%false}]");

        var ysonConsumer = new YTreeBuilder();
        binder.getSerializer().serialize(new YtSurveyRecord(record), ysonConsumer);
        var serialized = ysonConsumer.build();

        var deserialized = binder.getSerializer().deserialize(serialized).toSurveyRecord();
        assertThat(deserialized).isEqualTo(record);
    }

    static SurveyId createSurveyId(long partnerId, long userId) {
        return SurveyId.of(partnerId, userId, SurveyType.NPS_DROPSHIP, Instant.parse("2020-07-01T13:02:32Z"));
    }

    static SurveyRecord createSurveyRecord(long partnerId, long userId, String answer, String viewDetails) {
        return SurveyRecord.newBuilder()
                .withSurveyId(createSurveyId(partnerId, userId))
                .withStatus(SurveyStatus.ACTIVE)
                .withAnsweredAt(null)
                .withAnswer(answer)
                .withViewDetails(viewDetails)
                .build();
    }
}
