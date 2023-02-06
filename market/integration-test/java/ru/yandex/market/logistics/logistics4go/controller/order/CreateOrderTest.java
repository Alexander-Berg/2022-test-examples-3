package ru.yandex.market.logistics.logistics4go.controller.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;

import yandex.market.combinator.v0.CombinatorOuterClass;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.logistics4go.client.model.ApiError;
import ru.yandex.market.logistics.logistics4go.client.model.CreateOrderRequest;
import ru.yandex.market.logistics.logistics4go.client.model.CreateOrderResponse;
import ru.yandex.market.logistics.logistics4go.client.model.Dimensions;
import ru.yandex.market.logistics.logistics4go.client.model.ErrorType;
import ru.yandex.market.logistics.logistics4go.client.model.Item;
import ru.yandex.market.logistics.logistics4go.client.model.NotFoundError;
import ru.yandex.market.logistics.logistics4go.client.model.ResourceType;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationError;
import ru.yandex.market.logistics.logistics4go.client.model.ValidationViolation;
import ru.yandex.market.logistics.logistics4go.utils.LomFactory;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.OrderItemBoxDto;
import ru.yandex.market.logistics.lom.model.dto.RouteOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.personal.client.model.PersonalMultiTypeStoreResponse;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.logistics4go.utils.CombinatorFactory.combinatorDeliveryRouteBuilder;
import static ru.yandex.market.logistics.logistics4go.utils.CombinatorFactory.combinatorRequestBuilder;
import static ru.yandex.market.logistics.logistics4go.utils.CombinatorFactory.combinatorRouteBuilder;
import static ru.yandex.market.logistics.logistics4go.utils.CombinatorFactory.deliveryPackageBuilder;
import static ru.yandex.market.logistics.logistics4go.utils.LomFactory.lomRequest;
import static ru.yandex.market.logistics.logistics4go.utils.LomFactory.unit;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.baseCreateOrderRequest;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.c2cModifier;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.item;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.modifier;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.place;
import static ru.yandex.market.logistics.logistics4go.utils.PersonalDataFactory.createC2CStoreRequest;
import static ru.yandex.market.logistics.logistics4go.utils.PersonalDataFactory.createC2CStoreResponse;
import static ru.yandex.market.logistics.logistics4go.utils.PersonalDataFactory.createRecipientStoreResponse;
import static ru.yandex.market.logistics.logistics4go.utils.PersonalDataFactory.createStoreRequest;
import static ru.yandex.market.logistics.logistics4go.utils.PersonalDataFactory.createStoreResponse;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.assertJson;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание заказа")
@DatabaseSetup("/controller/order/create/before/sender.xml")
class CreateOrderTest extends AbstractOrderTest {

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2022-02-22T15:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Успешное создание заказа")
    @ExpectedDatabase(
        value = "/controller/order/create/after/order_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderSuccess(
        @SuppressWarnings("unused") String displayName,
        boolean isOnlyRequired,
        boolean isCourier,
        UnaryOperator<CreateOrderRequest> requestModifier,
        String routeSource
    ) {
        createOrder(isOnlyRequired, isCourier, requestModifier);
        verifyLomClient(isOnlyRequired, isCourier, routeSource);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Успешное создание заказа с зашифрованными персональными данными")
    @DatabaseSetup("/controller/order/create/before/use_encrypted_personal_data.xml")
    void createOrderSuccessEncryptedPersonalData(
        @SuppressWarnings("unused") String displayName,
        boolean isOnlyRequired,
        boolean isCourier,
        String requestSource,
        String routeSource
    ) {
        doReturn(createStoreResponse(isOnlyRequired, isCourier))
            .when(personalDataStoreApi).v1MultiTypesStorePost(createStoreRequest(isOnlyRequired, isCourier));
        createOrder(isOnlyRequired, isCourier, UnaryOperator.identity());
        verifyLomClient(requestSource, routeSource);
        verify(personalDataStoreApi).v1MultiTypesStorePost(createStoreRequest(isOnlyRequired, isCourier));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Успешное создание заказа только с зашифрованными персональными данными")
    @DatabaseSetup("/controller/order/create/before/use_only_encrypted_personal_data.xml")
    void createOrderSuccessOnlyEncryptedPersonalData(
        @SuppressWarnings("unused") String displayName,
        boolean isOnlyRequired,
        boolean isCourier,
        String requestSource,
        String routeSource
    ) {
        doReturn(createStoreResponse(isOnlyRequired, isCourier))
            .when(personalDataStoreApi).v1MultiTypesStorePost(createStoreRequest(isOnlyRequired, isCourier));
        createOrder(isOnlyRequired, isCourier, UnaryOperator.identity());
        verifyLomClient(requestSource, routeSource);
        verify(personalDataStoreApi).v1MultiTypesStorePost(createStoreRequest(isOnlyRequired, isCourier));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Ошибка, не удалось сохранить персональные данные")
    @DatabaseSetup(value = "/controller/order/create/before/use_only_encrypted_personal_data.xml")
    void createOrderErrorStorePersonalData(
        @SuppressWarnings("unused") String displayName,
        PersonalMultiTypeStoreResponse response,
        String errorMessage
    ) {
        doReturn(combinatorDeliveryRouteBuilder(true).build())
            .when(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());

        doReturn(response).when(personalDataStoreApi).v1MultiTypesStorePost(createStoreRequest(false, true));

        ApiError error = apiClient.orders().createOrder()
            .body(baseCreateOrderRequest(false, true))
            .execute(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR)))
            .as(ApiError.class);

        softly.assertThat(error).isEqualTo(
            new ApiError()
                .code(ErrorType.PERSONAL_MARKET_ERROR)
                .message(errorMessage)
        );

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());
        verify(personalDataStoreApi).v1MultiTypesStorePost(createStoreRequest(false, true));
    }

