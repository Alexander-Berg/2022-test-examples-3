package ru.yandex.market.checkout.mstat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.mstat.validators.OrderMStatContractValidator;

public class OrderTableMStatContractTest extends AbstractMStatContractTest<OrderMStatContractValidator> {

    @Autowired
    private OrderMStatContractValidator validator;

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль структуры таблицы orders")
    void tableStructureTest() {
        validateTableStructure();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль типов атрибутов таблицы orders")
    void tableAttributeTypeTest() {
        validateTableAttributesType();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль значений перечислений для атрибутов таблицы orders")
    void possibleEnumValuesTest() {
        validatePossibleEnumValues();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль записанных в таблицу orders значений")
    void savedValuesTest() {
        createOrder();
        validateSavedEntity(order.get().getId());
    }

    @Override
    protected OrderMStatContractValidator getValidator() {
        return validator;
    }

    @Override
    protected Set<String> getTableNecessaryColumns() {
        return Set.of(
                "id"
        );
    }
}
