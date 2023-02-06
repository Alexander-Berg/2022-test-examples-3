package ru.yandex.market.logistics.lom.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.admin.enums.AdminBillingProductType;
import ru.yandex.market.logistics.lom.admin.filter.AdminTransactionFilter;
import ru.yandex.market.logistics.lom.model.search.Direction;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lom.model.search.Sort;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/billing/before/billing_service_products.xml")
@DatabaseSetup("/controller/admin/transactions/before/admin_transactions.xml")
class SearchTransactionTest extends AbstractContextualTest {

    @DisplayName("Поиск транзакций")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchTransactionsArgument")
    void searchTest(@SuppressWarnings("unused") String caseName, AdminTransactionFilter filter, String responsePath)
        throws Exception {
        mockMvc.perform(get("/admin/transactions").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchTransactionsArgument() {
        return Stream.of(
            Triple.of(
                "Все транзакции",
                new AdminTransactionFilter(),
                "controller/admin/transactions/response/all_transactions.json"
            ),
            Triple.of(
                "Поиск транзакций по отгрузке",
                new AdminTransactionFilter().setShipmentId(1L),
                "controller/admin/transactions/response/withdraw_transactions.json"
            ),
            Triple.of(
                "Поиск транзакций по заказу",
                new AdminTransactionFilter().setOrderId(1L),
                "controller/admin/transactions/response/order_transactions.json"
            ),
            Triple.of(
                "Поиск транзакций по сумме",
                new AdminTransactionFilter().setAmount(BigDecimal.valueOf(-300)),
                "controller/admin/transactions/response/greater_300_transactions.json"
            ),
            Triple.of(
                "Поиск транзакций по продукту",
                new AdminTransactionFilter().setProductId(505058L),
                "controller/admin/transactions/response/product_transactions.json"
            ),
            Triple.of(
                "Поиск транзакций по дате и времени",
                new AdminTransactionFilter().setCreated(LocalDate.of(2019, 10, 12)),
                "controller/admin/transactions/response/after_4_hour_transactions.json"
            ),
            Triple.of(
                "Поиск транзакций по признаку корректировки",
                new AdminTransactionFilter().setIsCorrection(true),
                "controller/admin/transactions/response/id_2_transactions.json"
            ),
            Triple.of(
                "Поиск транзакций по идентификатору",
                new AdminTransactionFilter().setTransactionId(4L),
                "controller/admin/transactions/response/id_4_transactions.json"
            ),
            Triple.of(
                "Поиск транзакций — пустой результат",
                new AdminTransactionFilter().setTransactionId(42L),
                "controller/admin/transactions/response/empty_transactions.json"
            ),
            Triple.of(
                "Поиск транзакций по типу продукта",
                new AdminTransactionFilter().setProductType(AdminBillingProductType.WITHDRAW),
                "controller/admin/transactions/response/by_product_type.json"
            ),
            Triple.of(
                "Поиск транзакций все условия",
                new AdminTransactionFilter()
                    .setTransactionId(4L)
                    .setAmount(new BigDecimal("400.40").negate())
                    .setIsCorrection(false)
                    .setProductId(505164L)
                    .setOrderId(1L)
                    .setCreated(LocalDate.of(2019, 10, 9)),
                "controller/admin/transactions/response/id_4_transactions.json"
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @Test
    @DisplayName("Поиск транзакций, постраничные данные и сортировка")
    void searchTest() throws Exception {
        AdminTransactionFilter filter = new AdminTransactionFilter().setCreated(LocalDate.of(2019, 10, 9));

        MultiValueMap<String, String> params = toParams(filter);
        new Pageable(1, 3, new Sort(Direction.ASC, "created")).toUriParams()
            .forEach((key, value) -> params.add(key, value.stream().findFirst().orElse(null)));

        mockMvc.perform(get("/admin/transactions").params(params))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/transactions/response/pageble_transaction.json"));
    }

    @Test
    @DisplayName("Успешный поиск по идентификатору")
    void searchByIdTest() throws Exception {
        mockMvc.perform(get("/admin/transactions/4"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/transactions/response/id_4_detail_transactions.json"));
    }

    @Test
    @DisplayName("Транзакция не найдена по идентификатору")
    void searchByIdNotFoundTest() throws Exception {
        mockMvc.perform(get("/admin/transactions/42"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BILLING_TRANSACTION] with id [42]"));
    }
}
