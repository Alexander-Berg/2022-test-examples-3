package ru.yandex.client.so.shingler;

import java.util.Arrays;
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
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.test.util.TestBase;


public class ComplShinglerTest extends TestBase {
    private static final int TIMEOUT = 10000;
    private static final int INTERVAL = 100;
    private static final String GET_RESPONSE = "[" +
            "{" +
            "   \"scheme\": \"today_abuses\"," +
            "   \"find\": [" +
            "               {" +
            "                   \"shingle\": " + Fnv.fnv64("Все коммиты, сделанные в arc, сохраняют " +
            "изменённые данные в локальной файловой системе") +
            "                   ,\"type\": 1" +
            "                   ,\"day\": 7488" +
            "                   ,\"ham\": 1" +
            "                   ,\"spam\": 2" +
            "                   ,\"malic\": 3" +
            "                   ,\"pers_ham\": 4" +
            "                   ,\"pers_spam\": 5" +
            "                   ,\"complaint_ham\": 6" +
            "                   ,\"complaint_spam\": 7" +
            "                   ,\"virus_count\": 8" +
            "                   ,\"uniq_sd_spam\": 9" +
            "                   ,\"uniq_sd_ham\": 10" +
            "                   ,\"expert_spam\": \"11\"" +
            "                   ,\"expert_ham\": 12" +
            "               }" +
            "             ]" +
            "}," +
            "{" +
            "   \"scheme\": \"history_abuses\"," +
            "   \"find\": [" +
            "               {" +
            "                   \"shingle\": " + Fnv.fnv64("Каждое последующее добавление новых " +
            "коммитов можно делать с помощью arc push") +
            "                   ,\"type\": 2" +
            "                   ,\"firsttime\": 1583934479" +
            "                   ,\"lasttime\": 1583934480" +
            "                   ,\"ham\": 10" +
            "                   ,\"spam\": 20" +
            "                   ,\"malic\": 30" +
            "                   ,\"pers_ham\": 40" +
            "                   ,\"pers_spam\": 50" +
            "                   ,\"complaint_ham\": 60" +
            "                   ,\"complaint_spam\": 70" +
            "                   ,\"virus_count\": 80" +
            "                   ,\"uniq_sd_spam\": 90" +
            "                   ,\"uniq_sd_ham\": 100" +
            "                   ,\"expert_spam\": \"110\"" +
            "                   ,\"expert_ham\": 120" +
            "                   ,\"day_count_with_complaint\": \"130\"" +
            "                   ,\"day_count_with_virus\": 140" +
            "               }" +
            "             ]" +
            "}," +
            "{" +
            "   \"scheme\": \"user_weights\"," +
            "   \"find\": [" +
            "               {" +
            "                   \"shingle\": " + Fnv.fnv64("https://arc-vcs.yandex-team" +
            ".ru/docs/manual/arc/start.html") +
            "                   ,\"type\": 6" +
            "                   ,\"win_timeset\": 412" +
            "                   ,\"win_lastset\": 423" +
            "                   ,\"win_during\": 434" +
            "                   ,\"should_be_malic_in\": 1" +
            "                   ,\"win_weight\": 456.2" +
            "                   ,\"wout_timeset\": 442" +
            "                   ,\"wout_lastset\": 443" +
            "                   ,\"wout_during\": 444" +
            "                   ,\"should_be_malic_out\": 0" +
            "                   ,\"wout_weight\": 41236.5" +
            "               }," +
            "               {" +
            "                   \"shingle\": " + Fnv.fnv64("Все коммиты, сделанные в arc, сохраняют " +
            "изменённые данные в локальной файловой системе") +
            "                   ,\"type\": 1" +
            "                   ,\"win_timeset\": 412" +
            "                   ,\"win_lastset\": 423" +
            "                   ,\"win_during\": 434" +
            "                   ,\"should_be_malic_in\": 1" +
            "                   ,\"win_weight\": 456" +
            "                   ,\"wout_timeset\": 442" +
            "                   ,\"wout_lastset\": 443" +
            "                   ,\"wout_during\": 444" +
            "                   ,\"should_be_malic_out\": 1235" +
            "                   ,\"wout_weight\": 41236" +
            "               }" +
            "             ]" +
            "}" +
        "]";

