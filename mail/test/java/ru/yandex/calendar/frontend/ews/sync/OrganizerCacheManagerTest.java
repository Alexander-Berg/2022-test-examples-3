package ru.yandex.calendar.frontend.ews.sync;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class OrganizerCacheManagerTest extends AbstractConfTest {
    @Autowired
    private OrganizerCacheDao organizerCacheDao;
    @Autowired
    private OrganizerCacheManager organizerCacheManager;

    @Test
    public void noRecordFound() {
        String exchangeId = "012345ABCDF0==";
        Assert.none(organizerCacheDao.findOrganizerEmailByExchangeId(exchangeId));
    }

    @Test
    public void insert() {
        String exchangeId = "012345ABCDF1==";
        Email expectedOrgEmail = new Email("organizer.email@fakehost.com");
        organizerCacheManager.storeOrganizerCacheSafe(exchangeId, expectedOrgEmail);
        organizerCacheManager.storeOrganizerCacheSafe(exchangeId, expectedOrgEmail);
        final Option<Email> actualOrgEmail = organizerCacheDao.findOrganizerEmailByExchangeId(exchangeId);
        Assert.assertTrue(actualOrgEmail.get().equalsIgnoreCase(expectedOrgEmail));
    }

}
