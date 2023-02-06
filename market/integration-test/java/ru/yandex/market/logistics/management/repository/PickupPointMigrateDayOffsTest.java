package ru.yandex.market.logistics.management.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;

@DatabaseSetup({
    "/data/repository/service/service_codes.xml",
})
public class PickupPointMigrateDayOffsTest extends AbstractContextualAspectValidationTest {
    @Autowired
    private PickupPointMigrateDayOffsRepository pickupPointMigrateDayOffs;

    @Test
    @DatabaseSetup("/data/repository/points/logistics_point_with_day_offs.xml")
    @ExpectedDatabase(
        value = "/data/repository/points/service_day_offs_after_sync.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void migrateDayOffsTest() {
        pickupPointMigrateDayOffs.migrateDayOffsForLogisticsPoint(1);
    }
}
