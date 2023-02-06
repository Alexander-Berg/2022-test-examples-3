package ru.yandex.market.delivery.transport_manager.queue.task.transportation.master;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheme;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.inbound.PutInboundProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement.put.PutMovementProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.outbound.PutOutboundProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.shipment.ShipmentProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DatabaseSetup(
    value = "/repository/health/dbqueue/empty.xml",
    connection = "dbUnitDatabaseConnectionDbQueue"
)
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class TransportationMasterConsumerTest extends AbstractContextualTest {
    @Autowired
    TransportationMapper transportationMapper;
    @Autowired
    TransportationMasterConsumer transportationMasterConsumer;
    @Autowired
    ShipmentProducer shipmentProducer;
    @Autowired
    PutInboundProducer putInboundProducer;
    @Autowired
    PutOutboundProducer putOutboundProducer;
    @Autowired
    PutMovementProducer putMovementProducer;

    @BeforeEach
    void before() {
        clock.setFixed(Instant.parse("2020-07-09T21:00:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DatabaseSetup({
        "/repository/service/transportation_task_creator/transportation_in_shipment.xml",
        "/repository/service/transportation_task_creator/inbound_and_intake_methods.xml",
        "/repository/service/transportation_task_creator/delivery_partner.xml",
        "/repository/register/setup/register_plan_for_unit_2.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register/after/register_do_not_need_to_send.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void startScheduledTransportationsTest_newMethodOnly() {
        var executionResult = transportationMasterConsumer.execute(task(1L));
        verify(putInboundProducer, times(1)).enqueue(anyLong());
        verify(shipmentProducer, times(0)).enqueue(any(Transportation.class));
        verify(putMovementProducer, times(0)).enqueue(anyLong());
        softly.assertThat(executionResult).isEqualTo(TaskExecutionResult.finish());

        Transportation transportation = transportationMapper.getById(1L);
        softly.assertThat(transportation.getScheme()).isEqualTo(TransportationScheme.NEW);
        softly.assertThat(transportation.getStatus()).isEqualTo(TransportationStatus.OUTBOUND_IGNORED);
    }

    @Test
    @DatabaseSetup({
        "/repository/service/transportation_task_creator/transportation_in_shipment.xml",
        "/repository/service/transportation_task_creator/all_new_methods.xml",
        "/repository/register/setup/register_plan_for_unit_2.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register/after/register_do_not_need_to_send.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void startScheduledTransportationsTest_allNewMethodsOnly() {
        var executionResult = transportationMasterConsumer.execute(task(1L));
        verify(putInboundProducer, times(0)).enqueue(anyLong());
        verify(putOutboundProducer, times(0)).enqueue(any(Transportation.class));
        verify(putMovementProducer, times(1)).enqueue(anyLong());
        verify(shipmentProducer, times(0)).enqueue(any(Transportation.class));
        softly.assertThat(executionResult).isEqualTo(TaskExecutionResult.finish());

        var scheme = transportationMapper.getById(1L).getScheme();
        softly.assertThat(scheme).isEqualTo(TransportationScheme.NEW);
    }

    @Test
    @DatabaseSetup({
        "/repository/service/transportation_task_creator/transportation_in_shipment.xml",
        "/repository/service/transportation_task_creator/no_inbound_method.xml",
        "/repository/register/setup/register_plan_for_unit_2.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register/after/register_new.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void startScheduledTransportationsTest_noIntake() {
        var executionResult = transportationMasterConsumer.execute(task(1L));
        verify(putInboundProducer, times(0)).enqueue(anyLong());
        verify(putOutboundProducer, times(1)).enqueue(any(Transportation.class));
        verify(putMovementProducer, times(1)).enqueue(anyLong());
        verify(shipmentProducer, times(1)).enqueue(any(Transportation.class));
        softly.assertThat(executionResult).isEqualTo(TaskExecutionResult.finish());

        var scheme = transportationMapper.getById(1L).getScheme();
        softly.assertThat(scheme).isEqualTo(TransportationScheme.COMBINED);
    }

    @Test
    @DatabaseSetup("/repository/service/transportation_task_creator/transportation_in_shipment.xml")
    @DatabaseSetup("/repository/register/setup/register_plan_for_unit_2.xml")
    @ExpectedDatabase(
        value = "/repository/service/transportation_task_creator/transportation_could_not_be_matched.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/register/after/do_not_need_to_send.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void startScheduledTransportationsTest_noMethods() {
        var executionResult = transportationMasterConsumer.execute(task(1L));
        verify(putInboundProducer, never()).enqueue(anyLong());
        verify(putOutboundProducer, never()).enqueue(any(Transportation.class));
        verify(putMovementProducer, never()).enqueue(anyLong());
        verify(shipmentProducer, never()).enqueue(any(Transportation.class));
        softly.assertThat(executionResult).isEqualTo(TaskExecutionResult.finish());

        var scheme = transportationMapper.getById(1L).getScheme();
        softly.assertThat(scheme).isEqualTo(TransportationScheme.UNKNOWN);
    }

    @Test
    @DatabaseSetup({
        "/repository/service/transportation_task_creator/transportation_in_shipment.xml",
        "/repository/service/transportation_task_creator/no_inbound_method.xml"
    })
    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @ExpectedDatabase(
        value = "/repository/task/no_tasks.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void rollbackWhenErrorInTheMiddle() {
        doThrow(new RuntimeException("Some error in middle"))
            .when(putMovementProducer).enqueue(anyLong());

        assertThatThrownBy(() -> transportationMasterConsumer.execute(task(1L)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Some error in middle");

        verify(putInboundProducer, times(0)).enqueue(anyLong());
        verify(putOutboundProducer, times(1)).enqueue(any(Transportation.class));
        verify(shipmentProducer, times(1)).enqueue(any(Transportation.class));
    }

    @Test
    @DatabaseSetup({
        "/repository/service/transportation_task_creator/transportation_in_shipment.xml",
        "/repository/service/transportation_task_creator/inbound_and_intake_methods.xml",
        "/repository/service/transportation_task_creator/crossdoc_partner.xml",
        "/repository/register/setup/register_plan_for_unit_2.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register/after/do_not_need_to_send.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void startScheduledTransportationsTest_newMethods_outboundCrossdoc_noInteractions() {
        transportationMapper.setScheme(1L, TransportationScheme.UNKNOWN);
        var executionResult = transportationMasterConsumer.execute(task(1L));
        verify(putInboundProducer, never()).enqueue(anyLong());
        verify(putOutboundProducer, never()).enqueue(any(Transportation.class));
        verify(putMovementProducer, never()).enqueue(anyLong());
        verify(shipmentProducer, never()).enqueue(any(Transportation.class));
        softly.assertThat(executionResult).isEqualTo(TaskExecutionResult.finish());

        Transportation transportation = transportationMapper.getById(1L);
        softly.assertThat(transportation.getScheme()).isEqualTo(TransportationScheme.UNKNOWN);
        softly.assertThat(transportation.getStatus()).isEqualTo(TransportationStatus.COULD_NOT_BE_MATCHED);
    }

    @Test
    @DatabaseSetup("/repository/service/transportation_task_creator/transportation_in_shipment.xml")
    @DatabaseSetup("/repository/register/setup/register_plan_for_unit_2.xml")
    @DatabaseSetup(
        value = "/repository/service/transportation_task_creator/update/delete_transportation_1.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/repository/service/transportation_task_creator/after/cancel_transportation_task.xml",
        assertionMode = NON_STRICT_UNORDERED,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    void cancelDeletedTransportationWithoutOrders() {
        var executionResult = transportationMasterConsumer.execute(task(1L));
        softly.assertThat(executionResult).isEqualTo(TaskExecutionResult.finish());

        verifyNoMoreInteractions(putInboundProducer);
        verifyNoMoreInteractions(putOutboundProducer);
        verifyNoMoreInteractions(shipmentProducer);
    }

    @Test
    @DatabaseSetup({
        "/repository/transportation/all_kinds_of_transportation.xml",
        "/repository/register/setup/register_plan_for_unit_2.xml",
        "/repository/order_route/orders.xml",
        "/repository/order_route/after/after_binding.xml",
        "/repository/service/transportation_task_creator/inbound_and_intake_methods_for_5_6.xml"
    })
    @DatabaseSetup(
        value = "/repository/service/transportation_task_creator/update/delete_transportation_1.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/repository/register/after/register_do_not_need_to_send.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void startScheduledDeletedTransportationWithAttachedOrders() {
        var executionResult = transportationMasterConsumer.execute(task(1L));
        verify(shipmentProducer, times(0)).enqueue(any(Transportation.class));
        softly.assertThat(executionResult).isEqualTo(TaskExecutionResult.finish());
    }

    private static Task<TransportationMasterDto> task(Long transportationId) {
        var dto = new TransportationMasterDto();
        dto.setTransportationId(transportationId);

        return Task.<TransportationMasterDto>builder(new QueueShardId("123"))
            .withPayload(dto)
            .build();
    }
}
