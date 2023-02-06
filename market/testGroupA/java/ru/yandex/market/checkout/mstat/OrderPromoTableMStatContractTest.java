package ru.yandex.market.checkout.mstat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.mstat.validators.OrderPromoMStatContractValidator;

public class OrderPromoTableMStatContractTest extends AbstractMStatContractTest<OrderPromoMStatContractValidator> {

    @Autowired
    private OrderPromoMStatContractValidator validator;

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль структуры таблицы order_promo")
    void tableStructureTest() {
        validateTableStructure();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль типов атрибутов таблицы order_promo")
    void tableAttributeTypeTest() {
        validateTableAttributesType();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль значений перечислений для атрибутов таблицы order_promo")
    void possibleEnumValuesTest() {
        validatePossibleEnumValues();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль записанных в таблицу order_promo значений")
    void savedValuesTest() {

    }

    @Override
    protected OrderPromoMStatContractValidator getValidator() {
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
