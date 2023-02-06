package ru.yandex.market.checkout.mstat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.mstat.validators.ParcelHistoryMStatContractValidator;

public class ParcelHistoryTableMStatContractTest
        extends AbstractMStatContractTest<ParcelHistoryMStatContractValidator> {

    @Autowired
    private ParcelHistoryMStatContractValidator validator;

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль структуры таблицы parcel_history")
    void tableStructureTest() {
        validateTableStructure();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль типов атрибутов таблицы parcel_history")
    void tableAttributeTypeTest() {
        validateTableAttributesType();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль записанных в таблицу parcel_history значений")
    void savedValuesTest() {
        createOrder();
        validateSavedEntity("order_id", order.get().getId());
    }

    @Override
    protected ParcelHistoryMStatContractValidator getValidator() {
        return validator;
    }

    @Override
    protected Set<String> getTableNecessaryColumns() {
        return Set.of(
                "id",
                "order_id",
                "delivery_id"
        );
    }
}
