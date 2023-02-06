package ru.yandex.market.partner.notification.migration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;


public class TemplateMigrationChangeTest extends AbstractFunctionalTest {

    @Test
    @Disabled("Fix dbunit and lqb config")
    @DbUnitDataSet(after = "templateMigrationChange.after.csv")
    public void validateImport() {

    }
}
