package ru.yandex.market.tvmrules;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.passport.tvmauth.BlackboxEnv;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.CheckedUserTicket;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.passport.tvmauth.TicketStatus;
import ru.yandex.passport.tvmauth.TvmClient;

public class TvmRulesClientTest {

    private static final String SERVICE_NAME = "market_tsum-ui";

    @Test
    public void emptyDestinationsTest() {
        final TvmSettings tvmSettings = new TvmSettings(1111,
                "secret",
                BlackboxEnv.TEST_YATEAM,
                "production",
                SERVICE_NAME
        );
        try (AbstractTvmRulesClient client = new AbstractTvmRulesClient(tvmSettings) {

            @Override
            protected TvmClient initTvmClient(TvmSettings tvmSettings, int[] destinations) {
                return new TestTvmClient(destinations);
            }

            @Override
            protected TvmRulesInfo fetchTvmRulesInfo() {
                return new TvmRulesInfo();
            }
        }) {
            client.start();
            Assertions.assertThrows(RuntimeException.class, () -> client.getServiceTicketFor(345534),
                    TestTvmClient.NO_SUCH_DESTINATION_ERROR);
        }
    }

    @Test
    public void fetchedDestinationsTest() {
        final TvmSettings tvmSettings = new TvmSettings(1111,
                "secret",
                BlackboxEnv.TEST_YATEAM,
                "production",
                SERVICE_NAME
        );
        final String dstServiceName = "dstServiceName";
        final int dstServiceId = 543535;
        try (AbstractTvmRulesClient client = new AbstractTvmRulesClient(tvmSettings) {

            @Override
            protected TvmClient initTvmClient(TvmSettings tvmSettings, int[] destinations) {
                return new TestTvmClient(destinations);
            }

            @Override
            protected TvmRulesInfo fetchTvmRulesInfo() {
                final TvmRule tvmRule = new TvmRule();
                tvmRule.setSrc(SERVICE_NAME);
                tvmRule.setDst(dstServiceName);

                final TvmRulesInfo tvmRulesInfo = new TvmRulesInfo();
                tvmRulesInfo.setRules(List.of(tvmRule));
                tvmRulesInfo.setServiceIds(Map.of(
                        dstServiceName, dstServiceId
                ));
                return tvmRulesInfo;
            }
        }) {
            client.start();
            Assertions.assertEquals(TestTvmClient.DEFAULT_SERVICE_TICKET, client.getServiceTicketFor(dstServiceId));
        }
    }

    @Test
    public void fetchRulesPeriodicallyTest() {
        final TvmSettings tvmSettings = new TvmSettings(1111,
                "secret",
                BlackboxEnv.TEST_YATEAM,
                "production",
                SERVICE_NAME
        );
        // Should be finished in about 1 sec, we gave 5 sec
        tvmSettings.setRulesUpdateFrequencyMillis(100);
        final CountDownLatch latch = new CountDownLatch(10);
        try (AbstractTvmRulesClient client = new AbstractTvmRulesClient(tvmSettings) {

            @Override
            protected TvmClient initTvmClient(TvmSettings tvmSettings, int[] destinations) {
                return new TestTvmClient(destinations);
            }

            @Override
            protected TvmRulesInfo fetchTvmRulesInfo() {
                latch.countDown();
                return new TvmRulesInfo();
            }
        }) {
            client.start();
            try {
                Assertions.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                Assertions.fail();
            }
        }
    }

