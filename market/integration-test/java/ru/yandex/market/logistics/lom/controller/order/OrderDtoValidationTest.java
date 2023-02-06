package ru.yandex.market.logistics.lom.controller.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.CredentialsDto;
import ru.yandex.market.logistics.lom.model.dto.DeliveryIntervalDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderItemBoxDto;
import ru.yandex.market.logistics.lom.model.dto.OrderServiceDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Валидация заказа OrderDto")
public class OrderDtoValidationTest extends AbstractContextualTest {
    private static final BigDecimal NEGATIVE_DECIMAL = new BigDecimal("-0.25");

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource("invalidOrderDtoProvider")
    @DisplayName("Провалидировать поля")
    void validateOrderDto(
        Consumer<WaybillOrderRequestDto> orderModifier,
        String invalidField,
        String errorMessage
    ) throws Exception {
        clock.setFixed(Instant.parse("2020-01-01T00:00:00.00Z"), MOSCOW_ZONE);

        WaybillOrderRequestDto orderDto = new WaybillOrderRequestDto();
        orderDto.setSenderId(1L)
            .setPlatformClientId(3L)
            .setReturnSortingCenterId(1L);
        orderModifier.accept(orderDto);
        mockMvc.perform(request(HttpMethod.POST, "/orders", orderDto))
            .andExpect(status().isBadRequest())
            .andExpect(content().json(buildBadRequestMessage(invalidField, errorMessage)));
    }

