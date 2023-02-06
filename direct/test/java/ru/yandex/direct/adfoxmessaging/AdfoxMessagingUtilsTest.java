package ru.yandex.direct.adfoxmessaging;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import com.google.common.io.Resources;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.adfoxmessaging.protos.AdfoxDealCreatePayload;
import ru.yandex.direct.adfoxmessaging.protos.AdfoxDealUpdatePayload;
import ru.yandex.direct.adfoxmessaging.protos.CommonMessage;
import ru.yandex.direct.adfoxmessaging.protos.DirectDealUpdatePayload;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.adfoxmessaging.protos.AdfoxDealStatus.active;
import static ru.yandex.direct.adfoxmessaging.protos.AdfoxDealStatus.created;

class AdfoxMessagingUtilsTest {

    @Test
    void testAdfoxDealCreatePayloadPrinting() throws IOException {
        AdfoxDealCreatePayload.Builder builder = AdfoxDealCreatePayload.newBuilder();
        AdfoxDealCreatePayload payload = builder.setDealExportId(12345L)
                .setName("Super Deal")
                .setStatus(created)
                .build();

        String actualJson = AdfoxMessagingUtils.toJson(payload);
        String expectedJson = "{\"dealExportId\":\"12345\",\"name\":\"Super Deal\",\"status\":\"created\"}";
        assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void testDealCreateMessagePrinting() throws IOException {
        AdfoxDealCreatePayload payload =
                AdfoxDealCreatePayload.newBuilder()
                        .setDealExportId(12345L)
                        .setName("Super Deal")
                        .setStatus(created)
                        .build();

        CommonMessage message =
                CommonMessage.newBuilder()
                        .setType("create")
                        .setPayload(Any.pack(payload))
                        .build();

        String actualJsonMessage = AdfoxMessagingUtils.toJson(message);
        String expectedJsonMessage =
                "{\"type\":\"create\",\"payload\":{\"@type\":\"type.googleapis.com/NDirectAdfoxMessaging.AdfoxDealCreatePayload\",\"dealExportId\":\"12345\",\"name\":\"Super Deal\",\"status\":\"created\"}}";

        assertThat(actualJsonMessage).isEqualTo(expectedJsonMessage);
    }

    @Test
    void testDealCreateMessageParsing() throws InvalidProtocolBufferException {
        String incomingMessage =
                "{\"type\":\"create\",\"payload\":{\"@type\":\"type.googleapis.com/NDirectAdfoxMessaging.AdfoxDealCreatePayload\",\"dealExportId\":\"12345\",\"name\":\"Super Deal\",\"status\":\"created\"}}";
        CommonMessage commonMessage = AdfoxMessagingUtils.parseJson(incomingMessage);

        Any payload = commonMessage.getPayload();
        AdfoxDealCreatePayload actualPayload;
        if (payload.is(AdfoxDealCreatePayload.class)) {
            actualPayload = payload.unpack(AdfoxDealCreatePayload.class);
        } else {
            throw new IllegalArgumentException("Unsupported class in payload: " + payload.getTypeUrl());
        }

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(commonMessage.getType()).isEqualTo("create");
            softly.assertThat(actualPayload.getDealExportId()).isEqualTo(12345L);
            softly.assertThat(actualPayload.getName()).isEqualTo("Super Deal");
            softly.assertThat(actualPayload.getStatus()).isEqualTo(created);
        });
    }

    @Test
    void testAdfoxDealCreateParsing() throws Exception {
        String resourcePath = "ru/yandex/direct/adfoxmessaging/adfox_deal_create_message_example.json";
        String inputJson = getResourceFile(resourcePath);

        CommonMessage commonMessage = AdfoxMessagingUtils.parseJson(inputJson);

        Any payloadAny = commonMessage.getPayload();
        assertThat(payloadAny.is(AdfoxDealCreatePayload.class)).isTrue();
        AdfoxDealCreatePayload payload = payloadAny.unpack(AdfoxDealCreatePayload.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(commonMessage.getType()).isEqualTo("deal:create");

            softly.assertThat(payload.getDealExportId()).isEqualTo(123456789L);
            softly.assertThat(payload.getPublisherName()).isEqualTo("Auto.ru");
            softly.assertThat(payload.getName()).isEqualTo("Баннер вверху domain.ru по 100р за килопоказ");
            softly.assertThat(payload.getStatus()).isEqualTo(created);
            softly.assertThat(payload.getDateTimeStartUtc()).isEqualTo("2018-02-12T23:01:12Z");
            softly.assertThat(payload.getDateTimeEndUtc()).isEqualTo("2018-03-01T12:11:00Z");
            softly.assertThat(payload.getAgencyId()).isEqualTo(7654321L);
            softly.assertThat(payload.getDescription())
                    .isEqualTo("Сделка по продаже трафика, которую мы обсуждали с Игорем в прошлую пятницу");
            softly.assertThat(payload.getContacts()).isEqualTo("Иванов Пётр, тел: +7 (987) 123-45-67");
            softly.assertThat(payload.getTargetingsText())
                    .isEqualTo("Пол: муж\nВозраст: 35-45\nДоход: средний\nОбозреватель: chrome-like");
            softly.assertThat(payload.getAdfoxSpecials().getFieldsMap()).containsKey("someField");
            softly.assertThat(payload.getType()).isEqualTo(10);
            softly.assertThat(payload.getAgencyRevenueRatio()).isEqualTo(120000L);
            softly.assertThat(payload.getAgencyRevenueType()).isEqualTo(12);
            softly.assertThat(payload.getCurrencyId()).isEqualTo("643");
            softly.assertThat(payload.getCpm()).isEqualTo(123450000L);
            softly.assertThat(payload.getMarginRatio()).isEqualTo(50000L);
            softly.assertThat(payload.getExpectedImpressionsPerWeek()).isEqualTo(120000L);
            softly.assertThat(payload.getExpectedMoneyPerWeek()).isEqualTo(45000L);
            softly.assertThat(payload.getPlacementsCount()).isEqualTo(2);
        });
    }

    @Test
    void testAdfoxDealUpdatePrinting() throws InvalidProtocolBufferException {
        AdfoxDealUpdatePayload adfoxDealUpdatePayload = AdfoxDealUpdatePayload.newBuilder()
                .setDealExportId(2123456L)
                .setStatus(active)
                .build();
        CommonMessage message = CommonMessage.newBuilder()
                .setType("deal:update")
                .setPayload(Any.pack(adfoxDealUpdatePayload))
                .build();

        String actualJson = AdfoxMessagingUtils.getHumanReadablePrinter().print(message);
        String expected =
                getResourceFile("ru/yandex/direct/adfoxmessaging/adfox_deal_update_message_example.json").trim();
        assertThat(actualJson).isEqualTo(expected);
    }

    @Test
    void testDirectDealUpdatePrinting() throws InvalidProtocolBufferException {
        DirectDealUpdatePayload adfoxDealUpdatePayload = DirectDealUpdatePayload.newBuilder()
                .setDealExportId(2123456L)
                .setStatus(active)
                .build();
        CommonMessage message = CommonMessage.newBuilder()
                .setType("deal:update")
                .setPayload(Any.pack(adfoxDealUpdatePayload))
                .build();

        String actualJson = AdfoxMessagingUtils.getHumanReadablePrinter().print(message);
        String expected =
                getResourceFile("ru/yandex/direct/adfoxmessaging/direct_deal_update_message_example.json").trim();
        assertThat(actualJson).isEqualTo(expected);
    }

    @SuppressWarnings("UnstableApiUsage")
    private String getResourceFile(String resourcePath) {
        try {
            URL url = Resources.getResource(resourcePath);
            return Resources.toString(url, Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
