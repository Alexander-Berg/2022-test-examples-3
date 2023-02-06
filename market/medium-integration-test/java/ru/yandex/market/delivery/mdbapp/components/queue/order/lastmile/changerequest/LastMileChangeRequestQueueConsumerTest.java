package ru.yandex.market.delivery.mdbapp.components.queue.order.lastmile.changerequest;

import java.util.EnumSet;
import java.util.Optional;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import steps.UpdateLastMileSteps;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.LastMileChangeRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.LastMileChangeRequestQueueConsumer;
import ru.yandex.market.delivery.mdbapp.components.queue.order.lastmile.AbstractLastMileRequestQueueConsumerTest;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.delivery.mdbapp.utils.ResourceUtils.getFileContent;

public class LastMileChangeRequestQueueConsumerTest
    extends AbstractLastMileRequestQueueConsumerTest<LastMileChangeRequestDto> {

    private static final String SHARD_ID = "order.lastmile.changed.changerequest";

    private static final String PAYLOAD_PATH = "/components/queue/order/delivery/lastmile/changerequest/payload.json";

    @Captor
    private ArgumentCaptor<ChangeRequestPatchRequest> captor;

    private LastMileChangeRequestQueueConsumer consumer;

    @BeforeEach
    void beforeEach() {
        consumer = new LastMileChangeRequestQueueConsumer(logisticsOrderService, checkouterOrderService, objectMapper);
        mockCheckouterApi();
    }

    @AfterEach
    void tearDown() {
        verifyGetOrder();
        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @DisplayName("Статус change request'a в Checkouter успешно обновляется до APPLIED")
    void changeRequestSuccessfullyApplied() {
        doReturn(Optional.of(UpdateLastMileSteps.createLomOrderDto(
            ChangeOrderRequestStatus.SUCCESS,
            null,
            createPayload(PAYLOAD_PATH),
            ChangeOrderRequestType.LAST_MILE
        )))
            .when(lomClient)
            .getOrder(anyLong(), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)), eq(false));

        LastMileChangeRequestDto lastMileChangeRequestDto = UpdateLastMileSteps.createLastMileChangeRequestDto();
        TaskExecutionResult result = consumer.execute(createTask(lastMileChangeRequestDto, SHARD_ID));
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verify(checkouterAPI).updateChangeRequestStatus(
            eq(234L),
            eq(88586L),
            eq(ClientRole.SYSTEM),
            eq(null),
            captor.capture()
        );

        softly.assertThat(captor.getValue().getStatus()).isEqualTo(ChangeRequestStatus.APPLIED);
    }

    @Test
    @DisplayName("Статус change request'a в Checkouter успешно обновился до APPLIED (десериализуем payload таски")
    void changeRequestSuccessfullyAppliedDeserializingPayload() {
        doReturn(Optional.of(UpdateLastMileSteps.createLomOrderDto(
            ChangeOrderRequestStatus.SUCCESS,
            null,
            createPayload(PAYLOAD_PATH),
            ChangeOrderRequestType.LAST_MILE
        )))
            .when(lomClient)
            .getOrder(anyLong(), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)), eq(false));

        LastMileChangeRequestDto taskPayload = getTaskPayload();
        TaskExecutionResult result = consumer.execute(createTask(taskPayload, SHARD_ID));
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verify(checkouterAPI).updateChangeRequestStatus(
            eq(234L),
            eq(88586L),
            eq(ClientRole.SYSTEM),
            eq(null),
            captor.capture()
        );

        softly.assertThat(captor.getValue().getStatus()).isEqualTo(ChangeRequestStatus.APPLIED);
    }

    @Test
    @DisplayName("Статус change request'a в Checkouter успешно обновляется до REJECTED")
    void changeRequestSuccessfullyRejected() {
        doReturn(Optional.of(UpdateLastMileSteps.createLomOrderDto(
            ChangeOrderRequestStatus.FAIL,
            null,
            createPayload(PAYLOAD_PATH),
            ChangeOrderRequestType.LAST_MILE)
        ))
            .when(lomClient)
            .getOrder(anyLong(), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)), eq(false));

        LastMileChangeRequestDto lastMileChangeRequestDto = UpdateLastMileSteps.createLastMileChangeRequestDto();
        TaskExecutionResult result = consumer.execute(createTask(lastMileChangeRequestDto, SHARD_ID));
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verify(checkouterAPI).updateChangeRequestStatus(
            eq(234L),
            eq(88586L),
            eq(ClientRole.SYSTEM),
            eq(null),
            captor.capture()
        );

        softly.assertThat(captor.getValue().getStatus()).isEqualTo(ChangeRequestStatus.REJECTED);
    }

    @Nonnull
    @SneakyThrows
    private LastMileChangeRequestDto getTaskPayload() {
        return objectMapper.readValue(
            getFileContent("/components/queue/order/delivery/lastmile/changerequest/change_request_task_payload.json"),
            LastMileChangeRequestDto.class
        );
    }
}
