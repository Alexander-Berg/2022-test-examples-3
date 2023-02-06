package ru.yandex.market.checkout.mstat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.mstat.validators.RefundMStatContractValidator;

public class RefundTableMStatContractTest
        extends AbstractMStatContractTest<RefundMStatContractValidator> {

    @Autowired
    private RefundMStatContractValidator validator;

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль структуры таблицы refund")
    void tableStructureTest() {
        validateTableStructure();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль типов атрибутов таблицы refund")
    void tableAttributeTypeTest() {
        validateTableAttributesType();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль значений перечислений для атрибутов таблицы refund")
    void possibleEnumValuesTest() {
        validatePossibleEnumValues();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль записанных в таблицу refund значений")
    void savedValuesTest() throws Exception {
        checkouterProperties.setEnableServicesPrepay(true);
        createUnpaidOrder();

        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();

        refundTestHelper.checkRefundableItems();
        validateSavedEntity(refundTestHelper.makeFullRefund());
    }

    @Override
    protected RefundMStatContractValidator getValidator() {
        return validator;
    }

    @Override
    protected Set<String> getTableNecessaryColumns() {
        return Set.of(
                "id",
                "payment_id",
                "amount",
                "order_id"
        );
    }
}
