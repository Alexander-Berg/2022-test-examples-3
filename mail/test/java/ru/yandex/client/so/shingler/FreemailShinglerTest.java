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

public class FreemailShinglerTest extends TestBase {
    private static final int TIMEOUT = 10000;
    private static final int INTERVAL = 100;

    private static final String GET_RESPONSE1 = "[" +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"data\": \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\"?><pddinfo><ip>95.31.1.194</ip>" +
            "<karma>50</karma><firsttime>1530457063.0</firsttime><mailboxcnt>106</mailboxcnt><admlogin>136412480" +
            "</admlogin><org_id>219017</org_id><domains><domain>uk-t.ru</domain></domains></pddinfo>\"," +
            "            \"request\": \"so/domaininfo/\"," +
            "            \"param\": {" +
            "                \"domain\": \"uk-t.ru\"" +
            "            }," +
            "            \"code\": 200" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"pdd\"" +
            "}," +
            "{" +
            "    \"find\": []," +
            "    \"scheme\": \"domain\"" +
            "}," +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"date\": 7405," +
            "            \"domain\": \"uk-t.ru\"," +
            "            \"count\": 3" +
            "        }," +
            "        {" +
            "            \"date\": 7408," +
            "            \"domain\": \"uk-t.ru\"," +
            "            \"count\": 1" +
            "        }," +
            "        {" +
            "            \"date\": 7412," +
            "            \"domain\": \"uk-t.ru\"," +
            "            \"count\": 2" +
            "        }," +
            "        {" +
            "            \"date\": 7415," +
            "            \"domain\": \"uk-t.ru\"," +
            "            \"count\": 1" +
            "        }," +
            "        {" +
            "            \"date\": 7424," +
            "            \"domain\": \"uk-t.ru\"," +
            "            \"count\": 1" +
            "        }," +
            "        {" +
            "            \"date\": 7433," +
            "            \"domain\": \"uk-t.ru\"," +
            "            \"count\": 2" +
            "        }," +
            "        {" +
            "            \"date\": 7444," +
            "            \"domain\": \"uk-t.ru\"," +
            "            \"count\": 1" +
            "        }," +
            "        {" +
            "            \"date\": 7453," +
            "            \"domain\": \"uk-t.ru\"," +
            "            \"count\": 3" +
            "        }," +
            "        {" +
            "            \"date\": 7458," +
            "            \"domain\": \"uk-t.ru\"," +
            "            \"count\": 1" +
            "        }," +
            "        {" +
            "            \"date\": 7474," +
            "            \"domain\": \"uk-t.ru\"," +
            "            \"count\": 2" +
            "        }," +
            "        {" +
            "            \"date\": 7482," +
            "            \"domain\": \"uk-t.ru\"," +
            "            \"count\": 1" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"domain_all\"" +
            "}"
        + "]";
    private static final String GET_RESPONSE2 = "[" +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"complaint_ham\": 0," +
            "            \"complaint_spam\": 1," +
            "            \"send_ham\": 994," +
            "            \"receive_ham\": 196," +
            "            \"uuid\": 3314508500074748400," +
            "            \"date\": 493," +
            "            \"receive_spam\": 1," +
            "            \"recepients_count\": 62," +
            "            \"send_spam\": 0," +
            "            \"recepients_max_count\": 156," +
            "            \"current_day\": 7409" +
            "        }," +
            "        {" +
            "            \"complaint_ham\": 0," +
            "            \"complaint_spam\": 0," +
            "            \"send_ham\": 770," +
            "            \"receive_ham\": 539," +
            "            \"uuid\": 3314508500074748400," +
            "            \"date\": 494," +
            "            \"receive_spam\": 0," +
            "            \"recepients_count\": 45," +
            "            \"send_spam\": 0," +
            "            \"recepients_max_count\": 111," +
            "            \"current_day\": 7424" +
            "        }," +
            "        {" +
            "            \"complaint_ham\": 0," +
            "            \"complaint_spam\": 0," +
            "            \"send_ham\": 354," +
            "            \"receive_ham\": 263," +
            "            \"uuid\": 3314508500074748400," +
            "            \"date\": 495," +
            "            \"receive_spam\": 0," +
            "            \"recepients_count\": 30," +
            "            \"send_spam\": 0," +
            "            \"recepients_max_count\": 60," +
            "            \"current_day\": 7439" +
            "        }," +
            "        {" +
            "            \"complaint_ham\": 0," +
            "            \"complaint_spam\": 0," +
            "            \"send_ham\": 271," +
            "            \"receive_ham\": 175," +
            "            \"uuid\": 3314508500074748400," +
            "            \"date\": 496," +
            "            \"receive_spam\": 0," +
            "            \"recepients_count\": 5," +
            "            \"send_spam\": 0," +
            "            \"recepients_max_count\": 108," +
            "            \"current_day\": 7454" +
            "        }," +
            "        {" +
            "            \"complaint_ham\": 0," +
            "            \"complaint_spam\": 0," +
            "            \"send_ham\": 88," +
            "            \"receive_ham\": 66," +
            "            \"uuid\": 3314508500074748400," +
            "            \"date\": 499," +
            "            \"receive_spam\": 0," +
            "            \"recepients_count\": 38," +
            "            \"send_spam\": 0," +
            "            \"recepients_max_count\": 50," +
            "            \"current_day\": 7486" +
            "        }" +
            "    ]," +
            "   \"scheme\": \"counters_get\"" +
            "}," +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"date\": 7486," +
            "            \"geo\": \"RU\"," +
            "            \"uuid\": 3314508500074748400" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"geo_get\"" +
            "}," +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"date\": 7486," +
            "            \"active_days\": 730," +
            "            \"create_date\": 6379," +
            "            \"uuid\": 3314508500074748400" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"time_get\"" +
            "}," +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"date\": 7405," +
            "            \"hash\": 6396958541402060000," +
            "            \"spam\": true," +
            "            \"uuid\": 3314508500074748400" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"complaint_get\"" +
            "}," +
            "{" +
            "    \"find\": []," +
            "    \"scheme\": \"bounce_get\"" +
            "}," +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"day_from_millennium\": 7486" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"current\"" +
            "}" +
        "]";

