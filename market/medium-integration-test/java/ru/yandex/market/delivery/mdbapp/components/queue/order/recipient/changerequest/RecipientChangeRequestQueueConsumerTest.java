package ru.yandex.market.delivery.mdbapp.components.queue.order.recipient.changerequest;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.RecipientChangeRequestPayload;
import ru.yandex.market.delivery.mdbapp.AbstractMediumContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.recipient.changedbychangerequset.RecipientChangeRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.recipient.changedbychangerequset.RecipientChangeRequestQueueConsumer;
import ru.yandex.market.delivery.mdbapp.components.service.LogisticsOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class RecipientChangeRequestQueueConsumerTest extends AbstractMediumContextualTest {

    private static final long LOCAL_ORDER_ID = 123L;
    private static final long EXTERNAL_ORDER_ID = 234L;

    @Autowired
    private CheckouterOrderService checkouterOrderService;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private LogisticsOrderService logisticsOrderService;

    @Qualifier("commonJsonMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LomClient lomClient;

    @Captor
    private ArgumentCaptor<ChangeRequestPatchRequest> captor;

    private RecipientChangeRequestQueueConsumer consumer;

    @BeforeEach
    void beforeEach() {
        consumer = new RecipientChangeRequestQueueConsumer(
            logisticsOrderService,
            checkouterOrderService,
            objectMapper
        );
        doReturn(getOrder(getPerson())).when(checkouterAPI).getOrder(eq(EXTERNAL_ORDER_ID), any(), any(), any());
        doReturn(true).when(checkouterAPI).updateChangeRequestStatus(anyLong(), anyLong(), any(), any(), any());
    }

    @AfterEach
    void tearDown() {
        verify(lomClient).getOrder(
            eq(LOCAL_ORDER_ID),
            eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)),
            eq(false)
        );
        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @DisplayName("Успешное подтверждение существующей в чекауте заявки")
    void testConfirmSuccessExisting() {
        doReturn(Optional.of(getLomOrder(ChangeOrderRequestStatus.SUCCESS, 1L)))
            .when(lomClient)
            .getOrder(eq(LOCAL_ORDER_ID), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)), eq(false));

        TaskExecutionResult result = consumer.execute(createTask());
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verify(checkouterAPI).updateChangeRequestStatus(
            eq(EXTERNAL_ORDER_ID),
            eq(1L),
            any(),
            any(),
            captor.capture()
        );
        softly.assertThat(captor.getValue().getStatus()).isEqualTo(ChangeRequestStatus.APPLIED);
    }

    @Test
    @DisplayName("Успешное отклонение существующей в чекауте заявки")
    void testConfirmFail() {
        doReturn(Optional.of(getLomOrder(ChangeOrderRequestStatus.FAIL, 1L)))
            .when(lomClient)
            .getOrder(eq(LOCAL_ORDER_ID), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)), eq(false));

        TaskExecutionResult result = consumer.execute(createTask());
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verify(checkouterAPI).updateChangeRequestStatus(
            eq(EXTERNAL_ORDER_ID),
            eq(1L),
            any(),
            any(),
            captor.capture()
        );
        softly.assertThat(captor.getValue().getStatus()).isEqualTo(ChangeRequestStatus.REJECTED);
    }

    @Test
    @DisplayName("Проверка запроса с другим checkouterRequestId")
    void testDifferentId() {
        doReturn(Optional.of(getLomOrder(ChangeOrderRequestStatus.SUCCESS, 2L)))
            .when(lomClient)
            .getOrder(eq(LOCAL_ORDER_ID), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)), eq(false));

        TaskExecutionResult result = consumer.execute(createTask());
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verify(checkouterAPI, never()).updateChangeRequestStatus(anyLong(), anyLong(), any(), any(), any());
    }

    @Nonnull
    private Task<RecipientChangeRequestDto> createTask() {
        return new Task<>(
            new QueueShardId("order.recipient.changed.changerequest"),
            getDto(),
            5,
            ZonedDateTime.now(),
            null,
            null
        );
    }

    @Nonnull
    private RecipientPerson getPerson() {
        return new RecipientPerson("firstName", "middleName", "lastName");
    }

    @Nonnull
    private static ChangeRequest createChangeRequest(RecipientPerson recipientPerson) {
        RecipientChangeRequestPayload payload = new RecipientChangeRequestPayload(
            recipientPerson,
            "73334448888",
            "mail@ya.ru"
        );
        return new ChangeRequest(
            1L,
            EXTERNAL_ORDER_ID,
            payload,
            ChangeRequestStatus.PROCESSING,
            Instant.now(),
            "Any text",
            ClientRole.SYSTEM
        );
    }

    @Nonnull
    private static RecipientChangeRequestDto getDto() {
        return RecipientChangeRequestDto.builder()
            .changeRequestId(1L)
            .lomOrderId(LOCAL_ORDER_ID)
            .build();
    }

    @Nonnull
    @SneakyThrows
    private OrderDto getLomOrder(ChangeOrderRequestStatus changeOrderRequestStatus, Long checkouterRequestId) {
        return new OrderDto()
            .setId(LOCAL_ORDER_ID)
            .setBarcode("234")
            .setExternalId("234")
            .setChangeOrderRequests(
                List.of(
                    ChangeOrderRequestDto.builder()
                        .id(1L)
                        .requestType(ChangeOrderRequestType.RECIPIENT)
                        .status(changeOrderRequestStatus)
                        .payloads(Set.of(
                            ChangeOrderRequestPayloadDto.builder()
                                .status(ChangeOrderRequestStatus.INFO_RECEIVED)
                                .payload(objectMapper.readValue(
                                    "{" +
                                        "   \"barcode\": 234," +
                                        "   \"checkouterRequestId\": " + checkouterRequestId + "," +
                                        "   \"contact\": {" +
                                        "\"firstName\": \"firstName\"," +
                                        "\"middleName\": \"middleName\"," +
                                        "\"lastName\": \"lastName\"," +
                                        "\"phone\": \"73334448888\"" +
                                        "   }   " +
                                        "}",
                                    JsonNode.class
                                ))
                                .build()
                        ))
                        .build()
                )
            );
    }

    @Nonnull
    private Order getOrder(RecipientPerson recipient) {
        Order order = new Order();
        order.setId(EXTERNAL_ORDER_ID);
        order.setChangeRequests(List.of(
            createChangeRequest(recipient)
        ));
        return order;
    }
}
