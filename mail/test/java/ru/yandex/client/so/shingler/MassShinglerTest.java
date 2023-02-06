package ru.yandex.client.so.shingler;

import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.digest.Fnv;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.test.util.TestBase;

public class MassShinglerTest extends TestBase {
    private static final int TIMEOUT = 10000;
    private static final int INTERVAL = 100;
    private static final String GET_RESPONSE = MassShingleInfo.SHIDENT + Long.toHexString(Fnv.fnv64("Все коммиты, "
        + "сделанные в arc, сохраняют изменённые данные в локальной файловой системе")) + "-1 empty='0' "
        + "ti='1,3,2,4,5,6.500000' yi='1,0,0,0,0,0.000000' ak='0.971007' pr='oe' lmt='1592946000' >"
        + MassShingleInfo.SHIDENT + Long.toHexString(Fnv.fnv64("Каждое последующее добавление новых коммитов можно "
        + "делать с помощью arc push")) + "-2 empty='0' ti='0,0,0,0,0,0.000000' yi='12,0,3,0,1,0.000000' ak='0.971007' "
        + "pr='oe' lmt='1592946001' >"
        + MassShingleInfo.SHIDENT + Long.toHexString(Fnv.fnv64("https://arc-vcs.yandex-team.ru/docs/manual/arc/"
        + "start.html")) + "-6 empty='0' ti='108,0,25,0,0,0.000000' yi='3017,0,978,25,25,0.000000' ak='0.971007' "
        + "pr='oe' lmt='1592946000' >"
        + MassShingleInfo.SHIDENT + Long.toHexString(Fnv.fnv64("luckybug@yandex-team.ru")) + "-14 empty='0' "
        + "ti='261,0,24,0,0,0.000000' yi='5782,0,986,25,25,0.000000' ak='0.971018' pr='oe' lmt='1592946002' >";

    public MassShinglerTest() {
    }

    private void checkGetResponse(final MassShinglerResult results) {
        Assert.assertNotNull(results);
        {
            Assert.assertEquals(4, results.size());
            logger.info("checkGetResponse get results: " + results);
            final var fullTextInfos = results.get(ShingleType.FULL_TEXT_SHINGLE);
            Assert.assertEquals(1, fullTextInfos.size());
            {
                final MassShingleInfo info = fullTextInfos.get(0).shingleInfo();

                {
                    final MassShingleStats stats = info.todayStats();
                    Assert.assertNotNull(stats);
                    Assert.assertEquals(1, stats.ham());
                    Assert.assertEquals(2, stats.spam());
                    Assert.assertEquals(3, stats.malic());
                    Assert.assertEquals(4, stats.personalHam());
                    Assert.assertEquals(5, stats.personalSpam());
                    Assert.assertEquals(6.5, stats.weight(), 0.000001);
                }

                {
                    final MassShingleStats stats = info.yesterdayStats();
                    Assert.assertNotNull(stats);
                    Assert.assertEquals(1, stats.ham());
                    Assert.assertEquals(0, stats.spam());
                    Assert.assertEquals(0, stats.malic());
                    Assert.assertEquals(0, stats.personalHam());
                    Assert.assertEquals(0, stats.personalSpam());
                    Assert.assertEquals(0.0, stats.weight(), 0.000001);
                }

                {
                    Assert.assertEquals(0.971007, info.ageCoefficient(), 0.000001);
                    Assert.assertEquals(1592946000, (long)info.lastMidnight());
                }
            }

            final var clearTextInfos = results.get(ShingleType.CLEAR_TEXT_SHINGLE);
            Assert.assertEquals(1, clearTextInfos.size());
            {
                final MassShingleInfo info = clearTextInfos.get(0).shingleInfo();

                {
                    {
                        final MassShingleStats stats = info.todayStats();
                        Assert.assertNotNull(stats);
                        Assert.assertEquals(0, stats.ham());
                        Assert.assertEquals(0, stats.spam());
                        Assert.assertEquals(0, stats.malic());
                        Assert.assertEquals(0, stats.personalHam());
                        Assert.assertEquals(0, stats.personalSpam());
                        Assert.assertEquals(0.0, stats.weight(), 0.000001);
                    }

                    {
                        final MassShingleStats stats = info.yesterdayStats();
                        Assert.assertNotNull(stats);
                        Assert.assertEquals(12, stats.ham());
                        Assert.assertEquals(3, stats.spam());
                        Assert.assertEquals(0, stats.malic());
                        Assert.assertEquals(0, stats.personalHam());
                        Assert.assertEquals(1, stats.personalSpam());
                        Assert.assertEquals(0.0, stats.weight(), 0.000001);
                    }

                    {
                        Assert.assertEquals(0.971007, info.ageCoefficient(), 0.000001);
                        Assert.assertEquals(1592946001, (long) info.lastMidnight());
                    }
                }
            }

            final var hostInfos = results.get(ShingleType.HOST_SHINGLE);
            Assert.assertEquals(1, hostInfos.size());
            {
                final MassShingleInfo info = hostInfos.get(0).shingleInfo();

                {
                    {
                        final MassShingleStats stats = info.todayStats();
                        Assert.assertNotNull(stats);
                        Assert.assertEquals(108, stats.ham());
                        Assert.assertEquals(25, stats.spam());
                        Assert.assertEquals(0, stats.malic());
                        Assert.assertEquals(0, stats.personalHam());
                        Assert.assertEquals(0, stats.personalSpam());
                        Assert.assertEquals(0.0, stats.weight(), 0.000001);
                    }

                    {
                        final MassShingleStats stats = info.yesterdayStats();
                        Assert.assertNotNull(stats);
                        Assert.assertEquals(3017, stats.ham());
                        Assert.assertEquals(978, stats.spam());
                        Assert.assertEquals(0, stats.malic());
                        Assert.assertEquals(25, stats.personalHam());
                        Assert.assertEquals(25, stats.personalSpam());
                        Assert.assertEquals(0.0, stats.weight(), 0.000001);
                    }

                    {
                        Assert.assertEquals(0.971007, info.ageCoefficient(), 0.000001);
                        Assert.assertEquals(1592946000, (long) info.lastMidnight());
                    }
                }
            }

            final var fromAddrInfos = results.get(ShingleType.FROM_ADDR_SHINGLE);
            Assert.assertEquals(1, fromAddrInfos.size());
            {
                final MassShingleInfo info = fromAddrInfos.get(0).shingleInfo();

                {
                    {
                        final MassShingleStats stats = info.todayStats();
                        Assert.assertNotNull(stats);
                        Assert.assertEquals(261, stats.ham());
                        Assert.assertEquals(24, stats.spam());
                        Assert.assertEquals(0, stats.malic());
                        Assert.assertEquals(0, stats.personalHam());
                        Assert.assertEquals(0, stats.personalSpam());
                        Assert.assertEquals(0.0, stats.weight(), 0.000001);
                    }

                    {
                        final MassShingleStats stats = info.yesterdayStats();
                        Assert.assertNotNull(stats);
                        Assert.assertEquals(5782, stats.ham());
                        Assert.assertEquals(986, stats.spam());
                        Assert.assertEquals(0, stats.malic());
                        Assert.assertEquals(25, stats.personalHam());
                        Assert.assertEquals(25, stats.personalSpam());
                        Assert.assertEquals(0.0, stats.weight(), 0.000001);
                    }

                    {
                        Assert.assertEquals(0.971018, info.ageCoefficient(), 0.000001);
                        Assert.assertEquals(1592946002, (long) info.lastMidnight());
                    }
                }
            }
        }
    }