    @Test
    public void reInitClientOnNewDestinationsTest() {
        final TvmSettings tvmSettings = new TvmSettings(1111,
                "secret",
                BlackboxEnv.TEST_YATEAM,
                "production",
                SERVICE_NAME
        );
        tvmSettings.setRulesUpdateFrequencyMillis(100);
        final CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger dstChangeCount = new AtomicInteger();
        AtomicInteger clientInitCount = new AtomicInteger();
        try (AbstractTvmRulesClient client = new AbstractTvmRulesClient(tvmSettings) {

            @Override
            protected TvmClient initTvmClient(TvmSettings tvmSettings, int[] destinations) {
                clientInitCount.getAndIncrement();
                latch.countDown();
                return new TestTvmClient(destinations);
            }

            @Override
            protected TvmRulesInfo fetchTvmRulesInfo() {
                final TvmRule tvmRule = new TvmRule();
                int currCounter = dstChangeCount.getAndIncrement();
                String currName = "dstServiceName" + currCounter;
                tvmRule.setSrc(SERVICE_NAME);
                tvmRule.setDst(currName);

                final TvmRulesInfo tvmRulesInfo = new TvmRulesInfo();
                tvmRulesInfo.setRules(List.of(tvmRule));
                tvmRulesInfo.setServiceIds(Map.of(
                        currName, currCounter
                ));
                return tvmRulesInfo;
            }
        }) {
            client.start();
            try {
                Assertions.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
                Assertions.assertEquals(clientInitCount.get(), dstChangeCount.get());
            } catch (InterruptedException e) {
                Assertions.fail();
            }
        }
    }

    @Test
    public void dontReInitClientWhenDstNotChangedTest() {
        final TvmSettings tvmSettings = new TvmSettings(1111,
                "secret",
                BlackboxEnv.TEST_YATEAM,
                "production",
                SERVICE_NAME
        );
        tvmSettings.setRulesUpdateFrequencyMillis(100);
        final CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger clientInitCount = new AtomicInteger();
        try (AbstractTvmRulesClient client = new AbstractTvmRulesClient(tvmSettings) {

            @Override
            protected TvmClient initTvmClient(TvmSettings tvmSettings, int[] destinations) {
                clientInitCount.getAndIncrement();
                return new TestTvmClient(destinations);
            }

            @Override
            protected TvmRulesInfo fetchTvmRulesInfo() {
                final TvmRule tvmRule = new TvmRule();
                String currName = "dstServiceName";
                tvmRule.setSrc(SERVICE_NAME);
                tvmRule.setDst(currName);

                final TvmRulesInfo tvmRulesInfo = new TvmRulesInfo();
                tvmRulesInfo.setRules(List.of(tvmRule));
                tvmRulesInfo.setServiceIds(Map.of(
                        currName, 1
                ));
                latch.countDown();
                return tvmRulesInfo;
            }
        }) {
            client.start();
            try {
                Assertions.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
                Assertions.assertEquals(1 /*only initial*/, clientInitCount.get());
            } catch (InterruptedException e) {
                Assertions.fail();
            }
        }
    }

    @Test
    public void validServiceTicketCheckTest() {
        final TvmSettings tvmSettings = new TvmSettings(1111,
                "secret",
                BlackboxEnv.TEST_YATEAM,
                "production",
                SERVICE_NAME
        );
        final String srcServiceName = "srcServiceName";
        final int srcServiceId = 543535;
        try (AbstractTvmRulesClient client = new AbstractTvmRulesClient(tvmSettings) {

            @Override
            protected TvmClient initTvmClient(TvmSettings tvmSettings, int[] destinations) {
                return new TestTvmClient(destinations);
            }

            @Override
            protected TvmRulesInfo fetchTvmRulesInfo() {
                final TvmRule tvmRule = new TvmRule();
                tvmRule.setSrc(srcServiceName);
                tvmRule.setDst(SERVICE_NAME);

                final TvmRulesInfo tvmRulesInfo = new TvmRulesInfo();
                tvmRulesInfo.setRules(List.of(tvmRule));
                tvmRulesInfo.setServiceIds(Map.of(
                        srcServiceName, srcServiceId
                ));
                return tvmRulesInfo;
            }
        }) {
            client.start();
            final ServiceTicketCheckResult ticketCheckResult = client.checkServiceTicketWithSources(
                    TestTvmClient.generateServiceTicket(TicketStatus.OK, "debug", srcServiceId, 435)
            );
            Assertions.assertEquals(ServiceTicketCheckStatus.OK, ticketCheckResult.getCheckStatus());
            Assertions.assertNotNull(ticketCheckResult.getCheckedTicket());
        }
    }