    public FreemailShinglerTest() {
    }

    private void checkGetResponse1(final FreemailShingles result) {
        Assert.assertNotNull(result);
        {
            final var results = result.getResults();
            Assert.assertEquals(1, results.size());
            Assert.assertEquals(1, results.get(GeneralShingles.SHINGLE).size());
            logger.info("checkGetResponse get results: " + results + ", shingles="
                + Arrays.toString(results.get(GeneralShingles.SHINGLE).keySet().toArray()));
            final var shingleInfo = results.get(GeneralShingles.SHINGLE).get(0L);
            Assert.assertNotNull(shingleInfo);
            logger.info("checkGetResponse1: schemes=" + Arrays.toString(shingleInfo.keySet().toArray()));
            Assert.assertEquals(2, shingleInfo.size());
            {
                final Map<String, List<Object>> pddInfo = shingleInfo.get(FreemailScheme.PDD);
                Assert.assertNotNull(pddInfo);
                Assert.assertEquals("uk-t.ru", pddInfo.get("param.domain").get(0));
                Assert.assertEquals("so/domaininfo/", pddInfo.get("request").get(0));
                Assert.assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?><pddinfo><ip>95.31.1.194</ip>"
                    + "<karma>50</karma><firsttime>1530457063.0</firsttime><mailboxcnt>106</mailboxcnt><admlogin>"
                    + "136412480</admlogin><org_id>219017</org_id><domains><domain>uk-t.ru</domain></domains>"
                    + "</pddinfo>", pddInfo.get("data").get(0));
                Assert.assertEquals(200, pddInfo.get("code").get(0));
            }
            {
                final Map<String, List<Object>> domainAllInfo = shingleInfo.get(FreemailScheme.DOMAIN_ALL);
                Assert.assertNotNull(domainAllInfo);
                Assert.assertEquals(7405, domainAllInfo.get("date").get(0));
                Assert.assertEquals("uk-t.ru", domainAllInfo.get("domain").get(0));
                Assert.assertEquals(3, domainAllInfo.get("count").get(0));

                Assert.assertEquals(7408, domainAllInfo.get("date").get(1));
                Assert.assertEquals("uk-t.ru", domainAllInfo.get("domain").get(1));
                Assert.assertEquals(1, domainAllInfo.get("count").get(1));

                Assert.assertEquals(7412, domainAllInfo.get("date").get(2));
                Assert.assertEquals("uk-t.ru", domainAllInfo.get("domain").get(2));
                Assert.assertEquals(2, domainAllInfo.get("count").get(2));

                Assert.assertEquals(7482, domainAllInfo.get("date").get(10));
                Assert.assertEquals("uk-t.ru", domainAllInfo.get("domain").get(10));
                Assert.assertEquals(1, domainAllInfo.get("count").get(10));
            }
        }
    }

    private void checkGetResponse2(final FreemailShingles result) {
        Assert.assertNotNull(result);
        {
            final var results = result.getResults();
            Assert.assertEquals(1, results.size());
            Assert.assertEquals(2, results.get(GeneralShingles.SHINGLE).size());
            logger.info("checkGetResponse get results: " + results + ", shingles="
                + Arrays.toString(results.get(GeneralShingles.SHINGLE).keySet().toArray()));
            {
                final var shingleInfo = results.get(GeneralShingles.SHINGLE).get(3314508500074748400L);
                Assert.assertNotNull(shingleInfo);
                logger.info("checkGetResponse2: schemes=" + Arrays.toString(shingleInfo.keySet().toArray()));
                Assert.assertEquals(4, shingleInfo.size());
                {
                    final Map<String, List<Object>> countersGetInfo = shingleInfo.get(FreemailScheme.COUNTERS_GET);
                    Assert.assertNotNull(countersGetInfo);
                    Assert.assertEquals(3314508500074748400L, countersGetInfo.get("uuid").get(0));
                    Assert.assertEquals(493, countersGetInfo.get("date").get(0));
                    Assert.assertEquals(0, countersGetInfo.get("complaint_ham").get(0));
                    Assert.assertEquals(1, countersGetInfo.get("complaint_spam").get(0));
                    Assert.assertEquals(994L, countersGetInfo.get("send_ham").get(0));
                    Assert.assertEquals(0L, countersGetInfo.get("send_spam").get(0));
                    Assert.assertEquals(196L, countersGetInfo.get("receive_ham").get(0));
                    Assert.assertEquals(1L, countersGetInfo.get("receive_spam").get(0));
                    Assert.assertEquals(62L, countersGetInfo.get("recepients_count").get(0));
                    Assert.assertEquals(156L, countersGetInfo.get("recepients_max_count").get(0));
                    Assert.assertEquals(7409, countersGetInfo.get("current_day").get(0));

                    Assert.assertEquals(3314508500074748400L, countersGetInfo.get("uuid").get(1));
                    Assert.assertEquals(494, countersGetInfo.get("date").get(1));
                    Assert.assertEquals(0, countersGetInfo.get("complaint_ham").get(1));
                    Assert.assertEquals(0, countersGetInfo.get("complaint_spam").get(1));
                    Assert.assertEquals(770L, countersGetInfo.get("send_ham").get(1));
                    Assert.assertEquals(0L, countersGetInfo.get("send_spam").get(1));
                    Assert.assertEquals(539L, countersGetInfo.get("receive_ham").get(1));
                    Assert.assertEquals(0L, countersGetInfo.get("receive_spam").get(1));
                    Assert.assertEquals(45L, countersGetInfo.get("recepients_count").get(1));
                    Assert.assertEquals(111L, countersGetInfo.get("recepients_max_count").get(1));
                    Assert.assertEquals(7424, countersGetInfo.get("current_day").get(1));

                    Assert.assertEquals(3314508500074748400L, countersGetInfo.get("uuid").get(2));
                    Assert.assertEquals(495, countersGetInfo.get("date").get(2));
                    Assert.assertEquals(0, countersGetInfo.get("complaint_ham").get(2));
                    Assert.assertEquals(0, countersGetInfo.get("complaint_spam").get(2));
                    Assert.assertEquals(354L, countersGetInfo.get("send_ham").get(2));
                    Assert.assertEquals(0L, countersGetInfo.get("send_spam").get(2));
                    Assert.assertEquals(263L, countersGetInfo.get("receive_ham").get(2));
                    Assert.assertEquals(0L, countersGetInfo.get("receive_spam").get(2));
                    Assert.assertEquals(30L, countersGetInfo.get("recepients_count").get(2));
                    Assert.assertEquals(60L, countersGetInfo.get("recepients_max_count").get(2));
                    Assert.assertEquals(7439, countersGetInfo.get("current_day").get(2));

                    Assert.assertEquals(3314508500074748400L, countersGetInfo.get("uuid").get(4));
                    Assert.assertEquals(499, countersGetInfo.get("date").get(4));
                    Assert.assertEquals(0, countersGetInfo.get("complaint_ham").get(4));
                    Assert.assertEquals(0, countersGetInfo.get("complaint_spam").get(4));
                    Assert.assertEquals(88L, countersGetInfo.get("send_ham").get(4));
                    Assert.assertEquals(0L, countersGetInfo.get("send_spam").get(4));
                    Assert.assertEquals(66L, countersGetInfo.get("receive_ham").get(4));
                    Assert.assertEquals(0L, countersGetInfo.get("receive_spam").get(4));
                    Assert.assertEquals(38L, countersGetInfo.get("recepients_count").get(4));
                    Assert.assertEquals(50L, countersGetInfo.get("recepients_max_count").get(4));
                    Assert.assertEquals(7486, countersGetInfo.get("current_day").get(4));
                }
                {
                    final Map<String, List<Object>> geoGetInfo = shingleInfo.get(FreemailScheme.GEO_GET);
                    Assert.assertNotNull(geoGetInfo);
                    Assert.assertEquals(3314508500074748400L, geoGetInfo.get("uuid").get(0));
                    Assert.assertEquals(7486, geoGetInfo.get("date").get(0));
                    Assert.assertEquals("RU", geoGetInfo.get("geo").get(0));
                }
                {
                    final Map<String, List<Object>> timeGetInfo = shingleInfo.get(FreemailScheme.TIME_GET);
                    Assert.assertNotNull(timeGetInfo);
                    Assert.assertEquals(3314508500074748400L, timeGetInfo.get("uuid").get(0));
                    Assert.assertEquals(7486, timeGetInfo.get("date").get(0));
                    Assert.assertEquals(730, timeGetInfo.get("active_days").get(0));
                    Assert.assertEquals(6379, timeGetInfo.get("create_date").get(0));
                }
                {
                    final Map<String, List<Object>> complaintGetInfo = shingleInfo.get(FreemailScheme.COMPLAINT_GET);
                    Assert.assertNotNull(complaintGetInfo);
                    Assert.assertEquals(3314508500074748400L, complaintGetInfo.get("uuid").get(0));
                    Assert.assertEquals(7405, complaintGetInfo.get("date").get(0));
                    Assert.assertEquals(6396958541402060000L, complaintGetInfo.get("hash").get(0));
                    Assert.assertEquals(true, complaintGetInfo.get("spam").get(0));
                }
            }
            {
                final var shingleInfo = results.get(GeneralShingles.SHINGLE).get(0L);
                Assert.assertNotNull(shingleInfo);
                logger.info("checkGetResponse2: schemes=" + Arrays.toString(shingleInfo.keySet().toArray()));
                Assert.assertEquals(1, shingleInfo.size());
                {
                    final Map<String, List<Object>> currentInfo = shingleInfo.get(FreemailScheme.CURRENT);
                    Assert.assertNotNull(currentInfo);
                    Assert.assertEquals(7486, currentInfo.get("day_from_millennium").get(0));
                }
            }
        }
    }

    private void checkGetResponse(final FreemailShingles result, final int testNumber) {
        if (testNumber == 1) {
            checkGetResponse1(result);
        } else {
            checkGetResponse2(result);
        }
    }

    private static String getResponse(final int testNumber) {
        if (testNumber == 1) {
            return GET_RESPONSE1;
        } else {
            return GET_RESPONSE2;
        }
    }

    private void testFreemailShinglerGetRequest(final int testNumber) throws Exception {
        try (StaticServer shingler = new StaticServer(Configs.baseConfig("FreemailShingler"));
             final SharedConnectingIOReactor reactor =
                 new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final FreemailShinglerClient freemailShinglerClient =
                 new FreemailShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            freemailShinglerClient.start();

            final FreemailShingles data =
                new FreemailShingles(TypesafeValueContentHandler.parse(getResponse(testNumber)));

            shingler.add(GeneralShinglerClient.URI, getResponse(testNumber));

            final Future<?> future = freemailShinglerClient.getShingles(data, EmptyFutureCallback.INSTANCE);
            final FreemailShingles result = (FreemailShingles) future.get();

            checkGetResponse(result, testNumber);
            waitServerRequests(shingler, GeneralShinglerClient.URI, 1);
        }
    }

    private void testFreemailShinglerPutRequest(final int testNumber) throws Exception {
        try (TestShinglerServer shingler = new TestShinglerServer(Configs.baseConfig("FreemailShingler"));
             final SharedConnectingIOReactor reactor =
                 new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final FreemailShinglerClient freemailShinglerClient =
                 new FreemailShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            freemailShinglerClient.start();

            final FreemailShingles data =
                new FreemailShingles(TypesafeValueContentHandler.parse(getResponse(testNumber)));

            final String putBody = freemailShinglerClient.getPutQuery(data);
            shingler.add(GeneralShinglerClient.URI, putBody, HttpStatus.SC_OK);
            logger.info("PUT Query: " + putBody);

            final String getBody = freemailShinglerClient.getGetQuery(data);
            shingler.add(GeneralShinglerClient.URI, getBody, getResponse(testNumber));
            logger.info("GET Query: " + getBody);

            final Future<?> putFuture =
                freemailShinglerClient.putShingles(data, AbstractShinglerClient.StringFutureCallback.INSTANCE);
            final String putResult = (String) putFuture.get();

            Assert.assertNotNull(putResult);

            final Future<?> getFuture = freemailShinglerClient.getShingles(data, EmptyFutureCallback.INSTANCE);
            final FreemailShingles getResult = (FreemailShingles) getFuture.get();

            checkGetResponse(getResult, testNumber);

            waitServerRequests(shingler, GeneralShinglerClient.URI, putBody,1);
            waitServerRequests(shingler, GeneralShinglerClient.URI, getBody,1);
        }
    }

    @Test
    public void testFreemailShinglerGetRequest1() throws Exception {
        testFreemailShinglerGetRequest(1);
    }

    @Test
    public void testFreemailShinglerGetRequest2() throws Exception {
        testFreemailShinglerGetRequest(2);
    }

    @Test
    public void testFreemailShinglerPutRequest1() throws Exception {
        testFreemailShinglerPutRequest(1);
    }

    @Test
    public void testFreemailShinglerPutRequest2() throws Exception {
        testFreemailShinglerPutRequest(2);
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
