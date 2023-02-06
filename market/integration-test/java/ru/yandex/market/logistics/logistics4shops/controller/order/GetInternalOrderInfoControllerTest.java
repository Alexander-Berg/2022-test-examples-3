package ru.yandex.market.logistics.logistics4shops.controller.order;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.client.api.InternalOrderApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.LogisticOrderBox;
import ru.yandex.market.logistics.logistics4shops.client.api.model.LogisticOrderInfo;
import ru.yandex.market.logistics.logistics4shops.client.api.model.LogisticOrderSearchRequest;
import ru.yandex.market.logistics.logistics4shops.client.api.model.LogisticOrderSearchResponse;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderGroup;
import ru.yandex.market.logistics.logistics4shops.client.api.model.OrderType;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationViolation;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Внутренний контроллер заказов")
@ParametersAreNonnullByDefault
class GetInternalOrderInfoControllerTest extends AbstractIntegrationTest {

    @Autowired
    private LomClient lomClient;

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(lomClient);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Валидация запроса")
    void requestValidation(
        @SuppressWarnings("unused") String name,
        @Nullable List<OrderGroup> groups,
        String field,
        String message
    ) {
        ValidationError error = searchOrders(new LogisticOrderSearchRequest().orderGroups(groups))
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(error.getErrors())
            .containsExactly(new ValidationViolation().field(field).message(message));
    }

    @Nonnull
    private static Stream<Arguments> requestValidation() {
        return Stream.of(
            Arguments.of(
                "Пустой запрос — null",
                null,
                "orderGroups",
                "must not be null"
            ),
            Arguments.of(
                "Пустой запрос — empty",
                List.of(),
                "orderGroups",
                "size must be between 1 and 2147483647"
            ),
            Arguments.of(
                "Группа заказов без указания типа",
                List.of(orderGroup(null, List.of("1"))),
                "orderGroups[0].type",
                "must not be null"
            ),
            Arguments.of(
                "Группа заказов без указания заказов — null",
                List.of(orderGroup(OrderType.FBS, null)),
                "orderGroups[0].ids",
                "must not be null"
            ),
            Arguments.of(
                "Группа заказов без указания заказов — empty",
                List.of(orderGroup(OrderType.FBS, List.of())),
                "orderGroups[0].ids",
                "size must be between 1 and 2147483647"
            ),
            Arguments.of(
                "Повторяющийся тип группы заказов",
                List.of(orderGroup(OrderType.FBS, List.of("1")), orderGroup(OrderType.FBS, List.of("2"))),
                "orderGroups[].type",
                "must be unique"
            )
        );
    }

    @Test
    @DisplayName("Успешно сходили по всем типам заказов и получили результат")
    @DatabaseSetup({
        "/controller/order/internalsearchorders/setup_order.xml",
        "/controller/order/internalsearchorders/setup_boxes.xml"
    })
    void successAllTypes() throws Exception {
        try (
            var lomSearch1 = lomFactory.mockSearchWithDefaultOrders(PlatformClient.BERU, Set.of("2"));
            var lomSearch2 = lomFactory.mockSearchWithDefaultOrders(PlatformClient.FAAS, Set.of("3"))
        ) {
            LogisticOrderSearchResponse response = searchOrders(
                new LogisticOrderSearchRequest().orderGroups(List.of(
                    orderGroup(OrderType.FBS, List.of("1")),
                    orderGroup(OrderType.FBY, List.of("2")),
                    orderGroup(OrderType.FAAS, List.of("3"))
                ))
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(response)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(new LogisticOrderSearchResponse().orders(List.of(
                    new LogisticOrderInfo().type(OrderType.FBS).id("1").boxes(List.of(
                        new LogisticOrderBox()
                            .barcode("100-1")
                            .weight(500L)
                            .width(100L)
                            .length(132L)
                            .height(161L),
                        new LogisticOrderBox()
                            .barcode("100-2")
                            .weight(600L)
                            .width(140L)
                            .length(112L)
                            .height(176L)
                    )),
                    new LogisticOrderInfo().type(OrderType.FBY).id("2").addBoxesItem(
                        new LogisticOrderBox()
                            .barcode("2-1")
                            .weight(2123L)
                            .height(3L)
                            .width(4L)
                            .length(5L)
                    ),
                    new LogisticOrderInfo().type(OrderType.FAAS).id("3").addBoxesItem(
                        new LogisticOrderBox()
                            .barcode("3-1")
                            .weight(3123L)
                            .height(4L)
                            .width(5L)
                            .length(6L)
                    )
                )));
        }
    }

    @Test
    @DisplayName("Поиск в ломе батчами")
    void successBatchedSearch() throws Exception {
        try (
            var lomSearch1 = lomFactory.mockSearchWithDefaultOrders(PlatformClient.BERU, Set.of("2"));
            var lomSearch2 = lomFactory.mockSearchWithDefaultOrders(PlatformClient.BERU, Set.of("3"))
        ) {
            LogisticOrderSearchResponse response = searchOrders(
                new LogisticOrderSearchRequest().orderGroups(List.of(
                    orderGroup(OrderType.FBY, List.of("2", "3"))
                ))
            )
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

            softly.assertThat(response)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(new LogisticOrderSearchResponse().orders(List.of(
                    new LogisticOrderInfo().type(OrderType.FBY).id("2").addBoxesItem(
                        new LogisticOrderBox()
                            .barcode("2-1")
                            .weight(2123L)
                            .height(3L)
                            .width(4L)
                            .length(5L)
                    ),
                    new LogisticOrderInfo().type(OrderType.FBY).id("3").addBoxesItem(
                        new LogisticOrderBox()
                            .barcode("3-1")
                            .weight(3123L)
                            .height(4L)
                            .width(5L)
                            .length(6L)
                    )
                )));
        }
    }

    @Nonnull
    private static OrderGroup orderGroup(@Nullable OrderType orderType, @Nullable List<String> orderIds) {
        return new OrderGroup().type(orderType).ids(orderIds);
    }

    @Nonnull
    private InternalOrderApi.InternalSearchOrdersOper searchOrders(LogisticOrderSearchRequest request) {
        return apiClient.internalOrder().internalSearchOrders().body(request);
    }
}