    public ComplShinglerTest() {
    }

    private void checkGetResponse(final ComplShingles result) {
        Assert.assertNotNull(result);
        {
            final var results = result.getResults();
            logger.info("checkGetResponse get results: " + results + ", shinglesTypes="
                + Arrays.toString(results.keySet().toArray()));
            Assert.assertEquals(3, results.size());
            final var fullTextInfos = results.get(ShingleType.FULL_TEXT_SHINGLE);
            Assert.assertNotNull(fullTextInfos);
            logger.info("checkGetResponse: schemes=" + Arrays.toString(fullTextInfos.keySet().toArray()));
            Assert.assertEquals(1, fullTextInfos.size());
            {
                final ComplShingleInfo info = (ComplShingleInfo) fullTextInfos.get(Fnv.fnv64("Все коммиты, "
                    + "сделанные в arc, сохраняют изменённые данные в локальной файловой системе"));
                {
                    final ComplShingleInfo.DailyStats stats = info.getTodayStats();
                    Assert.assertNotNull(stats);
                    Assert.assertEquals(7488, stats.getDay());
                    Assert.assertEquals(1, stats.getHam());
                    Assert.assertEquals(2, stats.getSpam());
                    Assert.assertEquals(3, stats.getMalic());
                    Assert.assertEquals(4, stats.getPersHam());
                    Assert.assertEquals(5, stats.getPersSpam());
                    Assert.assertEquals(6, stats.getComplaintHam());
                    Assert.assertEquals(7, stats.getComplaintSpam());
                    Assert.assertEquals(8, stats.getVirusCount());
                    Assert.assertEquals(9, stats.getUniqSdSpam());
                    Assert.assertEquals(10, stats.getUniqSdHam());
                    Assert.assertEquals(11, stats.getExpertSpam());
                    Assert.assertEquals(12, stats.getExpertHam());
                }
                {
                    final ComplShingleInfo.HistoryStats stats = info.getHistoryStats();
                    Assert.assertNull(stats);
                }
                {
                    final ComplShingleInfo.UserWeights weights = info.getUserWeights();
                    Assert.assertNotNull(weights);

                    Assert.assertEquals(412, weights.getUserWeightIn().getCreation());
                    Assert.assertEquals(423, weights.getUserWeightIn().getUpdate());
                    Assert.assertEquals(434, weights.getUserWeightIn().getDuration());
                    Assert.assertTrue(weights.getUserWeightIn().isMalic());
                    Assert.assertEquals(456, weights.getUserWeightIn().getWeight(), 1e-6);

                    Assert.assertEquals(442, weights.getUserWeightOut().getCreation());
                    Assert.assertEquals(443, weights.getUserWeightOut().getUpdate());
                    Assert.assertEquals(444, weights.getUserWeightOut().getDuration());
                    Assert.assertTrue(weights.getUserWeightOut().isMalic());
                    Assert.assertEquals(41236, weights.getUserWeightOut().getWeight(), 1e-6);
                }
            }

            final var clearTextInfos = results.get(ShingleType.CLEAR_TEXT_SHINGLE);
            Assert.assertEquals(1, clearTextInfos.size());
            {
                final ComplShingleInfo info = (ComplShingleInfo) clearTextInfos.get(Fnv.fnv64("Каждое последующее "
                    + "добавление новых коммитов можно делать с помощью arc push"));
                {
                    final ComplShingleInfo.DailyStats stats = info.getTodayStats();
                    Assert.assertNull(stats);
                }
                {
                    final ComplShingleInfo.HistoryStats stats = info.getHistoryStats();
                    Assert.assertNotNull(stats);
                    Assert.assertEquals(1583934479, stats.getFirsttime());
                    Assert.assertEquals(1583934480, stats.getLasttime());
                    Assert.assertEquals(10, stats.getHam());
                    Assert.assertEquals(20, stats.getSpam());
                    Assert.assertEquals(30, stats.getMalic());
                    Assert.assertEquals(40, stats.getPersHam());
                    Assert.assertEquals(50, stats.getPersSpam());
                    Assert.assertEquals(60, stats.getComplaintHam());
                    Assert.assertEquals(70, stats.getComplaintSpam());
                    Assert.assertEquals(80, stats.getVirusCount());
                    Assert.assertEquals(90, stats.getUniqSdSpam());
                    Assert.assertEquals(100, stats.getUniqSdHam());
                    Assert.assertEquals(110, stats.getExpertSpam());
                    Assert.assertEquals(120, stats.getExpertHam());
                    Assert.assertEquals(130, stats.getDayCountWithComplaint());
                    Assert.assertEquals(140, stats.getDayCountWithVirus());
                }
                {
                    final ComplShingleInfo.UserWeights weights = info.getUserWeights();
                    Assert.assertNull(weights);
                }
            }

            final var hostInfos = results.get(ShingleType.HOST_SHINGLE);
            Assert.assertEquals(1, clearTextInfos.size());
            {
                final ComplShingleInfo info = (ComplShingleInfo) hostInfos.get(Fnv.fnv64("https://arc-vcs.yandex-team"
                    + ".ru/docs/manual/arc/start.html"));
                {
                    final ComplShingleInfo.DailyStats stats = info.getTodayStats();
                    Assert.assertNull(stats);
                }
                {
                    final ComplShingleInfo.HistoryStats stats = info.getHistoryStats();
                    Assert.assertNull(stats);
                }
                {
                    final ComplShingleInfo.UserWeights weights = info.getUserWeights();
                    Assert.assertNotNull(weights);

                    Assert.assertEquals(412, weights.getUserWeightIn().getCreation());
                    Assert.assertEquals(423, weights.getUserWeightIn().getUpdate());
                    Assert.assertEquals(434, weights.getUserWeightIn().getDuration());
                    Assert.assertTrue(weights.getUserWeightIn().isMalic());
                    Assert.assertEquals(456.2, weights.getUserWeightIn().getWeight(), 1e-6);

                    Assert.assertEquals(442, weights.getUserWeightOut().getCreation());
                    Assert.assertEquals(443, weights.getUserWeightOut().getUpdate());
                    Assert.assertEquals(444, weights.getUserWeightOut().getDuration());
                    Assert.assertFalse(weights.getUserWeightOut().isMalic());
                    Assert.assertEquals(41236.5, weights.getUserWeightOut().getWeight(), 1e-6);
                }
            }
        }
    }

