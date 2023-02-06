package ru.yandex.market.checkout.mstat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.mstat.validators.ReceiptMStatContractValidator;

public class ReceiptTableMStatContractTest extends AbstractMStatContractTest<ReceiptMStatContractValidator> {

    @Autowired
    private ReceiptMStatContractValidator validator;

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль структуры таблицы receipt")
    void tableStructureTest() {
        validateTableStructure();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль типов атрибутов таблицы receipt")
    void tableAttributeTypeTest() {
        validateTableAttributesType();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль значений перечислений для атрибутов таблицы receipt")
    void possibleEnumValuesTest() {
        validatePossibleEnumValues();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль записанных в таблицу receipt значений")
    void savedValuesTest() throws Exception {
        orderServiceTestHelper.createOrderUnpaid(true);
        Long receiptId = paymentTestHelper.initPayment();

        validateSavedEntity(receiptId);
    }

    @Override
    protected ReceiptMStatContractValidator getValidator() {
        return validator;
    }

    @Override
    protected Set<String> getTableNecessaryColumns() {
        return Set.of(
                "id",
                "payment_id",
                "refund_id"
        );
    }
}
