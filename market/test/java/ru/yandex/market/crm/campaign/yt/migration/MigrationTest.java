package ru.yandex.market.crm.campaign.yt.migration;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MigrationTest {
    @Test
    public void getIdForOldMigration() {
        String actual = Migration.getId("Migration__add_crypta_gender_users_table");
        Assert.assertEquals("add_crypta_gender_users_table", actual);
    }

    @Test
    public void getIdForNewMigration() {
        String actual = Migration.getId("MigrationUpdateSubTypesTableAddNewJournalTypes");
        Assert.assertEquals("UpdateSubTypesTableAddNewJournalTypes", actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getIdForInvalidMigrationThrowsException1() {
        Migration.getId("__Migration");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getIdForInvalidMigrationThrowsException2() {
        Migration.getId("__M");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getIdForInvalidMigrationThrowsException3() {
        Migration.getId("testMigrationTest");
    }
}
