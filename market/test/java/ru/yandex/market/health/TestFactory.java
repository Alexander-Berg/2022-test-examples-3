package ru.yandex.market.health;

import java.util.List;

import ru.yandex.market.health.context.HttpEndPointAlertContext;
import ru.yandex.market.health.factories.AbstractAlertFactory;

public class TestFactory extends AbstractAlertFactory {

    public static final String SOLOMON_SELECTOR = "QUERY_SELECTOR";

    public TestFactory(String project, String service, long periodMillis, List<String> notificationChannels) {
        super(project, service, periodMillis, notificationChannels);
    }

    @Override
    protected String getSolomonSelector(HttpEndPointAlertContext context) {
        return SOLOMON_SELECTOR;
    }
}
