package ru.yandex.market.logistics.logistics4shops.repository;

import java.time.Instant;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.protobuf.Timestamp;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.event.model.ExpressOrderCreatedPayload;
import ru.yandex.market.logistics.logistics4shops.model.entity.LogisticEvent;
import ru.yandex.market.logistics.logistics4shops.model.entity.enums.LogisticEventType;

@DisplayName("Репозиторий логистических событий")
class LogisticEventRepositoryTest extends AbstractIntegrationTest {
    @Autowired
    private LogisticEventRepository logisticEventRepository;

    @Test
    @DisplayName("Получение сущности")
    @DatabaseSetup("/repository/logisticevent/before/prepare.xml")
    void getById() {
        LogisticEvent logisticEvent = logisticEventRepository.findById(1L).orElseThrow();
        var eventToSerialize = getProtobufEvent();
        logisticEvent.setEventBytes(eventToSerialize.toByteArray());
        logisticEventRepository.save(logisticEvent);
        logisticEvent = logisticEventRepository.findById(1L).orElseThrow();
        softly.assertThat(logisticEvent)
            .usingRecursiveComparison()
            .ignoringFields("eventBytes")
            .isEqualTo(new LogisticEvent()
                .setId(1L)
                .setEventType(LogisticEventType.EXPRESS_ORDER_CREATED)
                .setRequestId("1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd")
                .setLogbrokerId(777L)
                .setCreated(Instant.ofEpochSecond(1643799600))
            );
        softly.assertThat(deserializeEvent(logisticEvent.getEventBytes())).isEqualTo(eventToSerialize);
    }

    @Nonnull
    private ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent getProtobufEvent() {
        ExpressOrderCreatedPayload expressOrderCreatedPayload = ExpressOrderCreatedPayload.newBuilder()
            .setOrderId(123L)
            .setShopId(456L)
            .setPackagingDeadline(Timestamp.newBuilder().setSeconds(1643799600).build())
            .build();
        return ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent.newBuilder()
            .setId(1L)
            .setCreated(Timestamp.newBuilder().setSeconds(1643799600).build())
            .setRequestId("1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd")
            .setExpressOrderCreatedPayload(expressOrderCreatedPayload)
            .build();
    }

    @Nonnull
    @SneakyThrows
    private ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent deserializeEvent(byte[] bytes) {
        return ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent.parseFrom(bytes);
    }
}
