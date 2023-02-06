package ru.yandex.market.core.util.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.google.protobuf.CodedInputStream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.proto.indexer.v2.BlueAssortment;
import ru.yandex.market.proto.indexer.v2.ProcessLog;

/**
 * Тесты для {@link ProtobufSequentialParser}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ProtobufSequentialParserTest {

    private final ProtobufSequentialParser parser = new ProtobufSequentialParser();

    @Test
    @DisplayName("Пустое сообщение")
    void testEmptyMessage() throws IOException {
        check(ignore -> {
        }).isEmpty();

    }

    @Test
    @DisplayName("Нет нужного родительского. Есть поле перед")
    void testWithoutFirst1() throws IOException {
        check(builder -> builder.addLogMessage(ProcessLog.LogMessage.newBuilder().build())).isEmpty();
    }

    @Test
    @DisplayName("Нет нужного родительского. Есть поле после")
    void testWithoutFirst2() throws IOException {
        check(builder -> builder.setXls2CsvOutput("ppp")).isEmpty();
    }

    @Test
    @DisplayName("Есть родительское сообщение, но вложенного нет")
    void testEmptyEmbedded() throws IOException {
        check(builder -> builder.setInputFeed(BlueAssortment.InputFeed.newBuilder().build())).isEmpty();
    }

    @Test
    @DisplayName("Нужное поле есть, но оно пустое")
    void testEmptyRequested() throws IOException {
        check(
                builder -> builder.setInputFeed(BlueAssortment.InputFeed.newBuilder()
                        .addOffer(BlueAssortment.InputOffer.newBuilder().build())
                        .build())
        ).containsExactlyInAnyOrder(BlueAssortment.InputOffer.newBuilder().build());
    }

    @Test
    @DisplayName("Есть только нужные поля")
    void testOnlyRequested() throws IOException {
        check(
                builder -> builder.setInputFeed(BlueAssortment.InputFeed.newBuilder()
                        .addOffer(BlueAssortment.InputOffer.newBuilder()
                                .setTitle("my_title")
                                .build())
                        .build())
        ).containsExactlyInAnyOrder(
                BlueAssortment.InputOffer.newBuilder()
                        .setTitle("my_title")
                        .build()
        );
    }

    @Test
    @DisplayName("Помимо запрошенных, есть еще поля до")
    void testRequestedWithPrev() throws IOException {
        check(
                builder -> builder.addLogMessage(ProcessLog.LogMessage.newBuilder()
                                .setCode("code")
                                .build())
                        .setInputFeed(BlueAssortment.InputFeed.newBuilder()
                                .addOffer(BlueAssortment.InputOffer.newBuilder()
                                        .setTitle("my_title")
                                        .build())
                                .build())
        ).containsExactlyInAnyOrder(
                BlueAssortment.InputOffer.newBuilder()
                        .setTitle("my_title")
                        .build()
        );
    }

    @Test
    @DisplayName("Помимо запрошенных, есть еще поля после")
    void testRequestedWithPost() throws IOException {
        check(
                builder -> builder.setInputFeed(BlueAssortment.InputFeed.newBuilder()
                                .addOffer(BlueAssortment.InputOffer.newBuilder()
                                        .setTitle("my_title")
                                        .build())
                                .build())
                        .setXls2CsvOutput("ppp")
        ).containsExactlyInAnyOrder(
                BlueAssortment.InputOffer.newBuilder()
                        .setTitle("my_title")
                        .build()
        );
    }

    @Test
    @DisplayName("Помимо запрошенных, есть еще поля до и после")
    void testRequestedWithPrevAndPost() throws IOException {
        check(
                builder -> builder.addLogMessage(ProcessLog.LogMessage.newBuilder()
                                .setCode("code")
                                .build())
                        .setInputFeed(BlueAssortment.InputFeed.newBuilder()
                                .addOffer(BlueAssortment.InputOffer.newBuilder()
                                        .setTitle("my_title")
                                        .build())
                                .build())
                        .setXls2CsvOutput("ppp")
        ).containsExactlyInAnyOrder(
                BlueAssortment.InputOffer.newBuilder()
                        .setTitle("my_title")
                        .build()
        );
    }

    @Test
    @DisplayName("Несколько запрошенных полей")
    void testMultipleRequested() throws IOException {
        check(
                builder -> builder.addLogMessage(ProcessLog.LogMessage.newBuilder()
                                .setCode("code")
                                .build())
                        .setInputFeed(BlueAssortment.InputFeed.newBuilder()
                                .addOffer(BlueAssortment.InputOffer.newBuilder()
                                        .setTitle("my_title1")
                                        .build())
                                .addOffer(BlueAssortment.InputOffer.newBuilder()
                                        .setTitle("my_title2")
                                        .build())
                                .build())
                        .setXls2CsvOutput("ppp")
        ).containsExactlyInAnyOrder(
                BlueAssortment.InputOffer.newBuilder()
                        .setTitle("my_title1")
                        .build(),
                BlueAssortment.InputOffer.newBuilder()
                        .setTitle("my_title2")
                        .build()
        );
    }

    @Test
    @DisplayName("Читаем string поле")
    void testStringField() throws IOException {
        BlueAssortment.CheckResult protoData = BlueAssortment.CheckResult.newBuilder()
                .addLogMessage(ProcessLog.LogMessage.newBuilder()
                        .setCode("code1")
                        .build())
                .addLogMessage(ProcessLog.LogMessage.newBuilder()
                        .setCode("code2")
                        .build())
                .setXls2CsvOutput("ppp")
                .build();
        CodedInputStream stream = CodedInputStream.newInstance(protoData.toByteArray());
        int[] path = {
                BlueAssortment.CheckResult.LOG_MESSAGE_FIELD_NUMBER,
                ProcessLog.LogMessage.CODE_FIELD_NUMBER
        };

        List<String> result = new ArrayList<>();
        parser.parse(stream, path, new ProtobufStringHandler() {
            @Override
            public void onField(@Nonnull String message) {
                result.add(message);
            }
        });
        Assertions.assertThat(result)
                .containsExactlyInAnyOrder("code1", "code2");
    }

    @Test
    @DisplayName("Читаем fixed32 поле")
    void testFixed32Field() throws IOException {
        BlueAssortment.CheckResult protoData = BlueAssortment.CheckResult.newBuilder()
                .addLogMessage(ProcessLog.LogMessage.newBuilder()
                        .setFeedId(100)
                        .build())
                .build();
        CodedInputStream stream = CodedInputStream.newInstance(protoData.toByteArray());
        int[] path = {
                BlueAssortment.CheckResult.LOG_MESSAGE_FIELD_NUMBER,
                ProcessLog.LogMessage.FEED_ID_FIELD_NUMBER
        };

        List<Integer> result = new ArrayList<>();
        parser.parse(stream, path, new ProtobufFixedInt32Handler() {
            @Override
            public void onField(@Nonnull Integer message) {
                result.add(message);
            }
        });
        Assertions.assertThat(result)
                .containsExactlyInAnyOrder(100);
    }

    @Test
    @DisplayName("Читаем int поле")
    void testIntField() throws IOException {
        BlueAssortment.CheckResult protoData = BlueAssortment.CheckResult.newBuilder()
                .setParseStats(BlueAssortment.ParseStats.newBuilder()
                        .setAcceptedOffers(200)
                        .build())
                .build();
        CodedInputStream stream = CodedInputStream.newInstance(protoData.toByteArray());
        int[] path = {
                BlueAssortment.CheckResult.PARSE_STATS_FIELD_NUMBER,
                BlueAssortment.ParseStats.ACCEPTED_OFFERS_FIELD_NUMBER
        };

        List<Integer> result = new ArrayList<>();
        parser.parse(stream, path, new ProtobufInt32Handler() {
            @Override
            public void onField(@Nonnull Integer message) {
                result.add(message);
            }
        });
        Assertions.assertThat(result)
                .containsExactlyInAnyOrder(200);
    }

    @Test
    @DisplayName("Читаем enum поле")
    void testLongField() throws IOException {
        BlueAssortment.CheckResult protoData = BlueAssortment.CheckResult.newBuilder()
                .addLogMessage(ProcessLog.LogMessage.newBuilder()
                        .setLevelEnum(NMarket.NProcessLog.ProcessLog.Level.FATAL)
                        .build())
                .build();
        CodedInputStream stream = CodedInputStream.newInstance(protoData.toByteArray());
        int[] path = {
                BlueAssortment.CheckResult.LOG_MESSAGE_FIELD_NUMBER,
                ProcessLog.LogMessage.LEVEL_ENUM_FIELD_NUMBER
        };

        List<NMarket.NProcessLog.ProcessLog.Level> result = new ArrayList<>();
        parser.parse(stream, path, new ProtobufEnumHandler<>(NMarket.NProcessLog.ProcessLog.Level::forNumber) {
            @Override
            public void onField(@Nonnull NMarket.NProcessLog.ProcessLog.Level message) {
                result.add(message);
            }
        });
        Assertions.assertThat(result)
                .containsExactlyInAnyOrder(NMarket.NProcessLog.ProcessLog.Level.FATAL);
    }


    private ListAssert<BlueAssortment.InputOffer> check(Consumer<BlueAssortment.CheckResult.Builder> builderConsumer) throws IOException {
        BlueAssortment.CheckResult.Builder dataBuilder = BlueAssortment.CheckResult.newBuilder();
        builderConsumer.accept(dataBuilder);
        CodedInputStream stream = CodedInputStream.newInstance(dataBuilder.build().toByteArray());

        int[] path = {
                BlueAssortment.CheckResult.INPUT_FEED_FIELD_NUMBER,
                BlueAssortment.InputFeed.OFFER_FIELD_NUMBER
        };

        List<BlueAssortment.InputOffer> result = new ArrayList<>();
        parser.parse(stream, path, new ProtobufMessageHandler<>(BlueAssortment.InputOffer.parser()) {
            @Override
            public void onField(@Nonnull BlueAssortment.InputOffer message) {
                result.add(message);
            }
        });
        return Assertions.assertThat(result);
    }
}
