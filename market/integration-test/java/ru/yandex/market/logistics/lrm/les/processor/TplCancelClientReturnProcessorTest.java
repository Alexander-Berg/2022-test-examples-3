package ru.yandex.market.logistics.lrm.les.processor;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressCancelReason;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressModificationSource;
import ru.yandex.market.logistics.les.tpl.TplReturnAtClientAddressCancelledEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.les.LesEventFactory;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.queue.processor.AsyncLesEventProcessor;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription.EntityMetaRecord;
import ru.yandex.market.logistics.lrm.service.meta.DetachedTypedEntity;
import ru.yandex.market.logistics.lrm.service.meta.EntityMetaService;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnCancellationMeta;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnCancellationMeta.Reason;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnCancellationMeta.Source;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static ru.yandex.market.logistics.lrm.config.LrmTestConfiguration.TEST_REQUEST_ID;

@DisplayName("Обработка отмены клиентского возврата курьеркой")
@DatabaseSetup("/database/les/tpl-cancel-client-return/before/minimal.xml")
class TplCancelClientReturnProcessorTest extends AbstractIntegrationYdbTest {

    private static final Instant NOW = Instant.parse("2022-03-04T11:12:13.00Z");

    @Autowired
    private AsyncLesEventProcessor processor;

    @Autowired
    private EntityMetaTableDescription entityMetaTableDescription;

    @Autowired
    private EntityMetaService entityMetaService;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTableDescription);
    }

    @BeforeEach
    void setup() {
        clock.setFixed(NOW, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Успех")
    @ExpectedDatabase(
        value = "/database/les/tpl-cancel-client-return/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        execute("1");

        softly.assertThat(
            getEntityMetaRecord(RETURN_1_HASH, "RETURN", 1L, "courier-client-return-cancellation")
                .map(EntityMetaRecord::value)
                .map(v -> readValue(v, CourierClientReturnCancellationMeta.class))
        ).contains(
            CourierClientReturnCancellationMeta.builder()
                .reason(Reason.CLIENT_REFUSED)
                .source(Source.COURIER)
                .build()
        );
    }

    @Test
    @DisplayName("Возврат не найден")
    @ExpectedDatabase(
        value = "/database/les/tpl-cancel-client-return/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void notFound() {
        softly.assertThatThrownBy(() -> execute(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("Unknown return for event");

        assertEmptyYdb();
    }

    @Test
    @DisplayName("Возврат с коробками")
    @DatabaseSetup("/database/les/tpl-cancel-client-return/before/boxes.xml")
    @ExpectedDatabase(
        value = "/database/les/tpl-cancel-client-return/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void hasBoxes() {
        softly.assertThatThrownBy(() -> execute("1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot cancel return 1 with boxes");

        assertEmptyYdb();
    }

    @Test
    @DisplayName("Возврат уже отменён")
    @DatabaseSetup("/database/les/tpl-cancel-client-return/before/cancelled.xml")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void alreadyCancelled() {
        CourierClientReturnCancellationMeta existingMeta = CourierClientReturnCancellationMeta.builder()
            .reason(Reason.TOO_MANY_RESCHEDULES)
            .source(Source.SYSTEM)
            .build();
        entityMetaService.save(new DetachedTypedEntity(EntityType.RETURN, 1L), existingMeta);

        execute("1");

        softly.assertThat(
            getEntityMetaRecord(RETURN_1_HASH, "RETURN", 1L, "courier-client-return-cancellation")
                .map(EntityMetaRecord::value)
                .map(v -> readValue(v, CourierClientReturnCancellationMeta.class))
        ).contains(existingMeta);
    }

    private void assertEmptyYdb() {
        softly.assertThat(getEntityMetaRecord(RETURN_1_HASH, "RETURN", 1L, "courier-client-return-cancellation"))
            .isEmpty();
    }

    private void execute(String returnId) {
        processor.execute(LesEventFactory.getDbQueuePayload(new TplReturnAtClientAddressCancelledEvent(
            TEST_REQUEST_ID,
            returnId,
            TplReturnAtClientAddressCancelReason.CLIENT_REFUSED,
            TplReturnAtClientAddressModificationSource.COURIER
        )));
    }

}
