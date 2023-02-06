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

public class UrlShinglerTest extends TestBase {
    private static final int TIMEOUT = 10000;
    private static final int INTERVAL = 100;

    private static final String GET_RESPONSE = "[" +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"date\": 7487," +
            "            \"ham\": 136226," +
            "            \"complaint_spam\": 31," +
            "            \"spam\": 832," +
            "            \"shingle\": 16080817364137480000," +
            "            \"complaint_ham\": 11" +
            "        }," +
            "        {" +
            "            \"date\": 7487," +
            "            \"ham\": 132099," +
            "            \"complaint_spam\": 0," +
            "            \"spam\": 686," +
            "            \"shingle\": 11043893452938396000," +
            "            \"complaint_ham\": 0" +
            "        }," +
            "        {" +
            "            \"date\": 7487," +
            "            \"ham\": 137050," +
            "            \"complaint_spam\": 30," +
            "            \"spam\": 787," +
            "            \"shingle\": 699704476085557400," +
            "            \"complaint_ham\": 11" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"today\"" +
            "}," +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"date\": 7487," +
            "            \"ham\": 774017023," +
            "            \"complaint_spam\": 2236824," +
            "            \"spam\": 46261356," +
            "            \"create_date\": 4923," +
            "            \"shingle\": 16080817364137480000," +
            "            \"complaint_ham\": 129496" +
            "        }," +
            "        {" +
            "            \"date\": 7487," +
            "            \"ham\": 420316882," +
            "            \"complaint_spam\": 241248," +
            "            \"spam\": 38221870," +
            "            \"create_date\": 4923," +
            "            \"shingle\": 11043893452938396000," +
            "            \"complaint_ham\": 42556" +
            "        }," +
            "        {" +
            "            \"date\": 7487," +
            "            \"ham\": 265065335," +
            "            \"complaint_spam\": 398102," +
            "            \"spam\": 19851832," +
            "            \"create_date\": 4923," +
            "            \"shingle\": 699704476085557400," +
            "            \"complaint_ham\": 69491" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"history\"" +
            "}," +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"day_from_millennium\": 7487" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"current\"" +
            "}," +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"value\": 4464206597446237000" +
            "        }," +
            "        {" +
            "            \"value\": 11437300496171442000" +
            "        }," +
            "        {" +
            "            \"value\": 13412132006994153000" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"malware\"" +
            "}," +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"resolved\": []," +
            "            \"host\": \"maxpark.com\"," +
            "            \"alias\": \"dbl\"" +
            "        }," +
            "        {" +
            "            \"resolved\": []," +
            "            \"host\": \"maxpark.com\"," +
            "            \"alias\": \"malware\"" +
            "        }," +
            "        {" +
            "            \"resolved\": []," +
            "            \"host\": \"maxpark.com\"," +
            "            \"alias\": \"malwareaggress\"" +
            "        }," +
            "        {" +
            "            \"resolved\": []," +
            "            \"host\": \"maxpark.com\"," +
            "            \"alias\": \"surbl\"" +
            "        }," +
            "        {" +
            "            \"resolved\": []," +
            "            \"host\": \"gidepark.ru\"," +
            "            \"alias\": \"dbl\"" +
            "        }," +
            "        {" +
            "            \"resolved\": []," +
            "            \"host\": \"gidepark.ru\"," +
            "            \"alias\": \"malware\"" +
            "        }," +
            "        {" +
            "            \"resolved\": []," +
            "            \"host\": \"gidepark.ru\"," +
            "            \"alias\": \"malwareaggress\"" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"dns\"" +
            "}" +
        "]";

    public UrlShinglerTest() {
    }

