package ru.yandex.market.vendors.analytics.platform.facade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.service.dashboard.DashboardService;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * Тесты для {@link DashboardService}.
 */
@DbUnitDataSet(before = "DefaultDashboardServiceTest.before.csv")
public class DashboardServiceTest extends FunctionalTest {

    @Autowired
    private DashboardService dashboardService;

    @Test
    @DisplayName("Проверка, что пользователю без дашбордов добавится дефолтный дешборд с виджетами")
    @DbUnitDataSet(after = "DefaultDashboardNewServiceTest.after.csv")
    void newUserDashboards() {
        dashboardService.ensureDashboardsExist(130);
    }

    @Test
    @DisplayName("Проверка, что пользователю без дашбордов в некоторых категориях "
            + "добавятся дефолтные дешборды в этих категориях")
    @DbUnitDataSet(after = "DefaultDashboardOldServiceTest.after.csv")
    void oldUserDashboards() {
        dashboardService.ensureDashboardsExist(125);
    }
}
