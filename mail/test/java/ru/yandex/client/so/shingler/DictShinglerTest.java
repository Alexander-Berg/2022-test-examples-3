package ru.yandex.client.so.shingler;

import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.data.compressor.DataCompressor;
import ru.yandex.digest.Fnv;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.test.util.TestBase;

public class DictShinglerTest extends TestBase {
    private static final int TIMEOUT = 10000;
    private static final int INTERVAL = 100;
    private static final long SHINGLE_1 = Fnv.fnv64("Все коммиты, сделанные в arc, сохраняют изменённые данные в "
        + "локальной файловой системе");
    private static final long SHINGLE_2 = Fnv.fnv64("Каждое последующее добавление новых коммитов можно делать с "
        + "помощью arc push");
    private static final long SHINGLE_3 = Fnv.fnv64("https://arc-vcs.yandex-team.ru/docs/manual/arc/start.html");
    private static final String GET_RESPONSE = DictShingleInfo.SHIDENT + Long.toHexString(SHINGLE_1).toUpperCase()
        + "-0: v='01' empty='0' tod='1591970531-1592007911-26439-1402-5-133-0-0-0-0-0-0' "
        + "yes='1591883964-1591970096-372727-28611-32-377-0-0-0-0-0-0' "
        + "his='1473072256-1592007911-129481584-62462780-33835-302141-0-0-0-0-0-0' fla='8796093022483'>"
        + DictShingleInfo.SHIDENT + Long.toHexString(SHINGLE_2).toUpperCase() + "-0: v='01' empty='0' "
        + "tod='1591970525-1592007969-2626742-251785-217-4262-0-0-0-0-0-0' "
        + "yes='1591883956-1591970103-11053798-651969-620-12360-0-1-0-0-0-0' "
        + "his='1473072205-1592007969-1126903960-2396631333-1807045-19781272-0-1-0-0-0-0' fla='19'>"
        + DictShingleInfo.SHIDENT + Long.toHexString(SHINGLE_3).toUpperCase() + "-0: v='01' empty='0' "
        + "tod='1591970525-1592007926-910155-11466-20-476-1-0-0-0-0-0' "
        + "yes='1591883957-1591970103-342162-23272-51-669-0-0-0-0-0-0' "
        + "his='1473072210-1592007926-283879834-85077514-94161-838263-1-0-0-0-0-0' fla='8796093022227'>";

    public DictShinglerTest() {
    }

