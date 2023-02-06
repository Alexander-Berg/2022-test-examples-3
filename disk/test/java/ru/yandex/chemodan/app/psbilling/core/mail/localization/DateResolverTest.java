package ru.yandex.chemodan.app.psbilling.core.mail.localization;

import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.inside.utils.Language;

public class DateResolverTest extends AbstractPsBillingCoreTest {

    @Autowired
    private DateResolver dateResolver;

    @Test
    public void testRussianDate() {
        Assert.assertEquals("18 июня 1991 года", dateResolver.getLocalizedMskDate(new Instant(677245970000L), Language.RUSSIAN));
    }
}
