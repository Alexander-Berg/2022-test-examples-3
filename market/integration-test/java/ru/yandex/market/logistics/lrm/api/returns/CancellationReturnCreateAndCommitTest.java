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
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi.CreateAndCommitCancellationReturnOper;
import ru.yandex.market.logistics.lrm.client.model.CancellationReturnBox;
import ru.yandex.market.logistics.lrm.client.model.CreateCancellationReturnRequest;
import ru.yandex.market.logistics.lrm.client.model.CreateReturnResponse;
import ru.yandex.market.logistics.lrm.client.model.OrderItemInfo;
import ru.yandex.market.logistics.lrm.client.model.ValidationError;
import ru.yandex.market.logistics.lrm.client.model.ValidationViolation;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.model.ReturnEntityEnrichEnqueuedMeta;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.springframework.http.HttpStatus.ALREADY_REPORTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultCancellationBox;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultCancellationItem;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultDimensions;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Создание и коммит невыкупа")
class CancellationReturnCreateAndCommitTest extends AbstractIntegrationYdbTest {

    private static final Instant DATETIME = Instant.parse("2022-03-02T11:12:13.00Z");

    @Autowired
    private EntityMetaTableDescription entityMetaTable;

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
    @DisplayName("208 код ответа: дублируется order_external_id")
    @DatabaseSetup("/database/api/returns/create-cancellation/before/return.xml")
    void duplicateOrderExternalId() {
        CreateReturnResponse response = create(defaultRequest())
            .executeAs(validatedWith(shouldBeCode(ALREADY_REPORTED.value())));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(2L);

        assertEmptyYdb();
    }

    @Test
    @DisplayName("Минимальный запрос")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/minimal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void minimal() {
        CreateReturnResponse response = create(defaultRequest())
            .executeAs(validatedWith(shouldBeCode(OK.value())));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);

        assertYdb();
    }

    @Test
    @DisplayName("Минимальный запрос с фейкпарселами")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/minimal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void requestWithFakeParcels() {
        CreateCancellationReturnRequest request = defaultRequest()
            .boxes(List.of(
                new CancellationReturnBox()
                    .externalId("box-external-id")
                    .dimensions(defaultDimensions()),
                new CancellationReturnBox()
                    .externalId("FAKEPARCEL-0000705249-00001-1")
                    .dimensions(defaultDimensions())
            ));
        CreateReturnResponse response = create(request)
            .executeAs(validatedWith(shouldBeCode(OK.value())));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);

        assertYdb();
    }

    @Test
    @DisplayName("Поля товара")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/item.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void itemFields() {
        CreateCancellationReturnRequest request = defaultRequest()
            .items(List.of(
                defaultCancellationItem()
                    .boxExternalId("box-external-id")
                    .instances(Map.of("CIS", "876IUYkjh"))
            ));
        CreateReturnResponse response = create(request)
            .executeAs(validatedWith(shouldBeCode(OK.value())));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);

        assertYdb();
    }

    @Test
    @DisplayName("Поля возврата")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnFields() {
        CreateCancellationReturnRequest request = defaultRequest()
            .orderItemsInfo(List.of(
                new OrderItemInfo()
                    .supplierId(300L)
                    .vendorCode("item-vendor-code")
                    .instances(List.of(Map.of("CIS", "876IUYkjh")))
            ));

        CreateReturnResponse response = create(request)
            .executeAs(validatedWith(shouldBeCode(OK.value())));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);

        assertYdb();
    }

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource({
        "requestValidation",
        "itemsValidation",
        "boxesValidation",
    })
    @DisplayName("Валидация запроса")
    void requestValidation(CreateCancellationReturnRequest request, String field, String message) {
        ValidationError response = create(request)
            .execute(validatedWith(shouldBeCode(BAD_REQUEST.value())))
            .as(ValidationError.class);

        softly.assertThat(response.getErrors())
            .containsExactly(
                new ValidationViolation()
                    .field(field)
                    .message(message)
            );

        assertEmptyYdb();
    }

    @Nonnull
    static Stream<Arguments> requestValidation() {
        return Stream.of(
            Arguments.of(
                defaultRequest().orderExternalId(null),
                "orderExternalId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().orderItemsInfo(List.of()),
                "orderItemsInfo",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().orderItemsInfo(Collections.nCopies(1001, new OrderItemInfo())),
                "orderItemsInfo",
                "size must be between 1 and 1000"
            )
        );
    }

    @Nonnull
    static Stream<Arguments> itemsValidation() {
        return Stream.of(
            Arguments.of(
                defaultRequest().items(null),
                "items",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().items(List.of()),
                "items",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().items(Collections.nCopies(1001, defaultCancellationItem())),
                "items",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().items(List.of(defaultCancellationItem().supplierId(null))),
                "items[0].supplierId",
                "must not be null"
            )
        );
    }

    @Nonnull
    static Stream<Arguments> boxesValidation() {
        return Stream.of(
            Arguments.of(
                defaultRequest().boxes(null),
                "boxes",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of()),
                "boxes",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().boxes(Collections.nCopies(1001, defaultCancellationBox())),
                "boxes",
                "size must be between 1 and 1000"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultCancellationBox().externalId(null))),
                "boxes[0].externalId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultCancellationBox().dimensions(null))),
                "boxes[0].dimensions",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultCancellationBox().dimensions(defaultDimensions().height(null)))),
                "boxes[0].dimensions.height",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultCancellationBox().dimensions(defaultDimensions().width(null)))),
                "boxes[0].dimensions.width",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultCancellationBox().dimensions(defaultDimensions().length(null)))),
                "boxes[0].dimensions.length",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().boxes(List.of(defaultCancellationBox().dimensions(defaultDimensions().weight(null)))),
                "boxes[0].dimensions.weight",
                "must not be null"
            )
        );
    }

    @Nonnull
    private static CreateCancellationReturnRequest defaultRequest() {
        return new CreateCancellationReturnRequest()
            .orderExternalId("order-external-id")
            .items(List.of(defaultCancellationItem()))
            .boxes(List.of(defaultCancellationBox()));
    }

    @Nonnull
    private CreateAndCommitCancellationReturnOper create(CreateCancellationReturnRequest request) {
        return apiClient.returns().createAndCommitCancellationReturn().body(request);
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

