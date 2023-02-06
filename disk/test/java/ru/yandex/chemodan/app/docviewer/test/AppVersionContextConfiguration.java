package ru.yandex.chemodan.app.docviewer.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.chemodan.boot.DiskAppVersion;
import ru.yandex.misc.version.Version;

/**
 * @author messiahlap
 */
@Configuration
public class AppVersionContextConfiguration {

    @Bean
    public Version verion() {
        return DiskAppVersion.VERSION;
    }
}
