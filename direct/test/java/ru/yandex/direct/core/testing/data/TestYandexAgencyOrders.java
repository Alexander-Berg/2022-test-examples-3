package ru.yandex.direct.core.testing.data;

import java.time.LocalDateTime;

import javax.annotation.Nullable;

import ru.yandex.direct.core.entity.yandexagencyorder.model.Status;
import ru.yandex.direct.core.entity.yandexagencyorder.model.YandexAgencyOrder;
import ru.yandex.direct.dbutil.model.ClientId;

public final class TestYandexAgencyOrders {
    private TestYandexAgencyOrders() {
    }

    public static YandexAgencyOrder defaultYandexAgencyOrder(@Nullable ClientId clientId, Long id) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        return new YandexAgencyOrder()
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withId(id)
                .withCreated(now)
                .withLastChange(now)
                .withProductType(177L)
                .withYaOrderStatus(Status.NEW);
    }
}
