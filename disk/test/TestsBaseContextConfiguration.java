package ru.yandex.chemodan.app.queller.test;

import org.springframework.context.annotation.Configuration;

import ru.yandex.chemodan.boot.ChemodanTestBaseContextConfiguration;
import ru.yandex.misc.version.SimpleAppName;

/**
 * @author dbrylev
 */
@Configuration
public class TestsBaseContextConfiguration extends ChemodanTestBaseContextConfiguration {
    @Override
    protected SimpleAppName appName() {
        return new SimpleAppName("disk", "queller");
    }
}
