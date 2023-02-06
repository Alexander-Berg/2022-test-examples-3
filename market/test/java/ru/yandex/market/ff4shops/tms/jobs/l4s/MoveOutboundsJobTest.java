package ru.yandex.market.ff4shops.tms.jobs.l4s;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.logistics4shops.client.api.OutboundApi;
import ru.yandex.market.logistics4shops.client.model.Outbound;
import ru.yandex.market.logistics4shops.client.model.OutboundsCreateRequest;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Перемещеие отправок в L4S")
public class MoveOutboundsJobTest extends FunctionalTest {

    @Autowired
    private OutboundApi outboundApi;

    @Autowired
    private MoveOutboundsJob moveOutboundsJob;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(outboundApi);
    }

    private static final List<Outbound> outbounds = List.of(
            new Outbound()
                    .id(1L)
                    .yandexId("10")
                    .externalId("externalId-1")
                    .created(Instant.parse("2021-03-15T11:59:35Z"))
                    .confirmed(Instant.parse("2021-03-24T13:59:35Z"))
                    .intervalFrom(Instant.parse("2021-04-19T09:47:35Z"))
                    .intervalTo(Instant.parse("2021-04-20T12:17:43Z"))
                    .orderIds(List.of("1", "2", "3")),
            new Outbound()
                    .id(2L)
                    .yandexId("20")
                    .externalId("externalId-2")
                    .created(Instant.parse("2021-03-16T11:59:35Z"))
                    .confirmed(Instant.parse("2021-03-25T13:59:35Z"))
                    .intervalFrom(Instant.parse("2021-04-20T09:47:35Z"))
                    .intervalTo(Instant.parse("2021-04-21T12:17:43Z"))
                    .orderIds(List.of()),
            new Outbound()
                    .id(3L)
                    .yandexId("30")
                    .externalId("externalId-3")
                    .created(Instant.parse("2021-03-17T11:59:35Z"))
                    .confirmed(Instant.parse("2021-03-26T13:59:35Z"))
                    .intervalFrom(Instant.parse("2021-04-21T09:47:35Z"))
                    .intervalTo(Instant.parse("2021-04-22T12:17:43Z"))
                    .orderIds(List.of("4")),
            new Outbound()
                    .id(4L)
                    .yandexId("40")
                    .externalId("externalId-4")
                    .created(Instant.parse("2021-03-18T11:59:35Z"))
                    .confirmed(Instant.parse("2021-03-27T13:59:35Z"))
                    .intervalFrom(Instant.parse("2021-04-22T09:47:35Z"))
                    .intervalTo(Instant.parse("2021-04-23T12:17:43Z"))
                    .orderIds(List.of("3", "2")),
            new Outbound()
                    .id(5L)
                    .yandexId("50")
                    .externalId("externalId-5")
                    .created(Instant.parse("2021-03-19T11:59:35Z"))
                    .confirmed(Instant.parse("2021-03-28T13:59:35Z"))
                    .intervalFrom(Instant.parse("2021-04-23T09:47:35Z"))
                    .intervalTo(Instant.parse("2021-04-24T12:17:43Z"))
                    .orderIds(List.of("5"))
    );

    @Test
    @DisplayName("Успешное перемещение в L4S")
    @DbUnitDataSet(before = "MoveOutboundsJobTest.before.csv", after = "MoveOutboundsJobTest.after.csv")
    void success() {
        ArgumentCaptor<OutboundsCreateRequest> captor = ArgumentCaptor.forClass(OutboundsCreateRequest.class);
        moveOutboundsJob.doJob(null);

        List<OutboundsCreateRequest> outboundRequests = List.of(
                new OutboundsCreateRequest().outbounds(
                        List.of(
                                outbounds.get(1),
                                outbounds.get(2)
                        )
                ),
                new OutboundsCreateRequest().outbounds(
                        List.of(
                                outbounds.get(3),
                                outbounds.get(4)
                        )
                )
        );
        verify(outboundApi, times(2)).createOutbounds(captor.capture());
        Assertions.assertIterableEquals(captor.getAllValues(), outboundRequests);
    }

    @Test
    @DisplayName("Все отправки уже перемещены")
    @DbUnitDataSet(
            before = "MoveOutboundsJobTest.all.moved.before.csv",
            after = "MoveOutboundsJobTest.all.moved.before.csv"
    )
    void allMoved() {
        moveOutboundsJob.doJob(null);
    }

    @Test
    @DisplayName("Нет идентификатора последней перемещенной отправки")
    @DbUnitDataSet(
            before = "MoveOutboundsJobTest.no.moved.id.before.csv",
            after = "MoveOutboundsJobTest.no.moved.id.after.csv"
    )
    void noMoveOutboundId() {
        ArgumentCaptor<OutboundsCreateRequest> captor = ArgumentCaptor.forClass(OutboundsCreateRequest.class);
        List<OutboundsCreateRequest> outboundRequests = List.of(
                new OutboundsCreateRequest().outbounds(
                        List.of(
                                outbounds.get(0),
                                outbounds.get(1)
                        )
                ),
                new OutboundsCreateRequest().outbounds(
                        List.of(
                                outbounds.get(2),
                                outbounds.get(3)
                        )
                )
        );
        moveOutboundsJob.doJob(null);
        verify(outboundApi, times(2)).createOutbounds(captor.capture());
        Assertions.assertIterableEquals(captor.getAllValues(), outboundRequests);
    }

    @Test
    @DisplayName("Успешное перемещение в L4S - нет идентификаторов заказов")
    @DbUnitDataSet(
            before = "MoveOutboundsJobTest.no.order.id.before.csv",
            after = "MoveOutboundsJobTest.no.order.id.after.csv"
    )
    void successNoOrderIds() {
        moveOutboundsJob.doJob(null);

        OutboundsCreateRequest outboundRequest = new OutboundsCreateRequest().outbounds(
                List.of(
                        outbounds.get(1),
                        outbounds.get(2)
                )
        );
        verify(outboundApi).createOutbounds(outboundRequest);
    }
}