    @Test
    @DisplayName("Несуществующий тег")
    void validateOrderDtoTag() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_order_with_source_invalid.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/order/response/create_order_with_source_invalid.json"));
    }

    @Nonnull
    private static Stream<Arguments> invalidOrderDtoProvider() {
        return Stream.of(
            invalidOrder(),
            invalidCredentials(),
            invalidDeliveryInterval(),
            invalidCosts(),
            negativeCosts(),
            nullPlaceDimensions(),
            negativePlaceDimensions(),
            zeroPlaceDimensions(),
            maxPlaceDimensions(),
            invalidMonetary(),
            nullItemDimensions(),
            negativeItemDimensions(),
            zeroItemDimensions(),
            maxItemDimensions(),
            invalidWaybill(),
            externalId(),
            invalidStorageUnits(),
            invalidNewItems(),
            nullTag()
        )
            .flatMap(Function.identity())
            .map(triple -> Arguments.of(
                triple.getLeft(),
                triple.getMiddle(),
                triple.getRight()
            ));

    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> invalidOrder() {
        return Stream.of(
            Triple.of(b -> b.setPlatformClientId(null), "platformClientId", "must not be null"),
            Triple.of(b -> b.setSenderId(null), "senderId", "must not be null"),
            Triple.of(
                b -> b.setContacts(Collections.singletonList(null)),
                "contacts[0]",
                "must not be null"
            ),
            Triple.of(
                b -> b.setMaxAbsentItemsPricePercent(new BigDecimal("101")),
                "maxAbsentItemsPricePercent",
                "must be less than or equal to 100"
            ),
            Triple.of(
                b -> b.setMaxAbsentItemsPricePercent(new BigDecimal("-1")),
                "maxAbsentItemsPricePercent",
                "must be greater than or equal to 0"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> invalidCredentials() {
        return Stream.of(
            Triple.of(
                b -> b.setCredentials(CredentialsDto.builder().email("invalid-email").build()),
                "credentials.email",
                "must be a well-formed email address"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> externalId() {
        return Stream.of(
            Triple.of(
                b -> b.setExternalId(null).setPlatformClientId(PlatformClient.YANDEX_GO.getId()),
                "externalId",
                "must not be null"
            ),
            Triple.of(
                b -> b.setExternalId("1234567890_1234567890"),
                "externalId",
                "size must be between 1 and 20"
            ),
            Triple.of(
                b -> b.setExternalId("абыр*:%№@"),
                "externalId",
                "must match \\\"^[a-zA-Z0-9\\\\-_\\\\\\\\/]*$\\\""
            ),
            Triple.of(
                b -> b.setExternalId(""),
                "externalId",
                "size must be between 1 and 20"
            ),
            Triple.of(
                b -> b.setExternalId("a".repeat(51)).setPlatformClientId(PlatformClient.YANDEX_GO.getId()),
                "externalId",
                "size must be between 1 and 50"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> invalidDeliveryInterval() {
        return Stream.of(
            Triple.of(
                b -> b.setDeliveryInterval(
                    DeliveryIntervalDto.builder().deliveryDateMin(LocalDate.of(1970, Month.JANUARY, 1)).build()
                ),
                "deliveryInterval.deliveryDateMin",
                "delivery date must not be earlier than today"
            ),
            Triple.of(
                b -> b.setDeliveryInterval(
                    DeliveryIntervalDto.builder().deliveryDateMax(LocalDate.of(1970, Month.JANUARY, 1)).build()
                ),
                "deliveryInterval.deliveryDateMax",
                "delivery date must not be earlier than today"
            ),
            Triple.of(
                b -> b.setDeliveryInterval(
                    DeliveryIntervalDto.builder()
                        .deliveryDateMin(LocalDate.of(2020, 2, 1))
                        .deliveryDateMax(LocalDate.of(2020, 1, 10))
                        .build()
                ),
                "deliveryInterval",
                "delivery interval min date must be before or equal to max date"
            ),
            Triple.of(
                b -> b.setDeliveryInterval(DeliveryIntervalDto.builder()
                    .fromTime(LocalTime.NOON)
                    .toTime(LocalTime.MIDNIGHT)
                    .build()),
                "deliveryInterval",
                "from must be earlier than to"
            ),
            Triple.of(
                b -> b.setDeliveryInterval(DeliveryIntervalDto.builder().tzOffset(-70000).build()),
                "deliveryInterval",
                "timezone offset mush be between -64800 and 64800"
            ),
            Triple.of(
                b -> b.setDeliveryInterval(DeliveryIntervalDto.builder().tzOffset(70000).build()),
                "deliveryInterval",
                "timezone offset mush be between -64800 and 64800"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> invalidCosts() {
        return Stream.of(
            Triple.of(
                b -> b.setCost(CostDto.builder().services(Collections.singletonList(null)).build()),
                "cost.services[0]",
                "must not be null"
            ),
            Triple.of(
                b -> b.setCost(
                    CostDto.builder()
                        .services(List.of(orderServiceDtoBuilder().cost(null).build()))
                        .build()
                ),
                "cost.services[0].cost",
                "must not be null"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> negativeCosts() {
        return Stream.of(
            Triple.of(
                b -> b.setCost(
                    CostDto.builder()
                        .services(List.of(
                            orderServiceDtoBuilder().cost(NEGATIVE_DECIMAL).build()
                        ))
                        .build()
                ),
                "cost.services[0].cost",
                "must be greater than or equal to 0"
            ),
            Triple.of(
                b -> b.setCost(
                    CostDto.builder()
                        .services(List.of(orderServiceDtoBuilder().customerPay(null).build()))
                        .build()
                ),
                "cost.services[0].customerPay",
                "must not be null"
            ),
            Triple.of(
                b -> b.setCost(CostDto.builder().cashServicePercent(NEGATIVE_DECIMAL).build()),
                "cost.cashServicePercent",
                "must be greater than or equal to 0"
            ),
            Triple.of(
                b -> b.setCost(CostDto.builder().assessedValue(NEGATIVE_DECIMAL).build()),
                "cost.assessedValue",
                "must be greater than or equal to 0"
            ),
            Triple.of(
                b -> b.setCost(CostDto.builder().delivery(NEGATIVE_DECIMAL).build()),
                "cost.delivery",
                "must be greater than or equal to 0"
            ),
            Triple.of(
                b -> b.setCost(CostDto.builder().deliveryForCustomer(NEGATIVE_DECIMAL).build()),
                "cost.deliveryForCustomer",
                "must be greater than or equal to 0"
            ),
            Triple.of(
                b -> b.setCost(CostDto.builder().manualDeliveryForCustomer(NEGATIVE_DECIMAL).build()),
                "cost.manualDeliveryForCustomer",
                "must be greater than or equal to 0"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> nullPlaceDimensions() {
        return Stream.of(
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().length(null).build()
                ).build())),
                "units[0].dimensions.length",
                "must not be null"
            ),
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().width(null).build()
                ).build())),
                "units[0].dimensions.width",
                "must not be null"
            ),
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().height(null).build()
                ).build())),
                "units[0].dimensions.height",
                "must not be null"
            ),
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().weightGross(null).build()
                ).build())),
                "units[0].dimensions.weightGross",
                "must not be null"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> negativePlaceDimensions() {
        return Stream.of(
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().length(-100).build()
                ).build())),
                "units[0].dimensions.length",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().width(-100).build()
                ).build())),
                "units[0].dimensions.width",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().height(-100).build()
                ).build())),
                "units[0].dimensions.height",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().weightGross(NEGATIVE_DECIMAL).build()
                ).build())),
                "units[0].dimensions.weightGross",
                "must be greater than 0"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> zeroPlaceDimensions() {
        return Stream.of(
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().length(0).build()
                ).build())),
                "units[0].dimensions.length",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().width(0).build()
                ).build())),
                "units[0].dimensions.width",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().height(0).build()
                ).build())),
                "units[0].dimensions.height",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().weightGross(BigDecimal.ZERO).build()
                ).build())),
                "units[0].dimensions.weightGross",
                "must be greater than 0"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> maxPlaceDimensions() {
        return Stream.of(
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().length(600).build()
                ).build())),
                "units[0].dimensions.length",
                "must be less than or equal to 500"
            ),
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().width(700).build()
                ).build())),
                "units[0].dimensions.width",
                "must be less than or equal to 500"
            ),
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().height(800).build()
                ).build())),
                "units[0].dimensions.height",
                "must be less than or equal to 500"
            ),
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().dimensions(
                    dimensionsBuilder().weightGross(new BigDecimal("1600")).build()
                ).build())),
                "units[0].dimensions.weightGross",
                "must be less than or equal to 1500"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> invalidNewItems() {
        return Stream.of(
            Triple.of(
                b -> b.setItems(Collections.singletonList(null)),
                "items[0]",
                "must not be null"
            ),
            Triple.of(
                b -> b.setItems(List.of(ItemDto.builder().count(-1).build())),
                "items[0].count",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setItems(List.of(ItemDto.builder().count(0).build())),
                "items[0].count",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setItems(List.of(
                    ItemDto.builder().price(MonetaryDto.builder().exchangeRate(NEGATIVE_DECIMAL).build()).build()
                    )
                ),
                "items[0].price.exchangeRate",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setItems(List.of(
                    ItemDto.builder().price(MonetaryDto.builder().exchangeRate(BigDecimal.ZERO).build()).build()
                    )
                ),
                "items[0].price.exchangeRate",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setItems(List.of(ItemDto.builder().boxes(List.of(
                    OrderItemBoxDto.builder().storageUnitExternalIds(Collections.singleton(null)).build())
                    ).build())
                ),
                "items[0].boxes[0].storageUnitExternalIds[]",
                "must not be blank"
            ),
            Triple.of(
                b -> b.setItems(List.of(ItemDto.builder().boxes(List.of(
                    OrderItemBoxDto.builder().storageUnitExternalIds(Collections.singleton("")).build())
                    ).build())
                ),
                "items[0].boxes[0].storageUnitExternalIds[]",
                "must not be blank"
            ),
            Triple.of(
                b -> b.setItems(List.of(ItemDto.builder().boxes(List.of(
                    OrderItemBoxDto.builder().storageUnitIndexes(Collections.singletonList(null)).build())
                    ).build())
                ),
                "items[0].boxes[0].storageUnitIndexes[0]",
                "must not be null"
            ),
            Triple.of(
                b -> b.setItems(List.of(ItemDto.builder().cargoTypes(Collections.singleton(null)).build())),
                "items[0].cargoTypes[]",
                "must not be null"
            ),
            Triple.of(
                b -> b.setItems(List.of(ItemDto.builder().cargoTypes(Set.of()).supplierInn("1".repeat(13)).build())),
                "items[0].supplierInn",
                "size must be between 0 and 12"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> invalidMonetary() {
        return Stream.of(
            Triple.of(
                b -> b.setItems(itemsWithInvalidCurrency("рублей")),
                "items[0].price.currency",
                "size must be between 3 and 3"
            ),
            Triple.of(
                b -> b.setItems(itemsWithInvalidValue(BigDecimal.valueOf(-1))),
                "items[0].price.value",
                "must be greater than or equal to 0"
            ),
            Triple.of(
                b -> b.setItems(itemsWithInvalidCurrency("р.")),
                "items[0].price.currency",
                "size must be between 3 and 3"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> nullItemDimensions() {
        return Stream.of(
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder()
                        .dimensions(dimensionsBuilder().length(null).build())
                        .build())
                ),
                "items[0].dimensions.length",
                "must not be null"
            ),
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder()
                        .dimensions(dimensionsBuilder().height(null).build())
                        .build())
                ),
                "items[0].dimensions.height",
                "must not be null"
            ),
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder()
                        .dimensions(dimensionsBuilder().width(null).build())
                        .build())
                ),
                "items[0].dimensions.width",
                "must not be null"
            ),
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder()
                        .dimensions(dimensionsBuilder().weightGross(null).build())
                        .build())
                ),
                "items[0].dimensions.weightGross",
                "must not be null"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> negativeItemDimensions() {
        return Stream.of(
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder()
                        .dimensions(dimensionsBuilder().length(-100).build())
                        .build())
                ),
                "items[0].dimensions.length",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder()
                        .dimensions(dimensionsBuilder().height(-100).build())
                        .build())
                ),
                "items[0].dimensions.height",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder()
                        .dimensions(dimensionsBuilder().width(-100).build())
                        .build())
                ),
                "items[0].dimensions.width",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder()
                        .dimensions(dimensionsBuilder().weightGross(NEGATIVE_DECIMAL).build())
                        .build())
                ),
                "items[0].dimensions.weightGross",
                "must be greater than 0"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> zeroItemDimensions() {
        return Stream.of(
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder()
                        .dimensions(dimensionsBuilder().length(0).build())
                        .build())
                ),
                "items[0].dimensions.length",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder()
                        .dimensions(dimensionsBuilder().height(0).build())
                        .build())
                ),
                "items[0].dimensions.height",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder()
                        .dimensions(dimensionsBuilder().width(0).build())
                        .build())
                ),
                "items[0].dimensions.width",
                "must be greater than 0"
            ),
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder()
                        .dimensions(dimensionsBuilder().weightGross(BigDecimal.ZERO).build())
                        .build())
                ),
                "items[0].dimensions.weightGross",
                "must be greater than 0"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> maxItemDimensions() {
        return Stream.of(
            Triple.of(
                b -> b.setItems(List.of(
                    ItemDto.builder().dimensions(dimensionsBuilder().length(600).build()).build()
                )),
                "items[0].dimensions.length",
                "must be less than or equal to 500"
            ),
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder().dimensions(dimensionsBuilder().width(700).build()).build())
                ),
                "items[0].dimensions.width",
                "must be less than or equal to 500"
            ),
            Triple.of(
                b -> b.setItems(
                    List.of(ItemDto.builder().dimensions(dimensionsBuilder().height(800).build()).build())
                ),
                "items[0].dimensions.height",
                "must be less than or equal to 500"
            ),
            Triple.of(
                b -> b.setItems(List.of(
                    ItemDto.builder()
                        .dimensions(dimensionsBuilder().weightGross(new BigDecimal("1600")).build())
                        .build()
                )),
                "items[0].dimensions.weightGross",
                "must be less than or equal to 1500"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> invalidWaybill() {
        return Stream.of(
            Triple.of(
                b -> b.setWaybill(Collections.singletonList(null)),
                "waybill[0]",
                "must not be null"
            ),
            Triple.of(
                b -> b.setWaybill(List.of(WaybillSegmentDto.builder().segmentType(null).build())),
                "waybill[0].segmentType",
                "must not be null"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> invalidStorageUnits() {
        return Stream.of(
            Triple.of(
                b -> b.setUnits(Collections.singletonList(null)),
                "units[0]",
                "must not be null"
            ),
            Triple.of(
                b -> b.setUnits(List.of(storageUnit().type(null).build())),
                "units[0].type",
                "must not be null"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillOrderRequestDto>, String, String>> nullTag() {
        return Stream.of(
            Triple.of(
                b -> b.setTags(Collections.singleton(null)),
                "tags[]",
                "must not be null"
            )
        );
    }

    @Nonnull
    private static StorageUnitDto.StorageUnitDtoBuilder storageUnit() {
        return StorageUnitDto.builder().type(StorageUnitType.ROOT).externalId("unit-id");
    }

    @Nonnull
    private static List<ItemDto> itemsWithInvalidCurrency(String currency) {
        return List.of(
            ItemDto.builder().price(MonetaryDto.builder().currency(currency).build()).build()
        );
    }

    @Nonnull
    private static List<ItemDto> itemsWithInvalidValue(BigDecimal value) {
        return List.of(
            ItemDto.builder().price(
                MonetaryDto.builder()
                    .currency("RUB")
                    .exchangeRate(BigDecimal.ONE)
                    .value(value)
                    .build()
            ).build()
        );
    }

    @Nonnull
    private static KorobyteDto.KorobyteDtoBuilder dimensionsBuilder() {
        return KorobyteDto.builder()
            .height(100)
            .length(200)
            .width(300)
            .weightGross(new BigDecimal("15.5"));
    }

    @Nonnull
    private static OrderServiceDto.OrderServiceDtoBuilder orderServiceDtoBuilder() {
        return OrderServiceDto.builder()
            .code(ShipmentOption.DELIVERY)
            .cost(new BigDecimal("100"))
            .customerPay(true);
    }

    @Nonnull
    private String buildBadRequestMessage(String fieldName, String message) {
        return String.format(
            "{\"message\": \"Following validation errors occurred:\\nField: '%s', message: '%s'\"}",
            fieldName, message
        );
    }
}
