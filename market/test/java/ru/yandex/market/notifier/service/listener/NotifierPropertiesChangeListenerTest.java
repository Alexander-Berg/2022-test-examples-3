package ru.yandex.market.notifier.service.listener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.NotifierPropertiesImpl;
import ru.yandex.market.notifier.service.NotifierPropertiesHolder;
import ru.yandex.market.notifier.trace.NotifierContextHolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NotifierPropertiesChangeListenerTest extends AbstractServicesTestBase {

    @Autowired
    private NotifierPropertiesHolder notifierPropertiesHolder;

    @Test
    void shouldSetValuesOnStartup() {
        assertThat(NotifierContextHolder.needToMinifyLogs(), is(true));
    }

    @Test
    void shouldUpdateValues() {
        notifierPropertiesHolder.updateValue(NotifierPropertiesImpl.MINIFY_OUTPUT_LOGS, "false");
        assertThat(NotifierContextHolder.needToMinifyLogs(), is(false));
    }
}
