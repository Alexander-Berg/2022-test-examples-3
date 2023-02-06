package ru.yandex.market.checkout.mstat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.mstat.validators.ParcelMStatContractValidator;

public class ParcelTableMStatContractTest extends AbstractMStatContractTest<ParcelMStatContractValidator> {

    @Autowired
    private ParcelMStatContractValidator validator;

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль структуры таблицы parcel")
    void tableStructureTest() {
        validateTableStructure();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль типов атрибутов таблицы parcel")
    void tableAttributeTypeTest() {
        validateTableAttributesType();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль значений перечислений для атрибутов таблицы parcel")
    void possibleEnumValuesTest() {
        validatePossibleEnumValues();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль записанных в таблицу parcel значений")
    void savedValuesTest() {
        createOrder();
        validateSavedEntity("order_id", order.get().getId());
    }

    @Override
    protected ParcelMStatContractValidator getValidator() {
        return validator;
    }

    @Override
    protected Set<String> getTableNecessaryColumns() {
        return Set.of(
                "id",
                "order_id",
                "shop_shipment_id"
        );
    }
}
