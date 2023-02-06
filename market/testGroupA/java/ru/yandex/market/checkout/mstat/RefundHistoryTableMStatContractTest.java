package ru.yandex.market.checkout.mstat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.mstat.validators.RefundHistoryMStatContractValidator;

public class RefundHistoryTableMStatContractTest
        extends AbstractMStatContractTest<RefundHistoryMStatContractValidator> {

    @Autowired
    private RefundHistoryMStatContractValidator validator;

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль структуры таблицы refund_history")
    void tableStructureTest() {
        validateTableStructure();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль типов атрибутов таблицы refund_history")
    void tableAttributeTypeTest() {
        validateTableAttributesType();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль записанных в таблицу refund_history значений")
    void savedValuesTest() throws Exception {
        checkouterProperties.setEnableServicesPrepay(true);
        createUnpaidOrder();

        paymentTestHelper.initAndHoldPayment();
        paymentTestHelper.clearPayment();

        refundTestHelper.checkRefundableItems();
        validateSavedEntity("refund_id", refundTestHelper.makeFullRefund());
    }

    @Override
    protected RefundHistoryMStatContractValidator getValidator() {
        return validator;
    }

    @Override
    protected Set<String> getTableNecessaryColumns() {
        return Set.of(
                "id",
                "refund_id",
                "order_id"
        );
    }
}
