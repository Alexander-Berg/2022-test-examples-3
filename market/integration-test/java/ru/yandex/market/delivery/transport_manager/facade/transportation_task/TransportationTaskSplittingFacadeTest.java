package ru.yandex.market.delivery.transport_manager.facade.transportation_task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.service.AxaptaStatusEventService;
import ru.yandex.market.delivery.transport_manager.service.ticket.service.StInterwarehouseTicketService;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGatesScheduleResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDateTimeResponse;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@DatabaseSetup("/repository/facade/transportation_task/transportation_task.xml")
class TransportationTaskSplittingFacadeTest extends AbstractContextualTest {
    private static final long FROM_PARTNER_ID = 1L;
    private static final long FROM_LOGISTICS_POINT = 10L;
    private static final long TO_PARTNER_ID = 2L;
    private static final long TO_LOGISTICS_POINT = 20L;

    @Autowired
    private TransportationTaskSplittingFacade transportationTaskSplittingFacade;

    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private AxaptaStatusEventService axaptaStatusEventService;
    @Autowired
    private StInterwarehouseTicketService stInterwarehouseTicketService;

    @BeforeEach
    void setUp() {
        clock.setFixed(
            LocalDateTime.of(2020, 10, 10, 15, 0, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        Mockito.when(lmsClient.getLogisticsPoint(eq(FROM_LOGISTICS_POINT))).thenReturn(Optional.of(
            LogisticsPointResponse.newBuilder().id(FROM_LOGISTICS_POINT).partnerId(FROM_PARTNER_ID).build()
        ));
        Mockito.when(lmsClient.getLogisticsPoint(eq(TO_LOGISTICS_POINT))).thenReturn(Optional.of(
            LogisticsPointResponse.newBuilder().id(TO_LOGISTICS_POINT).partnerId(TO_PARTNER_ID).build()
        ));

    }

    @ExpectedDatabase(
        value = "/repository/facade/transportation_task/after/transportation_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testCreateTransportations() {
        Mockito
            .when(lmsClient.getWarehousesGatesScheduleByPartnerId(
                eq(FROM_PARTNER_ID),
                eq(LocalDate.of(2020, 10, 10)),
                eq(LocalDate.of(2020, 10, 20))
            ))
            .thenReturn(List.of(
                LogisticsPointGatesScheduleResponse.newBuilder()
                    .logisticsPointId(FROM_LOGISTICS_POINT)
                    .schedule(List.of(
                        scheduleDate(LocalDate.of(2020, 10, 13)),
                        scheduleDate(LocalDate.of(2020, 10, 14)),
                        scheduleDate(LocalDate.of(2020, 10, 15)),
                        scheduleDate(LocalDate.of(2020, 10, 16)),
                        scheduleDate(LocalDate.of(2020, 10, 19)),
                        scheduleDate(LocalDate.of(2020, 10, 20))
                    ))
                    .build()
            ));

        List<Long> transporationsAndRegisterIds =
            transportationTaskSplittingFacade.createTransportations(List.of(1L));

        softly.assertThat(transporationsAndRegisterIds).isEqualTo(List.of(1L));

        Mockito.verify(lmsClient).getLogisticsPoint(eq(FROM_LOGISTICS_POINT));
        Mockito.verify(lmsClient).getLogisticsPoint(eq(TO_LOGISTICS_POINT));
        Mockito.verify(lmsClient).getWarehousesGatesScheduleByPartnerId(
            eq(FROM_PARTNER_ID),
            eq(LocalDate.of(2020, 10, 10)),
            eq(LocalDate.of(2020, 10, 20))
        );
        Mockito.verifyZeroInteractions(lmsClient);
        verify(axaptaStatusEventService).createNewTransportationEvent(Mockito.any());
        verify(stInterwarehouseTicketService).createTicketForNewInterwarehouse(Mockito.any());

    }

    @Test
    void testMissingWarehouseFrom() {
        Mockito.when(lmsClient.getLogisticsPoint(eq(FROM_LOGISTICS_POINT))).thenReturn(Optional.empty());
        Mockito
            .when(lmsClient.getWarehousesGatesScheduleByPartnerId(
                eq(FROM_PARTNER_ID),
                eq(LocalDate.of(2020, 10, 10)),
                eq(LocalDate.of(2020, 10, 20))
            ))
            .thenReturn(List.of(
                LogisticsPointGatesScheduleResponse.newBuilder()
                    .logisticsPointId(FROM_LOGISTICS_POINT)
                    .schedule(List.of(
                        scheduleDate(LocalDate.of(2020, 10, 13)),
                        scheduleDate(LocalDate.of(2020, 10, 14)),
                        scheduleDate(LocalDate.of(2020, 10, 15)),
                        scheduleDate(LocalDate.of(2020, 10, 16)),
                        scheduleDate(LocalDate.of(2020, 10, 19)),
                        scheduleDate(LocalDate.of(2020, 10, 20))
                    ))
                    .build()
            ));

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> transportationTaskSplittingFacade.createTransportations(List.of(1L))
        );

        Mockito.verify(lmsClient).getLogisticsPoint(eq(FROM_LOGISTICS_POINT));
        Mockito.verifyZeroInteractions(lmsClient);
    }

    @Test
    void testMissingWarehouseTo() {
        Mockito.when(lmsClient.getLogisticsPoint(eq(TO_LOGISTICS_POINT))).thenReturn(Optional.empty());
        Mockito
            .when(lmsClient.getWarehousesGatesScheduleByPartnerId(
                eq(FROM_PARTNER_ID),
                eq(LocalDate.of(2020, 10, 10)),
                eq(LocalDate.of(2020, 10, 20))
            ))
            .thenReturn(List.of(
                LogisticsPointGatesScheduleResponse.newBuilder()
                    .logisticsPointId(FROM_LOGISTICS_POINT)
                    .schedule(List.of(
                        scheduleDate(LocalDate.of(2020, 10, 13)),
                        scheduleDate(LocalDate.of(2020, 10, 14)),
                        scheduleDate(LocalDate.of(2020, 10, 15)),
                        scheduleDate(LocalDate.of(2020, 10, 16)),
                        scheduleDate(LocalDate.of(2020, 10, 19)),
                        scheduleDate(LocalDate.of(2020, 10, 20))
                    ))
                    .build()
            ));

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> transportationTaskSplittingFacade.createTransportations(List.of(1L))
        );

        Mockito.verify(lmsClient).getLogisticsPoint(eq(FROM_LOGISTICS_POINT));
        Mockito.verify(lmsClient).getLogisticsPoint(eq(TO_LOGISTICS_POINT));
        Mockito.verifyZeroInteractions(lmsClient);
    }

    @Test
    void testMissingScheduleFrom() {
        Mockito
            .when(lmsClient.getWarehousesGatesScheduleByPartnerId(
                eq(FROM_PARTNER_ID),
                eq(LocalDate.of(2020, 10, 10)),
                eq(LocalDate.of(2020, 10, 20))
            ))
            .thenReturn(null);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> transportationTaskSplittingFacade.createTransportations(List.of(1L))
        );

        Mockito.verify(lmsClient).getLogisticsPoint(eq(FROM_LOGISTICS_POINT));
        Mockito.verify(lmsClient).getLogisticsPoint(eq(TO_LOGISTICS_POINT));
        Mockito.verify(lmsClient).getWarehousesGatesScheduleByPartnerId(
            eq(FROM_PARTNER_ID),
            eq(LocalDate.of(2020, 10, 10)),
            eq(LocalDate.of(2020, 10, 20))
        );
        Mockito.verifyZeroInteractions(lmsClient);
    }

    @NotNull
    private ScheduleDateTimeResponse scheduleDate(LocalDate date) {
        return ScheduleDateTimeResponse.newBuilder().date(date).build();
    }
}