    @Test
    @DisplayName("Успешное создание заказа с отсутствующими ВГХ товара")
    @ExpectedDatabase(
        value = "/controller/order/create/after/order_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderWithNoItemsDimensionsSuccess() {
        boolean isOnlyRequired = false;
        boolean isCourier = true;

        doReturn(combinatorDeliveryRouteBuilder(isCourier).build())
            .when(combinatorGrpcClient).getDeliveryRouteFromPoint(
                combinatorRequestBuilder(isOnlyRequired, isCourier).build()
            );

        doReturn(new OrderDto().setId(1L).setExternalId("externalId").setBarcode("LOtesting-1"))
            .when(lomClient)
            .createOrder(any(RouteOrderRequestDto.class), eq(true));

        CreateOrderRequest l4gOrderRequest = baseCreateOrderRequest(isOnlyRequired, isCourier);
        l4gOrderRequest.getItems().get(0).setDimensions(null);
        CreateOrderResponse order = apiClient.orders().createOrder()
            .body(l4gOrderRequest)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(order.getId()).isEqualTo(1L);

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(
            combinatorRequestBuilder(isOnlyRequired, isCourier).build()
        );
        RouteOrderRequestDto lomOrderRequest = lomRequest(isOnlyRequired, isCourier);
        ItemDto item = lomOrderRequest.getItems().get(0);
        OrderItemBoxDto itemBoxDto = item.getBoxes().get(0).toBuilder().dimensions(null).build();
        lomOrderRequest.setItems(List.of(item.toBuilder().dimensions(null).boxes(List.of(itemBoxDto)).build()));
        verifyLomClient(lomOrderRequest);
    }

