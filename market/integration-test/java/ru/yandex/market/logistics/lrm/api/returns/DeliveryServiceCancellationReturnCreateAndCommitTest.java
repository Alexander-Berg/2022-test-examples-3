package ru.yandex.market.logistics.lrm.api.returns;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi;
import ru.yandex.market.logistics.lrm.client.model.CreateDeliveryServiceCancellationReturnRequest;
import ru.yandex.market.logistics.lrm.client.model.CreateReturnResponse;
import ru.yandex.market.logistics.lrm.client.model.LogisticPointType;
import ru.yandex.market.logistics.lrm.client.model.OrderItemInfo;
import ru.yandex.market.logistics.lrm.client.model.ValidationError;
import ru.yandex.market.logistics.lrm.client.model.ValidationViolation;
import ru.yandex.market.logistics.lrm.config.locals.UuidGenerator;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.ALREADY_REPORTED;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultCancellationBox;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultCancellationItem;
import static ru.yandex.market.logistics.lrm.api.returns.ReturnFactory.defaultDimensions;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Создание и коммит невыкупа в схеме, когда мерч работает напрямую со службой доставки")
class DeliveryServiceCancellationReturnCreateAndCommitTest extends AbstractIntegrationTest {
    private static final Instant DATETIME = Instant.parse("2022-03-02T11:12:13.00Z");
    private static final Long WITHDRAW_POINT_ID = 1001L;
    private static final Long WITHDRAW_POINT_PARTNER_ID = 2001L;
    private static final Long SHOP_LOGISTIC_POINT_ID = 1002L;
    private static final Long SHOP_PARTNER_ID = 2002L;
    private static final Long SHOP_ID = 3002L;

    @Autowired
    private UuidGenerator uuidGenerator;

    @BeforeEach
    void setup() {
        clock.setFixed(DATETIME, ZoneId.systemDefault());
        when(uuidGenerator.get()).thenReturn(
            UUID.fromString("e11c5e64-3694-40c9-b9b4-126efedaa091"),
            UUID.fromString("e11c5e64-3694-40c9-b9b4-126efedaa092")
        );
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
    }

    @Test
    @DisplayName("Минимальный запрос. Точка забора невыкупа - ПВЗ")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/delivery-service/after/minimal_pvz.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void minimalPvz() {
        CreateReturnResponse response = create(defaultRequest())
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("Минимальный запрос. Точка забора невыкупа - СЦ")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/delivery-service/after/minimal_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void minimalSc() {
        CreateReturnResponse response = create(defaultRequest().logisticPointType(LogisticPointType.SORTING_CENTER))
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("Многокоробочность")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/delivery-service/after/several_boxes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void severalBoxes() {
        when(uuidGenerator.get()).thenReturn(
            UUID.fromString("e11c5e64-3694-40c9-b9b4-126efedaa091"),
            UUID.fromString("e11c5e64-3694-40c9-b9b4-126efedaa092"),
            UUID.fromString("e11c5e64-3694-40c9-b9b4-126efedaa093"),
            UUID.fromString("e11c5e64-3694-40c9-b9b4-126efedaa094")
        );

        CreateReturnResponse response = create(
            defaultRequest()
                .boxes(List.of(
                    defaultCancellationBox().externalId("box-external-id-1"),
                    defaultCancellationBox().externalId("box-external-id-2")
                ))
        )
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .extracting(CreateReturnResponse::getId)
            .isEqualTo(1L);
    }

    @Test
    @DisplayName("Поля товара")
    @ExpectedDatabase(
        value = "/database/api/returns/create-cancellation/delivery-service/after/item.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void itemFields() {
        CreateDeliveryServiceCancellationReturnRequest request = defaultRequest()
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
        value = "/database/api/returns/create-cancellation/delivery-service/after/return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void returnFields() {
        CreateDeliveryServiceCancellationReturnRequest request = defaultRequest()
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
    void requestValidation(CreateDeliveryServiceCancellationReturnRequest request, String field, String message) {
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
    static Stream<Arguments> requestValidation() {
        return Stream.of(
            Arguments.of(
                defaultRequest().orderExternalId(null),
                "orderExternalId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().logisticPointId(null),
                "logisticPointId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().logisticPointExternalId(null),
                "logisticPointExternalId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().logisticPointPartnerId(null),
                "logisticPointPartnerId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().logisticPointType(null),
                "logisticPointType",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().logisticPointName(null),
                "logisticPointName",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().shopLogisticPointId(null),
                "shopLogisticPointId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().shopLogisticPointExternalId(null),
                "shopLogisticPointExternalId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().shopPartnerId(null),
                "shopPartnerId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().shopId(null),
                "shopId",
                "must not be null"
            ),
            Arguments.of(
                defaultRequest().shopName(null),
                "shopName",
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
    private static CreateDeliveryServiceCancellationReturnRequest defaultRequest() {
        return new CreateDeliveryServiceCancellationReturnRequest()
            .orderExternalId("order-external-id")
            .logisticPointId(WITHDRAW_POINT_ID)
            .logisticPointExternalId("withdraw-logistic-point-external-id")
            .logisticPointPartnerId(WITHDRAW_POINT_PARTNER_ID)
            .logisticPointType(LogisticPointType.PICKUP)
            .logisticPointName("withdraw-logistic-point-name")
            .shopLogisticPointId(SHOP_LOGISTIC_POINT_ID)
            .shopLogisticPointExternalId("shop-logistic-point-external-id")
            .shopPartnerId(SHOP_PARTNER_ID)
            .shopId(SHOP_ID)
            .shopName("shop-name")
            .items(List.of(defaultCancellationItem()))
            .boxes(List.of(defaultCancellationBox()));
    }

    @Nonnull
    private ReturnsApi.CreateAndCommitDeliveryServiceCancellationReturnOper create(
        CreateDeliveryServiceCancellationReturnRequest request
    ) {
        return apiClient.returns().createAndCommitDeliveryServiceCancellationReturn().body(request);
    }
}
