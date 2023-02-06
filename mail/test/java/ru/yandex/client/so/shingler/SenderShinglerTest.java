package ru.yandex.client.so.shingler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.test.util.TestBase;

public class SenderShinglerTest extends TestBase {
    private static final int TIMEOUT = 10000;
    private static final int INTERVAL = 100;

    private static final String GET_RESPONSE = "[" +
            "{" +
            "    \"find\": []," +
            "    \"scheme\": \"sender_totals\"" +
            "}," +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"cs\": \"1155264\"," +
            "            \"dkim_spam\": \"413005390\"," +
            "            \"ad\": \"388\"," +
            "            \"dkim_ham\": \"2627114686\"," +
            "            \"sendertype\": \"2\"," +
            "            \"pop3_spam\": \"17120814\"," +
            "            \"ba\": \"17468171\"," +
            "            \"hc\": \"2693639697\"," +
            "            \"pop3_ham\": \"106300192\"," +
            "            \"ps_spam\": \"13\"," +
            "            \"ut\": \"7487\"," +
            "            \"ab\": \"25152770\"," +
            "            \"hash\": \"-1297971109945453440\"," +
            "            \"sc\": \"432180016\"," +
            "            \"ct\": \"7094\"," +
            "            \"ph\": \"1099345\"," +
            "            \"ch\": \"194434\"," +
            "            \"ps\": \"322920558\"," +
            "            \"ps_ham\": \"3\"" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"sender_totals\"" +
            "}," +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"cs\": \"2023838\"," +
            "            \"dkim_spam\": \"726523733\"," +
            "            \"ad\": \"750\"," +
            "            \"dkim_ham\": \"5236823801\"," +
            "            \"ry\": \"4549\"," +
            "            \"sendertype\": \"1\"," +
            "            \"pop3_spam\": \"34509859\"," +
            "            \"ba\": \"42099104\"," +
            "            \"hc\": \"5379377768\"," +
            "            \"pop3_ham\": \"204906463\"," +
            "            \"ps_spam\": \"216\"," +
            "            \"ut\": \"7487\"," +
            "            \"ab\": \"57120148\"," +
            "            \"hash\": \"-5844812469897042107\"," +
            "            \"sc\": \"766541060\"," +
            "            \"ct\": \"6732\"," +
            "            \"ph\": \"2406279\"," +
            "            \"ch\": \"436374\"," +
            "            \"ps\": \"567299788\"," +
            "            \"ps_ham\": \"4263\"" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"sender_totals\"" +
            "}," +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"spam\": 13365663," +
            "            \"pers_spam\": 12780634," +
            "            \"dkim_spam\": 12567078," +
            "            \"dkim_ham\": 103040581," +
            "            \"deleted\": 0," +
            "            \"sendertype\": 1," +
            "            \"compl_spam\": 71424," +
            "            \"week\": 532," +
            "            \"ps_spam\": 0," +
            "            \"pers_ham\": 17336," +
            "            \"hash\": 12601931603812510000," +
            "            \"compl_ham\": 8828," +
            "            \"dkim_del\": 0," +
            "            \"read\": 0," +
            "            \"kooba\": 221129," +
            "            \"ham\": 106105798," +
            "            \"ps_ham\": 0," +
            "            \"abook\": 0" +
            "        }," +
            "        {" +
            "            \"spam\": 13289326," +
            "            \"pers_spam\": 12824702," +
            "            \"dkim_spam\": 12657545," +
            "            \"dkim_ham\": 99841129," +
            "            \"deleted\": 0," +
            "            \"sendertype\": 1," +
            "            \"compl_spam\": 49635," +
            "            \"week\": 533," +
            "            \"ps_spam\": 0," +
            "            \"pers_ham\": 60872," +
            "            \"hash\": 12601931603812510000," +
            "            \"compl_ham\": 7010," +
            "            \"dkim_del\": 0," +
            "            \"read\": 0," +
            "            \"kooba\": 639541," +
            "            \"ham\": 102200312," +
            "            \"ps_ham\": 0," +
            "            \"abook\": 0" +
            "        }," +
            "        {" +
            "            \"spam\": 10703592," +
            "            \"pers_spam\": 10426715," +
            "            \"dkim_spam\": 10297930," +
            "            \"dkim_ham\": 79887575," +
            "            \"deleted\": 0," +
            "            \"sendertype\": 1," +
            "            \"compl_spam\": 35950," +
            "            \"week\": 534," +
            "            \"ps_spam\": 0," +
            "            \"pers_ham\": 33076," +
            "            \"hash\": 12601931603812510000," +
            "            \"compl_ham\": 4633," +
            "            \"dkim_del\": 0," +
            "            \"read\": 0," +
            "            \"kooba\": 140093," +
            "            \"ham\": 81524681," +
            "            \"ps_ham\": 0," +
            "            \"abook\": 0" +
            "        }," +
            "        {" +
            "            \"spam\": 13365666," +
            "            \"pers_spam\": 12780634," +
            "            \"dkim_spam\": 12567078," +
            "            \"dkim_ham\": 103040581," +
            "            \"deleted\": 0," +
            "            \"sendertype\": 2," +
            "            \"compl_spam\": 71424," +
            "            \"week\": 532," +
            "            \"ps_spam\": 0," +
            "            \"pers_ham\": 17336," +
            "            \"hash\": 17148772963764100000," +
            "            \"compl_ham\": 8828," +
            "            \"dkim_del\": 0," +
            "            \"read\": 0," +
            "            \"kooba\": 221132," +
            "            \"ham\": 106105806," +
            "            \"ps_ham\": 0," +
            "            \"abook\": 0" +
            "        }," +
            "        {" +
            "            \"spam\": 13289328," +
            "            \"pers_spam\": 12824702," +
            "            \"dkim_spam\": 12657545," +
            "            \"dkim_ham\": 99841129," +
            "            \"deleted\": 0," +
            "            \"sendertype\": 2," +
            "            \"compl_spam\": 49635," +
            "            \"week\": 533," +
            "            \"ps_spam\": 0," +
            "            \"pers_ham\": 60872," +
            "            \"hash\": 17148772963764100000," +
            "            \"compl_ham\": 7010," +
            "            \"dkim_del\": 0," +
            "            \"read\": 0," +
            "            \"kooba\": 639545," +
            "            \"ham\": 102200317," +
            "            \"ps_ham\": 0," +
            "            \"abook\": 0" +
            "        }," +
            "        {" +
            "            \"spam\": 10703603," +
            "            \"pers_spam\": 10426723," +
            "            \"dkim_spam\": 10297938," +
            "            \"dkim_ham\": 79887641," +
            "            \"deleted\": 0," +
            "            \"sendertype\": 2," +
            "            \"compl_spam\": 35950," +
            "            \"week\": 534," +
            "            \"ps_spam\": 0," +
            "            \"pers_ham\": 33076," +
            "            \"hash\": 17148772963764100000," +
            "            \"compl_ham\": 4633," +
            "            \"dkim_del\": 0," +
            "            \"read\": 0," +
            "            \"kooba\": 140121," +
            "            \"ham\": 81524787," +
            "            \"ps_ham\": 0," +
            "            \"abook\": 0" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"sender_2weeks\"" +
            "}" +
        "]";