    @Test
    @DisplayName("Успешное создание заказа с несколькими товарами без грузомест")
    @ExpectedDatabase(
        value = "/controller/order/create/after/order_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderWithMultipleItemsAndNoPlacesSuccess() {
        boolean isOnlyRequired = false;
        boolean isCourier = true;
        int length = 50;
        int width = 40;
        int height1 = 20;
        int height2 = 10;
        double weightKg1 = 1;
        double weightKg2 = 0.234;
        int weightG1 = 1000;
        int weightG2 = 234;

        CombinatorOuterClass.DeliveryRouteFromPointRequest combinatorRequest =
            combinatorRequestBuilder(isOnlyRequired, isCourier)
                .clearItems()
                .addAllItems(List.of(
                    deliveryPackageBuilder(isOnlyRequired)
                        .clearCargoTypes()
                        .clearDimensions()
                        .addAllDimensions(List.of(length, width, height1))
                        .setWeight(weightG1)
                        .build(),
                    deliveryPackageBuilder(isOnlyRequired)
                        .clearCargoTypes()
                        .clearDimensions()
                        .addAllDimensions(List.of(length, width, height2))
                        .setWeight(weightG2)
                        .setPrice(0)
                        .build()
                ))
                .build();
        doReturn(combinatorDeliveryRouteBuilder(isCourier).build())
            .when(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequest);

        doReturn(new OrderDto().setId(1L).setExternalId("externalId").setBarcode("LOtesting-1"))
            .when(lomClient)
            .createOrder(any(RouteOrderRequestDto.class), eq(true));

        CreateOrderRequest l4gOrderRequest = baseCreateOrderRequest(isOnlyRequired, isCourier);
        Item item1 = item(isOnlyRequired)
            .externalId("ext-1")
            .dimensions(
                new Dimensions()
                    .height(height1)
                    .length(length)
                    .width(width)
                    .weight(BigDecimal.valueOf(weightKg1))
            )
            .cargoTypes(null)
            .placesExternalIds(null);
        Item item2 = item(isOnlyRequired)
            .externalId("ext-2")
            .dimensions(
                new Dimensions()
                    .height(height2)
                    .length(length)
                    .width(width)
                    .weight(BigDecimal.valueOf(weightKg2))
            ).cargoTypes(null)
            .price(BigDecimal.ZERO)
            .placesExternalIds(null);

        l4gOrderRequest.items(List.of(item1, item2)).places(null);
        CreateOrderResponse order = apiClient.orders().createOrder()
            .body(l4gOrderRequest)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(order.getId()).isEqualTo(1L);

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequest);
        RouteOrderRequestDto lomOrderRequest = lomRequest(isOnlyRequired, isCourier);

        KorobyteDto dimensions1 = LomFactory.dimensions(length, width, height1, weightKg1);
        KorobyteDto dimensions2 = LomFactory.dimensions(length, width, height2, weightKg2);
        OrderItemBoxDto itemBox1 = OrderItemBoxDto.builder().dimensions(dimensions1).build();
        OrderItemBoxDto itemBox2 = OrderItemBoxDto.builder().dimensions(dimensions2).build();
        ItemDto lomItem1 = LomFactory.item(isOnlyRequired).toBuilder()
            .article("ext-1")
            .dimensions(dimensions1)
            .boxes(List.of(itemBox1))
            .cargoTypes(null)
            .build();
        ItemDto lomItem2 = LomFactory.item(isOnlyRequired).toBuilder()
            .article("ext-2")
            .dimensions(dimensions2)
            .boxes(List.of(itemBox2))
            .price(LomFactory.price(0))
            .cargoTypes(null)
            .build();

