package ru.yandex.market.logistics.logistics4shops.controller.orderbox;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.logistics.logistics4shops.client.api.OrderBoxApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderBoxesDto;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderBoxesListDto;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderBoxesSearchRequest;
import ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory;
import ru.yandex.market.logistics.logistics4shops.service.checkouter.CheckouterPagerUtil;

import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DisplayName("Получение грузомест")
@DatabaseSetup("/controller/orderbox/search/before/search_order_boxes_before.xml")
@ParametersAreNonnullByDefault
class SearchOrdersBoxesControllerTest extends AbstractOrderBoxControllerTest {
    private static final Long SHOP_ID = 200100L;

    @Captor
    private ArgumentCaptor<OrderSearchRequest> orderSearchRequestCaptor;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Получить коробки по списку заказов")
    void searchOrderBoxes(
        @SuppressWarnings("unused") String name,
        OrderBoxesSearchRequest searchRequest,
        OrderBoxesListDto expectedResponse,
        Function<CheckouterAPI, Integer> mockRequestConsumer
    ) {
        int interactionsWithCheckouter = mockRequestConsumer.apply(checkouterAPI);

        OrderBoxesListDto response = apiOperation(searchRequest)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(response)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedResponse);

        verify(checkouterAPI, times(interactionsWithCheckouter)).getOrders(
            any(RequestClientInfo.class),
            any(OrderSearchRequest.class)
        );
    }

    @Test
    @DisplayName("В запросе есть заказ, которого нет в чекаутере вообще")
    @DatabaseSetup(
        value = "/controller/orderbox/search/before/no_checkouter_order_boxes_before.xml",
        type = DatabaseOperation.INSERT
    )
    void searchOrderBoxesNoCheckouterOrder() {
        when(checkouterAPI.getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class)))
            .thenReturn(new PagedOrders(
                List.of(),
                new Pager(0, 0, 0, 0, 1, 1)
            ));

        OrderBoxesListDto response = apiOperation(
            new OrderBoxesSearchRequest().orderIds(List.of("100101", "100104"))
        )
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        OrderBoxesListDto expected = new OrderBoxesListDto()
            .orderBoxes(List.of(
                new OrderBoxesDto()
                    .orderId("100101")
                    .boxes(List.of(buildOrderBox(1100L, "101-1", 100L, 200L, 300L, 400L, List.of())))
            ));

        softly.assertThat(response).isEqualTo(expected);

        verify(checkouterAPI).getOrders(any(RequestClientInfo.class), orderSearchRequestCaptor.capture());
        softly.assertThat(orderSearchRequestCaptor.getValue())
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(CheckouterFactory.orderSearchRequest(100104L));
    }

    @Nonnull
    private static Stream<Arguments> searchOrderBoxes() {
        return Stream.of(
            Arguments.of(
                "Несколько коробок по заказам",
                new OrderBoxesSearchRequest()
                    .orderIds(List.of("100100", "100101", "100102", "100104")),
                new OrderBoxesListDto()
                    .orderBoxes(List.of(
                        new OrderBoxesDto()
                            .orderId("100100")
                            .boxes(List.of(
                                    buildOrderBox(1000L, "100-1", 500L, 100L, 132L, 161L, List.of(
                                        buildBoxItem(123451L, 1),
                                        buildBoxItem(123452L, 5)
                                    )),
                                    buildOrderBox(1001L, "100-2", 600L, 140L, 112L, 176L, List.of()),
                                    buildOrderBox(1002L, "100-3", 1L, 1L, 1L, 1L, List.of())
                                )
                            ),
                        new OrderBoxesDto()
                            .orderId("100101")
                            .boxes(List.of(
                                buildOrderBox(9100L, "101-2", 901L, 902L, 904L, 903L, List.of(
                                    buildBoxItem(9091L, 9),
                                    buildBoxItem(9092L, 99)
                                ))
                            )),
                        new OrderBoxesDto()
                            .orderId("100104")
                            .boxes(List.of(
                                buildOrderBox(9100L, "100104-1", 901L, 902L, 904L, 903L, List.of(
                                    buildBoxItem(9091L, 9),
                                    buildBoxItem(9092L, 99)
                                ))
                            ))
                    )),
                (Function<CheckouterAPI, Integer>) (checkouterApi) -> {
                    var orderIds = StreamEx.of(100101L, 100102L, 100104L).toArray(Long.class);
                    mockCheckouterRequest(
                        checkouterApi,
                        orderIds,
                        CheckouterPagerUtil.defaultPager(),
                        new Pager(3, 1, 1, 1, 3, 1),
                        List.of(CheckouterFactory.createOrder(100101L, List.of("101-1")))
                    );
                    mockCheckouterRequest(
                        checkouterApi,
                        orderIds,
                        new Pager(3, 2, 2, 1, 3, 2),
                        new Pager(3, 2, 2, 1, 3, 2),
                        List.of(CheckouterFactory.createOrder(100101L, List.of("101-2")))
                    );
                    mockCheckouterRequest(
                        checkouterApi,
                        orderIds,
                        new Pager(3, 3, 3, 1, 3, 3),
                        new Pager(3, 3, 3, 1, 3, 3),
                        List.of(CheckouterFactory.createOrder(100104L, List.of("100104-1")))
                    );
                    return 3;
                }
            ),
            Arguments.of(
                "Один заказ в запросе",
                new OrderBoxesSearchRequest()
                    .orderIds(List.of("100100")),
                new OrderBoxesListDto()
                    .orderBoxes(List.of(
                        new OrderBoxesDto()
                            .orderId("100100")
                            .boxes(List.of(
                                    buildOrderBox(1000L, "100-1", 500L, 100L, 132L, 161L, List.of(
                                        buildBoxItem(123451L, 1),
                                        buildBoxItem(123452L, 5)
                                    )),
                                    buildOrderBox(1001L, "100-2", 600L, 140L, 112L, 176L, List.of()),
                                    buildOrderBox(1002L, "100-3", 1L, 1L, 1L, 1L, List.of())
                                )
                            )
                    )),
                (Function<CheckouterAPI, Integer>) (checkouterApi) -> 0
            ),
            Arguments.of(
                "Нет коробок в базе",
                new OrderBoxesSearchRequest()
                    .orderIds(List.of("100103")),
                new OrderBoxesListDto()
                    .orderBoxes(List.of(
                        new OrderBoxesDto()
                            .orderId("100103")
                            .boxes(List.of((buildOrderBox(9100L, "100103-1", 901L, 902L, 904L, 903L, List.of(
                                buildBoxItem(9091L, 9),
                                buildBoxItem(9092L, 99)
                            )))))
                    )),
                (Function<CheckouterAPI, Integer>) (checkouterApi) -> {
                    mockCheckouterRequest(
                        checkouterApi,
                        StreamEx.of(100103L).toArray(Long.class),
                        CheckouterPagerUtil.defaultPager(),
                        CheckouterPagerUtil.defaultPager().setTotal(1),
                        List.of(CheckouterFactory.buildOrder(100103L, SHOP_ID))
                    );
                    return 1;
                }
            ),
            Arguments.of(
                "Нет коробок ни в базе, ни в чекаутере",
                new OrderBoxesSearchRequest()
                    .orderIds(List.of("100103")),
                new OrderBoxesListDto()
                    .orderBoxes(List.of(
                        new OrderBoxesDto()
                            .orderId("100103")
                            .boxes(List.of())
                    )),
                (Function<CheckouterAPI, Integer>) (checkouterApi) -> {
                    mockCheckouterRequest(
                        checkouterApi,
                        StreamEx.of(100103L).toArray(Long.class),
                        CheckouterPagerUtil.defaultPager(),
                        CheckouterPagerUtil.defaultPager().setTotal(1),
                        List.of(CheckouterFactory.createOrder(100103L, List.of()))
                    );
                    return 1;
                }
            )
        );
    }

    @Nonnull
    private OrderBoxApi.SearchOrdersBoxesOper apiOperation(
        OrderBoxesSearchRequest orderBoxesSearchRequest
    ) {
        return apiClient.orderBox().searchOrdersBoxes()
            .mbiPartnerIdQuery(SHOP_ID)
            .body(orderBoxesSearchRequest);
    }

    private static void mockCheckouterRequest(
        CheckouterAPI checkouterAPI,
        Long[] orderIds,
        Pager requestPager,
        Pager responsePager,
        List<Order> orders
    ) {
        when(checkouterAPI.getOrders(
            any(RequestClientInfo.class),
            safeRefEq(OrderSearchRequest.builder()
                .withOrderIds(orderIds)
                .withPageInfo(requestPager)
                .withRgbs(Color.BLUE, Color.WHITE)
                .build()
            )
        ))
            .thenReturn(new PagedOrders(orders, responsePager));
    }

}
