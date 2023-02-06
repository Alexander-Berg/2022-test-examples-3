package ru.yandex.market.logistics.lom.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.admin.filter.AdminBalancePaymentFilter;
import ru.yandex.market.logistics.lom.utils.TestUtils;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.logistics.test.integration.utils.QueryParamUtils;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.order.DaasOrderPaymentDTO;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/admin/balance_payment/prepare.xml")
class AdminBalancePaymentTest extends AbstractContextualTest {

    private static final Map<Long, DaasOrderPaymentDTO> DTO_MAP = Map.of(
        100L, new DaasOrderPaymentDTO(100L, 500, "trustId1"),
        102L, new DaasOrderPaymentDTO(102L, 501, "trustId2"),
        103L, new DaasOrderPaymentDTO(103L, 500, "trustId3")
    );

    @Autowired
    private MbiApiClient mbiApiClient;

    @Test
    @DisplayName("Получение второй страницы платежных поручений")
    void getSecondPage() throws Exception {
        when(mbiApiClient.getDaasOrders(List.of(100L), List.of()))
            .thenReturn(List.of(DTO_MAP.get(100L)));
        when(mbiApiClient.getDaasOrders(List.of(102L), List.of()))
            .thenReturn(List.of(DTO_MAP.get(102L)));
        mockMvc.perform(get("/admin/balance-payment")
            .param("page", "2")
            .param("size", "1")
            .params(TestUtils.toParamWithCollections(new AdminBalancePaymentFilter()))
        )
            .andExpect(status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("controller/admin/balance_payment/balance_payment_4.json"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("arguments")
    @DisplayName("Поиск платежных поручений")
    void search(
        @SuppressWarnings("unused") String displayName,
        Map<String, String> filter,
        String responsePath,
        List<Long> expectedIds
    ) throws Exception {
        when(mbiApiClient.getDaasOrders(any(), eq(List.of())))
            .thenReturn(expectedIds.stream().map(DTO_MAP::get).filter(Objects::nonNull).collect(Collectors.toList()));
        when(mbiApiClient.getDaasOrders(List.of(), List.of(500L)))
            .thenReturn(List.of(DTO_MAP.get(100L), DTO_MAP.get(103L)));
        when(mbiApiClient.getDaasOrders(List.of(), List.of()))
            .thenReturn(List.of());

        mockMvc.perform(get("/admin/balance-payment")
            .params(QueryParamUtils.toParams(filter))
        )
            .andExpect(status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                Map.of(),
                "controller/admin/balance_payment/grid.json",
                List.of(104L, 100L, 102L)
            ),
            Arguments.of(
                "Фильтр по идентификатору заказа",
                Map.of("orderId", "1"),
                "controller/admin/balance_payment/balance_payment_1.json",
                List.of(100L)
            ),
            Arguments.of(
                "Фильтр по идентификатору корзины",
                Map.of("basketId", "balance_basket_1"),
                "controller/admin/balance_payment/balance_payment_1.json",
                List.of(100L)
            ),
            Arguments.of(
                "Фильтр по дате создания",
                Map.of("created", "2019-06-02 12:00"),
                "controller/admin/balance_payment/balance_payment_1.json",
                List.of(100L)
            ),
            Arguments.of(
                "Фильтр по сумме платежного поручения",
                Map.of("amount", "11002"),
                "controller/admin/balance_payment/balance_payment_2467.json",
                List.of(104L, 102L)
            ),
            Arguments.of(
                "Фильтр по идентификатору сендера",
                Map.of("senderId", "1"),
                "controller/admin/balance_payment/balance_payment_1.json",
                List.of(100L)
            ),
            Arguments.of(
                "Фильтр по номеру п/п",
                Map.of("bankOrderId", "500"),
                "controller/admin/balance_payment/balance_payment_16.json",
                List.of(100L, 103L)
            ),
            Arguments.of(
                "Фильтр по номеру п/п, несуществующие trustOrderIds",
                Map.of("bankOrderId", "5001"),
                "controller/admin/balance_payment/balance_payment_empty.json",
                List.of()
            ),
            Arguments.of(
                "Фильтр по всем полям",
                Map.of(
                    "orderId", "1",
                    "basketId", "balance_basket_1",
                    "created", "2019-06-02 12:00",
                    "amount", "11000",
                    "senderId", "1",
                    "bankOrderId", "500"
                ),
                "controller/admin/balance_payment/balance_payment_1.json",
                List.of(100L)
            )
        );
    }

    @Test
    @DisplayName("Получение только первой страницы по фильтру платежных поручений")
    void getOnlyFirstPageFilteredBankOrderId() throws Exception {
        when(mbiApiClient.getDaasOrders(List.of(), List.of(500L)))
            .thenReturn(List.of(DTO_MAP.get(100L), DTO_MAP.get(103L)));
        mockMvc.perform(get("/admin/balance-payment")
            .param("page", "1")
            .param("size", "1")
            .params(TestUtils.toParamWithCollections(new AdminBalancePaymentFilter().setBankOrderId(500L)))
        )
            .andExpect(status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("controller/admin/balance_payment/balance_payment_1.json"));
    }

    @Test
    @DatabaseSetup(value = "/controller/admin/balance_payment/order_without_total.xml", type = DatabaseOperation.UPDATE)
    @DisplayName("Поиск платежных поручений - заказ без стоимости")
    void searchWithoutOrder() throws Exception {
        when(mbiApiClient.getDaasOrders(any(), eq(List.of()))).thenReturn(new ArrayList<>(DTO_MAP.values()));
        mockMvc.perform(get("/admin/balance-payment")
            .params(TestUtils.toParamWithCollections(new AdminBalancePaymentFilter())))
            .andExpect(status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("controller/admin/balance_payment/without_total.json"));
    }
}