    private void checkGetResponse(final UrlShingles result) {
        Assert.assertNotNull(result);
        {
            final var results = result.getResults();
            Assert.assertEquals(1, results.size());
            Assert.assertEquals(4, results.get(GeneralShingles.SHINGLE).size());
            logger.info("checkGetResponse get results: " + results + ", shingles="
                + Arrays.toString(results.get(GeneralShingles.SHINGLE).keySet().toArray()));
            {
                final var shingleInfo = results.get(GeneralShingles.SHINGLE).get(0L);
                Assert.assertNotNull(shingleInfo);
                logger.info("checkGetResponse: schemes=" + Arrays.toString(shingleInfo.keySet().toArray()));
                Assert.assertEquals(3, shingleInfo.size());
                {
                    final Map<String, List<Object>> currentInfo = shingleInfo.get(UrlScheme.CURRENT);
                    Assert.assertNotNull(currentInfo);
                    Assert.assertEquals(7487, currentInfo.get("day_from_millennium").get(0));
                }
                {
                    final Map<String, List<Object>> malwareInfo = shingleInfo.get(UrlScheme.MALWARE);
                    Assert.assertNotNull(malwareInfo);
                    Assert.assertEquals(4464206597446237000L, malwareInfo.get("value").get(0));
                    Assert.assertEquals(Long.parseUnsignedLong("11437300496171442000"), malwareInfo.get("value").get(1));
                    Assert.assertEquals(Long.parseUnsignedLong("13412132006994153000"), malwareInfo.get("value").get(2));
                }
                {
                    final Map<String, List<Object>> dnsInfo = shingleInfo.get(UrlScheme.DNS);
                    Assert.assertNotNull(dnsInfo);
                    Assert.assertEquals("dbl", dnsInfo.get("alias").get(0));
                    Assert.assertEquals("maxpark.com", dnsInfo.get("host").get(0));

                    Assert.assertEquals("malware", dnsInfo.get("alias").get(1));
                    Assert.assertEquals("maxpark.com", dnsInfo.get("host").get(1));

                    Assert.assertEquals("malwareaggress", dnsInfo.get("alias").get(2));
                    Assert.assertEquals("maxpark.com", dnsInfo.get("host").get(2));

                    Assert.assertEquals("malware", dnsInfo.get("alias").get(5));
                    Assert.assertEquals("gidepark.ru", dnsInfo.get("host").get(5));

                    Assert.assertEquals("malwareaggress", dnsInfo.get("alias").get(6));
                    Assert.assertEquals("gidepark.ru", dnsInfo.get("host").get(6));
                }
            }
            {
                final var shingleInfo = results.get(GeneralShingles.SHINGLE)
                    .get(Long.parseUnsignedLong("16080817364137480000"));
                Assert.assertNotNull(shingleInfo);
                logger.info("checkGetResponse: schemes=" + Arrays.toString(shingleInfo.keySet().toArray()));
                Assert.assertEquals(2, shingleInfo.size());
                {
                    final Map<String, List<Object>> todayInfo = shingleInfo.get(UrlScheme.TODAY);
                    Assert.assertNotNull(todayInfo);
                    Assert.assertEquals(Long.parseUnsignedLong("16080817364137480000"), todayInfo.get("shingle").get(0));
                    Assert.assertEquals(7487, todayInfo.get("date").get(0));
                    Assert.assertEquals(136226L, todayInfo.get("ham").get(0));
                    Assert.assertEquals(832L, todayInfo.get("spam").get(0));
                    Assert.assertEquals(11L, todayInfo.get("complaint_ham").get(0));
                    Assert.assertEquals(31L, todayInfo.get("complaint_spam").get(0));
                }
                {
                    final Map<String, List<Object>> historyInfo = shingleInfo.get(UrlScheme.HISTORY);
                    Assert.assertNotNull(historyInfo);
                    Assert.assertEquals(Long.parseUnsignedLong("16080817364137480000"), historyInfo.get("shingle").get(0));
                    Assert.assertEquals(7487, historyInfo.get("date").get(0));
                    Assert.assertEquals(4923, historyInfo.get("create_date").get(0));
                    Assert.assertEquals(774017023L, historyInfo.get("ham").get(0));
                    Assert.assertEquals(46261356L, historyInfo.get("spam").get(0));
                    Assert.assertEquals(129496L, historyInfo.get("complaint_ham").get(0));
                    Assert.assertEquals(2236824L, historyInfo.get("complaint_spam").get(0));
                }
            }
            {
                final var shingleInfo = results.get(GeneralShingles.SHINGLE)
                    .get(Long.parseUnsignedLong("11043893452938396000"));
                Assert.assertNotNull(shingleInfo);
                logger.info("checkGetResponse: schemes=" + Arrays.toString(shingleInfo.keySet().toArray()));
                Assert.assertEquals(2, shingleInfo.size());
                {
                    final Map<String, List<Object>> todayInfo = shingleInfo.get(UrlScheme.TODAY);
                    Assert.assertNotNull(todayInfo);
                    Assert.assertEquals(Long.parseUnsignedLong("11043893452938396000"), todayInfo.get("shingle").get(0));
                    Assert.assertEquals(7487, todayInfo.get("date").get(0));
                    Assert.assertEquals(132099L, todayInfo.get("ham").get(0));
                    Assert.assertEquals(686L, todayInfo.get("spam").get(0));
                    Assert.assertEquals(0L, todayInfo.get("complaint_ham").get(0));
                    Assert.assertEquals(0L, todayInfo.get("complaint_spam").get(0));
                }
                {
                    final Map<String, List<Object>> historyInfo = shingleInfo.get(UrlScheme.HISTORY);
                    Assert.assertNotNull(historyInfo);
                    Assert.assertEquals(Long.parseUnsignedLong("11043893452938396000"), historyInfo.get("shingle").get(0));
                    Assert.assertEquals(7487, historyInfo.get("date").get(0));
                    Assert.assertEquals(4923, historyInfo.get("create_date").get(0));
                    Assert.assertEquals(420316882L, historyInfo.get("ham").get(0));
                    Assert.assertEquals(38221870L, historyInfo.get("spam").get(0));
                    Assert.assertEquals(42556L, historyInfo.get("complaint_ham").get(0));
                    Assert.assertEquals(241248L, historyInfo.get("complaint_spam").get(0));
                }
            }
            {
                final var shingleInfo = results.get(GeneralShingles.SHINGLE).get(699704476085557400L);
                Assert.assertNotNull(shingleInfo);
                logger.info("checkGetResponse: schemes=" + Arrays.toString(shingleInfo.keySet().toArray()));
                Assert.assertEquals(2, shingleInfo.size());
                {
                    final Map<String, List<Object>> todayInfo = shingleInfo.get(UrlScheme.TODAY);
                    Assert.assertNotNull(todayInfo);
                    Assert.assertEquals(699704476085557400L, todayInfo.get("shingle").get(0));
                    Assert.assertEquals(7487, todayInfo.get("date").get(0));
                    Assert.assertEquals(137050L, todayInfo.get("ham").get(0));
                    Assert.assertEquals(787L, todayInfo.get("spam").get(0));
                    Assert.assertEquals(11L, todayInfo.get("complaint_ham").get(0));
                    Assert.assertEquals(30L, todayInfo.get("complaint_spam").get(0));
                }
                {
                    final Map<String, List<Object>> historyInfo = shingleInfo.get(UrlScheme.HISTORY);
                    Assert.assertNotNull(historyInfo);
                    Assert.assertEquals(699704476085557400L, historyInfo.get("shingle").get(0));
                    Assert.assertEquals(7487, historyInfo.get("date").get(0));
                    Assert.assertEquals(4923, historyInfo.get("create_date").get(0));
                    Assert.assertEquals(265065335L, historyInfo.get("ham").get(0));
                    Assert.assertEquals(19851832L, historyInfo.get("spam").get(0));
                    Assert.assertEquals(69491L, historyInfo.get("complaint_ham").get(0));
                    Assert.assertEquals(398102L, historyInfo.get("complaint_spam").get(0));
                }
            }
        }
    }