        lomOrderRequest
            .setItems(List.of(lomItem1, lomItem2))
            .setUnits(List.of(LomFactory.rootUnit().toBuilder()
                .dimensions(LomFactory.dimensions(length, width, height1 + height2, weightKg1 + weightKg2))
                .build()
            ));
        verifyLomClient(lomOrderRequest);
    }

    @Test
    @DisplayName("Не найден маршрут в комбинаторе")
    void combinatorRouteNotFond() {
        doThrow(new RuntimeException("combinator error"))
            .when(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());

        ValidationError error = apiClient.orders().createOrder()
            .body(baseCreateOrderRequest(false, true))
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error).isEqualTo(
            new ValidationError()
                .code(ErrorType.FAILED_TO_FIND_ROUTE)
                .message("java.lang.RuntimeException: combinator error")
        );
        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());
    }

    @Test
    @DisplayName("Ошибка парсинга маршрута из комбинатора")
    void combinatorRouteParseError() {
        ApiError error = apiClient.orders().createOrder()
            .body(baseCreateOrderRequest(false, true))
            .execute(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR)))
            .as(ApiError.class);

        softly.assertThat(error.getCode()).isEqualTo(ErrorType.FAILED_TO_PARSE_ROUTE);
        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());
    }

    @Test
    @DisplayName("Не найден сендер")
    void senderNotFound() {
        doReturn(combinatorDeliveryRouteBuilder(true).build())
            .when(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());

        NotFoundError error = apiClient.orders().createOrder()
            .body(baseCreateOrderRequest(false, true).senderId(54321L))
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error).isEqualTo(
            new NotFoundError()
                .code(ErrorType.RESOURCE_NOT_FOUND)
                .addIdsItem(54321L)
                .resourceType(ResourceType.SENDER)
                .message("Failed to find SENDER with ids [54321]")
        );

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());
    }

    @Test
    @DisplayName("Не найден склад у сендера")
    void senderWarehouseNotFound() {
        doReturn(combinatorDeliveryRouteBuilder(true).build())
            .when(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());

        NotFoundError error = apiClient.orders().createOrder()
            .body(
                modifier(
                    r -> r.getDeliveryOption().getInbound(),
                    r -> r.fromLogisticsPointId(100001L),
                    CreateOrderRequest.class
                )
                    .apply(baseCreateOrderRequest(false, true))
            )
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(NotFoundError.class);

        softly.assertThat(error).isEqualTo(
            new NotFoundError()
                .code(ErrorType.RESOURCE_NOT_FOUND)
                .addIdsItem(100001L)
                .resourceType(ResourceType.WAREHOUSE)
                .message("Failed to find WAREHOUSE with ids [100001]")
        );

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());
    }

    @Test
    @DisplayName("Цикл в маршруте комбинатора")
    void cycleInRoute() {
        doReturn(
            CombinatorOuterClass.DeliveryRoute.newBuilder().setRoute(
                combinatorRouteBuilder(true)
                    .addPaths(
                        CombinatorOuterClass.Route.Path.newBuilder()
                            .setPointFrom(2)
                            .setPointTo(1)
                            .build()
                    )
            ).build()
        )
            .when(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());

        ApiError error = apiClient.orders().createOrder()
            .body(baseCreateOrderRequest(false, true))
            .execute(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR)))
            .as(ApiError.class);

        softly.assertThat(error).isEqualTo(
            new ApiError()
                .code(ErrorType.FAILED_TO_PARSE_ROUTE)
                .message("Can't find first point index")
        );

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("routeValidation")
    @DisplayName("Ошибки при валидации маршрута")
    void invalidRouteStart(
        String caseName,
        CombinatorOuterClass.Route returnedRoute,
        String errorMessage
    ) {
        when(combinatorGrpcClient.getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build()))
            .thenReturn(combinatorDeliveryRouteBuilder(true).setRoute(returnedRoute).build());

        ApiError error = apiClient.orders().createOrder()
            .body(baseCreateOrderRequest(false, true))
            .execute(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR)))
            .as(ApiError.class);

        softly.assertThat(error.getCode()).isEqualTo(ErrorType.FAILED_TO_PARSE_ROUTE);
        softly.assertThat(error.getMessage()).contains(errorMessage);

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());
    }

    @Nonnull
    private static Stream<Arguments> routeValidation() {
        return Stream.of(
            Arguments.of(
                "Пустой список путей",
                combinatorRouteBuilder(true).clearPaths().build(),
                "Route from combinator is empty"
            ),
            Arguments.of(
                "Пустой список точек",
                combinatorRouteBuilder(true).clearPoints().build(),
                "Route from combinator is empty"
            ),
            Arguments.of(
                "Цикл в маршруте",
                combinatorRouteBuilder(true)
                    .addPaths(
                        CombinatorOuterClass.Route.Path.newBuilder()
                            .setPointFrom(2)
                            .setPointTo(1)
                            .build()
                    )
                    .build(),
                "Can't find first point index"
            )
        );
    }

    @Test
    @DisplayName("Неизвестный карго-тип товара")
    void unknownCargoType() {
        var combinatorRequest = combinatorRequestBuilder(false, true)
            .clearItems()
            .addItems(deliveryPackageBuilder(true).addCargoTypes(1234).build())
            .build();

        doReturn(combinatorDeliveryRouteBuilder(true).build())
            .when(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequest);

        doReturn(new OrderDto().setId(1L).setExternalId("externalId").setBarcode("LOtesting-1"))
            .when(lomClient)
            .createOrder(any(RouteOrderRequestDto.class), eq(true));

        ValidationError actualValidationError = apiClient.orders().createOrder()
            .body(
                modifier(r -> r.getItems().get(0), r -> r.cargoTypes(List.of(1234)), CreateOrderRequest.class)
                    .apply(baseCreateOrderRequest(false, true))
            )
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(actualValidationError).isEqualTo(
            new ValidationError()
                .code(ErrorType.VALIDATION_ERROR)
                .errors(List.of(
                    new ValidationViolation()
                        .field("items[0].cargoTypes")
                        .message("cargo type 1234 not found in LOM")
                ))
        );

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequest);
    }

    @Test
    @DisplayName("Заказ с несколькими грузоместами")
    @ExpectedDatabase(
        value = "/controller/order/create/after/order_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void multiplePlaces() {
        var combinatorRequest = combinatorRequestBuilder(false, true)
            .addAllItems(List.of(
                deliveryPackageBuilder(true).setPrice(0).build(),
                deliveryPackageBuilder(true).setPrice(0).build()
            ))
            .build();

        doReturn(combinatorDeliveryRouteBuilder(true).build())
            .when(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequest);

        doReturn(new OrderDto().setId(1L).setExternalId("externalId").setBarcode("LOtesting-1"))
            .when(lomClient)
            .createOrder(any(RouteOrderRequestDto.class), eq(true));

        CreateOrderResponse order = apiClient.orders().createOrder()
            .body(baseCreateOrderRequest(false, true).places(List.of(place(), place(), place())))
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(order.getId()).isEqualTo(1L);

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequest);

        RouteOrderRequestDto request = lomRequest(false, true);
        request.setUnits(List.of(
            unit(),
            unit(),
            unit(),
            StorageUnitDto.builder()
                .externalId("l4g-generated-0")
                .type(StorageUnitType.ROOT)
                .dimensions(
                    KorobyteDto.builder()
                        .height(90)
                        .length(50)
                        .width(40)
                        .weightGross(BigDecimal.valueOf(3.702).setScale(3, RoundingMode.HALF_UP))
                        .build()
                )
                .build()
        ));

        verifyLomClient(request, "controller/order/create/route/lom_create_order_route_courier.json");
    }

    @Test
    @DisplayName("Заказ с таким externalId уже есть в ломе")
    void duplicateExternalId() {
        doReturn(combinatorDeliveryRouteBuilder(true).build())
            .when(combinatorGrpcClient).getDeliveryRouteFromPoint(
                combinatorRequestBuilder(false, true).build()
            );

        doThrow(new HttpTemplateException(409, "{\"message\": \"duplicate external id\"}"))
            .when(lomClient).createOrder(any(RouteOrderRequestDto.class), eq(true));

        ApiError apiError = apiClient.orders().createOrder()
            .body(baseCreateOrderRequest(false, true))
            .execute(validatedWith(shouldBeCode(SC_CONFLICT)))
            .as(ApiError.class);

        softly.assertThat(apiError).isEqualTo(
            new ApiError()
                .code(ErrorType.RESOURCE_ALREADY_EXISTS)
                .message("duplicate external id")
        );

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(combinatorRequestBuilder(false, true).build());
        verifyLomClient(false, true, "controller/order/create/route/lom_create_order_route_courier.json");
    }

    @Test
    @DisplayName("Создание C2C заказа")
    @ExpectedDatabase(
        value = "/controller/order/create/after/order_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void c2cOrder() {
        createOrder(false, false, c2cModifier());
        verifyLomClient(
            "controller/order/create/lom_request/c2c.json",
            "controller/order/create/route/lom_create_order_route_pickup.json"
        );
    }

    @Test
    @DisplayName("Создание C2C заказа с зашифрованными персональными данными")
    @DatabaseSetup("/controller/order/create/before/use_encrypted_personal_data.xml")
    void c2cOrderUseEncryptedPersonalData() {
        doReturn(createC2CStoreResponse()).when(personalDataStoreApi).v1MultiTypesStorePost(createC2CStoreRequest());
        createOrder(false, false, c2cModifier());
        verifyLomClient(
            "controller/order/create/lom_request/c2c_use_encrypted_data.json",
            "controller/order/create/route/lom_create_order_route_pickup.json"
        );
        verify(personalDataStoreApi).v1MultiTypesStorePost(createC2CStoreRequest());
    }

    @Test
    @DisplayName("Создание C2C заказа только с зашифрованными персональными данными")
    @DatabaseSetup("/controller/order/create/before/use_only_encrypted_personal_data.xml")
    void c2cOrderUseOnlyEncryptedPersonalData() {
        doReturn(createC2CStoreResponse()).when(personalDataStoreApi).v1MultiTypesStorePost(createC2CStoreRequest());
        createOrder(false, false, c2cModifier());
        verifyLomClient(
            "controller/order/create/lom_request/c2c_use_only_encrypted_data.json",
            "controller/order/create/route/lom_create_order_route_pickup.json"
        );
        verify(personalDataStoreApi).v1MultiTypesStorePost(createC2CStoreRequest());
    }

    @Test
    @DisplayName("Ошибка парсинга запроса")
    @SneakyThrows
    void requestParseError() {
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/create/request/parse_request_error.json"))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/order/create/response/parse_request_error.json"));
    }

    @Nonnull
    private static Stream<Arguments> createOrderSuccess() {
        return Stream.of(
            Arguments.of(
                "Доставка курьером, заполнены все поля",
                false,
                true,
                UnaryOperator.identity(),
                "controller/order/create/route/lom_create_order_route_courier.json"
            ),
            Arguments.of(
                "Доставка в ПВЗ, заполнены все поля",
                false,
                false,
                UnaryOperator.identity(),
                "controller/order/create/route/lom_create_order_route_pickup.json"
            ),
            Arguments.of(
                "Доставка курьером, заполнены только необходимые поля",
                true,
                true,
                UnaryOperator.identity(),
                "controller/order/create/route/lom_create_order_route_courier.json"
            ),
            Arguments.of(
                "Доставка в ПВЗ, заполнены только необходимые поля",
                true,
                false,
                UnaryOperator.identity(),
                "controller/order/create/route/lom_create_order_route_pickup.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> createOrderSuccessEncryptedPersonalData() {
        return Stream.of(
            Arguments.of(
                "Доставка курьером, заполнены все поля",
                false,
                true,
                "controller/order/create/lom_request/courier_use_encrypted_data.json",
                "controller/order/create/route/lom_create_order_route_courier.json"
            ),
            Arguments.of(
                "Доставка в ПВЗ, заполнены все поля",
                false,
                false,
                "controller/order/create/lom_request/pickup_use_encrypted_data.json",
                "controller/order/create/route/lom_create_order_route_pickup.json"
            ),
            Arguments.of(
                "Доставка курьером, заполнены только необходимые поля",
                true,
                true,
                "controller/order/create/lom_request/courier_required_only_use_encrypted_data.json",
                "controller/order/create/route/lom_create_order_route_courier.json"
            ),
            Arguments.of(
                "Доставка в ПВЗ, заполнены только необходимые поля",
                true,
                false,
                "controller/order/create/lom_request/pickup_required_only_use_encrypted_data.json",
                "controller/order/create/route/lom_create_order_route_pickup.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> createOrderSuccessOnlyEncryptedPersonalData() {
        return Stream.of(
            Arguments.of(
                "Доставка курьером, заполнены все поля",
                false,
                true,
                "controller/order/create/lom_request/courier_use_only_encrypted_data.json",
                "controller/order/create/route/lom_create_order_route_courier.json"
            ),
            Arguments.of(
                "Доставка в ПВЗ, заполнены все поля",
                false,
                false,
                "controller/order/create/lom_request/pickup_use_only_encrypted_data.json",
                "controller/order/create/route/lom_create_order_route_pickup.json"
            ),
            Arguments.of(
                "Доставка курьером, заполнены только необходимые поля",
                true,
                true,
                "controller/order/create/lom_request/courier_required_only_use_only_encrypted_data.json",
                "controller/order/create/route/lom_create_order_route_courier.json"
            ),
            Arguments.of(
                "Доставка в ПВЗ, заполнены только необходимые поля",
                true,
                false,
                "controller/order/create/lom_request/pickup_required_only_use_only_encrypted_data.json",
                "controller/order/create/route/lom_create_order_route_pickup.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> createOrderErrorStorePersonalData() {
        return Stream.of(
            Arguments.of(
                "Пустой ответ из сервиса",
                null,
                "Empty personal data store response"
            ),
            Arguments.of(
                "items == null в ответе сервиса",
                new PersonalMultiTypeStoreResponse().items(null),
                "Empty personal data store response"
            ),
            Arguments.of(
                "Некоторые айтемы отсутствуют в ответе",
                createRecipientStoreResponse(false),
                "Some values were not found in the personal data store response"
            ),
            Arguments.of(
                "Ошибка в одном из айтемов",
                modifier(
                    r -> r.getItems().get(0),
                    r -> r.id(null).error("Field error"),
                    PersonalMultiTypeStoreResponse.class
                ).apply(createStoreResponse(false, true)),
                "Failed to store personal data: Field error"
            )
        );
    }

    private void createOrder(
        boolean isOnlyRequired,
        boolean isCourier,
        UnaryOperator<CreateOrderRequest> requestModifier
    ) {
        doReturn(combinatorDeliveryRouteBuilder(isCourier).build())
            .when(combinatorGrpcClient).getDeliveryRouteFromPoint(
                combinatorRequestBuilder(isOnlyRequired, isCourier).build()
            );

        doReturn(new OrderDto().setId(1L).setExternalId("externalId").setBarcode("LOtesting-1"))
            .when(lomClient).createOrder(any(RouteOrderRequestDto.class), eq(true));

        CreateOrderResponse order = apiClient.orders().createOrder()
            .body(requestModifier.apply(baseCreateOrderRequest(isOnlyRequired, isCourier)))
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(order.getId()).isEqualTo(1L);

        verify(combinatorGrpcClient).getDeliveryRouteFromPoint(
            combinatorRequestBuilder(isOnlyRequired, isCourier).build()
        );
    }

    private void verifyLomClient(RouteOrderRequestDto expectedRequest) {
        verifyLomClient(expectedRequest, "controller/order/create/route/lom_create_order_route_courier.json");
    }

    private void verifyLomClient(boolean isOnlyRequired, boolean isCourier, String expectedRouteSource) {
        verifyLomClient(lomRequest(isOnlyRequired, isCourier), expectedRouteSource);
    }

    @SneakyThrows
    private void verifyLomClient(RouteOrderRequestDto expectedRequest, String expectedRouteSource) {
        ArgumentCaptor<RouteOrderRequestDto> captor = ArgumentCaptor.forClass(RouteOrderRequestDto.class);
        verify(lomClient).createOrder(captor.capture(), eq(true));
        RouteOrderRequestDto actualRequest = captor.getValue();

        assertJson(
            expectedRouteSource,
            objectMapper.writeValueAsString(actualRequest.getRoute())
        );
        softly.assertThat(actualRequest)
            .usingRecursiveComparison()
            .ignoringFields("route")
            .isEqualTo(expectedRequest);
    }

    @SneakyThrows
    private void verifyLomClient(String expectedRequestSource, String expectedRouteSource) {
        ArgumentCaptor<RouteOrderRequestDto> captor = ArgumentCaptor.forClass(RouteOrderRequestDto.class);
        verify(lomClient).createOrder(captor.capture(), eq(true));
        RouteOrderRequestDto actualRequest = captor.getValue();

        assertJson(
            expectedRouteSource,
            objectMapper.writeValueAsString(actualRequest.getRoute())
        );
        assertJson(
            expectedRequestSource,
            objectMapper.writeValueAsString(actualRequest),
            JSONCompareMode.LENIENT,
            "route"
        );
    }
}
