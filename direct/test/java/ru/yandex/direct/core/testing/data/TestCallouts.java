package ru.yandex.direct.core.testing.data;

import java.time.LocalDateTime;

import javax.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.dbutil.model.ClientId;

public class TestCallouts {

    private TestCallouts() {
    }

    public static Callout defaultCallout(@Nullable ClientId clientId) {
        LocalDateTime now = LocalDateTime.now();
        return new Callout()
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withText(RandomStringUtils.randomAlphanumeric(5))
                .withCreateTime(now)
                .withLastChange(now)
                .withDeleted(false);
    }
}