    private void checkGetResponse(final DictShinglerResult results) {
        Assert.assertNotNull(results);
        {
            Assert.assertEquals(1, results.size());
            Assert.assertEquals(3, results.get(GeneralShingles.SHINGLE).size());
            logger.info("checkGetResponse get results: " + results);
            final var shingleInfo1 = results.get(GeneralShingles.SHINGLE).get(0);
            Assert.assertNotNull(shingleInfo1);
            {
                Assert.assertEquals(SHINGLE_1, (long)shingleInfo1.shingle());
                Assert.assertEquals(8796093022483L, (long)shingleInfo1.languageRaw());

                {
                    final DictShingleInfo.Fields stats = shingleInfo1.counters().get(DictShingleInfo.Time.TODAY);
                    Assert.assertNotNull(stats);
                    Assert.assertEquals(1591970531L, (long)stats.get(DictShingleInfo.FieldType.FIRST_TIME));
                    Assert.assertEquals(1592007911L, (long)stats.get(DictShingleInfo.FieldType.LAST_TIME));
                    Assert.assertEquals(26439L, (long)stats.get(DictShingleInfo.FieldType.HAM));
                    Assert.assertEquals(1402L, (long)stats.get(DictShingleInfo.FieldType.SPAM));
                    Assert.assertEquals(5L, (long)stats.get(DictShingleInfo.FieldType.COMPLHAM));
                    Assert.assertEquals(133L, (long)stats.get(DictShingleInfo.FieldType.COMPLSPAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.VIRUS));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING_YANDEX));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.HACKED));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLHAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLSPAM));
                }

                {
                    final DictShingleInfo.Fields stats = shingleInfo1.counters().get(DictShingleInfo.Time.YESTERDAY);
                    Assert.assertNotNull(stats);
                    Assert.assertEquals(1591883964L, (long)stats.get(DictShingleInfo.FieldType.FIRST_TIME));
                    Assert.assertEquals(1591970096L, (long)stats.get(DictShingleInfo.FieldType.LAST_TIME));
                    Assert.assertEquals(372727L, (long)stats.get(DictShingleInfo.FieldType.HAM));
                    Assert.assertEquals(28611L, (long)stats.get(DictShingleInfo.FieldType.SPAM));
                    Assert.assertEquals(32L, (long)stats.get(DictShingleInfo.FieldType.COMPLHAM));
                    Assert.assertEquals(377L, (long)stats.get(DictShingleInfo.FieldType.COMPLSPAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.VIRUS));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING_YANDEX));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.HACKED));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLHAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLSPAM));
                }

                {
                    final DictShingleInfo.Fields stats = shingleInfo1.counters().get(DictShingleInfo.Time.HISTORY);
                    Assert.assertNotNull(stats);
                    Assert.assertEquals(1473072256L, (long)stats.get(DictShingleInfo.FieldType.FIRST_TIME));
                    Assert.assertEquals(1592007911L, (long)stats.get(DictShingleInfo.FieldType.LAST_TIME));
                    Assert.assertEquals(129481584L, (long)stats.get(DictShingleInfo.FieldType.HAM));
                    Assert.assertEquals(62462780L, (long)stats.get(DictShingleInfo.FieldType.SPAM));
                    Assert.assertEquals(33835L, (long)stats.get(DictShingleInfo.FieldType.COMPLHAM));
                    Assert.assertEquals(302141L, (long)stats.get(DictShingleInfo.FieldType.COMPLSPAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.VIRUS));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING_YANDEX));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.HACKED));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLHAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLSPAM));
                }
            }

            final var shingleInfo2 = results.get(GeneralShingles.SHINGLE).get(1);
            Assert.assertNotNull(shingleInfo2);
            {
                Assert.assertEquals(SHINGLE_2, (long)shingleInfo2.shingle());
                Assert.assertEquals(19L, (long)shingleInfo2.languageRaw());

                {
                    final DictShingleInfo.Fields stats = shingleInfo2.counters().get(DictShingleInfo.Time.TODAY);
                    Assert.assertNotNull(stats);
                    Assert.assertEquals(1591970525L, (long)stats.get(DictShingleInfo.FieldType.FIRST_TIME));
                    Assert.assertEquals(1592007969L, (long)stats.get(DictShingleInfo.FieldType.LAST_TIME));
                    Assert.assertEquals(2626742L, (long)stats.get(DictShingleInfo.FieldType.HAM));
                    Assert.assertEquals(251785L, (long)stats.get(DictShingleInfo.FieldType.SPAM));
                    Assert.assertEquals(217L, (long)stats.get(DictShingleInfo.FieldType.COMPLHAM));
                    Assert.assertEquals(4262L, (long)stats.get(DictShingleInfo.FieldType.COMPLSPAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.VIRUS));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING_YANDEX));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.HACKED));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLHAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLSPAM));
                }

                {
                    final DictShingleInfo.Fields stats = shingleInfo2.counters().get(DictShingleInfo.Time.YESTERDAY);
                    Assert.assertNotNull(stats);
                    Assert.assertEquals(1591883956L, (long)stats.get(DictShingleInfo.FieldType.FIRST_TIME));
                    Assert.assertEquals(1591970103L, (long)stats.get(DictShingleInfo.FieldType.LAST_TIME));
                    Assert.assertEquals(11053798L, (long)stats.get(DictShingleInfo.FieldType.HAM));
                    Assert.assertEquals(651969L, (long)stats.get(DictShingleInfo.FieldType.SPAM));
                    Assert.assertEquals(620L, (long)stats.get(DictShingleInfo.FieldType.COMPLHAM));
                    Assert.assertEquals(12360L, (long)stats.get(DictShingleInfo.FieldType.COMPLSPAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.VIRUS));
                    Assert.assertEquals(1L, (long)stats.get(DictShingleInfo.FieldType.FISHING));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING_YANDEX));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.HACKED));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLHAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLSPAM));
                }

                {
                    final DictShingleInfo.Fields stats = shingleInfo2.counters().get(DictShingleInfo.Time.HISTORY);
                    Assert.assertNotNull(stats);
                    Assert.assertEquals(1473072205L, (long)stats.get(DictShingleInfo.FieldType.FIRST_TIME));
                    Assert.assertEquals(1592007969L, (long)stats.get(DictShingleInfo.FieldType.LAST_TIME));
                    Assert.assertEquals(1126903960L, (long)stats.get(DictShingleInfo.FieldType.HAM));
                    Assert.assertEquals(2396631333L, (long)stats.get(DictShingleInfo.FieldType.SPAM));
                    Assert.assertEquals(1807045L, (long)stats.get(DictShingleInfo.FieldType.COMPLHAM));
                    Assert.assertEquals(19781272L, (long)stats.get(DictShingleInfo.FieldType.COMPLSPAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.VIRUS));
                    Assert.assertEquals(1L, (long)stats.get(DictShingleInfo.FieldType.FISHING));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING_YANDEX));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.HACKED));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLHAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLSPAM));
                }
            }

            final var shingleInfo3 = results.get(GeneralShingles.SHINGLE).get(2);
            Assert.assertNotNull(shingleInfo3);
            {
                Assert.assertEquals(SHINGLE_3, (long)shingleInfo3.shingle());
                Assert.assertEquals(8796093022227L, (long)shingleInfo3.languageRaw());

                {
                    final DictShingleInfo.Fields stats = shingleInfo3.counters().get(DictShingleInfo.Time.TODAY);
                    Assert.assertNotNull(stats);
                    Assert.assertEquals(1591970525L, (long)stats.get(DictShingleInfo.FieldType.FIRST_TIME));
                    Assert.assertEquals(1592007926L, (long)stats.get(DictShingleInfo.FieldType.LAST_TIME));
                    Assert.assertEquals(910155L, (long)stats.get(DictShingleInfo.FieldType.HAM));
                    Assert.assertEquals(11466L, (long)stats.get(DictShingleInfo.FieldType.SPAM));
                    Assert.assertEquals(20L, (long)stats.get(DictShingleInfo.FieldType.COMPLHAM));
                    Assert.assertEquals(476L, (long)stats.get(DictShingleInfo.FieldType.COMPLSPAM));
                    Assert.assertEquals(1L, (long)stats.get(DictShingleInfo.FieldType.VIRUS));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING_YANDEX));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.HACKED));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLHAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLSPAM));
                }

                {
                    final DictShingleInfo.Fields stats = shingleInfo3.counters().get(DictShingleInfo.Time.YESTERDAY);
                    Assert.assertNotNull(stats);
                    Assert.assertEquals(1591883957L, (long)stats.get(DictShingleInfo.FieldType.FIRST_TIME));
                    Assert.assertEquals(1591970103L, (long)stats.get(DictShingleInfo.FieldType.LAST_TIME));
                    Assert.assertEquals(342162L, (long)stats.get(DictShingleInfo.FieldType.HAM));
                    Assert.assertEquals(23272L, (long)stats.get(DictShingleInfo.FieldType.SPAM));
                    Assert.assertEquals(51L, (long)stats.get(DictShingleInfo.FieldType.COMPLHAM));
                    Assert.assertEquals(669L, (long)stats.get(DictShingleInfo.FieldType.COMPLSPAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.VIRUS));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING_YANDEX));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.HACKED));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLHAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLSPAM));
                }

                {
                    final DictShingleInfo.Fields stats = shingleInfo3.counters().get(DictShingleInfo.Time.HISTORY);
                    Assert.assertNotNull(stats);
                    Assert.assertEquals(1473072210L, (long)stats.get(DictShingleInfo.FieldType.FIRST_TIME));
                    Assert.assertEquals(1592007926L, (long)stats.get(DictShingleInfo.FieldType.LAST_TIME));
                    Assert.assertEquals(283879834L, (long)stats.get(DictShingleInfo.FieldType.HAM));
                    Assert.assertEquals(85077514L, (long)stats.get(DictShingleInfo.FieldType.SPAM));
                    Assert.assertEquals(94161L, (long)stats.get(DictShingleInfo.FieldType.COMPLHAM));
                    Assert.assertEquals(838263L, (long)stats.get(DictShingleInfo.FieldType.COMPLSPAM));
                    Assert.assertEquals(1L, (long)stats.get(DictShingleInfo.FieldType.VIRUS));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.FISHING_YANDEX));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.HACKED));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLHAM));
                    Assert.assertEquals(0L, (long)stats.get(DictShingleInfo.FieldType.EXPERT_COMPLSPAM));
                }
            }
        }
    }

    @Test
    public void testDictShinglerGetRequest() throws Exception {
        try (StaticServer shingler = new StaticServer(Configs.baseConfig("DictShingler"));
             final SharedConnectingIOReactor reactor =
                 new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final DictShinglerClient dictShinglerClient = new DictShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            dictShinglerClient.start();

            final Shingles shingles = new Shingles();
            shingles.add(GeneralShingles.SHINGLE, "Все коммиты, сделанные в arc, сохраняют изменённые данные в "
                + "локальной файловой системе");
            shingles.add(GeneralShingles.SHINGLE, "Каждое последующее добавление новых коммитов можно делать с"
                + " помощью arc push");
            shingles.add(GeneralShingles.SHINGLE, "https://arc-vcs.yandex-team.ru/docs/manual/arc/start.html");

            String getResponseBody = DataCompressor.LZO.compressAndBase64(GET_RESPONSE);
            logger.info("GET body: " + getResponseBody);
            shingler.add(
                DictShinglerClient.RequestType.GET.uri(),
                new StaticHttpResource(HttpStatus.SC_OK, new StringEntity(getResponseBody)));

            final Future<?> future = dictShinglerClient.getShingles(shingles, EmptyFutureCallback.INSTANCE);
            final DictShinglerResult result = (DictShinglerResult) future.get();

            checkGetResponse(result);
            waitServerRequests(shingler, DictShinglerClient.RequestType.GET.uri(), 1);
        }
    }

    @Test
    public void testDictShinglerPutRequest() throws Exception {
        try (StaticServer shingler = new StaticServer(Configs.baseConfig("DictShingler"));
             final SharedConnectingIOReactor reactor =
                 new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final DictShinglerClient dictShinglerClient =
                 new DictShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            dictShinglerClient.start();

            final Shingles shingles = new Shingles();
            shingles.add(GeneralShingles.SHINGLE, "Все коммиты, сделанные в arc, сохраняют изменённые данные в "
                + "локальной файловой системе");
            shingles.add(GeneralShingles.SHINGLE, "Каждое последующее добавление новых коммитов можно делать с"
                + " помощью arc push");
            shingles.add(GeneralShingles.SHINGLE, "https://arc-vcs.yandex-team.ru/docs/manual/arc/start.html");

            final DictShinglerResult data =
                new DictShinglerResult(DataCompressor.LZO.compressAndBase64(GET_RESPONSE));

            final String putQuery = DictShinglerClient.getPutQuery(data);
            shingler.add(DictShinglerClient.RequestType.PUT.uri(), HttpStatus.SC_OK);
            logger.info("PUT requestBody: " + putQuery);

            final String getQuery = DictShinglerClient.getGetQuery(shingles);
            shingler.add(
                DictShinglerClient.RequestType.GET.uri(),
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity(DataCompressor.LZO.compressAndBase64(GET_RESPONSE))));
            logger.info("GET request body: " + getQuery);

            final Future<?> putFuture =
                dictShinglerClient.putShingles(data, AbstractShinglerClient.StringFutureCallback.INSTANCE);
            final String putResult = (String) putFuture.get();

            Assert.assertNotNull(putResult);

            final Future<?> getFuture = dictShinglerClient.getShingles(shingles, EmptyFutureCallback.INSTANCE);
            final DictShinglerResult getResult = (DictShinglerResult) getFuture.get();

            checkGetResponse(getResult);

            waitServerRequests(shingler, DictShinglerClient.RequestType.GET.uri(), 1);
            waitServerRequests(shingler, DictShinglerClient.RequestType.PUT.uri(), 1);
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
