package ru.yandex.market.delivery.transport_manager.facade.xdoc.ff;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.util.XDocTestingConstants;

class BreakBulkXDockFfTransportationCreatorTest extends AbstractContextualTest {
    @Autowired
    private BreakBulkXDockFfTransportationCreator transportationCreator;

    @DisplayName("Создание виртуального перемещения Break Bulk XDock товаров от РЦ(WMS) в FF")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_ff_transportation_break_bulk_xdock.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/tag/after/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void createTransportation() {
        transportationCreator.createTransportation(XDocTestingConstants.X_DOC_CREATE_DATA_BREAK_BULK_XDOCK);
    }
}
