package ru.yandex.direct.jobs.adfox.messaging.export;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;

import com.google.protobuf.InvalidProtocolBufferException;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.adfoxmessaging.protos.AdfoxDealStatus;
import ru.yandex.direct.adfoxmessaging.protos.CommonMessage;
import ru.yandex.direct.adfoxmessaging.protos.DirectDealUpdatePayload;
import ru.yandex.direct.core.entity.deal.model.DealBase;
import ru.yandex.direct.core.entity.deal.model.DealDirect;
import ru.yandex.direct.core.entity.deal.model.StatusDirect;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.jobs.adfox.messaging.export.DealToUpdateMessageConverter.DEAL_UPDATE_MESSAGE_TYPE;

@Disabled("Не работающая фича 'частные сделки'")
class DealToUpdateMessageConverterTest {

    private DealToUpdateMessageConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DealToUpdateMessageConverter();
    }

    @ParameterizedTest
    @MethodSource("parametersForConvertStatusTest")
    void convertStatusTest(StatusDirect directStatus, AdfoxDealStatus expected) {
        AdfoxDealStatus actual = converter.convertStatus(directStatus);
        assertThat(actual).isEqualTo(expected);
    }

    static Collection<Object[]> parametersForConvertStatusTest() {
        return asList(new Object[][]{
                {StatusDirect.RECEIVED, AdfoxDealStatus.created},
                {StatusDirect.ACTIVE, AdfoxDealStatus.active},
                {StatusDirect.COMPLETED, AdfoxDealStatus.closed},
                {StatusDirect.ARCHIVED, AdfoxDealStatus.closed},
        });
    }

    @Test
    void dealToUpdateMessage_success() throws InvalidProtocolBufferException {
        // задаём время с миллисекундами, чтобы проверить, что в сообщении время округляется до секунд
        LocalDateTime localDateTime = LocalDateTime.of(2017, Month.DECEMBER, 11, 12, 0, 0, 1_000_000);
        Instant inputTimestamp = Instant.from(ZonedDateTime.of(localDateTime, ZoneOffset.UTC));
        String expectedTimestamp = "2017-12-11T12:00:00Z";

        long dealId = 12345L;

        DealDirect dealToConvert = new DealBase()
                .withId(dealId)
                .withDirectStatus(StatusDirect.ACTIVE);

        CommonMessage message = converter.dealToUpdateMessage(dealToConvert, inputTimestamp);
        // Проверяем успешный сценарий в два этапа
        // 1. строковый тип, время и тип Payload'а соответствуют ожиданиям
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(message.getType())
                    .describedAs("message.type")
                    .isEqualTo(DEAL_UPDATE_MESSAGE_TYPE);
            softly.assertThat(message.getTimestampUtc())
                    .describedAs("message.timestampUtc")
                    .isEqualTo(expectedTimestamp);
            softly.assertThat(message.getPayload().is(DirectDealUpdatePayload.class))
                    .describedAs("message.payload type is %s", DirectDealUpdatePayload.class.getSimpleName())
                    .isTrue();
        });
        // 2. Поля Payload'а соответствуют ожиданиям
        DirectDealUpdatePayload dealUpdatePayload = message.getPayload().unpack(DirectDealUpdatePayload.class);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(dealUpdatePayload.getDealExportId())
                    .describedAs("message.payload.dealExportId")
                    .isEqualTo(dealId);
            softly.assertThat(dealUpdatePayload.getStatus())
                    .describedAs("message.payload.status")
                    .isEqualTo(AdfoxDealStatus.active);
        });
    }
}