    @Test
    public void testUrlShinglerGetRequest() throws Exception {
        try (StaticServer shingler = new StaticServer(Configs.baseConfig("UrlShingler"));
             final SharedConnectingIOReactor reactor =
                 new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final UrlShinglerClient urlShinglerClient = new UrlShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            urlShinglerClient.start();

            final UrlShingles data = new UrlShingles(TypesafeValueContentHandler.parse(GET_RESPONSE));

            shingler.add(GeneralShinglerClient.URI, GET_RESPONSE);

            final Future<?> future = urlShinglerClient.getShingles(data, EmptyFutureCallback.INSTANCE);
            final UrlShingles result = (UrlShingles) future.get();

            checkGetResponse(result);
            waitServerRequests(shingler, GeneralShinglerClient.URI, 1);
        }
    }

    @Test
    public void testUrlShinglerPutRequest() throws Exception {
        try (TestShinglerServer shingler = new TestShinglerServer(Configs.baseConfig("UrlShingler"));
             final SharedConnectingIOReactor reactor =
                 new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final UrlShinglerClient urlShinglerClient = new UrlShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            urlShinglerClient.start();

            final UrlShingles data = new UrlShingles(TypesafeValueContentHandler.parse(GET_RESPONSE));

            final String putBody = urlShinglerClient.getPutQuery(data);
            shingler.add(GeneralShinglerClient.URI, putBody, HttpStatus.SC_OK);
            logger.info("PUT Query: " + putBody);

            final String getBody = urlShinglerClient.getGetQuery(data);
            shingler.add(GeneralShinglerClient.URI, getBody, GET_RESPONSE);
            logger.info("GET Query: " + getBody);

            final Future<?> putFuture =
                urlShinglerClient.putShingles(data, AbstractShinglerClient.StringFutureCallback.INSTANCE);
            final String putResult = (String) putFuture.get();

            Assert.assertNotNull(putResult);

            final Future<?> getFuture = urlShinglerClient.getShingles(data, EmptyFutureCallback.INSTANCE);
            final UrlShingles getResult = (UrlShingles) getFuture.get();

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
