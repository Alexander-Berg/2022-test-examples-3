package ru.yandex.calendar.frontend.caldav.ban;

import org.joda.time.Duration;
import org.junit.Test;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.lang.Check;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author gutman
 */
public class CaldavBanManagerTestLong {
    @Test
    public void test() throws InterruptedException {
        ListF<User> users = Cf.list(
                new User(PassportUid.cons(1), new AtomicLong(100)),
                new User(PassportUid.cons(2), new AtomicLong(200)),
                new User(PassportUid.cons(3), new AtomicLong(300)),
                new User(PassportUid.cons(4), new AtomicLong(400)),
                new User(PassportUid.cons(5), new AtomicLong(500)),
                new User(PassportUid.cons(6), new AtomicLong(600)),
                new User(PassportUid.cons(7), new AtomicLong(700))
        );

        Duration banUserInterval = Duration.standardSeconds(6);
        Duration requestsHistoryInterval = Duration.standardSeconds(3);
        Duration sleepBetweenBanChecks = Duration.standardSeconds(3);

        long start = System.currentTimeMillis();
        CaldavBanManager caldavBanManager = new CaldavBanManager(
                banUserInterval, requestsHistoryInterval, sleepBetweenBanChecks, 1000, 501);
        caldavBanManager.init();

        int threads = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);
        
        for (int i = 0; i < threads; i++) {
            new DosThread(caldavBanManager, users.unique(), startLatch, finishLatch).start();
        }
        startLatch.countDown();
        finishLatch.await();

        long elapsed = System.currentTimeMillis() - start;
        Check.isTrue(elapsed < sleepBetweenBanChecks.getMillis());
        ThreadUtils.sleep(sleepBetweenBanChecks.getMillis()); // wait for ban

        Assert.A.equals(UserBanStatus.NOT_BANNED, caldavBanManager.handleUserRequest(PassportUid.cons(1)));
        Assert.A.equals(UserBanStatus.NOT_BANNED, caldavBanManager.handleUserRequest(PassportUid.cons(2)));
        Assert.A.equals(UserBanStatus.NOT_BANNED, caldavBanManager.handleUserRequest(PassportUid.cons(3)));
        Assert.A.equals(UserBanStatus.NOT_BANNED, caldavBanManager.handleUserRequest(PassportUid.cons(4)));
        Assert.A.equals(UserBanStatus.NOT_BANNED, caldavBanManager.handleUserRequest(PassportUid.cons(5)));
        Assert.A.equals(UserBanStatus.BANNED, caldavBanManager.handleUserRequest(PassportUid.cons(6)));
        Assert.A.equals(UserBanStatus.BANNED, caldavBanManager.handleUserRequest(PassportUid.cons(7)));
        
        elapsed = System.currentTimeMillis() - start;
        Check.isTrue(elapsed < banUserInterval.getMillis());
        ThreadUtils.sleep(banUserInterval.plus(sleepBetweenBanChecks)); // wait for ban timeout and next ban check

        Assert.A.equals(UserBanStatus.NOT_BANNED, caldavBanManager.handleUserRequest(PassportUid.cons(6)));
        Assert.A.equals(UserBanStatus.NOT_BANNED, caldavBanManager.handleUserRequest(PassportUid.cons(7)));

        caldavBanManager.destroy();
    }
    
    private static class User {
        private final PassportUid uid;
        private final AtomicLong requestsCounter;

        private User(PassportUid uid, AtomicLong requestsCounter) {
            this.uid = uid;
            this.requestsCounter = requestsCounter;
        }
    }

    private class DosThread extends Thread {
        private final CaldavBanManager caldavBanManager;
        private final SetF<User> users;
        private final CountDownLatch startLatch;
        private final CountDownLatch finishLatch;

        public DosThread(
                CaldavBanManager caldavBanManager, SetF<User> users, CountDownLatch startLatch, CountDownLatch finishLatch)
        {
            this.caldavBanManager = caldavBanManager;
            this.users = Cf.toHashSet(users);
            this.startLatch = startLatch;
            this.finishLatch = finishLatch;
        }

        @Override
        public void run() {
            try {
                startLatch.await();
            } catch (InterruptedException e) {}
            while (true) {
                try {
                    User user = Random2.R.randomElement(users.toList());
                    if (user.requestsCounter.decrementAndGet() > 0) {
                        caldavBanManager.handleUserRequest(user.uid);
                    } else {
                        users.removeTs(user);
                    }
                    ThreadUtils.sleep(10);
                } catch (IllegalArgumentException e) {
                    finishLatch.countDown();
                    return;
                }
            }
        }
    }

}
