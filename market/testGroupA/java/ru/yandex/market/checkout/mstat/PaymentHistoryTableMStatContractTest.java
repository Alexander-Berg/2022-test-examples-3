package ru.yandex.market.checkout.mstat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.mstat.validators.PaymentHistoryMStatContractValidator;

public class PaymentHistoryTableMStatContractTest
        extends AbstractMStatContractTest<PaymentHistoryMStatContractValidator> {

    @Autowired
    private PaymentHistoryMStatContractValidator validator;

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль структуры таблицы payment_history")
    void tableStructureTest() {
        validateTableStructure();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль типов атрибутов таблицы payment_history")
    void tableAttributeTypeTest() {
        validateTableAttributesType();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль записанных в таблицу payment_history значений")
    void savedValuesTest() throws Exception {
        orderServiceTestHelper.createOrderUnpaid(true);
        Long receiptId = paymentTestHelper.initPayment();

        Map<String, Object> result = jdbcTemplate.queryForMap(
                "SELECT * FROM receipt WHERE id = ?",
                receiptId
        );

        validateSavedEntity("payment_id", result.get("payment_id"));
    }

    @Override
    protected PaymentHistoryMStatContractValidator getValidator() {
        return validator;
    }

    @Override
    protected Set<String> getTableNecessaryColumns() {
        return Set.of(
                "id",
                "author_id",
                "payment_id",
                "order_id"
        );
    }
}
