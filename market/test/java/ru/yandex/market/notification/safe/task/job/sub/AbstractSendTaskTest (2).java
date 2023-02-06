package ru.yandex.market.notification.safe.task.job.sub;

import org.junit.jupiter.api.Disabled;

import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.PersistentNotificationAddress;
import ru.yandex.market.notification.safe.model.vo.SendResultInfo;
import ru.yandex.market.notification.safe.service.facade.PersistentNotificationFacade;
import ru.yandex.market.notification.service.registry.NotificationFacadeRegistry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

/**
 * Базовый класс для unit-тестов задач отправки нотификаций.
 *
 * @author Vladislav Bauer
 */
@Disabled
abstract class AbstractSendTaskTest {

    PersistentNotificationAddress createAddress() {
        return mock(PersistentNotificationAddress.class);
    }

    PersistentNotification createNotification() {
        return mock(PersistentNotification.class);
    }

    PersistentNotificationFacade createFacade() {
        return mock(PersistentNotificationFacade.class);
    }

    NotificationFacadeRegistry createRegistry() {
        return mock(NotificationFacadeRegistry.class);
    }

    void checkErrorResultInfo(final SendResultInfo resultInfo) {
        assertThat(resultInfo, notNullValue());
        assertThat(resultInfo.hasError(), equalTo(true));
        assertThat(resultInfo.getErrorMessages(), hasSize(1));
        assertThat(resultInfo.getAllErrors(), hasSize(1));
        assertThat(resultInfo.getResults(), empty());
    }

}
