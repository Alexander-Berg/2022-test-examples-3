package ru.yandex.market.logistics.lrm.tasks.returns;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterReturnApi;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.returns.ReturnCancelReason;
import ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus;
import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnStatus;
import ru.yandex.market.logistics.lrm.model.exception.ResourceNotFoundException;
import ru.yandex.market.logistics.lrm.queue.payload.ChangeClientReturnStatusPayload;
import ru.yandex.market.logistics.lrm.queue.processor.ChangeClientReturnStatusProcessor;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.DetachedTypedEntity;
import ru.yandex.market.logistics.lrm.service.meta.EntityMetaService;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnCancellationMeta;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnCancellationMeta.Reason;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnCancellationMeta.Source;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ParametersAreNonnullByDefault
@DisplayName("Обновление статуса возврата в чекаутере")
@DatabaseSetup("/database/tasks/returns/change-client-return-status/before/prepare.xml")
class ChangeClientReturnStatusProcessorTest extends AbstractIntegrationYdbTest {

    @Autowired
    private ChangeClientReturnStatusProcessor processor;

    @Autowired
    private CheckouterReturnApi checkouterReturnApi;

    @Autowired
    private EntityMetaTableDescription entityMetaTableDescription;

    @Autowired
    private EntityMetaService entityMetaService;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTableDescription);
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(checkouterReturnApi);
    }

    @Test
    @DisplayName("Возврат с заданным идентификатором не найден")
    void returnNotFound() {
        softly.assertThatCode(() -> processor.execute(payload(100000L, ReturnStatus.CANCELLED)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find RETURN with ids [100000]");
    }

    @Test
    @DisplayName("Статус возврата и статус в пейлоаде не совпадают")
    void returnInDifferentStatus() {
        softly.assertThatCode(() -> processor.execute(payload(1L, ReturnStatus.EXPIRED)))
            .doesNotThrowAnyException();

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            """
                level=WARN\t\
                format=plain\t\
                payload=Trying to update status for return 1 with status RECEIVED to not actual status EXPIRED\
                """
        );
    }

    @Test
    @DisplayName("У возврата нет externalId")
    void returnWithoutExternalId() {
        softly.assertThatThrownBy(() -> processor.execute(payload(2L, ReturnStatus.IN_TRANSIT)))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Статус CREATED не высылается")
    void created() {
        processor.execute(payload(1L, ReturnStatus.CREATED));
    }

    @Test
    @DisplayName("Успешное обновление статуса возврата")
    void success() {
        processor.execute(payload(1L, ReturnStatus.RECEIVED));

        verify(checkouterReturnApi).changeReturnDeliveryStatus(
            111,
            123456789,
            ReturnDeliveryStatus.SENDER_SENT,
            null,
            ClientRole.SYSTEM,
            null
        );
    }

    @Test
    @DisplayName("Успешное обновление статуса возврата для ФФ сегмента")
    void successForFf() {
        processor.execute(payload(3L, ReturnStatus.FULFILMENT_RECEIVED));

        verify(checkouterReturnApi).changeReturnDeliveryStatus(
            333,
            33333,
            ReturnDeliveryStatus.DELIVERED,
            null,
            ClientRole.SYSTEM,
            null
        );
    }

    @Test
    @DisplayName("Успешное обновление статуса на Отменён с причиной")
    void successCancelReason() {
        entityMetaService.save(
            new DetachedTypedEntity(EntityType.RETURN, 4L),
            CourierClientReturnCancellationMeta.builder()
                .source(Source.COURIER)
                .reason(Reason.CLIENT_REFUSED)
                .build()
        );

        processor.execute(payload(4L, ReturnStatus.CANCELLED));

        verify(checkouterReturnApi).changeReturnDeliveryStatus(
            444,
            44,
            ReturnDeliveryStatus.CANCELLED,
            ReturnCancelReason.USER_REQUESTED_CANCELLATION,
            ClientRole.DELIVERY_SERVICE,
            null
        );
    }

    @Nonnull
    private ChangeClientReturnStatusPayload payload(long returnId, ReturnStatus status) {
        return ChangeClientReturnStatusPayload.builder()
            .returnId(returnId)
            .status(status)
            .build();
    }
}
