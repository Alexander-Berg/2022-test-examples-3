package ru.yandex.chemodan.app.uaas.test;

import ru.yandex.chemodan.boot.ChemodanTestBaseContextConfiguration;
import ru.yandex.misc.version.SimpleAppName;

public class UaasTestContextConfiguration extends ChemodanTestBaseContextConfiguration {

    @Override
    protected SimpleAppName appName() {
        return new SimpleAppName("disk", "uaas");
    }
}
