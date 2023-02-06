package ru.yandex.direct.dbutil.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.JUnitSoftAssertions;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.scheduling.TaskScheduler;

import ru.yandex.direct.db.config.DbConfig;
import ru.yandex.direct.db.config.DbConfigException;
import ru.yandex.direct.db.config.DbConfigFactory;
import ru.yandex.direct.db.config.DbConfigListener;
import ru.yandex.direct.dbutil.configuration.DbUtilConfiguration;
import ru.yandex.direct.liveresource.LiveResource;
import ru.yandex.direct.liveresource.LiveResourceEvent;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.liveresource.LiveResourceWatcherFactory;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DbConfigFactoryTest {

    private static final String PATH = "classpath:///ru/yandex/direct/dbutil/config/";
    private static final String VALID_CONFIG = PATH + "db-config.json";
    private static final String REFRESHED_CONFIG = PATH + "db-config-refreshed.json";
    private static final String INVALID_CONFIG = PATH + "db-config-bad.json";
    private static final String PASSWORD_IN_FILE = "31337P455W0RD";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private static DbConfigFactory readConfig(LiveResource liveResource,
                                              LiveResourceWatcherFactory liveResourceWatcherFactory,
                                              Map<String, DbConfig> dbConfigOverrides) {
        return DbUtilConfiguration.createAndWatch(liveResource, liveResourceWatcherFactory, dbConfigOverrides);
    }

    private static Map<String, DbConfig> createDbConfigOverridesMap() {
        Map<String, DbConfig> overridesMap = new HashMap<>();

        DbConfig db = new DbConfig();
        db.setDb("defaultdb");
        db.setUser("test_user");
        db.setPass("testPass");
        overridesMap.put("test_db:from_override", db);

        return overridesMap;
    }

    private DbConfigFactory testDbConfigFactory;
    private LiveResource liveResource;
    private LiveResourceWatcherFactory liveResourceWatcherFactory;
    private String refreshedContent;

    @Before
    public void before() throws IOException {
        File passwordFile = temporaryFolder.newFile("password");
        File dbConfigFile = temporaryFolder.newFile("db-config.json");
        FileUtils.write(passwordFile, PASSWORD_IN_FILE, Charsets.UTF_8);
        LiveResource tmpLiveResource = LiveResourceFactory.get(VALID_CONFIG);
        FileUtils.write(dbConfigFile,
                tmpLiveResource.getContent().replaceAll("FILE_REPLACEMENT", passwordFile.getAbsolutePath()),
                Charsets.UTF_8);
        liveResource = LiveResourceFactory.get("file://" + dbConfigFile.getAbsolutePath());
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        liveResourceWatcherFactory = new LiveResourceWatcherFactory(taskScheduler);
        testDbConfigFactory = readConfig(liveResource, liveResourceWatcherFactory, createDbConfigOverridesMap());
        refreshedContent = LiveResourceFactory.get(REFRESHED_CONFIG).getContent();
    }

    @Test
    public void get_AllDefaults() {
        DbConfig config = testDbConfigFactory.get("db1");
        assertThat(config.getUser(), Matchers.is("default-user"));
        assertThat(config.getHosts(), Matchers.hasItems("default-host"));
    }

    @Test
    public void get_UserOverriddenAtDbLevel() {
        DbConfig config = testDbConfigFactory.get("db2");
        assertThat(config.getUser(), Matchers.is("db2-user"));
    }

    @Test
    public void get_WeightIsNullIfNotDefined() {
        DbConfig config = testDbConfigFactory.get("db2");
        assertNull(config.getWeight());
    }

    @Test
    public void get_WeightIsParsedCorrectly() {
        DbConfig config = testDbConfigFactory.get("db3");
        assertThat(config.getWeight(), Matchers.is(4));
    }

    @Test
    public void get_HostOverriddenAtDbLevel() {
        DbConfig config = testDbConfigFactory.get("db3");
        assertThat(config.getHosts(),
                Matchers.both(Matchers.hasItems("db3-host")).and(Matchers.not(Matchers.hasItems("default-host"))));
    }

    @Test
    public void get_UserNotOverriddenAtDbLevel() {
        DbConfig config = testDbConfigFactory.get("db3");
        assertThat(config.getUser(), Matchers.is("default-user"));
    }

    @Test
    public void get_HostNotOverriddenAtDbLevel() {
        DbConfig config = testDbConfigFactory.get("db2");
        assertThat(config.getHosts(), Matchers.hasItems("default-host"));
    }

    @Test
    public void get_UserOverriddenAtShardLevel() {
        DbConfig config = testDbConfigFactory.get("ppc:1");
        assertThat(config.getUser(), Matchers.is("ppc1-user"));
    }

    @Test
    public void get_UserOverriddenAtUnderscoreLevel() {
        DbConfig config = testDbConfigFactory.get("ppc:2");
        assertThat(config.getUser(), Matchers.is("ppc2-user"));
    }

    @Test
    public void get_UserOverriddenAtQualifierLevel() {
        DbConfig config = testDbConfigFactory.get("ppc:2:heavy");
        assertThat(config.getUser(), Matchers.is("ppc2-user-heavy"));
    }

    @Test(expected = DbConfigException.class)
    public void get_MissingDatabase_throwsException() {
        testDbConfigFactory.get("no-such-database");
    }

    @Test(expected = DbConfigException.class)
    public void get_MissingUnderscoreAtChilds_throwsException() {
        testDbConfigFactory.get("db4");
    }


    @Test(expected = DbConfigException.class)
    public void instantiate_InvalidResourceJson() {
        liveResource = LiveResourceFactory.get(INVALID_CONFIG);
        readConfig(liveResource, liveResourceWatcherFactory, emptyMap());
    }


    @Test
    public void getChildNames_SimpleNameAndNoChildUnderscore() {
        List<String> childNames = testDbConfigFactory.getChildNames("ppc");
        assertThat(childNames, Matchers.<List<String>>allOf(
                Matchers.hasItems("1", "2", "3", "not-a-shard"), Matchers.hasSize(4)));
    }

    @Test
    public void getChildNames_ComplexNameAndNoChildUnderscore() {
        List<String> childNames = testDbConfigFactory.getChildNames("ppc:3");
        assertThat(childNames, Matchers.<List<String>>allOf(Matchers.hasItems("heavy", "heavy2"), Matchers.hasSize(2)));
    }

    @Test
    public void getChildNames_ComplexNameAndChildUnderscore() {
        List<String> childNames = testDbConfigFactory.getChildNames("ppc:2");
        assertThat(childNames, Matchers.<List<String>>allOf(Matchers.hasItems("heavy"), Matchers.hasSize(1)));
    }


    @Test
    public void getShardNumbers_ShardsOnly() {
        List<Integer> shardNumbers = testDbConfigFactory.getShardNumbers("db4");
        assertThat(shardNumbers, Matchers.<List<Integer>>allOf(Matchers.hasItems(1, 2), Matchers.hasSize(2)));
    }

    @Test
    public void getShardNumbers_ShardsAndNotShards() {
        List<Integer> shardNumbers = testDbConfigFactory.getShardNumbers("ppc");
        assertThat(shardNumbers, Matchers.<List<Integer>>allOf(Matchers.hasItems(1, 2, 3), Matchers.hasSize(3)));
    }


    @Test
    public void update_ConfigRefreshed() {
        testDbConfigFactory.update(new LiveResourceEvent(refreshedContent));
        String refreshedUser = testDbConfigFactory.get("db2").getUser();
        assertThat("db config has not refreshed after resource refreshed", refreshedUser, Matchers.is("db2-user" +
                "-refreshed"));
    }

    @Test
    public void update_NotifiesListener() {
        DbConfigListener listener = mock(DbConfigListener.class);
        testDbConfigFactory.addListener(listener);
        testDbConfigFactory.update(new LiveResourceEvent(refreshedContent));
        verify(listener).update(any());
    }

    @Test
    public void get_PasswordFromFile() {
        String password = testDbConfigFactory.get("dbwithfilepass").getPass();
        softly.assertThat(password).isEqualTo(PASSWORD_IN_FILE);
    }

    @Test
    public void get_ExtraUsers() {
        DbConfig dbConfig = testDbConfigFactory.get("dbextrausers");
        softly.assertThat(dbConfig.getPass()).isEqualTo("default");
        softly.assertThat(dbConfig.getExtraUsers())
                .isEqualTo(ImmutableMap.<String, String>builder()
                        .put("user31337", PASSWORD_IN_FILE)
                        .put("dummyuser", "dummy")
                        .build());
    }

    @Test
    public void get_DatabaseFromOverrides() {
        DbConfig config = testDbConfigFactory.get("test_db:from_override");
        assertThat(config.getUser(), Matchers.is("test_user"));
        assertThat(config.getPass(), Matchers.is("testPass"));
        assertThat(config.getDb(), Matchers.is("defaultdb"));
    }
}