    public SenderShinglerTest() {
    }

    private void checkGetResponse(final SenderShingles result) {
        Assert.assertNotNull(result);
        {
            final var results = result.getResults();
            Assert.assertEquals(1, results.size());
            logger.info("checkGetResponse get results: " + results + ", shingles="
                    + Arrays.toString(results.get(GeneralShingles.SHINGLE).keySet().toArray()));
            Assert.assertEquals(4, results.get(GeneralShingles.SHINGLE).size()); // different shingles count
            {
                final var shingleInfo = results.get(GeneralShingles.SHINGLE).get(-1297971109945453440L);
                Assert.assertNotNull(shingleInfo);
                logger.info("checkGetResponse: schemes=" + Arrays.toString(shingleInfo.keySet().toArray()));
                Assert.assertEquals(1, shingleInfo.size());
                {
                    final Map<String, List<Object>> countersInfo = shingleInfo.get(SenderScheme.SENDER_TOTALS);
                    Assert.assertNotNull(countersInfo);
                    Assert.assertEquals((byte) 2, countersInfo.get("sendertype").get(0));
                    Assert.assertEquals(388L, countersInfo.get("ad").get(0));
                    Assert.assertEquals(7094, countersInfo.get("ct").get(0));
                    Assert.assertEquals(7487, countersInfo.get("ut").get(0));
                    Assert.assertEquals(2693639697L, countersInfo.get("hc").get(0));
                    Assert.assertEquals(432180016L, countersInfo.get("sc").get(0));
                    Assert.assertNull(countersInfo.get("ry"));
                    Assert.assertEquals(194434L, countersInfo.get("ch").get(0));
                    Assert.assertEquals(1155264L, countersInfo.get("cs").get(0));
                    Assert.assertNull(countersInfo.get("fl"));
                    Assert.assertEquals(1099345L, countersInfo.get("ph").get(0));
                    Assert.assertEquals(322920558L, countersInfo.get("ps").get(0));
                    Assert.assertEquals(25152770L, countersInfo.get("ab").get(0));
                    Assert.assertEquals(17468171L, countersInfo.get("ba").get(0));
                    Assert.assertNull(countersInfo.get("c1"));
                    Assert.assertNull(countersInfo.get("c2"));
                    Assert.assertNull(countersInfo.get("read"));
                    Assert.assertNull(countersInfo.get("deleted"));
                    Assert.assertEquals(413005390L, countersInfo.get("dkim_spam").get(0));
                    Assert.assertEquals(2627114686L, countersInfo.get("dkim_ham").get(0));
                    Assert.assertEquals(17120814L, countersInfo.get("pop3_spam").get(0));
                    Assert.assertEquals(106300192L, countersInfo.get("pop3_ham").get(0));
                    Assert.assertNull(countersInfo.get("dkim_complaint_spam"));
                    Assert.assertNull(countersInfo.get("dkim_del"));
                    Assert.assertEquals(3L, countersInfo.get("ps_ham").get(0));
                    Assert.assertEquals(13L, countersInfo.get("ps_spam").get(0));
                }
            }
            {
                final var shingleInfo = results.get(GeneralShingles.SHINGLE).get(-5844812469897042107L);
                Assert.assertNotNull(shingleInfo);
                logger.info("checkGetResponse: schemes=" + Arrays.toString(shingleInfo.keySet().toArray()));
                Assert.assertEquals(1, shingleInfo.size());
                {
                    final Map<String, List<Object>> countersInfo = shingleInfo.get(SenderScheme.SENDER_TOTALS);
                    Assert.assertNotNull(countersInfo);
                    Assert.assertEquals((byte) 1, countersInfo.get("sendertype").get(0));
                    Assert.assertEquals(750L, countersInfo.get("ad").get(0));
                    Assert.assertEquals(6732, countersInfo.get("ct").get(0));
                    Assert.assertEquals(7487, countersInfo.get("ut").get(0));
                    Assert.assertEquals(5379377768L, countersInfo.get("hc").get(0));
                    Assert.assertEquals(766541060L, countersInfo.get("sc").get(0));
                    Assert.assertEquals(4549L, countersInfo.get("ry").get(0));
                    Assert.assertEquals(436374L, countersInfo.get("ch").get(0));
                    Assert.assertEquals(2023838L, countersInfo.get("cs").get(0));
                    Assert.assertNull(countersInfo.get("fl"));
                    Assert.assertEquals(2406279L, countersInfo.get("ph").get(0));
                    Assert.assertEquals(567299788L, countersInfo.get("ps").get(0));
                    Assert.assertEquals(57120148L, countersInfo.get("ab").get(0));
                    Assert.assertEquals(42099104L, countersInfo.get("ba").get(0));
                    Assert.assertNull(countersInfo.get("c1"));
                    Assert.assertNull(countersInfo.get("c2"));
                    Assert.assertNull(countersInfo.get("read"));
                    Assert.assertNull(countersInfo.get("deleted"));
                    Assert.assertEquals(726523733L, countersInfo.get("dkim_spam").get(0));
                    Assert.assertEquals(5236823801L, countersInfo.get("dkim_ham").get(0));
                    Assert.assertEquals(34509859L, countersInfo.get("pop3_spam").get(0));
                    Assert.assertEquals(204906463L, countersInfo.get("pop3_ham").get(0));
                    Assert.assertNull(countersInfo.get("dkim_complaint_spam"));
                    Assert.assertNull(countersInfo.get("dkim_del"));
                    Assert.assertEquals(4263L, countersInfo.get("ps_ham").get(0));
                    Assert.assertEquals(216L, countersInfo.get("ps_spam").get(0));
                }
            }
            {
                final var shingleInfo = results.get(GeneralShingles.SHINGLE)
                    .get(Long.parseUnsignedLong("12601931603812510000"));
                Assert.assertNotNull(shingleInfo);
                logger.info("checkGetResponse: schemes=" + Arrays.toString(shingleInfo.keySet().toArray()));
                Assert.assertEquals(1, shingleInfo.size());
                {
                    final Map<String, List<Object>> countersInfo = shingleInfo.get(SenderScheme.SENDER_2WEEKS);
                    Assert.assertNotNull(countersInfo);

                    Assert.assertEquals((byte) 1, countersInfo.get("sendertype").get(0));
                    Assert.assertEquals(106105798L, countersInfo.get("ham").get(0));
                    Assert.assertEquals(13365663L, countersInfo.get("spam").get(0));
                    Assert.assertEquals(8828L, countersInfo.get("compl_ham").get(0));
                    Assert.assertEquals(71424L, countersInfo.get("compl_spam").get(0));
                    Assert.assertEquals(17336L, countersInfo.get("pers_ham").get(0));
                    Assert.assertEquals(12780634L, countersInfo.get("pers_spam").get(0));
                    Assert.assertEquals(0L, countersInfo.get("abook").get(0));
                    Assert.assertEquals(221129L, countersInfo.get("kooba").get(0));
                    Assert.assertEquals(0L, countersInfo.get("read").get(0));
                    Assert.assertEquals(0L, countersInfo.get("deleted").get(0));
                    Assert.assertEquals(12567078L, countersInfo.get("dkim_spam").get(0));
                    Assert.assertEquals(103040581L, countersInfo.get("dkim_ham").get(0));
                    Assert.assertEquals(0L, countersInfo.get("dkim_del").get(0));
                    Assert.assertEquals(0L, countersInfo.get("ps_ham").get(0));
                    Assert.assertEquals(0L, countersInfo.get("ps_spam").get(0));
                    Assert.assertEquals(532, countersInfo.get("week").get(0));

                    Assert.assertEquals((byte) 1, countersInfo.get("sendertype").get(1));
                    Assert.assertEquals(102200312L, countersInfo.get("ham").get(1));
                    Assert.assertEquals(13289326L, countersInfo.get("spam").get(1));
                    Assert.assertEquals(7010L, countersInfo.get("compl_ham").get(1));
                    Assert.assertEquals(49635L, countersInfo.get("compl_spam").get(1));
                    Assert.assertEquals(60872L, countersInfo.get("pers_ham").get(1));
                    Assert.assertEquals(12824702L, countersInfo.get("pers_spam").get(1));
                    Assert.assertEquals(0L, countersInfo.get("abook").get(1));
                    Assert.assertEquals(639541L, countersInfo.get("kooba").get(1));
                    Assert.assertEquals(0L, countersInfo.get("read").get(1));
                    Assert.assertEquals(0L, countersInfo.get("deleted").get(1));
                    Assert.assertEquals(12657545L, countersInfo.get("dkim_spam").get(1));
                    Assert.assertEquals(99841129L, countersInfo.get("dkim_ham").get(1));
                    Assert.assertEquals(0L, countersInfo.get("dkim_del").get(1));
                    Assert.assertEquals(0L, countersInfo.get("ps_ham").get(1));
                    Assert.assertEquals(0L, countersInfo.get("ps_spam").get(1));
                    Assert.assertEquals(533, countersInfo.get("week").get(1));

                    Assert.assertEquals((byte) 1, countersInfo.get("sendertype").get(2));
                    Assert.assertEquals(81524681L, countersInfo.get("ham").get(2));
                    Assert.assertEquals(10703592L, countersInfo.get("spam").get(2));
                    Assert.assertEquals(4633L, countersInfo.get("compl_ham").get(2));
                    Assert.assertEquals(35950L, countersInfo.get("compl_spam").get(2));
                    Assert.assertEquals(33076L, countersInfo.get("pers_ham").get(2));
                    Assert.assertEquals(10426715L, countersInfo.get("pers_spam").get(2));
                    Assert.assertEquals(0L, countersInfo.get("abook").get(2));
                    Assert.assertEquals(140093L, countersInfo.get("kooba").get(2));
                    Assert.assertEquals(0L, countersInfo.get("read").get(2));
                    Assert.assertEquals(0L, countersInfo.get("deleted").get(2));
                    Assert.assertEquals(10297930L, countersInfo.get("dkim_spam").get(2));
                    Assert.assertEquals(79887575L, countersInfo.get("dkim_ham").get(2));
                    Assert.assertEquals(0L, countersInfo.get("dkim_del").get(2));
                    Assert.assertEquals(0L, countersInfo.get("ps_ham").get(2));
                    Assert.assertEquals(0L, countersInfo.get("ps_spam").get(2));
                    Assert.assertEquals(534, countersInfo.get("week").get(2));
                }
            }
            {
                final var shingleInfo = results.get(GeneralShingles.SHINGLE)
                        .get(Long.parseUnsignedLong("17148772963764100000"));
                Assert.assertNotNull(shingleInfo);
                logger.info("checkGetResponse: schemes=" + Arrays.toString(shingleInfo.keySet().toArray()));
                Assert.assertEquals(1, shingleInfo.size());
                {
                    final Map<String, List<Object>> countersInfo = shingleInfo.get(SenderScheme.SENDER_2WEEKS);
                    Assert.assertNotNull(countersInfo);

                    Assert.assertEquals((byte) 2, countersInfo.get("sendertype").get(0));
                    Assert.assertEquals(106105806L, countersInfo.get("ham").get(0));
                    Assert.assertEquals(13365666L, countersInfo.get("spam").get(0));
                    Assert.assertEquals(8828L, countersInfo.get("compl_ham").get(0));
                    Assert.assertEquals(71424L, countersInfo.get("compl_spam").get(0));
                    Assert.assertEquals(17336L, countersInfo.get("pers_ham").get(0));
                    Assert.assertEquals(12780634L, countersInfo.get("pers_spam").get(0));
                    Assert.assertEquals(0L, countersInfo.get("abook").get(0));
                    Assert.assertEquals(221132L, countersInfo.get("kooba").get(0));
                    Assert.assertEquals(0L, countersInfo.get("read").get(0));
                    Assert.assertEquals(0L, countersInfo.get("deleted").get(0));
                    Assert.assertEquals(12567078L, countersInfo.get("dkim_spam").get(0));
                    Assert.assertEquals(103040581L, countersInfo.get("dkim_ham").get(0));
                    Assert.assertEquals(0L, countersInfo.get("dkim_del").get(0));
                    Assert.assertEquals(0L, countersInfo.get("ps_ham").get(0));
                    Assert.assertEquals(0L, countersInfo.get("ps_spam").get(0));
                    Assert.assertEquals(532, countersInfo.get("week").get(0));

                    Assert.assertEquals((byte) 2, countersInfo.get("sendertype").get(1));
                    Assert.assertEquals(102200317L, countersInfo.get("ham").get(1));
                    Assert.assertEquals(13289328L, countersInfo.get("spam").get(1));
                    Assert.assertEquals(7010L, countersInfo.get("compl_ham").get(1));
                    Assert.assertEquals(49635L, countersInfo.get("compl_spam").get(1));
                    Assert.assertEquals(60872L, countersInfo.get("pers_ham").get(1));
                    Assert.assertEquals(12824702L, countersInfo.get("pers_spam").get(1));
                    Assert.assertEquals(0L, countersInfo.get("abook").get(1));
                    Assert.assertEquals(639545L, countersInfo.get("kooba").get(1));
                    Assert.assertEquals(0L, countersInfo.get("read").get(1));
                    Assert.assertEquals(0L, countersInfo.get("deleted").get(1));
                    Assert.assertEquals(12657545L, countersInfo.get("dkim_spam").get(1));
                    Assert.assertEquals(99841129L, countersInfo.get("dkim_ham").get(1));
                    Assert.assertEquals(0L, countersInfo.get("dkim_del").get(1));
                    Assert.assertEquals(0L, countersInfo.get("ps_ham").get(1));
                    Assert.assertEquals(0L, countersInfo.get("ps_spam").get(1));
                    Assert.assertEquals(533, countersInfo.get("week").get(1));

                    Assert.assertEquals((byte) 2, countersInfo.get("sendertype").get(2));
                    Assert.assertEquals(81524787L, countersInfo.get("ham").get(2));
                    Assert.assertEquals(10703603L, countersInfo.get("spam").get(2));
                    Assert.assertEquals(4633L, countersInfo.get("compl_ham").get(2));
                    Assert.assertEquals(35950L, countersInfo.get("compl_spam").get(2));
                    Assert.assertEquals(33076L, countersInfo.get("pers_ham").get(2));
                    Assert.assertEquals(10426723L, countersInfo.get("pers_spam").get(2));
                    Assert.assertEquals(0L, countersInfo.get("abook").get(2));
                    Assert.assertEquals(140121L, countersInfo.get("kooba").get(2));
                    Assert.assertEquals(0L, countersInfo.get("read").get(2));
                    Assert.assertEquals(0L, countersInfo.get("deleted").get(2));
                    Assert.assertEquals(10297938L, countersInfo.get("dkim_spam").get(2));
                    Assert.assertEquals(79887641L, countersInfo.get("dkim_ham").get(2));
                    Assert.assertEquals(0L, countersInfo.get("dkim_del").get(2));
                    Assert.assertEquals(0L, countersInfo.get("ps_ham").get(2));
                    Assert.assertEquals(0L, countersInfo.get("ps_spam").get(2));
                    Assert.assertEquals(534, countersInfo.get("week").get(2));
                }
            }
        }
    }

