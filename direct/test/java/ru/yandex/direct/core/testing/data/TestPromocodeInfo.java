package ru.yandex.direct.core.testing.data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.promocodes.model.PromocodeInfo;

import static org.apache.commons.lang3.RandomUtils.nextLong;

public class TestPromocodeInfo {

    public static PromocodeInfo createPromocodeInfo() {
        return new PromocodeInfo()
                .withId(nextLong(0L, Integer.MAX_VALUE))
                .withCode(RandomStringUtils.randomAlphanumeric(10))
                .withInvoiceEnabledAt(LocalDateTime.now().minusHours(3).truncatedTo(ChronoUnit.SECONDS))
                .withInvoiceId(RandomUtils.nextLong())
                .withInvoiceExternalId("Б-" + RandomUtils.nextInt() + "-" + RandomUtils.nextInt(400));
    }
}
