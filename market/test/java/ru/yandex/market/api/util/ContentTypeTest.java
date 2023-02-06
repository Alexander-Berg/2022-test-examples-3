package ru.yandex.market.api.util;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.http.ContentType;

/**
 * @author dimkarp93
 */
public class ContentTypeTest extends UnitTestBase {
    @Test
    public void withCharset() {
        Assert.assertEquals(
            "application/json; charset=utf-8",
            ContentType.JSON.withCharset("utf-8").getValue()
        );
    }
}