    @Test
    public void testSenderShinglerGetRequest() throws Exception {
        try (StaticServer shingler = new StaticServer(Configs.baseConfig("SenderShingler"));
             final SharedConnectingIOReactor reactor =
                 new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final SenderShinglerClient senderShinglerClient =
                 new SenderShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            senderShinglerClient.start();

            final SenderShingles data = new SenderShingles(TypesafeValueContentHandler.parse(GET_RESPONSE));

            shingler.add(GeneralShinglerClient.URI, GET_RESPONSE);

            final Future<?> future = senderShinglerClient.getShingles(data, EmptyFutureCallback.INSTANCE);
            final SenderShingles result = (SenderShingles) future.get();

            checkGetResponse(result);
            waitServerRequests(shingler, GeneralShinglerClient.URI, 1);
        }
    }

    @Test
    public void testSenderShinglerPutRequest() throws Exception {
        try (TestShinglerServer shingler = new TestShinglerServer(Configs.baseConfig("SenderShingler"));
             final SharedConnectingIOReactor reactor =
                 new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final SenderShinglerClient senderShinglerClient =
                 new SenderShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            senderShinglerClient.start();

            final SenderShingles data = new SenderShingles(TypesafeValueContentHandler.parse(GET_RESPONSE));

            final String putBody = senderShinglerClient.getPutQuery(data, false);
            shingler.add(GeneralShinglerClient.URI, putBody, HttpStatus.SC_OK);
            logger.info("PUT Query: " + putBody);

            final String getBody = senderShinglerClient.getGetQuery(data);
            shingler.add(GeneralShinglerClient.URI, getBody, GET_RESPONSE);
            logger.info("GET Query: " + getBody);

            final Future<?> putFuture =
                senderShinglerClient.putShingles(putBody, AbstractShinglerClient.StringFutureCallback.INSTANCE);
            final String putResult = (String) putFuture.get();

            Assert.assertNotNull(putResult);

            final Future<?> getFuture = senderShinglerClient.getShingles(data, EmptyFutureCallback.INSTANCE);
            final SenderShingles getResult = (SenderShingles) getFuture.get();

            checkGetResponse(getResult);

            waitServerRequests(shingler, GeneralShinglerClient.URI, putBody,1);
            waitServerRequests(shingler, GeneralShinglerClient.URI, getBody,1);
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