    @Test
    public void testComplShinglerGetRequest() throws Exception {
        try (StaticServer shingler = new StaticServer(Configs.baseConfig("ComplShingler"));
             final SharedConnectingIOReactor reactor =
                 new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final ComplShinglerClient complShinglerClient =
                 new ComplShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            complShinglerClient.start();

            final Shingles shingles = new Shingles();
            shingles.add(ShingleType.FULL_TEXT_SHINGLE, "Все коммиты, сделанные в arc, сохраняют изменённые данные в "
                + "локальной файловой системе");
            shingles.add(ShingleType.CLEAR_TEXT_SHINGLE, "Каждое последующее добавление новых коммитов можно делать с"
                + " помощью arc push");
            shingles.add(ShingleType.HOST_SHINGLE, "https://arc-vcs.yandex-team.ru/docs/manual/arc/start.html");
            shingles.add(ShingleType.FROM_ADDR_SHINGLE, "luckybug@yandex-team.ru");

            shingler.add(ComplShinglerClient.URI, GET_RESPONSE);

            final ComplShingles data = new ComplShingles(shingles);
            logger.info("ComplShingles types count: " + data.size() + ". Types: "
                + Arrays.toString(data.keySet().toArray()));
            final Future<?> future = complShinglerClient.getShingles(data, EmptyFutureCallback.INSTANCE);
            logger.info("ComplShingler's GET query: " + complShinglerClient.getGetQuery(data));
            final ComplShingles result = (ComplShingles) future.get();

            waitServerRequests(shingler, ComplShinglerClient.URI, 1);
            checkGetResponse(result);
        }
    }

