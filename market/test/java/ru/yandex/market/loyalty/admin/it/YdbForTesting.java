package ru.yandex.market.loyalty.admin.it;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.loyalty.admin.config.ITConfig;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.notifications.NotificationType;
import ru.yandex.market.loyalty.core.dao.ydb.NotificationDao;
import ru.yandex.market.loyalty.core.dao.ydb.UserBlockPromoDao;
import ru.yandex.market.loyalty.core.dao.ydb.UserPromoDao;
import ru.yandex.market.loyalty.core.dao.ydb.YdbPersonalPromoPerksDao;
import ru.yandex.market.loyalty.core.dao.ydb.YdbUserPromoDao;
import ru.yandex.market.loyalty.core.dao.ydb.model.UserBlockPromo;
import ru.yandex.market.loyalty.core.dao.ydb.model.UserPromo;
import ru.yandex.market.loyalty.core.model.notification.Notification;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

/**
 * @author : poluektov
 * date: 2019-09-09.
 * <p>
 * Перед запуском тыкни токен из секретницы в it.properties
 */
@Ignore("this test suite should be run manually because it uses real YDB")
@ContextConfiguration(classes = ITConfig.class)
@RunWith(LoyaltySpringTestRunner.class)
@WebAppConfiguration
public class YdbForTesting {
    @Autowired
    private UserPromoDao userPromoDao;
    @Autowired
    private YdbPersonalPromoPerksDao ydbPersonalPromoPerksDao;
    @Autowired
    private NotificationDao notificationDao;
    @Autowired
    private UserBlockPromoDao userBlockPromoDao;

    private String environmentBefore;

    @Before
    public void setUp() {
        environmentBefore = System.getProperty("environment");
        System.setProperty("environment", "testing");
    }

    @After
    public void cleanUp() {
        if (environmentBefore != null) {
            System.setProperty("environment", environmentBefore);
        } else {
            System.clearProperty("environment");
        }
    }

    @Test
    public void testGetPromo() {
        UserPromo result = userPromoDao.selectByUid(123L).get(0);
        assertThat(result.getUid(), equalTo(123L));
        assertThat(result.getActionId(), equalTo("actionIdForIT"));
        assertThat(result.getUpdatedAt(), notNullValue());
        assertThat(result.getPromoType(), equalTo(PromoType.SECRET_SALE));
    }

    //    @Test
    public void testInsertPromo() {
        YdbUserPromoDao.InsertUserPromoRow userPromo = new YdbUserPromoDao.InsertUserPromoRow(
                123L,
                "actionIdForIT",
                PromoType.SECRET_SALE
        );
        userPromoDao.insertPromo(userPromo);
    }

    @Test
    public void shouldInsertAndGetPerks() {
        long uid = 10000L;
        ydbPersonalPromoPerksDao.upsertPersonalPerks(uid, Set.of("testPerk1,testPerk2,testPerk3"));
        assertThat(ydbPersonalPromoPerksDao.getPersonalPerks(uid), hasSize(3));
    }

    @Test
    public void testInsertAndFindNotification() {
        var uuid = UUID.randomUUID().toString();
        notificationDao.insertNotification(
                new Notification(
                        uuid,
                        DEFAULT_UID,
                        NotificationType.REFERRAL_ACCRUAL,
                        Instant.now(),
                        Instant.now().plus(1, ChronoUnit.DAYS),
                        "payload",
                        false)
        );

        List<Notification> notifications = notificationDao.findNotifications(DEFAULT_UID);

        assertFalse(notifications.isEmpty());
        assertEquals(notifications.get(0).getUid(), DEFAULT_UID);
        assertSame(notifications.get(0).getType(), NotificationType.REFERRAL_ACCRUAL);
    }

    @Test
    public void shouldUpsertAndGetBlockPromos() {
        long uid = 10000L;
        String thresholdName = "blockpromo_ASFFQ12_2";
        userBlockPromoDao.upsertUserBlockPromo(uid, thresholdName, true);

        List<UserBlockPromo> userBlockedPromo = userBlockPromoDao.getUserBlockedPromo(uid);
        assertThat(userBlockedPromo, hasSize(1));
        assertThat(userBlockedPromo.get(0), allOf(
                hasProperty("uid", equalTo(uid)),
                hasProperty("thresholdName", equalTo(thresholdName)),
                hasProperty("enabled", equalTo(true))
        ));

        userBlockPromoDao.upsertUserBlockPromo(uid, thresholdName, false);

        userBlockedPromo = userBlockPromoDao.getUserBlockedPromo(uid);
        assertThat(userBlockedPromo, hasSize(1));
        assertThat(userBlockedPromo.get(0), allOf(
                hasProperty("uid", equalTo(uid)),
                hasProperty("thresholdName", equalTo(thresholdName)),
                hasProperty("enabled", equalTo(false))
        ));
    }
}
