package ru.yandex.calendar.frontend.ews.subscriber;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.YtEwsSubscriptionDao;
import ru.yandex.calendar.logic.user.TestUsers;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.test.generic.CalendarSpringJUnit4ClassRunner;
import ru.yandex.calendar.util.idlent.YandexUser;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
@RunWith(CalendarSpringJUnit4ClassRunner.class)
public class ExchangeSubscriberTest extends AbstractConfTest {
    @Autowired
    private ExchangeSubscriber exchangeSubscriber;
    @Autowired
    private YtEwsSubscriptionDao ytEwsSubscriptionDao;
    @Autowired
    private TestManager testManager;

    @Test
    public void subscribtion() {
        testManager.prepareYandexUser(new YandexUser(
                TestUsers.AKIRAKOZOV, PassportLogin.cons(TestManager.testExchangeUserEmail.getLocalPart()),
                Option.empty(), Option.of(TestManager.testExchangeUserEmail), Option.empty(), Option.empty(), Option.empty()));

        ytEwsSubscriptionDao.removeUserSubscription(TestUsers.AKIRAKOZOV);
        Assert.none(ytEwsSubscriptionDao.findUserSubscription(TestUsers.AKIRAKOZOV));
        exchangeSubscriber.subscribeUser(TestUsers.AKIRAKOZOV);
        Assert.some(ytEwsSubscriptionDao.findUserSubscription(TestUsers.AKIRAKOZOV));
    }

    @Ignore
    @Test
    public void resubscribeExpired() {
        exchangeSubscriber.resubscribeExpired();
    }
}
