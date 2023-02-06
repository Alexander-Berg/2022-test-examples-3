package ru.yandex.chemodan.app.notifier.admin.dao.test;

import ru.yandex.chemodan.boot.ChemodanTestBaseContextConfiguration;
import ru.yandex.misc.version.SimpleAppName;

/**
 * @author akirakozov
 */
@ImportNotificationEmbeddedPg
public class NotifierJdbcDaoTestsContextConfiguration extends ChemodanTestBaseContextConfiguration {
    @Override
    protected SimpleAppName appName() {
        return new SimpleAppName("disk", "notifier");
    }
}
