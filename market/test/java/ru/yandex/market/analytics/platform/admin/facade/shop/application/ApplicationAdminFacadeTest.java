package ru.yandex.market.analytics.platform.admin.facade.shop.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.analytics.platform.admin.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.model.partner.application.ApplicationStatus;

@DbUnitDataSet(before = "ApplicationAdminFacadeTest.before.csv")
public class ApplicationAdminFacadeTest extends FunctionalTest {
    private final int APPLICATION_ID = 1;

    @Autowired
    private ApplicationAdminFacade applicationAdminFacade;

    @Test
    @DisplayName("Тестирует смену статуса заявки")
    @DbUnitDataSet(after = "ApplicationAdminFacadeTest.after.csv")
    void switchStatusTest() {
        applicationAdminFacade.switchStatus(APPLICATION_ID, ApplicationStatus.APPROVED);
    }
}
