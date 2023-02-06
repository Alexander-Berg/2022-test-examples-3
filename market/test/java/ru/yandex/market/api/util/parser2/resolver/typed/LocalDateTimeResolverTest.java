package ru.yandex.market.api.util.parser2.resolver.typed;

import java.time.LocalDateTime;

import org.junit.Test;

import ru.yandex.market.api.util.parser2.resolver.ResolverTestUtils;

/**
 * Created by fettsery on 13.06.18.
 */
public class LocalDateTimeResolverTest {

    private LocalDateTimeResolver resolver = new LocalDateTimeResolver();

    @Test
    public void ignoreNullValue() {
        ResolverTestUtils.assertIgnore(resolver, null);
    }

    @Test
    public void ignoreEmptyString() {
        ResolverTestUtils.assertIgnore(resolver, "");
    }

    @Test
    public void handleLocalDateTime() {
        ResolverTestUtils.assertResolve(resolver,
            "2018-06-06T10:00:00",
            LocalDateTime.of(2018, 6, 6,10, 0, 0));
    }

    @Test
    public void formatError() {
        ResolverTestUtils.assertExpectedFormatError(resolver, "trash", "YYYY-MM-DDThh:mm:ss");
    }
}
