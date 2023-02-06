package ru.yandex.market.logistics.lrm.api.returns;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi.UpdateReturnOper;
import ru.yandex.market.logistics.lrm.client.model.NotFoundError;
import ru.yandex.market.logistics.lrm.client.model.ResourceType;
import ru.yandex.market.logistics.lrm.client.model.ReturnBoxRequest;
import ru.yandex.market.logistics.lrm.client.model.UpdateReturnRequest;
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
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultBox;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultItem;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Обновление возврата")
class ReturnUpdateTest extends AbstractIntegrationYdbTest {

    private static final long RETURN_ID = 1;
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
        clock.setFixed(DATETIME, ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Успех. Возврат был обогащен на этапе создания")
    @DatabaseSetup("/database/api/returns/update/before/minimal.xml")
    @ExpectedDatabase(
        value = "/database/api/returns/update/after/success_enriched_on_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successEnrichedOnCreate() {
        setupEntityMetaData();
        UpdateReturnRequest request = defaultRequest()
            .items(List.of(defaultItem().boxExternalId("box-external-id")));
        updateReturn(RETURN_ID, request)
            .execute(validatedWith(shouldBeCode(SC_OK)));
        assertYdb();
    }

    @Test
    @DisplayName("Успех. Возврат не был обогащен на этапе создания")
    @DatabaseSetup("/database/api/returns/update/before/minimal.xml")
    @ExpectedDatabase(
        value = "/database/api/returns/update/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() {
        assertEmptyYdb();
        UpdateReturnRequest request = defaultRequest()
            .items(List.of(defaultItem().boxExternalId("box-external-id")));
        updateReturn(RETURN_ID, request)
            .execute(validatedWith(shouldBeCode(SC_OK)));
        assertYdb();
    }

    @Test
    @DisplayName("Товар совпал по кодам")
    @DatabaseSetup("/database/api/returns/update/before/instances.xml")
    @ExpectedDatabase(
        value = "/database/api/returns/update/after/success_instances.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void instancesSuccess() {
        assertEmptyYdb();
        UpdateReturnRequest request = defaultRequest()
            .items(List.of(
                defaultItem()
                    .boxExternalId("box-external-id")
                    .instances(Map.of("CIS", "876qwe"))
            ));
        updateReturn(RETURN_ID, request)
            .execute(validatedWith(shouldBeCode(SC_OK)));
        assertYdb();
    }

    @Test
    @DisplayName("Нет товаров в запросе")
    @DatabaseSetup("/database/api/returns/update/before/minimal.xml")
    @ExpectedDatabase(
        value = "/database/api/returns/update/after/success_no_items.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void noItems() {
        assertEmptyYdb();
        updateReturn(RETURN_ID, defaultRequest().items(null))
            .execute(validatedWith(shouldBeCode(SC_OK)));
        assertYdb();
    }

    @Test
    @DisplayName("Возврат не найден")
    void notFound() {
        NotFoundError error = updateReturn(2, defaultRequest())
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error.getResourceType()).isEqualTo(ResourceType.RETURN);
        softly.assertThat(error.getIds()).isEqualTo(List.of(2L));
        softly.assertThat(error.getMessage()).isNotNull();
        assertEmptyYdb();
    }

    @Test
    @DisplayName("Возврат уже оформлен")
    @DatabaseSetup("/database/api/returns/update/before/committed.xml")
    void committed() {
        ValidationError error = updateReturn(1, defaultRequest())
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getErrors())
            .extracting(ValidationViolation::getMessage)
            .containsExactly("Cannot update committed return");
        assertEmptyYdb();
    }

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource
    @DisplayName("Валидация запроса")
    @DatabaseSetup("/database/api/returns/update/before/existing_return.xml")
    void requestValidation(UpdateReturnRequest request, String field, String message) {
        ValidationError error = updateReturn(RETURN_ID, request)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);
        softly.assertThat(error.getErrors()).containsExactly(
            new ValidationViolation()
                .field(field)
                .message(message)
        );
        assertEmptyYdb();
    }

    @Nonnull
    public static Stream<Arguments> requestValidation() {
        return Stream.of(
            Arguments.of(defaultRequest().boxes(null), "boxes", "must not be null"),
            Arguments.of(defaultRequest().boxes(List.of()), "boxes", "size must be between 1 and 1000"),
            Arguments.of(
                defaultRequest().boxes(Collections.nCopies(1001, defaultBox())),
                "boxes",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().boxes(Collections.nCopies(3, defaultBox())),
                "boxes",
                "non-unique externalId: [box-external-id]"
            ),
            Arguments.of(
                defaultRequest()
                    .items(null)
                    .boxes(Collections.nCopies(3, defaultBox())),
                "boxes",
                "non-unique externalId: [box-external-id]"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultBox().externalId(null))),
                "boxes[0].externalId",
                "must not be null"
            ),
            Arguments.of(defaultRequest().items(List.of()), "items", "size must be between 1 and 1000"),
            Arguments.of(
                defaultRequest().items(Collections.nCopies(1001, defaultItem())),
                "items",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem().vendorCode(null))),
                "items[0].vendorCode",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem(), defaultItem().boxExternalId("invalid-box-id"))),
                "items[1].boxExternalId",
                "non-existent box id"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem(), defaultItem())),
                "items",
                "must match existing items"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem().vendorCode("another-vendor-code"))),
                "items[0]",
                "must match existing items"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem().supplierId(200L))),
                "items[0]",
                "must match existing items"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultItem().instances(Map.of()))),
                "items[0]",
                "must match existing items"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(new ReturnBoxRequest().externalId("existing-box-external-id"))),
                "boxes[0].externalId",
                "already used in returns: [2]"
            ),
            Arguments.of(
                defaultRequest()
                    .items(null)
                    .boxes(List.of(new ReturnBoxRequest().externalId("existing-box-external-id"))),
                "boxes[0].externalId",
                "already used in returns: [2]"
            )
        );
    }

    @Nonnull
    private static UpdateReturnRequest defaultRequest() {
        return new UpdateReturnRequest()
            .boxes(List.of(defaultBox()))
            .items(List.of(defaultItem()));
    }

    @Nonnull
    private UpdateReturnOper updateReturn(long id, UpdateReturnRequest request) {
        return apiClient.returns().updateReturn().returnIdPath(id).body(request);
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
