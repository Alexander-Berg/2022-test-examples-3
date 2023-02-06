package ru.yandex.market.logistics.lom.controller.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.ImmutableSortedMap;
import one.util.streamex.EntryStream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.DeliveryIntervalDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto;
import ru.yandex.market.logistics.lom.model.dto.OrderServiceDto;
import ru.yandex.market.logistics.lom.model.dto.RecipientDto;
import ru.yandex.market.logistics.lom.model.dto.RouteOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;
import ru.yandex.market.logistics.lom.model.enums.PointType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание заказа с маршрутом Комбинатора")
class OrderWithRouteValidationTest extends AbstractContextualTest {

    private static final ObjectMapper COMBINATOR_ROUTE_MAPPER = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2019-01-01T00:00:00.00Z"), ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Валидация отсутствия маршрута")
    void validateNullRoute() throws Exception {
        doCreateOrder(routeOrderRequestDto().setRoute(null))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Following validation errors occurred:\nField: 'route', message: 'must not be null'"
            ));
    }

    @DisplayName("Валидация маршрута")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("routeValidatingArguments")
    void validateRouteOrder(
        Map<String, String> errors,
        UnaryOperator<CombinatorRoute.DeliveryRoute> routeModifier
    ) throws Exception {
        JsonNode jsonRoute = COMBINATOR_ROUTE_MAPPER.valueToTree(
            new CombinatorRoute().setRoute(routeModifier.apply(deliveryRoute()))
        );

        doCreateOrder(routeOrderRequestDto().setRoute(jsonRoute))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(buildErrorMessage(errors)));
    }

    @DisplayName("Валидация полей заказа")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource(
        value = {
            "platformClientIdValidatingArgument",
            "deliveryTypeValidatingArgument",
            "deliveryIntervalValidatingArguments",
            "senderIdValidatingArguments",
            "recipientValidatingArguments",
            "costValidatingArguments",
            "itemsValidatingArguments",
            "unitsValidatingArguments",
            "contactsValidatingArguments",
            "pickupPointIdValidatingArgument",
            "deliveryAddressValidatingArgument",
            "insuranceServiceValidatingArgument",
        }
    )
    void validateOrder(
        String testCaseName,
        RouteOrderRequestDto order,
        String errorMessage
    ) throws Exception {
        doCreateOrder(order)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(errorMessage));
    }

    @Nonnull
    private static CombinatorRoute.DeliveryRoute deliveryRoute() {
        return new CombinatorRoute.DeliveryRoute()
            .setPoints(List.of(new CombinatorRoute.Point().setIds(new CombinatorRoute.PointIds())))
            .setPaths(List.of(new CombinatorRoute.Path().setPointTo(1)));
    }

    @Nonnull
    private static RouteOrderRequestDto routeOrderRequestDto() {
        JsonNode jsonRoute = COMBINATOR_ROUTE_MAPPER.valueToTree(
            new CombinatorRoute().setRoute(deliveryRoute())
        );
        RouteOrderRequestDto orderDto = new RouteOrderRequestDto().setRoute(jsonRoute);
        orderDto
            .setSenderId(1L)
            .setPlatformClientId(3L)
            .setReturnSortingCenterId(1L)
            .setDeliveryType(DeliveryType.PICKUP)
            .setPickupPointId(1L)
            .setItems(List.of(
                ItemDto.builder()
                    .name("Товар")
                    .vendorId(1L)
                    .article("Артикул")
                    .count(10)
                    .price(
                        MonetaryDto.builder()
                            .currency("RUR")
                            .value(BigDecimal.ONE)
                            .exchangeRate(BigDecimal.ONE)
                            .build()
                    )
                    .build()
            ))
            .setMarketIdFrom(1L)
            .setDeliveryInterval(
                DeliveryIntervalDto.builder()
                    .deliveryDateMin(LocalDate.of(2020, 11, 10))
                    .deliveryDateMax(LocalDate.of(2020, 11, 10))
                    .build()
            )
            .setCost(
                CostDto.builder()
                    .assessedValue(BigDecimal.ONE)
                    .cashServicePercent(BigDecimal.ONE)
                    .delivery(BigDecimal.ONE)
                    .deliveryForCustomer(BigDecimal.ONE)
                    .isFullyPrepaid(true)
                    .paymentMethod(PaymentMethod.CASH)
                    .tariffId(1L)
                    .services(List.of(
                        OrderServiceDto.builder()
                            .code(ShipmentOption.INSURANCE)
                            .cost(BigDecimal.ONE)
                            .customerPay(true)
                            .build()
                    ))
                    .build()
            )
            .setRecipient(
                RecipientDto.builder()
                    .address(
                        AddressDto.builder()
                            .country("Россия")
                            .region("Новосибирская область")
                            .locality("Новосибирск")
                            .street("Николаева")
                            .house("18")
                            .geoId(65)
                            .build()
                    )
                    .firstName("Иван")
                    .lastName("Иванов")
                    .build()
            );
        return orderDto;
    }

    @Nonnull
    private String buildErrorMessage(Map<String, String> errors) {
        return EntryStream.of(errors)
            .mapKeyValue((field, message) -> "FieldError(propertyPath=" + field + ", message=" + message + ")")
            .collect(Collectors.joining(", ", "[", "]"));
    }

    @Nonnull
    private static Stream<Arguments> routeValidatingArguments() {
        return Stream.<Pair<Map<String, String>, UnaryOperator<CombinatorRoute.DeliveryRoute>>>of(
            Pair.of(
                Map.of("route.route", "must not be null"),
                r -> null
            ),
            Pair.of(
                Map.of("route.route.points", "must not be empty"),
                r -> r.setPoints(null)
            ),
            Pair.of(
                Map.of("route.route.points[1]", "must not be null"),
                r -> r.setPoints(Arrays.asList(
                    new CombinatorRoute.Point().setIds(new CombinatorRoute.PointIds()),
                    null
                ))
            ),
            Pair.of(
                Map.of("route.route.paths", "must not be empty"),
                r -> r.setPaths(null)
            ),
            Pair.of(
                ImmutableSortedMap.of(
                    "route.route.paths[1]", "must not be null",
                    "route.route.paths[2]", "must not be null"
                ),
                r -> r.setPaths(Arrays.asList(new CombinatorRoute.Path(), null, null))
            ),
            Pair.of(
                Map.of("route.route.paths", "Point indexes in paths must be in bounds from 0 included to 3 included"),
                OrderWithRouteValidationTest::pointIndexesOutOfBounds
            ),
            Pair.of(
                Map.of("route.route.paths", "Not all points used in route"),
                OrderWithRouteValidationTest::extraPointsInRoute
            ),
            Pair.of(
                Map.of("route.route.paths", "Route has loop"),
                OrderWithRouteValidationTest::routeWithLoop
            ),
            Pair.of(
                Map.of("route.route.paths", "Route has branching"),
                OrderWithRouteValidationTest::routeWithBranching
            ),
            Pair.of(
                Map.of(
                    "route.route.points",
                    "Last point must be of segment type [HANDING, PICKUP, GO_PLATFORM], but was LINEHAUL"
                ),
                OrderWithRouteValidationTest::invalidLastPointRoute
            ),
            Pair.of(
                Map.of("route.route.points[1].ids", "must not be null"),
                OrderWithRouteValidationTest::nullPointIds
            ),
            Pair.of(
                Map.of(
                    "route.route.points",
                    "Invalid point connections: [(WAREHOUSE,LINEHAUL), (LINEHAUL,MOVEMENT), (MOVEMENT,PICKUP)]"
                ),
                OrderWithRouteValidationTest::invalidPointConnections
            ),
            Pair.of(
                Map.of("route.route.points", "First point must be of segment type WAREHOUSE"),
                OrderWithRouteValidationTest::unsupportedFirstPoint
            )
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @Nonnull
    private static Stream<Arguments> platformClientIdValidatingArgument() {
        return Stream.of(
            Arguments.of(
                "Проверка заполненности platformClientId",
                routeOrderRequestDto().setPlatformClientId(null),
                "Following validation errors occurred:\nField: 'platformClientId', message: 'must not be null'"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> deliveryTypeValidatingArgument() {
        return Stream.of(
            Arguments.of(
                "Проверка заполненности deliveryType",
                routeOrderRequestDto().setDeliveryType(null),
                "Following validation errors occurred:\nField: 'deliveryType', message: 'must not be null'"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> deliveryIntervalValidatingArguments() {
        return Stream.of(
            Arguments.of(
                "Проверка заполненности deliveryInterval",
                routeOrderRequestDto().setDeliveryInterval(null),
                "Following validation errors occurred:\nField: 'deliveryInterval', message: 'must not be null'"
            ),
            Arguments.of(
                "Проверка заполненности внутренних полей deliveryInterval",
                routeOrderRequestDto().setDeliveryInterval(DeliveryIntervalDto.builder().build()),
                "Following validation errors occurred:\n" +
                    "Field: 'deliveryInterval.deliveryDateMax', message: 'must not be null'\n" +
                    "Field: 'deliveryInterval.deliveryDateMin', message: 'must not be null'"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> senderIdValidatingArguments() {
        return Stream.of(
            Arguments.of(
                "Проверка заполненности senderId",
                routeOrderRequestDto().setSenderId(null),
                "Following validation errors occurred:\nField: 'senderId', message: 'must not be null'"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> recipientValidatingArguments() {
        return Stream.of(
            Arguments.of(
                "Проверка заполненности recipient",
                routeOrderRequestDto().setRecipient(null),
                "Following validation errors occurred:\nField: 'recipient', message: 'must not be null'"
            ),
            Arguments.of(
                "Проверка заполненности внутренних полей recipient",
                routeOrderRequestDto().setRecipient(RecipientDto.builder().build()),
                "Following validation errors occurred:\n" +
                    "Field: 'recipient.firstName', message: 'must not be blank'\n" +
                    "Field: 'recipient.lastName', message: 'must not be blank'"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> costValidatingArguments() {
        return Stream.of(
            Arguments.of(
                "Проверка заполненности cost",
                routeOrderRequestDto().setCost(null),
                "Following validation errors occurred:\nField: 'cost', message: 'must not be null'"
            ),
            Arguments.of(
                "Проверка заполненности внутренних полей cost",
                routeOrderRequestDto().setCost(CostDto.builder().build()),
                "Following validation errors occurred:\n" +
                    "Field: 'cost.assessedValue', message: 'must not be null'\n" +
                    "Field: 'cost.cashServicePercent', message: 'must not be null'\n" +
                    "Field: 'cost.delivery', message: 'must not be null'\n" +
                    "Field: 'cost.deliveryForCustomer', message: 'must not be null'\n" +
                    "Field: 'cost.isFullyPrepaid', message: 'must not be null'\n" +
                    "Field: 'cost.paymentMethod', message: 'must not be null'"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> itemsValidatingArguments() {
        return Stream.of(
            Arguments.of(
                "Проверка заполненности items",
                routeOrderRequestDto().setItems(null),
                "Following validation errors occurred:\nField: 'items', message: 'must not be empty'"
            ),
            Arguments.of(
                "Проверка заполненности внутренних полей items",
                routeOrderRequestDto().setItems(List.of(ItemDto.builder().article("").build())),
                "Following validation errors occurred:\n" +
                    "Field: 'items[0].article', message: 'field value must be not blank OR null'\n" +
                    "Field: 'items[0].count', message: 'must not be null'\n" +
                    "Field: 'items[0].name', message: 'must not be blank'\n" +
                    "Field: 'items[0].price', message: 'must not be null'\n" +
                    "Field: 'items[0].vendorId', message: 'must not be null'"
            ),
            Arguments.of(
                "Проверка заполненности внутренних полей items.price",
                routeOrderRequestDto().setItems(List.of(
                    ItemDto.builder()
                        .name("Товар")
                        .vendorId(1L)
                        .article("Артикул")
                        .count(10)
                        .price(MonetaryDto.builder().build())
                        .build()
                )),
                "Following validation errors occurred:\n" +
                    "Field: 'items[0].price.currency', message: 'must not be blank'\n" +
                    "Field: 'items[0].price.exchangeRate', message: 'must not be null'\n" +
                    "Field: 'items[0].price.value', message: 'must not be null'"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> unitsValidatingArguments() {
        return Stream.of(
            Arguments.of(
                "Проверка заполненности внутренних полей units",
                routeOrderRequestDto().setUnits(List.of(StorageUnitDto.builder().build())),
                "Following validation errors occurred:\n" +
                    "Field: 'units[0].type', message: 'must not be null'"
            ),
            Arguments.of(
                "Проверка заполненности externalId у всех юнитов для синего заказа",
                routeOrderRequestDto()
                    .setPlatformClientId(1L)
                    .setUnits(List.of(
                        StorageUnitDto.builder()
                            .type(StorageUnitType.ROOT)
                            .dimensions(dimensions())
                            .build(),
                        StorageUnitDto.builder()
                            .type(StorageUnitType.PLACE)
                            .dimensions(dimensions())
                            .build()
                    )),
                "[FieldError(propertyPath=units[0].externalId, message=must not be null), " +
                    "FieldError(propertyPath=units[1].externalId, message=must not be null)]"
            ),
            Arguments.of(
                "Проверка заполненности externalId у корневого юнита DAAS заказа",
                routeOrderRequestDto().setUnits(List.of(
                    StorageUnitDto.builder()
                        .type(StorageUnitType.ROOT)
                        .dimensions(dimensions())
                        .build(),
                    StorageUnitDto.builder()
                        .type(StorageUnitType.PLACE)
                        .dimensions(dimensions())
                        .build()
                )),
                "[FieldError(propertyPath=units, message=root units must have externalId)]"
            ),
            Arguments.of(
                "Проверка заполненности внутренних полей units.dimensions",
                routeOrderRequestDto().setUnits(List.of(
                    StorageUnitDto.builder()
                        .externalId("1")
                        .type(StorageUnitType.ROOT)
                        .dimensions(KorobyteDto.builder().build())
                        .build()
                )),
                "Following validation errors occurred:\n" +
                    "Field: 'units[0].dimensions.height', message: 'must not be null'\n" +
                    "Field: 'units[0].dimensions.length', message: 'must not be null'\n" +
                    "Field: 'units[0].dimensions.weightGross', message: 'must not be null'\n" +
                    "Field: 'units[0].dimensions.width', message: 'must not be null'"
            ),
            Arguments.of(
                "Проверка заполненности units.dimensions для корня",
                routeOrderRequestDto().setUnits(List.of(
                    StorageUnitDto.builder()
                        .externalId("1")
                        .type(StorageUnitType.ROOT)
                        .dimensions(null)
                        .build()
                )),
                "Following validation errors occurred:\n" +
                    "Field: 'units[0]', message: 'root units must have dimensions'"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> contactsValidatingArguments() {
        return Stream.of(
            Arguments.of(
                "Проверка заполненности внутренних полей contacts",
                routeOrderRequestDto().setContacts(List.of(OrderContactDto.builder().build())),
                "Following validation errors occurred:\n" +
                    "Field: 'contacts[0]', " +
                        "message: 'firstName and lastName or personalFullnameId fields must be not null'\n" +
                    "Field: 'contacts[0]', message: 'phone or personalPhoneId fields must be not null'\n" +
                    "Field: 'contacts[0].contactType', message: 'must not be null'"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> pickupPointIdValidatingArgument() {
        return Stream.of(
            Arguments.of(
                "Проверка заполненности pickupPointId при заказе в ПВЗ",
                routeOrderRequestDto().setPickupPointId(null),
                "Following validation errors occurred:\nObject: 'routeOrderRequestDto', " +
                    "message: 'when delivery type is pickup point, pickupPointId must be not null'"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> deliveryAddressValidatingArgument() {
        return Stream.of(
            Arguments.of(
                "Проверка заполненности адреса при доставке курьером или в почтовое отделение",
                routeOrderRequestDto()
                    .setDeliveryType(DeliveryType.POST)
                    .setRecipient(
                        RecipientDto.builder()
                            .address(AddressDto.builder().build())
                            .firstName("Иван")
                            .lastName("Иванов")
                            .build()
                    ),
                "Following validation errors occurred:\n" +
                    "Field: 'recipient.address.country', message: 'country must be not empty'\n" +
                    "Field: 'recipient.address.geoId', message: 'geoId must be not null'\n" +
                    "Field: 'recipient.address.house', message: 'house must be not empty'\n" +
                    "Field: 'recipient.address.locality', message: 'locality must be not empty'\n" +
                    "Field: 'recipient.address.region', message: 'region must be not empty'\n" +
                    "Field: 'recipient.personalAddressId', " +
                        "message: 'order recipient personal field address must be not null'"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> insuranceServiceValidatingArgument() {
        return Stream.of(
            Arguments.of(
                "Заказ с ненулевой объявленной ценностью должен содержать услугу Страховка",
                routeOrderRequestDto()
                    .setCost(
                        CostDto.builder()
                            .assessedValue(BigDecimal.ONE)
                            .cashServicePercent(BigDecimal.ONE)
                            .delivery(BigDecimal.ONE)
                            .deliveryForCustomer(BigDecimal.ONE)
                            .isFullyPrepaid(true)
                            .paymentMethod(PaymentMethod.CASH)
                            .tariffId(1L)
                            .build()
                    ),
                "Following validation errors occurred:\n" +
                    "Field: 'cost', message: 'order with positive assessedValue must have insurance service'"
            )
        );
    }

    @Nonnull
    private static CombinatorRoute.DeliveryRoute nullPointIds(CombinatorRoute.DeliveryRoute route) {
        return route
            .setPoints(List.of(
                pointOf(PointType.WAREHOUSE),
                pointOf(PointType.MOVEMENT).setIds(null),
                pointOf(PointType.LINEHAUL),
                pointOf(PointType.HANDING)
            ))
            .setPaths(List.of(pathOf(0, 1), pathOf(1, 2), pathOf(2, 3)));
    }

    @Nonnull
    private static CombinatorRoute.DeliveryRoute unsupportedFirstPoint(CombinatorRoute.DeliveryRoute route) {
        return route
            .setPoints(List.of(pointOf(PointType.MOVEMENT), pointOf(PointType.PICKUP)))
            .setPaths(List.of(pathOf(0, 1)));
    }

    @Nonnull
    private static CombinatorRoute.DeliveryRoute invalidPointConnections(CombinatorRoute.DeliveryRoute route) {
        return route
            .setPoints(List.of(
                pointOf(PointType.WAREHOUSE),
                pointOf(PointType.LINEHAUL),
                pointOf(PointType.MOVEMENT),
                pointOf(PointType.PICKUP)
            ))
            .setPaths(List.of(pathOf(0, 1), pathOf(1, 2), pathOf(2, 3)));
    }

    @Nonnull
    private static CombinatorRoute.DeliveryRoute invalidLastPointRoute(CombinatorRoute.DeliveryRoute route) {
        return route
            .setPoints(List.of(
                pointOf(PointType.LINEHAUL),
                pointOf(PointType.MOVEMENT),
                pointOf(PointType.WAREHOUSE),
                pointOf(PointType.WAREHOUSE)
            ))
            .setPaths(List.of(pathOf(3, 2), pathOf(2, 1), pathOf(1, 0)));
    }

    @Nonnull
    private static CombinatorRoute.DeliveryRoute routeWithBranching(CombinatorRoute.DeliveryRoute route) {
        return route
            .setPoints(List.of(point(), point(), point(), point()))
            .setPaths(List.of(pathOf(0, 1), pathOf(1, 2), pathOf(1, 3)));
    }

    @Nonnull
    private static CombinatorRoute.DeliveryRoute routeWithLoop(CombinatorRoute.DeliveryRoute route) {
        return route
            .setPoints(List.of(point(), point(), point()))
            .setPaths(List.of(pathOf(0, 1), pathOf(1, 2), pathOf(2, 0)));
    }

    @Nonnull
    private static CombinatorRoute.DeliveryRoute extraPointsInRoute(CombinatorRoute.DeliveryRoute route) {
        return route
            .setPoints(List.of(point(), point(), point(), point(), point()))
            .setPaths(List.of(pathOf(1, 2), pathOf(2, 3)));
    }

    @Nonnull
    private static CombinatorRoute.DeliveryRoute pointIndexesOutOfBounds(CombinatorRoute.DeliveryRoute route) {
        return route
            .setPoints(List.of(point(), point(), point(), point()))
            .setPaths(List.of(pathOf(-1, 0), pathOf(0, 1), pathOf(1, 2)));
    }

    @Nonnull
    private static CombinatorRoute.Point point() {
        return new CombinatorRoute.Point().setIds(new CombinatorRoute.PointIds());
    }

    @Nonnull
    private static CombinatorRoute.Point pointOf(PointType pointType) {
        return point().setSegmentType(pointType);
    }

    @Nonnull
    private static CombinatorRoute.Path pathOf(int from, int to) {
        return new CombinatorRoute.Path().setPointFrom(from).setPointTo(to);
    }

    @Nonnull
    private static KorobyteDto dimensions() {
        return KorobyteDto.builder()
            .weightGross(BigDecimal.ONE)
            .length(1)
            .width(2)
            .height(3)
            .build();
    }

    @Nonnull
    private ResultActions doCreateOrder(RouteOrderRequestDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.POST, "/orders/with-route", request));
    }
}