    @Test
    public void invalidServiceTicketCheckTest() {
        final TvmSettings tvmSettings = new TvmSettings(1111,
                "secret",
                BlackboxEnv.TEST_YATEAM,
                "production",
                SERVICE_NAME
        );
        final String srcServiceName = "srcServiceName";
        final int srcServiceId = 543535;
        try (AbstractTvmRulesClient client = new AbstractTvmRulesClient(tvmSettings) {

            @Override
            protected TvmClient initTvmClient(TvmSettings tvmSettings, int[] destinations) {
                return new TestTvmClient(destinations);
            }

            @Override
            protected TvmRulesInfo fetchTvmRulesInfo() {
                final TvmRule tvmRule = new TvmRule();
                tvmRule.setSrc(srcServiceName);
                tvmRule.setDst(SERVICE_NAME);

                final TvmRulesInfo tvmRulesInfo = new TvmRulesInfo();
                tvmRulesInfo.setRules(List.of(tvmRule));
                tvmRulesInfo.setServiceIds(Map.of(
                        srcServiceName, srcServiceId
                ));
                return tvmRulesInfo;
            }
        }) {
            client.start();
            final ServiceTicketCheckResult ticketCheckResult = client.checkServiceTicketWithSources(
                    TestTvmClient.generateServiceTicket(TicketStatus.SIGN_BROKEN, "debug", srcServiceId, 435)
            );
            Assertions.assertEquals(ServiceTicketCheckStatus.INVALID_TICKET, ticketCheckResult.getCheckStatus());
            Assertions.assertNotNull(ticketCheckResult.getCheckedTicket());
        }
    }

    @Test
    public void invalidSourceTest() {
        final TvmSettings tvmSettings = new TvmSettings(1111,
                "secret",
                BlackboxEnv.TEST_YATEAM,
                "production",
                SERVICE_NAME
        );
        final int srcServiceId = 543535;
        try (AbstractTvmRulesClient client = new AbstractTvmRulesClient(tvmSettings) {

            @Override
            protected TvmClient initTvmClient(TvmSettings tvmSettings, int[] destinations) {
                return new TestTvmClient(destinations);
            }

            @Override
            protected TvmRulesInfo fetchTvmRulesInfo() {
                return new TvmRulesInfo();
            }
        }) {
            client.start();
            final ServiceTicketCheckResult ticketCheckResult = client.checkServiceTicketWithSources(
                    TestTvmClient.generateServiceTicket(TicketStatus.OK, "debug", srcServiceId, 435)
            );
            Assertions.assertEquals(ServiceTicketCheckStatus.INVALID_SOURCE, ticketCheckResult.getCheckStatus());
            Assertions.assertNotNull(ticketCheckResult.getCheckedTicket());
        }
    }

