package ru.yandex.market.logistics.lrm.api.returns;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressCancelReason;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressModificationSource;
import ru.yandex.market.logistics.les.lrm.LrmReturnAtClientAddressCancelledEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi.CancelCourierReturnOper;
import ru.yandex.market.logistics.lrm.client.model.NotFoundError;
import ru.yandex.market.logistics.lrm.client.model.ResourceType;
import ru.yandex.market.logistics.lrm.client.model.ValidationError;
import ru.yandex.market.logistics.lrm.client.model.ValidationViolation;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnCancellationMeta;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnCancellationMeta.Reason;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnCancellationMeta.Source;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.lrm.config.LocalsConfiguration.TEST_UUID;

@DisplayName("Отмена клиентского курьерского возврата клиентом")
@DatabaseSetup("/database/api/returns/cancel-client-courier/before/minimal.xml")
class ClientCourierReturnCancelTest extends AbstractIntegrationYdbTest {

    private static final long RETURN_ID = 1L;
    private static final long RETURN_EXTERNAL_ID = 345L;
    private static final Instant NOW = Instant.parse("2022-04-06T08:10:12.00Z");

    @Autowired
    private EntityMetaTableDescription entityMetaTable;

    @Autowired
    private LesProducer lesProducer;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTable);
    }

    @BeforeEach
    void setup() {
        clock.setFixed(NOW, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Возврат не найден")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnNotFound() {
        NotFoundError error = cancel(2L)
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error.getIds()).containsExactly(2L);
        softly.assertThat(error.getResourceType()).isEqualTo(ResourceType.CLIENT_RETURN);
        softly.assertThat(error.getMessage()).isNotNull();

        assertEmptyYdb();
    }

    @Test
    @DisplayName("Не клиентский возврат")
    @DatabaseSetup(
        value = "/database/api/returns/cancel-client-courier/before/non_client.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void nonClientReturn() {
        NotFoundError error = cancel(RETURN_EXTERNAL_ID)
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error.getIds()).containsExactly(RETURN_EXTERNAL_ID);
        softly.assertThat(error.getResourceType()).isEqualTo(ResourceType.CLIENT_RETURN);
        softly.assertThat(error.getMessage()).isNotNull();

        assertEmptyYdb();
    }

    @Test
    @DisplayName("Не курьерский возврат")
    @DatabaseSetup(
        value = "/database/api/returns/cancel-client-courier/before/non_courier.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void nonCourierReturn() {
        ValidationError error = cancel(RETURN_EXTERNAL_ID)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getErrors())
            .extracting(ValidationViolation::getMessage)
            .containsExactly("must be client courier return");

        assertEmptyYdb();
    }

    @Test
    @DisplayName("Возврат уже отменён")
    @DatabaseSetup(
        value = "/database/api/returns/cancel-client-courier/before/cancelled.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks_and_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void alreadyCancelled() {
        cancel(RETURN_EXTERNAL_ID)
            .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    @DisplayName("Успех")
    @ExpectedDatabase(
        value = "/database/api/returns/cancel-client-courier/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        cancel(RETURN_EXTERNAL_ID)
            .execute(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(getEntityMetaRecord(RETURN_1_HASH, "RETURN", RETURN_ID, "courier-client-return-cancellation"))
            .map(EntityMetaTableDescription.EntityMetaRecord::value)
            .map(v -> readValue(v, CourierClientReturnCancellationMeta.class))
            .contains(
                CourierClientReturnCancellationMeta.builder()
                    .source(Source.CLIENT)
                    .reason(Reason.CLIENT_REQUESTED_CANCELLATION)
                    .build()
            );

        verify(lesProducer).send(
            new Event(
                SOURCE_FOR_LES,
                TEST_UUID,
                NOW.toEpochMilli(),
                "LRM_RETURN_AT_CLIENT_ADDRESS_CANCELLED",
                new LrmReturnAtClientAddressCancelledEvent(
                    RETURN_ID,
                    "345",
                    TplReturnAtClientAddressModificationSource.CLIENT,
                    TplReturnAtClientAddressCancelReason.CLIENT_REQUESTED_CANCELLATION
                ),
                ""
            ),
            OUT_LES_QUEUE
        );
    }

    private void assertEmptyYdb() {
        softly.assertThat(getEntityMetaRecord(RETURN_1_HASH, "RETURN", RETURN_ID, "courier-client-return-cancellation"))
            .isEmpty();
    }

    @Nonnull
    private CancelCourierReturnOper cancel(Long id) {
        return apiClient.returns().cancelCourierReturn().externalIdPath(id);
    }

}
