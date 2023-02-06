package ru.yandex.market.logistics.logistics4shops.controller.orderbox;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import one.util.streamex.EntryStream;
import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.collections.Quadruple;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.logistics.logistics4shops.client.api.OrderBoxApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ApiError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderBox;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderBoxItem;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderBoxRequestDto;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderBoxesDto;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderBoxesRequestDto;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationViolation;
import ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DisplayName("Сохранение грузомест")
@DatabaseSetup("/controller/orderbox/put/before/put_order_boxes_before.xml")
@ParametersAreNonnullByDefault
class PutOrderBoxControllerTest extends AbstractOrderBoxControllerTest {
    private static final long PARCEL_ID = 101L;

    @Test
    @DisplayName("Обновить список коробок в заказе")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/before/put_order_boxes_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putOrderBoxes() throws Exception {
        long orderId = 100100L;
        long mbiPartnerId = 200100L;
        OrderBoxesRequestDto request = buildRequest();
        OrderBoxesDto expectedResponse = buildExpectedResponse(orderId, request);

        try (var ignored = mockCheckouter(orderId, mbiPartnerId, expectedResponse, false)) {
            OrderBoxesDto orderBoxesDto = apiOperation(
                String.valueOf(orderId),
                null,
                List.of(mbiPartnerId, 1L, 4L),
                request
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderBoxesDto).isEqualTo(expectedResponse);
        }
    }