    @Test
    public void runtimeSourcesUpdateTest() {
        final TvmSettings tvmSettings = new TvmSettings(1111,
                "secret",
                BlackboxEnv.TEST_YATEAM,
                "production",
                SERVICE_NAME
        );
        tvmSettings.setRulesUpdateFrequencyMillis(100);
        final AtomicInteger fetchNumber = new AtomicInteger();
        final ReadWriteLock lock = new ReentrantReadWriteLock();
        final int fetchWithSourcesNumber = 3;
        final int totalFetches = 6;

        final String srcServiceName = "srcServiceName";
        final int srcServiceId = 543535;
        try (AbstractTvmRulesClient client = new AbstractTvmRulesClient(tvmSettings) {

            @Override
            protected TvmClient initTvmClient(TvmSettings tvmSettings, int[] destinations) {
                return new TestTvmClient(destinations);
            }

            @Override
            protected TvmRulesInfo fetchTvmRulesInfo() {
                try {
                    lock.writeLock().lock();
                    if (fetchNumber.getAndIncrement() < fetchWithSourcesNumber) {
                        return new TvmRulesInfo();
                    } else {
                        final TvmRule tvmRule = new TvmRule();
                        tvmRule.setSrc(srcServiceName);
                        tvmRule.setDst(SERVICE_NAME);

                        final TvmRulesInfo tvmRulesInfo = new TvmRulesInfo();
                        tvmRulesInfo.setRules(List.of(tvmRule));
                        tvmRulesInfo.setServiceIds(Map.of(
                                srcServiceName, srcServiceId
                        ));
                        return tvmRulesInfo;
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }) {
            client.start();
            int checkNumber = 0;
            while (checkNumber < totalFetches) {
                try {
                    lock.readLock().lock();
                    // Check if fetch happened
                    if (fetchNumber.get() != checkNumber) {
                        // Fetch happened
                        System.out.println("Fetch number: " + fetchNumber.get() + ", Check number: " + checkNumber);
                        int retry = 0;
                        boolean success = false;
                        // Retrying, waiting for sources updated
                        while (retry++ < 100) {
                            final ServiceTicketCheckResult ticketCheckResult = client.checkServiceTicketWithSources(
                                    TestTvmClient.generateServiceTicket(TicketStatus.OK, "debug", srcServiceId, 435)
                            );
                            System.out.println(ticketCheckResult.getCheckStatus());
                            // Fetch 0 - 2 returns no sources
                            if (checkNumber < fetchWithSourcesNumber && ticketCheckResult.getCheckStatus().equals(ServiceTicketCheckStatus.INVALID_SOURCE)) {
                                success = true;
                                break;
                            // Fetch 3+ return source
                            } else if (checkNumber >= fetchWithSourcesNumber && ticketCheckResult.getCheckStatus().equals(ServiceTicketCheckStatus.OK)) {
                                success = true;
                                break;
                            }
                        }
                        if (!success) {
                            Assertions.fail();
                        }
                        checkNumber++;
                    }
                } finally {
                    lock.readLock().unlock();
                }
            }
        }
    }

    public static class TestTvmClient implements TvmClient {

        public static final String DELIMITER = "::";
        public static final String DEFAULT_SERVICE_TICKET = "testServiceTicket";
        public static final String DEFAULT_USER_TICKET = "testUserTicket";

        public static final String NO_SUCH_DESTINATION_ERROR = "No such destination";

        private final String serviceTicket;
        private final String userTicket;

        private final Set<Integer> destinations;

        public TestTvmClient(String serviceTicket, String userTicket, int[] destinations) {
            this.serviceTicket = serviceTicket;
            this.userTicket = userTicket;
            this.destinations = Arrays.stream(destinations).boxed().collect(Collectors.toSet());
        }

        public TestTvmClient(int[] destinations) {
            this.destinations = Arrays.stream(destinations).boxed().collect(Collectors.toSet());
            this.serviceTicket = DEFAULT_SERVICE_TICKET;
            this.userTicket = DEFAULT_USER_TICKET;
        }

        @Override
        public ClientStatus getStatus() {
            return new ClientStatus(ClientStatus.Code.OK, "OK");
        }

        @Override
        public String getServiceTicketFor(String alias) {
            return serviceTicket;
        }

        @Override
        public String getServiceTicketFor(int tvmId) {
            if (!destinations.contains(tvmId)) {
                throw new RuntimeException(NO_SUCH_DESTINATION_ERROR);
            }
            return serviceTicket;
        }

        @Override
        public CheckedServiceTicket checkServiceTicket(String ticketBody) {
            final String[] parts = ticketBody.split(DELIMITER);
            return new CheckedServiceTicket(
                    TicketStatus.valueOf(parts[0]), parts[1], Integer.parseInt(parts[2]), Long.parseLong(parts[3])
            );
        }

        @Override
        public CheckedUserTicket checkUserTicket(String ticketBody) {
            final String[] parts = ticketBody.split(DELIMITER);
            final String[] uidsString = parts[3].split(",");
            final long[] uids = new long[uidsString.length];
            for (int i = 0; i < uids.length; i++) {
                uids[i] = Long.parseLong(uidsString[i]);
                i++;
            }
            return new CheckedUserTicket(
                    TicketStatus.valueOf(parts[0]), parts[0], parts[1].split(","), Long.parseLong(parts[2]), uids
            );
        }

        @Override
        public CheckedUserTicket checkUserTicket(String ticketBody, BlackboxEnv overridedBbEnv) {
            return checkUserTicket(ticketBody);
        }

        @Override
        public void close() {

        }

        public static String generateServiceTicket(TicketStatus status, String debugInfo, int src, long issuerUid) {
            return status.toString() + DELIMITER + debugInfo + DELIMITER + src + DELIMITER + issuerUid;
        }
    }
}
