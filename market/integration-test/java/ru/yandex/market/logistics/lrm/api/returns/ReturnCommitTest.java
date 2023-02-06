package ru.yandex.market.logistics.lrm.api.returns;

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

import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi.CommitReturnOper;
import ru.yandex.market.logistics.lrm.client.model.NotFoundError;
import ru.yandex.market.logistics.lrm.client.model.ResourceType;
import ru.yandex.market.logistics.lrm.client.model.ValidationError;
import ru.yandex.market.logistics.lrm.client.model.ValidationViolation;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.DetachedTypedEntity;
import ru.yandex.market.logistics.lrm.service.meta.EntityMetaService;
import ru.yandex.market.logistics.lrm.service.meta.model.ReturnEntityEnrichEnqueuedMeta;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Подтверждение возврата")
class ReturnCommitTest extends AbstractIntegrationYdbTest {

    private static final Instant DATETIME = Instant.parse("2021-08-30T11:12:13.00Z");

    @Autowired
    private EntityMetaTableDescription entityMetaTable;

    @Autowired
    private EntityMetaService entityMetaService;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTable);
    }

    @BeforeEach
    void setup() {
        clock.setFixed(DATETIME, MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/database/api/returns/commit/before/full.xml")
    @ExpectedDatabase(
        value = "/database/api/returns/commit/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        setupEntityMetaData();
        commitReturn(1)
            .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Test
    @DisplayName("У товара нет коробки")
    @DatabaseSetup("/database/api/returns/commit/before/no_item_box.xml")
    @ExpectedDatabase(
        value = "/database/api/returns/commit/after/no_item_box.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void noItemBox() {
        setupEntityMetaData();
        commitReturn(1)
            .execute(validatedWith(shouldBeCode(SC_OK)));
        assertYdb();
    }

    @Test
    @DisplayName("Возврат уже подтверждён")
    @DatabaseSetup("/database/api/returns/commit/before/committed.xml")
    @ExpectedDatabase(
        value = "/database/api/returns/commit/after/committed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void alreadyCommitted() {
        commitReturn(1)
            .execute(validatedWith(shouldBeCode(SC_OK)));
        assertEmptyYdb();
    }

    @Test
    @DisplayName("Возврат не найден")
    void notFound() {
        NotFoundError error = commitReturn(2)
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error.getIds()).containsExactly(2L);
        softly.assertThat(error.getResourceType()).isEqualTo(ResourceType.RETURN);
        softly.assertThat(error.getMessage()).isNotNull();
    }

    @Test
    @DisplayName("У возврата нет коробок")
    @DatabaseSetup("/database/api/returns/commit/before/no_boxes.xml")
    void noBoxes() {
        ValidationError error = commitReturn(1)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getErrors())
            .extracting(ValidationViolation::getMessage)
            .containsExactly("Return must have boxes on commit");
    }

    @Nonnull
    private CommitReturnOper commitReturn(long returnId) {
        return apiClient.returns().commitReturn().returnIdPath(returnId);
    }

    private void setupEntityMetaData() {
        entityMetaService.save(
            new DetachedTypedEntity(EntityType.RETURN, 1L),
            ReturnEntityEnrichEnqueuedMeta.builder().datetime(DATETIME).build()
        );
    }

    private void assertYdb() {
        softly.assertThat(
            getEntityMetaRecord(RETURN_1_HASH, "RETURN", 1L, "return-entity-enrich-enqueued")
                .map(EntityMetaTableDescription.EntityMetaRecord::value)
                .map(v -> readValue(v, ReturnEntityEnrichEnqueuedMeta.class))
        ).contains(
            ReturnEntityEnrichEnqueuedMeta.builder()
                .datetime(DATETIME)
                .build()
        );
    }

    private void assertEmptyYdb() {
        softly.assertThat(getEntityMetaRecord(RETURN_1_HASH, "RETURN", 1L, "return-entity-enrich-enqueued")).isEmpty();
    }
}