    @Test
    public void testMassShinglerGetRequest() throws Exception {
        try (StaticServer shingler = new StaticServer(Configs.baseConfig("MassShingler"));
             final SharedConnectingIOReactor reactor =
                 new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final MassShinglerClient massShinglerClient = new MassShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            massShinglerClient.start();

            final Shingles shingles = new Shingles();
            shingles.add(ShingleType.FULL_TEXT_SHINGLE, "Все коммиты, сделанные в arc, сохраняют изменённые данные в "
                + "локальной файловой системе");
            shingles.add(ShingleType.CLEAR_TEXT_SHINGLE, "Каждое последующее добавление новых коммитов можно делать с"
                + " помощью arc push");
            shingles.add(ShingleType.HOST_SHINGLE, "https://arc-vcs.yandex-team.ru/docs/manual/arc/start.html");
            shingles.add(ShingleType.FROM_ADDR_SHINGLE, "luckybug@yandex-team.ru");

            final String getQuery = MassShinglerClient.getGetQuery(shingles);
            shingler.add(getQuery, GET_RESPONSE);

            final Future<?> future = massShinglerClient.getShingles(shingles, EmptyFutureCallback.INSTANCE);
            final MassShinglerResult result = (MassShinglerResult) future.get();

            checkGetResponse(result);
            waitServerRequests(shingler, getQuery, 1);
        }
    }

    @Test
    public void testMassShinglerPutRequest() throws Exception {
        try (StaticServer shingler = new StaticServer(Configs.baseConfig("MassShingler"));
             final SharedConnectingIOReactor reactor =
                     new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final MassShinglerClient massShinglerClient =
                     new MassShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            massShinglerClient.start();

            final Shingles shingles = new Shingles();
            shingles.add(ShingleType.FULL_TEXT_SHINGLE, "Все коммиты, сделанные в arc, сохраняют изменённые данные в "
                + "локальной файловой системе");
            shingles.add(ShingleType.CLEAR_TEXT_SHINGLE, "Каждое последующее добавление новых коммитов можно делать с"
                + " помощью arc push");
            shingles.add(ShingleType.HOST_SHINGLE, "https://arc-vcs.yandex-team.ru/docs/manual/arc/start.html");
            shingles.add(ShingleType.FROM_ADDR_SHINGLE, "luckybug@yandex-team.ru");

            final MassShinglerResult data = new MassShinglerResult(shingles, GET_RESPONSE);

            final String putQuery = MassShinglerClient.getPutQuery(data);
            shingler.add(putQuery, HttpStatus.SC_OK);
            logger.info("PUT Query: " + putQuery);

            final String getQuery = MassShinglerClient.getGetQuery(shingles);
            shingler.add(getQuery, GET_RESPONSE);
            logger.info("GET Query: " + getQuery);

            final Future<?> putFuture =
                massShinglerClient.putShingles(data, AbstractShinglerClient.StringFutureCallback.INSTANCE);
            final String putResult = (String) putFuture.get();

            Assert.assertNotNull(putResult);

            final Future<?> getFuture = massShinglerClient.getShingles(shingles, EmptyFutureCallback.INSTANCE);
            final MassShinglerResult getResult = (MassShinglerResult) getFuture.get();

            checkGetResponse(getResult);

            waitServerRequests(shingler, getQuery, 1);
            waitServerRequests(shingler, putQuery, 1);
        }
    }

    public void waitServerRequests(final StaticServer server, final String uri, final int count)
            throws Exception
    {
        long start = System.currentTimeMillis();
        while (server.accessCount(uri) != count) {
            Thread.sleep(INTERVAL);
            if (System.currentTimeMillis() - start > TIMEOUT) {
                throw new TimeoutException("Expecting " + count + " requests to " + uri + " but got "
                        + server.accessCount(uri));
            }
        }
    }
}
