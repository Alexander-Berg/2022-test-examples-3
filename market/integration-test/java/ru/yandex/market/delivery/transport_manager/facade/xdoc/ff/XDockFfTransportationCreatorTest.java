package ru.yandex.market.delivery.transport_manager.facade.xdoc.ff;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.util.XDocTestingConstants;

class XDockFfTransportationCreatorTest extends AbstractContextualTest {
    @Autowired
    private XDockFfTransportationCreator transportationCreator;

    @DisplayName("Создание виртуального перемещения 3p товаров от мерча в FF")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_ff_transportation_3p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/tag/after/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createTransportation3p() {
        transportationCreator.createTransportation(XDocTestingConstants.X_DOC_CREATE_DATA_3P);
    }

    @DisplayName("Создание виртуального перемещения 1p товаров от мерча в FF")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_ff_transportation_1p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/tag/after/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createTransportation1p() {
        transportationCreator.createTransportation(XDocTestingConstants.X_DOC_CREATE_DATA_1P);
    }

}