    @Test
    public void testComplShinglerPutRequest() throws Exception {
        try (TestShinglerServer shingler = new TestShinglerServer(Configs.baseConfig("ComplShingler"));
             final SharedConnectingIOReactor reactor =
                 new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final ComplShinglerClient complShinglerClient =
                 new ComplShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            complShinglerClient.start();

            final ComplShingles data = new ComplShingles(TypesafeValueContentHandler.parse(GET_RESPONSE));
            final Shingles shingles = new Shingles();
            shingles.add(ShingleType.FULL_TEXT_SHINGLE, "Все коммиты, сделанные в arc, сохраняют изменённые данные в "
                + "локальной файловой системе");
            shingles.add(ShingleType.CLEAR_TEXT_SHINGLE, "Каждое последующее добавление новых коммитов можно делать с"
                + " помощью arc push");
            shingles.add(ShingleType.HOST_SHINGLE, "https://arc-vcs.yandex-team.ru/docs/manual/arc/start.html");
            shingles.add(ShingleType.FROM_ADDR_SHINGLE, "luckybug@yandex-team.ru");

            final String putBody = complShinglerClient.getPutQuery(data);
            shingler.add(ComplShinglerClient.URI, putBody, HttpStatus.SC_OK);
            logger.info("PUT Query: " + putBody);

            final String getBody = complShinglerClient.getGetQuery(data);
            shingler.add(ComplShinglerClient.URI, getBody, GET_RESPONSE);
            logger.info("GET Query: " + getBody);

            final String getBody2 = complShinglerClient.getGetBasicQuery(shingles);
            shingler.add(ComplShinglerClient.URI, getBody2, GET_RESPONSE);
            logger.info("GET Query2: " + getBody2);
            final ComplShingles data2 = new ComplShingles(shingles);
            System.err.println("ComplShingles types count: " + data2.size() + ". Types: "
                + Arrays.toString(data2.keySet().toArray()));

            final Future<?> putFuture =
                complShinglerClient.putShingles(data, AbstractShinglerClient.StringFutureCallback.INSTANCE);
            final String putResult = (String) putFuture.get();

            waitServerRequests(shingler, ComplShinglerClient.URI, putBody,1);
            Assert.assertNotNull(putResult);

            final Future<?> getFuture = complShinglerClient.getShingles(data, EmptyFutureCallback.INSTANCE);
            final ComplShingles getResult = (ComplShingles) getFuture.get();

            waitServerRequests(shingler, ComplShinglerClient.URI, getBody,1);
            checkGetResponse(getResult);

            final Future<?> getFuture2 = complShinglerClient.getShingles(data2, EmptyFutureCallback.INSTANCE);
            logger.info("ComplShingles 2nd GET query's input data: " + data2);
            final String getBody3 = complShinglerClient.getGetQuery(data2);
            logger.info("ComplShingles 2nd GET query's body: " + getBody3);
            final ComplShingles getResult2 = (ComplShingles) getFuture2.get();

            waitServerRequests(shingler, ComplShinglerClient.URI, getBody2,1);
            checkGetResponse(getResult2);
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

    public void waitServerRequests(
        final TestShinglerServer server,
        final String uri,
        final String body,
        final int count)
        throws Exception
    {
        long start = System.currentTimeMillis();
        while (server.accessCount(uri, body) != count) {
            Thread.sleep(INTERVAL);
            if (System.currentTimeMillis() - start > TIMEOUT) {
                throw new TimeoutException("Expecting " + count + " requests to " + uri + " but got "
                        + server.accessCount(uri, body));
            }
        }
    }
}
