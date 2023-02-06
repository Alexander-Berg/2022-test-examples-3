package ru.yandex.market.checkout.mstat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.mstat.validators.ReceiptItemMStatContractValidator;

public class ReceiptItemTableMStatContractTest extends AbstractMStatContractTest<ReceiptItemMStatContractValidator> {

    @Autowired
    private ReceiptItemMStatContractValidator validator;

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль структуры таблицы receipt_item")
    void tableStructureTest() {
        validateTableStructure();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль типов атрибутов таблицы receipt_item")
    void tableAttributeTypeTest() {
        validateTableAttributesType();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль значений перечислений для атрибутов таблицы receipt_item")
    void possibleEnumValuesTest() {
        validatePossibleEnumValues();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль записанных в таблицу receipt_item значений")
    void savedValuesTest() throws Exception {
        orderServiceTestHelper.createOrderUnpaid(true);
        Long receiptId = paymentTestHelper.initPayment();

        validateSavedEntity("receipt_id", receiptId);
    }

    @Override
    protected ReceiptItemMStatContractValidator getValidator() {
        return validator;
    }

    @Override
    protected Set<String> getTableNecessaryColumns() {
        return Set.of(
                "order_id",
                "receipt_id",
                "item_id",
                "id"
        );
    }
}
