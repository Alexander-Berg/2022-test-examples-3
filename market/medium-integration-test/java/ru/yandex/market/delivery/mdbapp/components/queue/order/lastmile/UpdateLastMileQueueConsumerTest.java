package ru.yandex.market.delivery.mdbapp.components.queue.order.lastmile;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import steps.UpdateLastMileSteps;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.UpdateLastMileDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.UpdateLastMileQueueConsumer;
import ru.yandex.market.delivery.mdbapp.integration.converter.UpdateLastMilePayloadConverter;
import ru.yandex.market.logistics.lom.model.dto.UpdateLastMileRequestDto;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.utils.ResourceUtils.getFileContent;

public class UpdateLastMileQueueConsumerTest extends AbstractLastMileRequestQueueConsumerTest<UpdateLastMileDto> {

    private static final String SHARD_ID = "order.update.lastmile";

    @Autowired
    private UpdateLastMilePayloadConverter payloadConverter;

    private UpdateLastMileQueueConsumer consumer;

    @Captor
    private ArgumentCaptor<UpdateLastMileRequestDto> captor;

    @BeforeEach
    public void setUp() {
        consumer = new UpdateLastMileQueueConsumer(checkouterOrderService, logisticsOrderService, payloadConverter);
    }

    @Test
    @DisplayName("Успешное создание чендж реквеста при постановки задачи в очередь")
    public void successfulCreation() {
        UpdateLastMileDto updateLastMileDto = UpdateLastMileSteps.createUpdateLastMileDto();
        TaskExecutionResult result = consumer.execute(createTask(updateLastMileDto, SHARD_ID));
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verifySuccessfulCheckouterCall();
        verify(lomClient).updateLastMile(captor.capture());

        assertFields(updateLastMileDto, captor.getValue());
    }

    @Test
    @DisplayName("Успешное создание чендж реквеста используя пример реальной таски")
    public void successfulCreationUsingTaskPayload() throws IOException {
        UpdateLastMileDto updateLastMileDto = getTaskPayload();
        TaskExecutionResult result = consumer.execute(createTask(updateLastMileDto, SHARD_ID));
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verify(checkouterAPI).updateChangeRequestStatus(
            eq(33077113L),
            eq(1423902L),
            eq(ClientRole.SYSTEM),
            eq(null),
            any());
        verify(lomClient).updateLastMile(captor.capture());

        assertFields(updateLastMileDto, captor.getValue());
    }

    @Test
    @DisplayName("Ошибка при обновлении статуса в чекаутере")
    public void checkouterFail() {
        when(checkouterAPI.updateChangeRequestStatus(
            eq(7000L),
            eq(77586L),
            eq(ClientRole.SYSTEM),
            eq(null),
            any()
        )).thenThrow(new RuntimeException("Something went wrong"));

        UpdateLastMileDto updateLastMileDto = UpdateLastMileSteps.createUpdateLastMileDto();
        TaskExecutionResult result = consumer.execute(createTask(updateLastMileDto, SHARD_ID));
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());
    }

    @Test
    @DisplayName("Ошибка при создании change request в LOM")
    public void lomFail() {
        when(lomClient.updateLastMile(any()))
            .thenThrow(new RuntimeException("Something went wrong"));
        UpdateLastMileDto updateLastMileDto = UpdateLastMileSteps.createUpdateLastMileDto();
        TaskExecutionResult result = consumer.execute(createTask(updateLastMileDto, SHARD_ID));
        softly.assertThat(result).isEqualTo(TaskExecutionResult.fail());

        verifySuccessfulCheckouterCall();
    }

    private void verifySuccessfulCheckouterCall() {
        verify(checkouterAPI).updateChangeRequestStatus(
            eq(7000L),
            eq(88586L),
            eq(ClientRole.SYSTEM),
            eq(null),
            any());
    }

    @Nonnull
    private UpdateLastMileDto getTaskPayload() throws IOException {
        return objectMapper.readValue(
            getFileContent("/components/queue/order/delivery/lastmile/changerequest/task_payload.json"),
            UpdateLastMileDto.class
        );
    }

    private void assertFields(UpdateLastMileDto initial, UpdateLastMileRequestDto value) {
        UpdateLastMileRequestDto expected = payloadConverter.toLomDto(initial);
        softly.assertThat(value.getBarcode()).isEqualTo(expected.getBarcode());
        softly.assertThat(value.getRoute()).isEqualTo(expected.getRoute());
        softly.assertThat(value.getDeliveryType()).isEqualTo(expected.getDeliveryType());
        softly.assertThat(value.getPayload()).isEqualTo(expected.getPayload());
    }
}
