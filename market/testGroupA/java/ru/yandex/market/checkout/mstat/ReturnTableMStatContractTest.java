package ru.yandex.market.checkout.mstat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.mstat.validators.ReturnMStatContractValidator;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.helpers.ReturnHelper;

import static ru.yandex.market.checkout.providers.ReturnProvider.generateReturn;

public class ReturnTableMStatContractTest extends AbstractMStatContractTest<ReturnMStatContractValidator> {

    @Autowired
    private ReturnMStatContractValidator validator;
    @Autowired
    private ReturnHelper returnHelper;

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль структуры таблицы return")
    void tableStructureTest() {
        validateTableStructure();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль типов атрибутов таблицы return")
    void tableAttributeTypeTest() {
        validateTableAttributesType();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль значений перечислений для атрибутов таблицы return")
    void possibleEnumValuesTest() {
        validatePossibleEnumValues();
    }

    @Test
    @DisplayName("Проверка контракта с Marketstat: контроль записанных в таблицу return значений")
    void savedValuesTest() {
        returnHelper.mockSupplierInfo();
        returnHelper.mockShopInfo();
        Order order = orderServiceTestHelper.createDeliveredBlueOrder();
        returnHelper.mockActualDelivery(order);
        Return aReturn = returnHelper.createReturn(order.getId(), generateReturn(order));

        validateSavedEntity(aReturn.getId());
    }

    @Override
    protected ReturnMStatContractValidator getValidator() {
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
