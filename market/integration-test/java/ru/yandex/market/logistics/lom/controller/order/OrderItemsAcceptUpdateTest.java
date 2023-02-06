package ru.yandex.market.logistics.lom.controller.order;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderServiceDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderItemsRequest;
import ru.yandex.market.logistics.lom.model.enums.CargoType;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.VatType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.lom.utils.TestUtils.NON_NEGATIVE_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.NOT_BLANK_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.NOT_EMPTY_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.NOT_NULL_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.OPTIONAL_NOT_BLANK_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.POSITIVE_ERROR_MESSAGE;
import static ru.yandex.market.logistics.lom.utils.TestUtils.fieldValidationErrorMatcher;
import static ru.yandex.market.logistics.lom.utils.TestUtils.propertyValidationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Подтверждение обновления товаров в заказе")
@ParametersAreNonnullByDefault
class OrderItemsAcceptUpdateTest extends AbstractContextualTest {
    private static final ChangeOrderRequestPayload PAYLOAD_UPDATE = PayloadFactory.createChangeOrderRequestPayload(
        1L,
        "1",
        1
    );

    @Test
    @DisplayName("Успешное подтверждение обновления товаров заказа и их обновление (нет changeRequest-a)")
    @DatabaseSetup("/controller/order/updateitems/before/update_order_items_successful.xml")
    @ExpectedDatabase(
        value = "/controller/order/updateitems/after/order_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderItemsSuccess() throws Exception {
        performOrderItemsUpdate(validRequestBuilder().build())
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD_UPDATE);
    }

    @Test
    @DisplayName("Успешное подтверждение обновления товаров заказа с плохими настройками карготипа и кизов")
    @DatabaseSetup("/controller/order/updateitems/before/update_order_items_successful.xml")
    @ExpectedDatabase(
        value = "/controller/order/updateitems/after/order_update_with_cargo_type.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderItemsWithBadCargoTypeSuccess() throws Exception {
        performOrderItemsUpdate(validRequestBuilderWOCisAndWCargoType().build())
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD_UPDATE);
    }

    @Test
    @DisplayName("Успешное подтверждение обновления товаров заказа через ORDER_ITEM_IS_NOT_SUPPLIED changeRequest")
    @DatabaseSetup("/controller/order/updateitems/before/update_order_items_successful_change_request.xml")
    @ExpectedDatabase(
        value = "/controller/order/updateitems/after/order_accept_with_item_not_supplied.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderItemsSuccessChangeRequestFlow() throws Exception {
        performOrderItemsUpdate(validRequestBuilder().build())
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Успешное подтверждение обновления товаров заказа через ORDER_CHANGED_BY_PARTNER changeRequest")
    @DatabaseSetup("/controller/order/updateitems/before/update_order_items_successful_change_request.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/before/order_changed_by_partner_change_request.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/order/updateitems/after/order_accept_with_changed_by_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/order/updateitems/after/payload_with_external_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderItemsSuccessOrderChangedByPartnerChangeRequestFlow() throws Exception {
        performOrderItemsUpdate(validRequestBuilder().externalRequestId("external-request-id").build())
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName(
        "Успешное подтверждение обновления товаров заказа через ORDER_CHANGED_BY_PARTNER " +
            "changeRequest с входящими instances"
    )
    @DatabaseSetup("/controller/order/updateitems/before/update_order_items_successful_change_request.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/before/order_changed_by_partner_change_request.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/order/updateitems/after/order_accept_with_changed_by_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderItemsWithNewInstancesSuccessOrderChangedByPartnerChangeRequestFlow() throws Exception {
        performOrderItemsUpdate(validRequestBuilderWithInstances().build())
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName(
        "Успешное подтверждение обновления товаров заказа через ORDER_CHANGED_BY_PARTNER" +
            "changeRequest с текущими instances и пустыми входящими не соответствующими текущим"
    )
    @DatabaseSetup("/controller/order/updateitems/before/update_order_items_successful_change_request.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/before/order_changed_by_partner_change_request.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup(
        value = "/controller/order/updateitems/before/items_with_instances.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/order/updateitems/after/order_accept_with_changed_by_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderItemsWithOldInstancesSuccessOrderChangedByPartnerChangeRequestFlow() throws Exception {
        performOrderItemsUpdate(validRequestBuilder().build())
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName(
        "Успешное подтверждение обновления товаров заказа через " +
            "ORDER_CHANGED_BY_PARTNER changeRequest с текущими instances и пустыми входящими соответствующими текущим"
    )
    @DatabaseSetup("/controller/order/updateitems/before/update_order_items_successful_change_request.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/before/order_changed_by_partner_change_request.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup(
        value = "/controller/order/updateitems/before/items_with_instances.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/order/updateitems/after/order_accept_with_changed_by_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderItemsWithSameOldInstancesSuccessOrderChangedByPartnerChangeRequestFlow() throws Exception {
        performOrderItemsUpdate(validRequestBuilderWithInstances(
            "item article 1",
            100L,
            List.of(Map.of("cis", "abc321"))
        ).build())
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Успешное подтверждение обновления товаров заказа через ITEM_NOT_FOUND changeRequest")
    @DatabaseSetup("/controller/order/updateitems/before/update_order_items_successful_change_request.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/before/item_not_found_change_request.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/order/updateitems/after/order_accept_with_item_not_found.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderItemsSuccessItemNotFoundChangeRequestFlow() throws Exception {
        performOrderItemsUpdate(validRequestBuilder().build())
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Подтверждение обновления товаров несуществующего заказа")
    void updateItemsOfNonexistentOrder() throws Exception {
        performOrderItemsUpdate(validRequestBuilder().build())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with id [LOinttest-1]"));
    }

    @Test
    @DisplayName("Подтверждение обновления товаров заказа без запроса на изменение")
    @DatabaseSetup("/controller/order/updateitems/before/update_order_items_successful.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/before/without_order_changed_by_partner_request.xml",
        type = DatabaseOperation.REFRESH
    )
    void updateItemsOfOrderWithoutChangeRequest() throws Exception {
        performOrderItemsUpdate(validRequestBuilder().build())
            .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
            .andExpect(errorMessage(
                "Failed to update items of order 1 without change request in status INFO_RECEIVED"
            ));
    }

    @Test
    @DatabaseSetup("/controller/order/updateitems/before/update_order_items_successful.xml")
    @DatabaseSetup("/controller/order/updateitems/before/items_connected_to_places.xml")
    @DisplayName("Подтверждение обновления товаров, связанных с местами")
    void updateItemsConnectedToPlaces() throws Exception {
        performOrderItemsUpdate(validRequestBuilder().build())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Updating of items will remove connection between boxes and places of order 1"
            ));
    }

    @Test
    @DatabaseSetup("/controller/order/updateitems/before/update_order_items_successful.xml")
    @DatabaseSetup("/controller/order/updateitems/before/items_connected_to_root.xml")
    @DisplayName("Подтверждение обновления товаров, связанных с рутом")
    void updateItemsConnectedToRoot() throws Exception {
        performOrderItemsUpdate(validRequestBuilder().build())
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(QueueType.CHANGE_ORDER_REQUEST, PAYLOAD_UPDATE);
    }

    @Test
    @DatabaseSetup("/controller/order/updateitems/before/update_order_items_successful.xml")
    @DatabaseSetup("/service/orderchangedbypartner/before/order_cancelled.xml")
    @DisplayName("Подтверждение обновления товаров отменённого заказа")
    void updateItemsOfCancelledOrder() throws Exception {
        performOrderItemsUpdate(validRequestBuilder().build())
            .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
            .andExpect(errorMessage("Failed to update items of cancelled order 1"));
    }

    @DatabaseSetup("/controller/order/updateitems/before/update_order_items_successful.xml")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validateRequestArguments")
    @DisplayName("Валидация запроса на подтверждение обновления товаров в заказе")
    @ExpectedDatabase(
        value = "/controller/order/updateitems/before/update_order_items_successful.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void validateRequest(
        String displayName,
        UpdateOrderItemsRequest.UpdateOrderItemsRequestBuilder request,
        ResultMatcher resultMatcher
    ) throws Exception {
        performOrderItemsUpdate(request.build())
            .andExpect(status().isBadRequest())
            .andExpect(resultMatcher);
        assertOrderHistoryNeverChanged(1L);
    }

    private static Stream<Arguments> validateRequestArguments() {
        return Stream.of(
            Arguments.of(
                "barcode заказа не указан",
                validRequestBuilder().barcode(null),
                fieldValidationErrorMatcher("barcode", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Оценочная стоимость не указана",
                validRequestBuilder().cost(validCostBuilder().assessedValue(null).build()),
                propertyValidationErrorMatcher("cost.assessedValue", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Оценочная стоимость отрицательна",
                validRequestBuilder().cost(validCostBuilder().assessedValue(BigDecimal.valueOf(-1)).build()),
                fieldValidationErrorMatcher("cost.assessedValue", POSITIVE_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Стоимость доставки не указана",
                validRequestBuilder().cost(validCostBuilder().delivery(null).build()),
                propertyValidationErrorMatcher("cost.delivery", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Стоимость доставки отрицательна",
                validRequestBuilder().cost(validCostBuilder().delivery(BigDecimal.valueOf(-1)).build()),
                fieldValidationErrorMatcher("cost.delivery", POSITIVE_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Список товаров не указан",
                validRequestBuilder().items(null),
                fieldValidationErrorMatcher("items", NOT_EMPTY_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Список товаров пуст",
                validRequestBuilder().items(List.of()),
                fieldValidationErrorMatcher("items", NOT_EMPTY_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Название товара не указано",
                validRequestBuilder(validItemBuilder().name(null)),
                propertyValidationErrorMatcher("items[].name", NOT_BLANK_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Название товара пусто",
                validRequestBuilder(validItemBuilder().name("")),
                propertyValidationErrorMatcher("items[].name", NOT_BLANK_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Идентификатор поставщика отсутствует",
                validRequestBuilder(validItemBuilder().vendorId(null)),
                propertyValidationErrorMatcher("items[].vendorId", NOT_NULL_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Артикул товара пуст",
                validRequestBuilder(validItemBuilder().article("")),
                propertyValidationErrorMatcher("items[].article", OPTIONAL_NOT_BLANK_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Количество товара не указано",
                validRequestBuilder(validItemBuilder().count(null)),
                content().string(containsString("FieldError(propertyPath=items[].count, message=must not be null)"))
            ),
            Arguments.of(
                "Количество товара неположительно",
                validRequestBuilder(validItemBuilder().count(0)),
                fieldValidationErrorMatcher("items[0].count", NON_NEGATIVE_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Валюта цены не указана",
                validRequestBuilder(validItemBuilder().price(
                    MonetaryDto.builder()
                        .exchangeRate(BigDecimal.ONE)
                        .value(BigDecimal.valueOf(2))
                        .build()
                )),
                propertyValidationErrorMatcher("items[].price.currency", NOT_BLANK_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Обменный курс валюты цены не указан",
                validRequestBuilder(validItemBuilder().price(
                    MonetaryDto.builder()
                        .currency("RUB")
                        .value(BigDecimal.valueOf(2))
                        .build()
                )),
                content().string(containsString(
                    "FieldError(propertyPath=items[].price.exchangeRate, message=must not be null)"
                ))
            ),
            Arguments.of(
                "Цена не указана",
                validRequestBuilder(validItemBuilder().price(
                    MonetaryDto.builder()
                        .currency("RUB")
                        .exchangeRate(BigDecimal.ONE)
                        .build()
                )),
                content().string(containsString(
                    "FieldError(propertyPath=items[].price, message=must not be null)"
                ))
            ),
            Arguments.of(
                "Валюта объявленной ценности не указана",
                validRequestBuilder(validItemBuilder().assessedValue(MonetaryDto.builder()
                    .exchangeRate(BigDecimal.valueOf(3))
                    .value(BigDecimal.valueOf(4))
                    .build()
                )),
                propertyValidationErrorMatcher("items[].assessedValue.currency", NOT_BLANK_ERROR_MESSAGE)
            ),
            Arguments.of(
                "Обменный курс валюты объявленной ценности не указан",
                validRequestBuilder(validItemBuilder().assessedValue(MonetaryDto.builder()
                    .currency("RUB")
                    .value(BigDecimal.valueOf(4))
                    .build()
                )),
                propertyValidationErrorMatcher("items[].assessedValue.exchangeRate", NOT_NULL_ERROR_MESSAGE)
            )
        );
    }

    @Nonnull
    private static UpdateOrderItemsRequest.UpdateOrderItemsRequestBuilder validRequestBuilder() {
        return validRequestBuilder(validItemBuilder());
    }

    @Nonnull
    private static UpdateOrderItemsRequest.UpdateOrderItemsRequestBuilder validRequestBuilderWithInstances() {
        return validRequestBuilder(validItemBuilder().instances(List.of(Map.of("cis", "123abc"))));
    }

    @Nonnull
    private static UpdateOrderItemsRequest.UpdateOrderItemsRequestBuilder validRequestBuilderWOCisAndWCargoType() {
        return validRequestBuilder(
            validItemBuilder()
                .cargoTypes(Set.of(CargoType.CIS_REQUIRED))
        );
    }

    @Nonnull
    private static UpdateOrderItemsRequest.UpdateOrderItemsRequestBuilder validRequestBuilderWithInstances(
        String article, Long vendorId, List<Map<String, String>> ins
    ) {
        return validRequestBuilder(
            validItemBuilder()
                .article(article)
                .vendorId(vendorId)
                .instances(ins));
    }

    @Nonnull
    private static UpdateOrderItemsRequest.UpdateOrderItemsRequestBuilder validRequestBuilder(
        ItemDto.ItemDtoBuilder itemDtoBuilder
    ) {
        return UpdateOrderItemsRequest.builder()
            .barcode("LOinttest-1")
            .cost(validCostBuilder().build())
            .items(List.of(itemDtoBuilder.build()));
    }

    @Nonnull
    private static ItemDto.ItemDtoBuilder validItemBuilder() {
        return ItemDto.builder()
            .name("test-item-name")
            .msku(123L)
            .vendorId(1L)
            .article("test-item-article")
            .count(10)
            .price(
                MonetaryDto.builder()
                    .currency("RUB")
                    .exchangeRate(BigDecimal.ONE)
                    .value(BigDecimal.valueOf(2))
                    .build()
            )
            .assessedValue(MonetaryDto.builder()
                .currency("RUB")
                .exchangeRate(BigDecimal.valueOf(3))
                .value(BigDecimal.valueOf(4))
                .build()
            )
            .vatType(VatType.NO_VAT)
            .dimensions(KorobyteDto.builder()
                .weightGross(BigDecimal.ONE)
                .length(1)
                .width(2)
                .height(3)
                .build()
            );
    }

    @Nonnull
    private static CostDto.CostDtoBuilder validCostBuilder() {
        return CostDto.builder()
            .paymentMethod(PaymentMethod.CARD)
            .cashServicePercent(BigDecimal.valueOf(5))
            .assessedValue(BigDecimal.valueOf(100))
            .amountPrepaid(BigDecimal.valueOf(0))
            .itemsSum(BigDecimal.valueOf(2000))
            .delivery(BigDecimal.valueOf(1000))
            .deliveryForCustomer(BigDecimal.valueOf(2000))
            .manualDeliveryForCustomer(BigDecimal.valueOf(5000))
            .isFullyPrepaid(false)
            .total(BigDecimal.valueOf(4000))
            .tariffId(1L)
            .services(List.of(
                OrderServiceDto.builder()
                    .code(ShipmentOption.INSURANCE)
                    .cost(BigDecimal.valueOf(40.5))
                    .taxes(Collections.emptySortedSet())
                    .customerPay(false)
                    .build()
            ));
    }

    @Nonnull
    private ResultActions performOrderItemsUpdate(UpdateOrderItemsRequest request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/updateItems", request));
    }
}
