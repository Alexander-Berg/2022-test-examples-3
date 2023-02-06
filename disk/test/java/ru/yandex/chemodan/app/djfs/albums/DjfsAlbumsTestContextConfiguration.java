package ru.yandex.chemodan.app.djfs.albums;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.chemodan.boot.ChemodanPropertiesLoadStrategy;
import ru.yandex.chemodan.queller.worker.CeleryTaskManager;
import ru.yandex.chemodan.util.jdbc.JdbcDatabaseConfigurator;
import ru.yandex.chemodan.zk.configuration.ZkEmbedded;
import ru.yandex.commune.a3.action.result.ApplicationInfo;
import ru.yandex.commune.zk2.ZkConfiguration;
import ru.yandex.commune.zk2.ZkPath;
import ru.yandex.misc.net.HostnameUtils;
import ru.yandex.misc.property.PropertiesHolder;
import ru.yandex.misc.property.eval.PropertyPlaceholderConfigurer2;
import ru.yandex.misc.property.load.PropertiesLoader;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

import static org.mockito.Mockito.mock;

@Configuration
public class DjfsAlbumsTestContextConfiguration extends DjfsAlbumsBaseContextConfiguration {

    @Bean
    public static AppName appName() {
        return new SimpleAppName("disk", "djfs-albums");
    }

    @Bean
    public static ApplicationInfo applicationInfo() {
        return ApplicationInfo.UNKNOWN;
    }

    @Bean
    public static JdbcDatabaseConfigurator jdbcDatabaseConfigurator() {
        return mock(JdbcDatabaseConfigurator.class);
    }

    @Bean
    public PropertyPlaceholderConfigurer2 propertyPlaceholderConfigurer2() {
        PropertyPlaceholderConfigurer2 configurer2 = new PropertyPlaceholderConfigurer2();
        PropertiesLoader.initialize(new ChemodanPropertiesLoadStrategy(appName(), true));
        configurer2.setProperties(PropertiesHolder.properties());
        return configurer2;
    }

    @Bean
    public ZkPath zkRoot(ZkConfiguration configuration) {
        return new ZkPath("/djfs-"
                + HostnameUtils.localHostname()
                + "_" + Random2.R.nextAlnum(5));
    }

    @Bean
    public ZkConfiguration zkConfiguration(ZkEmbedded zkEmbedded) {
        return zkEmbedded.getConfiguration();
    }

    @Bean
    public ZkEmbedded zkEmbedded() throws Exception {
        return new ZkEmbedded();
    }

    @Bean
    @Primary
    public CeleryTaskManager celeryTaskManager() {
        return mock(CeleryTaskManager.class);
    }

}
