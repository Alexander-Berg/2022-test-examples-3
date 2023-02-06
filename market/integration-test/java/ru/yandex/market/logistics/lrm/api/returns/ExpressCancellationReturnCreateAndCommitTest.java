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
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi;
import ru.yandex.market.logistics.lrm.client.model.CreateExpressCancellationReturnRequest;
import ru.yandex.market.logistics.lrm.client.model.CreateReturnResponse;
import ru.yandex.market.logistics.lrm.client.model.OrderItemInfo;
import ru.yandex.market.logistics.lrm.client.model.ValidationError;
import ru.yandex.market.logistics.lrm.client.model.ValidationViolation;
import ru.yandex.market.logistics.lrm.config.properties.FeatureProperties;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.ALREADY_REPORTED;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultCancellationBox;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultCancellationItem;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultDimensions;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Создание и коммит невыкупа экспресс-заказа")
class ExpressCancellationReturnCreateAndCommitTest extends AbstractIntegrationYdbTest {

    private static final Instant DATETIME = Instant.parse("2022-03-02T11:12:13.00Z");

    @Autowired
    private EntityMetaTableDescription entityMetaTable;
    @Autowired
    private FeatureProperties featureProperties;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTable);
    }

    @BeforeEach
    void setup() {
        clock.setFixed(DATETIME, ZoneId.systemDefault());
        when(featureProperties.isEnableCreateControlPointForDropshipReturn()).thenReturn(false);
    }

    @Test
    @DisplayName("208 код ответа: дублируется order_external_id")
    @DatabaseSetup("/database/api/returns/create-cancellation/before/return.xml")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/before/return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/empty_control_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void duplicateOrderExternalId() {
        CreateReturnResponse response = create(defaultRequest())
            .executeAs(validatedWith(shouldBeCode(ALREADY_REPORTED.value())));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(2L);
    }

    @Test
    @DisplayName("208 код ответа: дублируется order_external_id. Включен флаг создания КП")
    @DatabaseSetup("/database/api/returns/create-cancellation/before/return.xml")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/empty_control_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void duplicateOrderExternalIdWithControlPoint() {
        when(featureProperties.isEnableCreateControlPointForDropshipReturn()).thenReturn(true);

        CreateReturnResponse response = create(defaultRequest())
            .executeAs(validatedWith(shouldBeCode(ALREADY_REPORTED.value())));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(2L);
    }

    @Test
    @DisplayName("Минимальный запрос")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/minimal_express.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/empty_control_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void minimal() {
        CreateReturnResponse response = create(defaultRequest())
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("Минимальный запрос. Включен флаг создания КП")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/minimal_express_cp.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void minimalWithControlPoint() {
        when(featureProperties.isEnableCreateControlPointForDropshipReturn()).thenReturn(true);

        CreateReturnResponse response = create(defaultRequest())
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("Поля товара")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/item_express.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/empty_control_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void itemFields() {
        CreateExpressCancellationReturnRequest request = defaultRequest()
            .items(List.of(
                defaultCancellationItem()
                    .boxExternalId("box-external-id")
                    .instances(Map.of("CIS", "876IUYkjh"))
            ));
        CreateReturnResponse response = create(request)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("Поля товара. Включен флаг создания КП")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/item_express_cp.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void itemFieldsWithControlPoint() {
        when(featureProperties.isEnableCreateControlPointForDropshipReturn()).thenReturn(true);

        CreateExpressCancellationReturnRequest request = defaultRequest()
            .items(List.of(
                defaultCancellationItem()
                    .boxExternalId("box-external-id")
                    .instances(Map.of("CIS", "876IUYkjh"))
            ));
        CreateReturnResponse response = create(request)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("Поля возврата")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/return_express.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/empty_control_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnFields() {
        CreateExpressCancellationReturnRequest request = defaultRequest()
            .orderItemsInfo(List.of(
                new OrderItemInfo()
                    .supplierId(300L)
                    .vendorCode("item-vendor-code")
                    .instances(List.of(Map.of("CIS", "876IUYkjh")))
            ));

        CreateReturnResponse response = create(request)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("Поля возврата. Включен флаг создания КП")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/after/return_express_cp.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnFieldsWithControlPoint() {
        when(featureProperties.isEnableCreateControlPointForDropshipReturn()).thenReturn(true);

        CreateExpressCancellationReturnRequest request = defaultRequest()
            .orderItemsInfo(List.of(
                new OrderItemInfo()
                    .supplierId(300L)
                    .vendorCode("item-vendor-code")
                    .instances(List.of(Map.of("CIS", "876IUYkjh")))
            ));

        CreateReturnResponse response = create(request)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);
    }

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource({
        "requestValidation",
        "itemsValidation",
        "boxesValidation",
    })
    @DisplayName("Валидация запроса")
    void requestValidation(CreateExpressCancellationReturnRequest request, String field, String message) {
        ValidationError response = create(request)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(response.getErrors())
            .containsExactly(
                new ValidationViolation()
                    .field(field)
                    .message(message)
            );
    }

    @Nonnull
    private static Stream<Arguments> requestValidation() {
        return Stream.of(
            Arguments.of(
                defaultRequest().orderExternalId(null),
                "orderExternalId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().returnSortingCenterId(null),
                "returnSortingCenterId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().returnPartnerSortingCenterId(null),
                "returnPartnerSortingCenterId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().shopId(null),
                "shopId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().shopPartnerId(null),
                "shopPartnerId",
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
    private static Stream<Arguments> itemsValidation() {
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
    private static Stream<Arguments> boxesValidation() {
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
    private static CreateExpressCancellationReturnRequest defaultRequest() {
        return new CreateExpressCancellationReturnRequest()
            .orderExternalId("order-external-id")
            .returnSortingCenterId(100L)
            .returnPartnerSortingCenterId(10L)
            .shopPartnerId(500L)
            .shopId(500500L)
            .items(List.of(defaultCancellationItem()))
            .boxes(List.of(defaultCancellationBox()));
    }

    @Nonnull
    private ReturnsApi.CreateAndCommitExpressCancellationReturnOper create(
        CreateExpressCancellationReturnRequest request
    ) {
        return apiClient.returns().createAndCommitExpressCancellationReturn().body(request);
    }
}

