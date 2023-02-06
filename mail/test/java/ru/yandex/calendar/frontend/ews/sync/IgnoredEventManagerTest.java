package ru.yandex.calendar.frontend.ews.sync;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class IgnoredEventManagerTest extends AbstractConfTest {
    @Autowired
    private IgnoredEventDao ignoredEventDao;
    @Autowired
    private IgnoredEventManager ignoredEventManager;

    @Test
    public void insert() {
        String exchangeId = "012345ABCDEF==";
        ignoredEventManager.storeIgnoredEventSafe(exchangeId, IgnoreReason.CANCELED_EVENT_NOT_FOUND, Option.<String>empty());
        ignoredEventManager.storeIgnoredEventSafe(exchangeId, IgnoreReason.UNSUPPORTED_REPETITION, Option.<String>of("Rep=..."));
        ignoredEventManager.storeIgnoredEventSafe(exchangeId, IgnoreReason.BAD_ORGANIZER_EMAIL, Option.<String>of("@email@"));
        Assert.assertTrue(ignoredEventDao.findIgnoredEventByExchangeId(exchangeId).isPresent());
    }

}