    @Test
    @DisplayName("Обновить список коробок в заказе: включено сохранение в базу")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/after/put_order_boxes_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putOrderBoxesEnableDbSaving() throws Exception {
        setSaveBoxesThreshold(0);

        long orderId = 100100L;
        long mbiPartnerId = 200100L;
        OrderBoxesRequestDto request = buildRequest();
        OrderBoxesDto expectedResponse = buildExpectedResponse(orderId, request);

        try (var ignored = mockCheckouter(orderId, mbiPartnerId, expectedResponse, true)) {
            OrderBoxesDto orderBoxesDto = apiOperation(String.valueOf(orderId), mbiPartnerId, null, request)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderBoxesDto).isEqualTo(expectedResponse);
        }
    }

    @Test
    @DisplayName("Обновить список коробок в заказе: коробки не изменились")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/before/put_order_boxes_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putOrderBoxesNoChanges() {
        long orderId = 100100L;
        long mbiPartnerId = 200100L;
        OrderBoxesRequestDto request = buildRequestWithoutChanges();
        OrderBoxesDto expectedResponse = buildExpectedResponseWithoutChanges();

        mockCheckouterWithoutVerify(orderId, mbiPartnerId, expectedResponse, false, true);
        OrderBoxesDto orderBoxesDto = apiOperation(String.valueOf(orderId), mbiPartnerId, List.of(), request)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(orderBoxesDto).isEqualTo(expectedResponse);
        verify(checkouterAPI).getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
    }

    @Test
    @DisplayName("Обновить список коробок в заказе: включено сохранение в базу, коробки не изменились")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/before/put_order_boxes_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putOrderBoxesEnableDbSavingNoChanges() {
        setSaveBoxesThreshold(0);

        long orderId = 100100L;
        long mbiPartnerId = 200100L;
        OrderBoxesRequestDto request = buildRequestWithoutChanges();
        OrderBoxesDto expectedResponse = buildExpectedResponseWithoutChanges();

        mockCheckouterWithoutVerify(orderId, mbiPartnerId, expectedResponse, false, true);
        OrderBoxesDto orderBoxesDto = apiOperation(String.valueOf(orderId), mbiPartnerId, null, request)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(orderBoxesDto).isEqualTo(expectedResponse);
        verify(checkouterAPI).getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
    }

    @Test
    @DisplayName("Обновить список коробок в заказе, где коробок еще не было")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/before/put_order_boxes_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putOrderBoxesForOrderWithoutBoxesBefore() throws Exception {
        long orderId = 100101L;
        long mbiPartnerId = 200102L;
        OrderBoxesRequestDto request = buildRequest();
        OrderBoxesDto expectedResponse = buildExpectedResponse(orderId, request);

        try (var ignored = mockCheckouter(orderId, mbiPartnerId, expectedResponse, false)) {
            OrderBoxesDto orderBoxesDto = apiOperation(String.valueOf(orderId), null, List.of(mbiPartnerId), request)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderBoxesDto).isEqualTo(expectedResponse);
        }
    }

    @Test
    @DisplayName("Обновить список коробок в заказе, где коробок еще не было: включено сохранение в базу")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/after/put_order_boxes_no_boxes_before_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putOrderBoxesForOrderWithoutBoxesBeforeEnableDbSaving() throws Exception {
        setSaveBoxesThreshold(100100);

        long orderId = 100101L;
        long mbiPartnerId = 200102L;
        OrderBoxesRequestDto request = buildRequest();
        OrderBoxesDto expectedResponse = buildExpectedResponse(orderId, request);

        try (var ignored = mockCheckouter(orderId, mbiPartnerId, expectedResponse, true)) {
            OrderBoxesDto orderBoxesDto = apiOperation(String.valueOf(orderId), null, List.of(mbiPartnerId), request)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderBoxesDto).isEqualTo(expectedResponse);
        }
    }

    @Test
    @DisplayName("Обновить список коробок в заказе, которого нет в базе L4S, но есть в чекаутере: "
                 + "включено сохранение в базу")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/after/put_order_boxes_no_db_order_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putOrderBoxesNoDbOrder() throws Exception {
        setSaveBoxesThreshold(999998);

        long orderId = 999999L;
        long mbiPartnerId = 200100L;
        OrderBoxesRequestDto request = buildRequest();
        OrderBoxesDto expectedResponse = buildExpectedResponse(orderId, request);

        try (var ignored = mockCheckouter(orderId, mbiPartnerId, expectedResponse, true)) {
            OrderBoxesDto orderBoxesDto = apiOperation(String.valueOf(orderId), null, List.of(mbiPartnerId), request)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderBoxesDto).isEqualTo(expectedResponse);
        }
    }

    @Test
    @DisplayName("Обновить список коробок в заказе, где коробки были только в чекаутере, коробки не изменились")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/before/put_order_boxes_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putOrderBoxesCheckouterBoxesOnlyNoChanges() {
        long orderId = 100101L;
        long mbiPartnerId = 200102L;
        OrderBoxesRequestDto request = buildRequest();
        OrderBoxesDto expectedResponse = buildExpectedResponse(orderId, request);

        mockCheckouterWithoutVerify(orderId, mbiPartnerId, expectedResponse, false, true);
        OrderBoxesDto orderBoxesDto = apiOperation(String.valueOf(orderId), null, List.of(mbiPartnerId), request)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(orderBoxesDto).isEqualTo(expectedResponse);
        verify(checkouterAPI).getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
    }

    @Test
    @DisplayName("Обновить список коробок в заказе, где коробок еще не было: включено сохранение в базу, "
                 + "коробки не изменились")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/after/put_order_boxes_no_boxes_before_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putOrderBoxesForOrderWithoutBoxesBeforeEnableDbSavingNoChanges() throws Exception {
        setSaveBoxesThreshold(100100);

        long orderId = 100101L;
        long mbiPartnerId = 200102L;
        OrderBoxesRequestDto request = buildRequest();
        OrderBoxesDto expectedResponse = buildExpectedResponse(orderId, request);

        try (var ignored = mockCheckouter(orderId, mbiPartnerId, expectedResponse, true)) {
            OrderBoxesDto orderBoxesDto = apiOperation(String.valueOf(orderId), null, List.of(mbiPartnerId), request)
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(orderBoxesDto).isEqualTo(expectedResponse);
        }
    }

    @Test
    @DisplayName("Обновить список коробок в заказе, которого нет в базе L4S, но есть в чекаутере: "
                 + "включено сохранение в базу, в чекаутере сохранены те же коробки")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/after/put_order_boxes_no_db_order_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putOrderBoxesNoDbOrderNoChanges() {
        setSaveBoxesThreshold(999998);

        long orderId = 999999L;
        long mbiPartnerId = 200100L;
        OrderBoxesRequestDto request = buildRequest();
        OrderBoxesDto expectedResponse = buildExpectedResponse(orderId, request);

        mockCheckouterWithoutVerify(orderId, mbiPartnerId, expectedResponse, true, true);
        OrderBoxesDto orderBoxesDto = apiOperation(String.valueOf(orderId), null, List.of(mbiPartnerId), request)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(orderBoxesDto).isEqualTo(expectedResponse);
        verify(checkouterAPI).getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
        verify(checkouterAPI).putParcelBoxes(
            eq(orderId),
            eq(PARCEL_ID),
            anyList(),
            any(RequestClientInfo.class)
        );
    }

    @Test
    @DisplayName("Ошибка обновления коробок: заказа нет ни в БД, ни в чекаутере")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/before/put_order_boxes_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putOrderBoxesNoCheckouterOrder() {
        when(checkouterAPI.getOrder(any(RequestClientInfo.class), any(OrderRequest.class)))
            .thenThrow(new OrderNotFoundException(999999L));

        long orderId = 999999L;
        long mbiPartnerId = 200100L;
        OrderBoxesRequestDto request = buildRequest();

        ApiError error = apiOperation(String.valueOf(orderId), null, List.of(mbiPartnerId), request)
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(error.getMessage()).isEqualTo("Failed to find [ORDER] with id [999999]");

        verify(checkouterAPI).getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
    }

    @Test
    @DisplayName("Ошибка обновления коробок: нет маппинга партнера в базе L4S")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/before/put_order_boxes_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putOrderBoxesNoPartnerMapping() {
        long orderId = 100100L;
        long mbiPartnerId = 999999L;
        OrderBoxesRequestDto request = buildRequest();

        mockCheckouterWithoutVerify(
            orderId,
            mbiPartnerId,
            buildExpectedResponseWithoutChanges(),
            false,
            false
        );

        ApiError error = apiOperation(String.valueOf(orderId), mbiPartnerId,  null, request)
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
            .as(ApiError.class);

        softly.assertThat(error.getMessage()).isEqualTo("Failed to find [MBI_PARTNER] with id [999999]");

        verify(checkouterAPI).getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Ошибка обновления коробок: неправильное сочетание параметров mbiPartnerId и mbiPartnerIds")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/before/put_order_boxes_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putOrderBoxesInvalidShopIds(
        @SuppressWarnings("unused") String displayName,
        Long mbiPartnerId,
        List<Long> mbiPartnerIds,
        String message
    ) {
        long orderId = 100100L;

        ValidationError error = apiOperation(
            String.valueOf(orderId),
            mbiPartnerId,
            mbiPartnerIds,
            buildRequestWithoutChanges()
        )
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getMessage()).isEqualTo(message);
    }

    @Nonnull
    private static Stream<Arguments> putOrderBoxesInvalidShopIds() {
        return Stream.of(
            Arguments.of(
                "Оба null",
                null,
                null,
                "Method must contain exactly one parameter of (mbiPartnerId, mbiPartnerIds): "
                + "got mbiPartnerId=null, mbiPartnerIds=[]"),
            Arguments.of(
                "Null и пустая коллекция",
                null,
                List.of(),
                "Method must contain exactly one parameter of (mbiPartnerId, mbiPartnerIds): "
                + "got mbiPartnerId=null, mbiPartnerIds=[]"),
            Arguments.of(
                "Оба не null и коллекция не пустая",
                200100L,
                List.of(200100L),
                "Method must contain exactly one parameter of (mbiPartnerId, mbiPartnerIds): "
                + "got mbiPartnerId=200100, mbiPartnerIds=[200100]")
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса")
    @ExpectedDatabase(
        value = "/controller/orderbox/put/before/put_order_boxes_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void requestValidations(
        @SuppressWarnings("unused") String displayName,
        OrderBoxesRequestDto request,
        String field,
        String error,
        boolean checkouterRequested
    ) {
        long orderId = 100100L;
        long mbiPartnerId = 200100L;

        when(checkouterAPI.getOrder(any(RequestClientInfo.class), any(OrderRequest.class)))
            .thenReturn(CheckouterFactory.buildOrder(orderId, mbiPartnerId));

        ValidationError result = apiOperation(String.valueOf(orderId), mbiPartnerId, null, request)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(result.getErrors())
            .containsExactly(new ValidationViolation().field(field).message(error));

        if (checkouterRequested) {
            verify(checkouterAPI)
                .getOrder(
                    safeRefEq(new RequestClientInfo(ClientRole.SHOP, mbiPartnerId)),
                    safeRefEq(OrderRequest.builder(orderId).build())
                );
        }
    }

    @Nonnull
    private static Stream<Arguments> requestValidations() {
        return Stream.concat(
            validationsBeforeCheckouter()
                .map(q -> Arguments.of(q.getFirst(), q.getSecond(), q.getThird(), q.getFourth(), false)),
            validationsAfterCheckouter()
                .map(q -> Arguments.of(q.getFirst(), q.getSecond(), q.getThird(), q.getFourth(), true))
        );
    }

    @Nonnull
    private static Stream<Quadruple<String, OrderBoxesRequestDto, String, String>> validationsBeforeCheckouter() {
        return Stream.concat(
            Stream.<Quadruple<String, UnaryOperator<OrderBoxesRequestDto>, String, String>>of(
                    Quadruple.of(
                        "Нет коробок в запросе",
                        b -> b.boxes(null),
                        "boxes",
                        "must not be null"
                    ),
                    Quadruple.of(
                        "Пустые коробок в запросе",
                        b -> b.boxes(List.of()),
                        "boxes",
                        "size must be between 1 and 2147483647"
                    )
                )
                .map(q -> Quadruple.of(
                    q.getFirst(),
                    q.getSecond().apply(buildSimpleBoxesRequest()),
                    q.getThird(),
                    q.getFourth()
                )),
            Stream.<Quadruple<String, UnaryOperator<OrderBoxRequestDto>, String, String>>of(
                    Quadruple.of(
                        "Вес коробки",
                        b -> b.weight(0L),
                        "boxes[0].weight",
                        "must be greater than or equal to 1"
                    ),
                    Quadruple.of(
                        "Ширина коробки",
                        b -> b.width(0L),
                        "boxes[0].width",
                        "must be greater than or equal to 1"
                    ),
                    Quadruple.of(
                        "Длина коробки",
                        b -> b.length(0L),
                        "boxes[0].length",
                        "must be greater than or equal to 1"
                    ),
                    Quadruple.of(
                        "Высота коробки",
                        b -> b.height(0L),
                        "boxes[0].height",
                        "must be greater than or equal to 1"
                    ),
                    Quadruple.of(
                        "Длина штрихкода",
                        b -> b.barcode("a".repeat(50)),
                        "boxes[0].barcode",
                        "size must be between 0 and 49"
                    ),
                    Quadruple.of(
                        "Идентификатор товара",
                        b -> b.items(List.of(new OrderBoxItem().count(1))),
                        "boxes[0].items[0].id",
                        "must not be null"
                    ),
                    Quadruple.of(
                        "Количество товара",
                        b -> b.items(List.of(new OrderBoxItem().id(1L))),
                        "boxes[0].items[0].count",
                        "must not be null"
                    ),
                    Quadruple.of(
                        "Количество товара положительно",
                        b -> b.items(List.of(new OrderBoxItem().id(1L).count(0))),
                        "boxes[0].items[0].count",
                        "must be greater than or equal to 1"
                    )
                )
                .map(q -> Quadruple.of(
                    q.getFirst(),
                    buildSimpleBoxesRequest().boxes(List.of(q.getSecond().apply(buildOrderBox()))),
                    q.getThird(),
                    q.getFourth()
                ))
            );
    }

    @Nonnull
    private static Stream<Quadruple<String, OrderBoxesRequestDto, String, String>> validationsAfterCheckouter() {
        return Stream.of(
            Quadruple.of(
                "null среди коробок",
                new OrderBoxesRequestDto().boxes(Collections.singletonList(null)),
                "boxes",
                "must not contain nulls"
            ),
            Quadruple.of(
                "Наличие штрихкода (null)",
                new OrderBoxesRequestDto().boxes(List.of(buildOrderBox().barcode(null))),
                "boxes[0].barcode",
                "must not be blank"
            ),
            Quadruple.of(
                "Наличие штрихкода (blank)",
                new OrderBoxesRequestDto().boxes(List.of(buildOrderBox().barcode(" \n\t"))),
                "boxes[0].barcode",
                "must not be blank"
            ),
            Quadruple.of(
                "Уникальные штрихкоды",
                new OrderBoxesRequestDto().boxes(List.of(buildOrderBox(), buildOrderBox())),
                "boxes[].barcode",
                "must be unique"
            ),
            Quadruple.of(
                "null среди товаров",
                new OrderBoxesRequestDto().boxes(List.of(buildOrderBox().items(Collections.singletonList(null)))),
                "boxes[0].items",
                "must not contain nulls"
            ),
            Quadruple.of(
                "Уникальные идентификаторы товара в коробке",
                new OrderBoxesRequestDto().boxes(List.of(buildOrderBox().items(List.of(
                    new OrderBoxItem().id(9092L).count(1),
                    new OrderBoxItem().id(9092L).count(1)
                )))),
                "boxes[0].items[].id",
                "must be unique"
            ),
            Quadruple.of(
                "Проверка раскладки: слишком много товара в одной коробке",
                new OrderBoxesRequestDto().boxes(List.of(buildOrderBox().items(List.of(
                    new OrderBoxItem().id(9091L).count(2)
                )))),
                "boxes[].items[]",
                "must match order items"
            ),
            Quadruple.of(
                "Проверка раскладки: слишком много товара в разных коробках",
                new OrderBoxesRequestDto().boxes(List.of(
                    buildOrderBox()
                            .items(List.of(new OrderBoxItem().id(9092L).count(3))),
                    buildOrderBox()
                            .barcode("orderId-2")
                            .items(List.of(new OrderBoxItem().id(9092L).count(3)))
                )),
                "boxes[].items[]",
                "must match order items"
            ),
            Quadruple.of(
                "Проверка раскладки: нет такого товара в заказе",
                new OrderBoxesRequestDto().boxes(List.of(buildOrderBox().items(List.of(
                    new OrderBoxItem().id(9090L).count(1)
                )))),
                "boxes[].items[]",
                "must match order items"
            )
        );
    }

    @Nonnull
    private static OrderBoxesRequestDto buildSimpleBoxesRequest() {
        return new OrderBoxesRequestDto()
            .boxes(List.of(buildOrderBox()));
    }

    @Nonnull
    private AutoCloseable mockCheckouter(
        long orderId,
        long mbiPartnerId,
        OrderBoxesDto expectedResponse,
        boolean withExternalId
    ) {
        List<ParcelBox> checkouterRequest =
            mockCheckouterWithoutVerify(orderId, mbiPartnerId, expectedResponse, withExternalId, false);
        return () -> {
            verify(checkouterAPI).getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
            verify(checkouterAPI).putParcelBoxes(
                eq(orderId),
                eq(PARCEL_ID),
                eq(checkouterRequest),
                any(RequestClientInfo.class)
            );
        };
    }

    @Nonnull
    private List<ParcelBox> mockCheckouterWithoutVerify(
        long orderId,
        long mbiPartnerId,
        OrderBoxesDto expectedResponse,
        boolean withExternalId,
        boolean equalBoxes
    ) {
        Order order = CheckouterFactory.buildOrder(orderId, mbiPartnerId);
        List<ParcelBox> checkouterRequest = buildExpectedCheckouterRequest(expectedResponse, withExternalId);
        List<ParcelBox> checkouterResponse = buildExpectedCheckouterResponse(checkouterRequest, withExternalId);

        when(
            checkouterAPI.putParcelBoxes(
                eq(orderId),
                eq(PARCEL_ID),
                eq(checkouterRequest),
                any(RequestClientInfo.class)
            )
        )
            .thenReturn(checkouterResponse);

        if (equalBoxes) {
            order.getDelivery().getParcels().get(0).setBoxes(checkouterResponse);
        } else {
            ParcelBox parcelBox = CheckouterFactory.buildParcelBox("something-1");
            parcelBox.setItems(List.of(CheckouterFactory.buildBoxItem(102L, 5)));
            order.getDelivery().getParcels().get(0).setBoxes(List.of(parcelBox));
        }
        when(checkouterAPI.getOrder(any(RequestClientInfo.class), any(OrderRequest.class)))
            .thenReturn(order);

        return checkouterRequest;
    }

    @Nonnull
    private List<ParcelBox> buildExpectedCheckouterResponse(List<ParcelBox> checkouterRequest, boolean withExternalId) {
        return EntryStream.of(checkouterRequest).mapKeyValue((index, box) -> {
                ParcelBox parcelBox = new ParcelBox();
                parcelBox.setFulfilmentId(box.getFulfilmentId());
                parcelBox.setHeight(box.getHeight());
                parcelBox.setWeight(box.getWeight());
                parcelBox.setWidth(box.getWidth());
                parcelBox.setDepth(box.getDepth());
                parcelBox.setParcelId(PARCEL_ID);
                parcelBox.setId(withExternalId ? box.getId() : index.longValue() + 1);
                parcelBox.setItems(
                    EntryStream.of(box.getItems())
                        .mapKeyValue((idx, item) -> {
                            ParcelBoxItem boxItem = new ParcelBoxItem();
                            boxItem.setItemId(item.getItemId());
                            boxItem.setCount(item.getCount());
                            boxItem.setId(idx);
                            return boxItem;
                        })
                        .toList());
                return parcelBox;
            })
            .toList();
    }

    @Nonnull
    private OrderBoxApi.PutOrderBoxesOper apiOperation(
        String orderId,
        @Nullable Long mbiPartnerId,
        @Nullable List<Long> mbiPartnerIds,
        OrderBoxesRequestDto body
    ) {
        return apiClient.orderBox().putOrderBoxes()
            .orderIdPath(orderId)
            .mbiPartnerIdQuery(mbiPartnerId)
            .mbiPartnerIdsQuery(mbiPartnerIds == null ? null : mbiPartnerIds.toArray())
            .body(body);
    }

    @Nonnull
    private OrderBoxesRequestDto buildRequest() {
        return new OrderBoxesRequestDto()
            .boxes(List.of(
                buildOrderBox("123456-1", 100L, 100L, 100L, 100L, List.of()),
                buildOrderBox("123456-2", 102L, 102L, null, 102L, List.of(
                    new OrderBoxItem().id(9091L).count(1),
                    new OrderBoxItem().id(9092L).count(5)
                )),
                new OrderBoxRequestDto().barcode("123456-3")
            ));
    }

    @Nonnull
    private OrderBoxesDto buildExpectedResponse(long orderId, OrderBoxesRequestDto request) {
        if (request.getBoxes() == null) {
            return new OrderBoxesDto().boxes(List.of());
        }
        List<OrderBox> orderBoxes = request.getBoxes().stream()
            .map(box -> new OrderBox()
                .barcode(box.getBarcode())
                .weight(box.getWeight())
                .height(box.getHeight())
                .length(box.getLength())
                .width(box.getWidth())
                .items(ListUtils.emptyIfNull(box.getItems()))
            )
            .toList();
        for (int i = 0; i < orderBoxes.size(); i++) {
            orderBoxes.get(i).id(i + 1L);
        }
        return new OrderBoxesDto().orderId(String.valueOf(orderId)).boxes(orderBoxes);
    }

    @Nonnull
    private OrderBoxesRequestDto buildRequestWithoutChanges() {
        List<OrderBoxRequestDto> orderBoxes = List.of(
            buildOrderBox("100-1", 500L, 100L, 132L, 161L, null),
            buildOrderBox("100-2", 600L, 140L, 112L, 176L, null),
            buildOrderBox("100-3", 1L, 1L, 1L, 1L, null)
        );
        return new OrderBoxesRequestDto().boxes(orderBoxes);
    }

    @Nonnull
    private OrderBoxesDto buildExpectedResponseWithoutChanges() {
        List<OrderBox> orderBoxes = List.of(
            buildOrderBox(1000L, "100-1", 500L, 100L, 132L, 161L, List.of()),
            buildOrderBox(1001L, "100-2", 600L, 140L, 112L, 176L, List.of()),
            buildOrderBox(1002L, "100-3", 1L, 1L, 1L, 1L, List.of())
        );
        return new OrderBoxesDto().orderId("100100").boxes(orderBoxes);
    }

    @Nonnull
    private List<ParcelBox> buildExpectedCheckouterRequest(OrderBoxesDto request, boolean withBoxId) {
        Objects.requireNonNull(request.getBoxes());
        return request.getBoxes().stream().map(box -> {
            ParcelBox parcelBox = new ParcelBox();
            if (withBoxId) {
                parcelBox.setId(box.getId());
            }
            parcelBox.setFulfilmentId(box.getBarcode());
            parcelBox.setHeight(box.getHeight());
            parcelBox.setWeight(box.getWeight());
            parcelBox.setWidth(box.getWidth());
            parcelBox.setDepth(box.getLength());
            parcelBox.setParcelId(PARCEL_ID);
            parcelBox.setItems(CollectionUtils.emptyIfNull(box.getItems()).stream().map(item -> {
                ParcelBoxItem parcelBoxItem = new ParcelBoxItem();
                parcelBoxItem.setItemId(item.getId());
                parcelBoxItem.setCount(item.getCount());
                return parcelBoxItem;
            }).toList());
            return parcelBox;
        }).toList();
    }

    @Nonnull
    private static OrderBoxRequestDto buildOrderBox() {
        return buildOrderBox("orderId-1", null, null, null, null, null);
    }

    @Nonnull
    private static OrderBoxRequestDto buildOrderBox(
        String barcode,
        @Nullable Long weight,
        @Nullable Long width,
        @Nullable Long length,
        @Nullable Long height,
        @Nullable List<OrderBoxItem> boxItems
    ) {
        return new OrderBoxRequestDto()
            .barcode(barcode)
            .weight(weight)
            .width(width)
            .length(length)
            .height(height)
            .items(boxItems);
    }
}
