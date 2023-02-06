package ru.yandex.chemodan.app.urlshortener.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.app.urlshortener.service.UrlShortenerManagerContextConfiguration;
import ru.yandex.chemodan.boot.ChemodanTestBaseContextConfiguration;
import ru.yandex.commune.dynproperties.DynamicProperty;
import ru.yandex.commune.dynproperties.DynamicPropertyManager;
import ru.yandex.commune.dynproperties.DynamicPropertyWatcher;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

/**
 * @author tolmalev
 */
@Configuration
@Import({
        UrlShortenerManagerContextConfiguration.class,
        UrlShortenerEmbeddedPgConfiguration.class,
})
public class TestBaseContextConfiguration extends ChemodanTestBaseContextConfiguration {
    @Bean
    @Override
    public AppName appName() {
        return new SimpleAppName("disk", "urlshortener");
    }

    @Bean
    public DynamicPropertyManager dynamicPropertyManager() {
        return new DynamicPropertyManager(null, null, null) {
            @Override
            public <T> void registerAndFireWatcher(DynamicProperty<T> property, DynamicPropertyWatcher<T> watcher) {
                watcher.updated(property.get());
            }
        };
    }
}
