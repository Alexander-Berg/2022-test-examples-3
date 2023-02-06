package ru.yandex.market.checkout.mstat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.mstat.validators.ParcelItemMStatContractValidator;

public class ParcelItemTableMStatContractTest extends AbstractMStatContractTest<ParcelItemMStatContractValidator> {

    @Autowired
    private ParcelItemMStatContractValidator validator;

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль структуры таблицы parcel_item")
    void tableStructureTest() {
        validateTableStructure();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль типов атрибутов таблицы parcel_item")
    void tableAttributeTypeTest() {
        validateTableAttributesType();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль значений перечислений для атрибутов таблицы parcel_item")
    void possibleEnumValuesTest() {
        validatePossibleEnumValues();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль записанных в таблицу parcel_item значений")
    void savedValuesTest() {

    }

    @Override
    protected ParcelItemMStatContractValidator getValidator() {
        return validator;
    }

    @Override
    protected Set<String> getTableNecessaryColumns() {
        return Set.of(
                "shipment_id",
                "delivery_id",
                "item_id"
        );
    }
}
