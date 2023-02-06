package ru.yandex.market.logistics.lom.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/billing/before/billing_service_products.xml")
@DatabaseSetup("/controller/admin/transactions/before/admin_transactions.xml")
class CorrectionTransactionTest extends AbstractContextualTest {

    @Test
    @DisplayName("Подготовка корректировки, транзакция не найдена")
    void prepareCorrectTxNotFound() throws Exception {
        mockMvc.perform(get("/admin/transactions/42"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("message").value("Failed to find [BILLING_TRANSACTION] with id [42]"));
    }

    @Test
    @DisplayName("Успешная подготовка корректировки")
    void prepareCorrection() throws Exception {
        mockMvc.perform(get("/admin/transactions/4"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/transactions/response/correction_4_transactions.json"));
    }

    @Test
    @DisplayName("Корректировка транзакции по заказу")
    @ExpectedDatabase(
        value = "/controller/admin/transactions/after/corrected_order_transaction.xml",
        assertionMode = NON_STRICT
    )
    void correctionOrder() throws Exception {
        mockMvc.perform(
            put("/admin/transactions/correction/4")
                .content(extractFileContent("controller/admin/transactions/request/correction_order_tx.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/admin/transactions/response/order_tx_correction_response.json",
                "item.values.created"
            ));
    }

    @Test
    @DisplayName("Корректировка транзакции по отгрузке")
    @ExpectedDatabase(
        value = "/controller/admin/transactions/after/corrected_shipment_transaction.xml",
        assertionMode = NON_STRICT
    )
    void correctionShipment() throws Exception {
        mockMvc.perform(
            put("/admin/transactions/correction/8")
                .content(extractFileContent("controller/admin/transactions/request/correction_shipment_tx.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/admin/transactions/response/shipment_tx_correction_response.json",
                "item.values.created"
            ));
    }

    @Test
    @DisplayName("Корректировка транзакции без указания суммы")
    void correctionNoAmount() throws Exception {
        mockMvc.perform(
            put("/admin/transactions/correction/8")
                .content(extractFileContent("controller/admin/transactions/request/correction_no_amount.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].field").value("newAmount"))
            .andExpect(jsonPath("errors[0].defaultMessage").value("must not be null"));
    }

}
