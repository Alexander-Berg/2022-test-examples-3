package ru.yandex.market.checkout.mstat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.mstat.validators.ReturnItemMStatContractValidator;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.helpers.ReturnHelper;

import static ru.yandex.market.checkout.providers.ReturnProvider.generateReturn;

public class ReturnItemTableMStatContractTest extends AbstractMStatContractTest<ReturnItemMStatContractValidator> {

    @Autowired
    private ReturnHelper returnHelper;

    @Autowired
    private ReturnItemMStatContractValidator validator;

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль структуры таблицы return_item")
    void tableStructureTest() {
        validateTableStructure();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль типов атрибутов таблицы return_item")
    void tableAttributeTypeTest() {
        validateTableAttributesType();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль значений перечислений для атрибутов таблицы return_item")
    void possibleEnumValuesTest() {
        validatePossibleEnumValues();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль записанных в таблицу return_item значений")
    void savedValuesTest() {
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
        Order order = orderServiceTestHelper.createDeliveredBlueOrder();
        returnHelper.mockActualDelivery(order);
        Return aReturn = returnHelper.createReturn(order.getId(), generateReturn(order));

        validateSavedEntity(
                "return_id",
                aReturn.getId()
        );
    }

    @Override
    protected ReturnItemMStatContractValidator getValidator() {
        return validator;
    }

    @Override
    protected Set<String> getTableNecessaryColumns() {
        return Set.of(
                "id",
                "return_id",
                "item_id",
                "order_id"
        );
    }
}
