package ru.yandex.chemodan.app.fotki.dao.test;

import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.app.fotki.dao.configuration.FotkiJdbcContextConfiguration;
import ru.yandex.chemodan.boot.ChemodanTestBaseContextConfiguration;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.version.SimpleAppName;

@Import(FotkiJdbcContextConfiguration.class)
@ImportEmbeddedPg
public class FotkiTestJdbcContextConfiguration extends ChemodanTestBaseContextConfiguration {

    @Override
    protected SimpleAppName appName() {
        return new SimpleAppName("disk", "fotki");
    }
}

