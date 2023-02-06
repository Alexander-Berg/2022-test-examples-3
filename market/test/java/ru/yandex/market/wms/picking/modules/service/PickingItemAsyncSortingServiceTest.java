package ru.yandex.market.wms.picking.modules.service;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;

public class PickingItemAsyncSortingServiceTest extends IntegrationTest {
    @Autowired
    private PickingItemAsyncSortingService service;

    @Test
    @DatabaseSetup(value = "/service/sort-items-async/multiple-locs/init_db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/sort-items-async/multiple-locs/task-detail-numeration.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void sortAllPickingItems() {
        service.sortAssignments(List.of("AN1", "AN2"));
    }

    @Test
    @DatabaseSetup(value = "/service/sort-items-async/multiple-locs-item-added/init_db.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/sort-items-async/multiple-locs-item-added/task-detail-numeration.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void resortAllPickingItems() {
        service.sortAssignments(List.of("AN1", "AN2"));
    }

    @Test
    @DatabaseSetup(value = "/service/sort-items-async/multiple-locs/init_db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/sort-items-async/multiple-locs/task-detail-numeration.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void sortAllPickingItemsByTaskDetailKeys() {
        service.sortPickingItems(List.of("000000001", "000000002", "000000003", "000000004", "000000005"), null);
    }

    @Test
    @DatabaseSetup(value = "/service/sort-items-async/multiple-locs-item-added/init_db.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/sort-items-async/multiple-locs-item-added/task-detail-numeration.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void resortAllPickingItemsByTaskDetailKeys() {
        service.sortPickingItems(List.of("000000001", "000000002", "000000003", "000000004", "000000005"), null);
    }
}
