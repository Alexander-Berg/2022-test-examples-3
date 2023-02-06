package ru.yandex.chemodan.app.djfs.migrator.integration;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.chemodan.app.djfs.core.client.ActivateInMemoryClient;
import ru.yandex.chemodan.app.djfs.core.db.mongo.ActivateInMemoryMongo;
import ru.yandex.chemodan.app.djfs.core.db.pg.ActivateRealPg;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.migrator.DjfsMigrationState;
import ru.yandex.chemodan.app.djfs.migrator.DjfsMigrator;
import ru.yandex.chemodan.app.djfs.migrator.migrations.DjfsMigrationPlan;
import ru.yandex.chemodan.util.AppNameHolder;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.version.SimpleAppName;


@Ignore("not use on CI, only for testing by hand")
@ContextConfiguration(classes = {
        DjfsMigratorIntegrationTestConfig.class
})
@ActiveProfiles({
        ActivateInMemoryClient.PROFILE,
        ActivateInMemoryMongo.PROFILE,
        ActivateRealPg.PROFILE,
})
@RunWith(JUnit4.class)
public class DjfsMigratorIntegrationTest extends AbstractTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @BeforeClass
    public static void setAppName() {
        AppNameHolder.setIfNotPresent(new SimpleAppName("disk", "djfs-migrator"));
    }

    @Autowired
    private DjfsMigrator migrator;

    @Test
    public void migrateGnefedev() {
        DjfsMigrator.CopyResult result = migrator.copyDataAndSwitchShard(DjfsUid.cons(4012031284L), 42, 41, false,
                DjfsMigrationPlan.allTablesMigrations);
        Assert.equals(DjfsMigrationState.COPY_SUCCESS, result.getState(), result.getMessage());
    }

}
