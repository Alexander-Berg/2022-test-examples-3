package ru.yandex.market.logistics.lrm.les.processor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterReturnApi;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryReschedulingReason;
import ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryReschedulingRequest;
import ru.yandex.market.logistics.les.dto.TplRequestIntervalDto;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressChangeReason;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressModificationSource;
import ru.yandex.market.logistics.les.tpl.TplReturnAtClientAddressChangedEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.les.LesEventFactory;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.model.exception.ResourceNotFoundException;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.DetachedTypedEntity;
import ru.yandex.market.logistics.lrm.service.meta.EntityMetaService;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta.Interval;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.lrm.config.LrmTestConfiguration.TEST_REQUEST_ID;

@DisplayName("Обработка события изменения интервала забора возврата курьеркой")
class TplChangeClientReturnProcessorTest extends AbstractIntegrationYdbTest {

    private static final LocalDate FROM_DATE = LocalDate.of(2022, 4, 5);
    private static final LocalTime FROM_TIME = LocalTime.of(10, 11);
    private static final LocalDate TO_DATE = LocalDate.of(2022, 5, 6);
    private static final LocalTime TO_TIME = LocalTime.of(11, 12);

    @Autowired
    private AsyncLesEventProcessor processor;
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
    @DisplayName("Нет нового интервала")
    void noInterval() {
        softly.assertThatThrownBy(() -> execute("1", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("No new interval in event");
    }

    @Test
    @DisplayName("Нет идентификатора возврата")
    void noReturnId() {
        softly.assertThatThrownBy(() -> execute(null, defaultInterval()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("Unknown return for event");
    }

    @Test
    @DisplayName("Возврат не найден")
    void noReturn() {
        softly.assertThatThrownBy(() -> execute("2", defaultInterval()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find RETURN with ids [2]");
    }

    @Test
    @DisplayName("Нет метаданных курьерского возврата")
    @DatabaseSetup("/database/les/tpl-change-client-return/before/minimal.xml")
    void noMeta() {
        softly.assertThatThrownBy(() -> execute("1", defaultInterval()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("Failed to find CourierClientReturnMeta");
    }

    @Test
    @DisplayName("Ошибка от чекаутера")
    @DatabaseSetup("/database/les/tpl-change-client-return/before/minimal.xml")
    void checkouterError() {
        CourierClientReturnMeta initialMeta = CourierClientReturnMeta.builder()
            .interval(
                Interval.builder()
                    .dateFrom(FROM_DATE.minusDays(1))
                    .timeFrom(FROM_TIME.minusHours(1))
                    .dateTo(TO_DATE.minusDays(1))
                    .timeTo(TO_TIME.minusHours(1))
                    .build()
            )
            .build();
        entityMetaService.save(new DetachedTypedEntity(EntityType.RETURN, 1L), initialMeta);

        doThrow(new RuntimeException("Checkouter error"))
            .when(checkouterReturnApi)
            .rescheduleDelivery(any(), any(), any());

        softly.assertThatThrownBy(() -> execute("1", defaultInterval()))
            .hasMessageStartingWith("Checkouter error");

        assertClientReturnMeta(initialMeta);
        verifyCheckouter();
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/database/les/tpl-change-client-return/before/minimal.xml")
    void success() {
        entityMetaService.save(
            new DetachedTypedEntity(EntityType.RETURN, 1L),
            CourierClientReturnMeta.builder().build()
        );

        execute("1", defaultInterval());

        assertClientReturnMeta(
            CourierClientReturnMeta.builder()
                .interval(
                    Interval.builder()
                        .dateFrom(FROM_DATE)
                        .timeFrom(FROM_TIME)
                        .dateTo(TO_DATE)
                        .timeTo(TO_TIME)
                        .build()
                )
                .build()
        );

        verifyCheckouter();
    }

    private void verifyCheckouter() {
        verify(checkouterReturnApi).rescheduleDelivery(
            654321L,
            new ReturnDeliveryReschedulingRequest(
                FROM_DATE,
                TO_DATE,
                FROM_TIME,
                TO_TIME,
                ReturnDeliveryReschedulingReason.WRONG_ADDRESS
            ),
            ClientRole.DELIVERY_SERVICE
        );
    }

    private void assertClientReturnMeta(CourierClientReturnMeta expected) {
        softly.assertThat(
            getEntityMetaRecord(RETURN_1_HASH, "RETURN", 1L, "courier-client-return")
                .map(EntityMetaTableDescription.EntityMetaRecord::value)
                .map(v -> readValue(v, CourierClientReturnMeta.class))
        ).contains(expected);
    }

    private void execute(@Nullable String returnId, TplRequestIntervalDto newInterval) {
        processor.execute(LesEventFactory.getDbQueuePayload(new TplReturnAtClientAddressChangedEvent(
            TEST_REQUEST_ID,
            returnId,
            TplReturnAtClientAddressChangeReason.WRONG_ADDRESS,
            TplReturnAtClientAddressModificationSource.COURIER,
            newInterval
        )));
    }

    @Nonnull
    private TplRequestIntervalDto defaultInterval() {
        return new TplRequestIntervalDto(FROM_DATE, FROM_TIME, TO_DATE, TO_TIME);
    }

}
