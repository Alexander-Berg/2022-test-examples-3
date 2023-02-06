package ru.yandex.market.delivery.mdbapp.components.queue.order.lastmile;

import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import steps.UpdateLastMileSteps;
import steps.UpdateRecipientSteps;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.delivery.mdbapp.AbstractMediumContextualTest;
import ru.yandex.market.delivery.mdbapp.components.logging.BackLogOrdersTskvLogger;
import ru.yandex.market.delivery.mdbapp.components.service.LogisticsOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.last_mile.ChangeLastMileService;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.delivery.mdbapp.utils.ResourceUtils.getFileContent;

public abstract class AbstractLastMileRequestQueueConsumerTest<T>
    extends AbstractMediumContextualTest {

    @Autowired
    protected CheckouterOrderService checkouterOrderService;

    @Autowired
    protected CheckouterAPI checkouterAPI;

    @Autowired
    protected LogisticsOrderService logisticsOrderService;

    @Autowired
    protected ChangeLastMileService changeLastMileService;

    @Autowired
    protected LomClient lomClient;

    @Qualifier("commonJsonMapper")
    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected BackLogOrdersTskvLogger backLogOrdersTskvLogger;

    protected void mockCheckouterApi() {
        doReturn(UpdateLastMileSteps.createCheckouterOrder())
            .when(checkouterAPI)
            .getOrder(anyLong(), any(), any(), any());
        doReturn(true)
            .when(checkouterAPI)
            .updateChangeRequestStatus(anyLong(), anyLong(), any(), any(), any());
    }

    protected void verifyGetOrder() {
        verify(lomClient).getOrder(
                eq(UpdateRecipientSteps.ORDER_ID),
                eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)),
                eq(false)
        );
        verify(checkouterAPI).getOrder(
                anyLong(),
                eq(ClientRole.SYSTEM),
                eq(null),
                eq(Set.of(ru.yandex.market.checkout.checkouter.order.OptionalOrderPart.CHANGE_REQUEST))
        );
    }

    protected void verifyGetOrderWithoutCheckouterChangeRequests() {
        verify(lomClient).getOrder(
            eq(UpdateRecipientSteps.ORDER_ID),
            eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)),
            eq(false)
        );
        verify(checkouterAPI).getOrder(
            anyLong(),
            eq(ClientRole.SYSTEM),
            eq(null)
        );
    }

    protected void verifyParcel() {
        verify(checkouterAPI, never()).updateParcel(
                anyLong(),
                anyLong(),
                any(),
                any(),
                anyLong()
        );
    }

    @Nonnull
    protected Task<T> createTask(T taskPayload, String shardId) {
        return new Task<>(new QueueShardId(shardId), taskPayload, 5, ZonedDateTime.now(), null, null);
    }

    @Nonnull
    @SneakyThrows
    protected JsonNode createPayload(String filePath) {
        return objectMapper.readValue(getFileContent(filePath), JsonNode.class);
    }
}
