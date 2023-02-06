package ru.yandex.market.logistics.management.service.health;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.health.lgw.point.warehouse.creation.WarehousesCreationChecker;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
class WarehousesCreationCheckerTest extends AbstractContextualTest {
    @Autowired
    private WarehousesCreationChecker warehousesCreationChecker;

    @Test
    void errorsOk() {
        softly.assertThat(warehousesCreationChecker.checkErrors())
            .as("Ok error statement was not checked correctly")
            .isEqualTo("0;OK");
    }

    @Test
    void pushingOk() {
        softly.assertThat(warehousesCreationChecker.checkPushing())
            .as("Ok pushing statement was not checked correctly")
            .isEqualTo("0;OK");
    }

    @Test
    @DatabaseSetup("/data/controller/health/warehouses_creation_failures.xml")
    void errorsExists() {
        softly.assertThat(warehousesCreationChecker.checkErrors())
            .as("Problems with error statement was not checked correctly")
            .isEqualTo("2;2 warehouses could not be created in partner with id=3; " +
                "1 warehouse could not be created in partner with id=4");
    }

    @Test
    @DatabaseSetup("/data/controller/health/warehouses_creation_failures.xml")
    void pushErrorsExists() {
        softly.assertThat(warehousesCreationChecker.checkPushing())
            .as("Problems with pushing statement was not checked correctly")
            .isEqualTo("2;2 warehouses could not be pushed for creation in partner id=4");
    }
}
