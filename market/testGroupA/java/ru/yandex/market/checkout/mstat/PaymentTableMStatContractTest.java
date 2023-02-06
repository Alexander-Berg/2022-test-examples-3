package ru.yandex.market.checkout.mstat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.mstat.validators.PaymentMStatContractValidator;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.util.sberbank.SberMockConfigurer;

public class PaymentTableMStatContractTest extends AbstractMStatContractTest<PaymentMStatContractValidator> {

    @Autowired
    protected OrderPayHelper paymentHelper;
    @Autowired
    protected SberMockConfigurer sberMockConfigurer;
    @Autowired
    private PaymentMStatContractValidator validator;

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль структуры таблицы payments")
    void tableStructureTest() {
        validateTableStructure();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль типов атрибутов таблицы payments")
    void tableAttributeTypeTest() {
        validateTableAttributesType();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль значений перечислений для атрибутов таблицы payments")
    void possibleEnumValuesTest() {
        validatePossibleEnumValues();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль записанных в таблицу payments значений")
    void savedValuesTest() throws Exception {
        orderServiceTestHelper.createOrderUnpaid(true);
        Long receiptId = paymentTestHelper.initPayment();

        Map<String, Object> result = jdbcTemplate.queryForMap(
                "SELECT * FROM receipt WHERE id = ?",
                receiptId
        );
        validateSavedEntity(result.get("payment_id"));
    }

    @Override
    protected PaymentMStatContractValidator getValidator() {
        return validator;
    }

    @Override
    protected Set<String> getTableNecessaryColumns() {
        return Set.of(
                "id",
                "order_id"
        );
    }
}
