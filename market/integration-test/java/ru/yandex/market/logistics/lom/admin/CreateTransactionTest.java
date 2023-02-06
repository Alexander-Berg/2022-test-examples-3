package ru.yandex.market.logistics.lom.admin;

import java.math.BigDecimal;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.admin.dto.TransactionCreateDto;
import ru.yandex.market.logistics.lom.admin.enums.AdminBillingProductType;
import ru.yandex.market.logistics.lom.admin.enums.AdminShipmentOption;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/billing/before/billing_service_products.xml")
@DatabaseSetup("/controller/admin/transactions/before/admin_transactions.xml")
class CreateTransactionTest extends AbstractContextualTest {

    @Test
    @DisplayName("Создание транзакции, заказ не найден")
    void createOrderNotFound() throws Exception {
        testCreate(
            TransactionCreateDto.builder()
                .orderId(42L)
                .amount(BigDecimal.valueOf(300))
                .productType(AdminBillingProductType.WITHDRAW)
                .build(),
            status().isNotFound(),
            jsonPath("message").value("Failed to find [ORDER] with id [42]")
        );
    }

    @Test
    @DisplayName("Создание транзакции, отгрузка не найдена")
    void createShipmentNotFound() throws Exception {
        testCreate(
            TransactionCreateDto.builder()
                .shipmentId(42L)
                .amount(BigDecimal.valueOf(300))
                .productType(AdminBillingProductType.WITHDRAW)
                .build(),
            status().isNotFound(),
            jsonPath("message").value("Failed to find [SHIPMENT] with id [42]")
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validateRequestDtoArguments")
    @DisplayName("Валидация создания новой транзакции")
    void createNewValidation(
        String fieldPath,
        UnaryOperator<TransactionCreateDto.TransactionCreateDtoBuilder> builderModifier
    ) throws Exception {
        createTransaction(builderModifier.apply(transactionCreateDto()).build())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].field").value(fieldPath))
            .andExpect(jsonPath("errors[0].defaultMessage").value("must not be null"));
    }

    @Nonnull
    private static Stream<Arguments> validateRequestDtoArguments() {
        return Stream.<Pair<String, UnaryOperator<TransactionCreateDto.TransactionCreateDtoBuilder>>>of(
            Pair.of("amount", b -> b.amount(null)),
            Pair.of("productType", b -> b.productType(null))
        )
            .map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    @Test
    @DisplayName("Создание транзакции, не указан заказ и отгрузка")
    void createNoShipmentAndOrder() throws Exception {
        testCreate(
            TransactionCreateDto.builder()
                .amount(BigDecimal.valueOf(300))
                .productType(AdminBillingProductType.WITHDRAW)
                .build(),
            status().isBadRequest(),
            jsonPath("message").value(
                "Для создания корректирующей транзакции необходимо указать идентификатор заказа или отгрузки"
            )
        );
    }

    @Test
    @DisplayName("Создание транзакции для отгрузки с типом продукта SERVICE")
    void createServiceForWithdraw() throws Exception {
        testCreate(
            TransactionCreateDto.builder()
                .shipmentId(1L)
                .amount(BigDecimal.valueOf(300))
                .productType(AdminBillingProductType.WITHDRAW)
                .serviceType(AdminShipmentOption.CHECK)
                .build(),
            status().isBadRequest(),
            jsonPath("message").value("Не найден продукт по типу WITHDRAW и типу услуги CHECK.")
        );
    }

    @Test
    @DisplayName("Успешное создание транзакции")
    void createTransaction() throws Exception {
        testCreate(
            TransactionCreateDto.builder()
                .orderId(1L)
                .amount(BigDecimal.valueOf(200.25))
                .productType(AdminBillingProductType.SERVICE)
                .serviceType(AdminShipmentOption.CHECK)
                .comment("check tx correct")
                .build(),
            status().isOk(),
            content().string("9")
        );
    }

    private void testCreate(
        TransactionCreateDto requestBody,
        ResultMatcher status,
        ResultMatcher responseMatcher
    ) throws Exception {
        createTransaction(requestBody)
            .andExpect(status)
            .andExpect(responseMatcher);
    }

    @Nonnull
    private ResultActions createTransaction(TransactionCreateDto requestBody) throws Exception {
        return mockMvc.perform(request(HttpMethod.POST, "/admin/transactions", requestBody));
    }

    @Nonnull
    private TransactionCreateDto.TransactionCreateDtoBuilder transactionCreateDto() {
        return TransactionCreateDto.builder()
            .amount(BigDecimal.TEN)
            .productType(AdminBillingProductType.SERVICE);
    }
}
