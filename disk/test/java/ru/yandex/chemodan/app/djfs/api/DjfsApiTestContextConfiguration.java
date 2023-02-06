package ru.yandex.chemodan.app.djfs.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.commune.a3.action.result.ApplicationInfo;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

@Configuration
public class DjfsApiTestContextConfiguration extends DjfsApiBaseContextConfiguration {

    @Bean
    public AppName appName() {
        return new SimpleAppName("disk", "djfs-api");
    }

    @Bean
    public ApplicationInfo applicationInfo() {
        return ApplicationInfo.UNKNOWN;
    }
}
