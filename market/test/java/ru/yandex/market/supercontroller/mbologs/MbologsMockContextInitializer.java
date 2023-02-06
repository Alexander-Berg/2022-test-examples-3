package ru.yandex.market.supercontroller.mbologs;

import ru.yandex.market.mbo.utils.test.MockContextInitializer;

import java.util.Arrays;
import java.util.List;

/**
 * @author amaslak
 */
public class MbologsMockContextInitializer extends MockContextInitializer {

    @Override
    protected List<String> getExtraProperties() {
        return Arrays.asList(
            "/mbo-logs/test/mbo-logs-test.properties",
            "/mbo-logs/test/datasources.properties"
        );
    }
}
