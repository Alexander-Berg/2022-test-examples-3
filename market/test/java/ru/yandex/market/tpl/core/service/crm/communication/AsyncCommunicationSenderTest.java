package ru.yandex.market.tpl.core.service.crm.communication;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.format.annotation.DateTimeFormat;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.market.tpl.common.communication.crm.model.CommunicationEventType;
import ru.yandex.market.tpl.common.communication.crm.service.CommunicationSender;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.service.crm.communication.model.CourierPlatformCommunicationDto;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.util.ObjectMappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RequiredArgsConstructor
public class AsyncCommunicationSenderTest extends TplAbstractTest {
    private final CommunicationSender<CourierPlatformCommunicationDto> asyncCommunicationSender;
    private final ObjectMapper objectMapper = ObjectMappers.baseObjectMapper();

    private final DbQueueTestUtil dbQueueTestUtil;
    private final AsyncProducer mockedAsyncProducer;

    @AfterEach
    void after() {
        Mockito.clearInvocations(mockedAsyncProducer);
    }

    @SneakyThrows
    @Test
    void happyTest_sentCommunicationToDbQueue() {
        asyncCommunicationSender.send(
                TestCommunicationDto.ForUnitTest.builder()
                        .recipientEmail("email")
                        .someDate(LocalDate.of(2021, 7, 3))
                        .someTime(LocalTime.of(15, 40))
                        .someDuration(Duration.ofMinutes(97))
                        .recipientYandexUid(8972402013L)
                        .build()
        );

        //есть в очереди на отправку в логброкер
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.LOGBROKER_WRITER);

        //вытаскиваем байты, которые отправляем в логброкер
        verify(mockedAsyncProducer, times(1)).write(any());
        var byteCaptor = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(mockedAsyncProducer).write(byteCaptor.capture());
        byte[] bytesReadySentToLogbroker = byteCaptor.getValue();

        //парсим байты в json и проверяем нужные поля
        JsonNode jsonNode = objectMapper.readValue(bytesReadySentToLogbroker, JsonNode.class);
        assertThat(jsonNode.get("recipientEmail").asText()).isEqualTo("email");
        assertThat(jsonNode.get("someDate").asText()).isEqualTo("2021-07-03");
        assertThat(jsonNode.get("someTime").asText()).isEqualTo("15:40:00");
        assertThat(jsonNode.get("someDuration").asText()).isEqualTo("PT1H37M");
        assertThat(jsonNode.get("timestamp").isNull()).isFalse();
    }

    @SuperBuilder
    @Getter
    private static abstract class TestCommunicationDto extends CourierPlatformCommunicationDto {

        @SuperBuilder
        @Getter
        private static class ForUnitTest extends TestCommunicationDto {
            final CommunicationEventType eventType = CommunicationEventType.FOR_UNIT_TEST;

            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate someDate;

            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime someTime;

            Duration someDuration;

        }
    }

}
