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

public class ActivityShinglerTest extends TestBase {
    private static final int TIMEOUT = 10000;
    private static final int INTERVAL = 100;

    private static final String GET_RESPONSE = "[" +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"last_dt\": 1593932400," +
            "            \"module\": \"hound\"," +
            "            \"uid\": 231040157" +
            "        }," +
            "        {" +
            "            \"last_dt\": 1593414000," +
            "            \"module\": \"jsintegration\"," +
            "            \"uid\": 231040157" +
            "        }," +
            "        {" +
            "            \"last_dt\": 1593932400," +
            "            \"module\": \"mailbox_oper\"," +
            "            \"uid\": 231040157" +
            "        }," +
            "        {" +
            "            \"last_dt\": 1523862000," +
            "            \"module\": \"mobile\"," +
            "            \"uid\": 231040157" +
            "        }," +
            "        {" +
            "            \"last_dt\": 1538118000," +
            "            \"module\": \"search\"," +
            "            \"uid\": 231040157" +
            "        }," +
            "        {" +
            "            \"last_dt\": 1593500400," +
            "            \"module\": \"sendbernar\"," +
            "            \"uid\": 231040157" +
            "        }," +
            "        {" +
            "            \"last_dt\": 1593932400," +
            "            \"module\": \"spam_report\"," +
            "            \"uid\": 231040157" +
            "        }," +
            "        {" +
            "            \"last_dt\": 1459407600," +
            "            \"module\": \"unknown\"," +
            "            \"uid\": 231040157" +
            "        }," +
            "        {" +
            "            \"last_dt\": 1534143600," +
            "            \"module\": \"wmi\"," +
            "            \"uid\": 231040157" +
            "        }," +
            "        {" +
            "            \"last_dt\": 1481871600," +
            "            \"module\": \"yserver_imap\"," +
            "            \"uid\": 231040157" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"activity\"" +
            "}," +
            "{" +
            "    \"find\": [" +
            "        {" +
            "            \"days_with_complaints\": \"31\"," +
            "            \"firsttime\": \"6133\"," +
            "            \"complaints\": \"27\"," +
            "            \"lasttime\": \"7461\"," +
            "            \"uid\": \"231040157\"" +
            "        }" +
            "    ]," +
            "    \"scheme\": \"compls\"" +
            "}" +
        "]";

    public ActivityShinglerTest() {
    }

    private void checkGetResponse(final ActivityShingles result) {
        Assert.assertNotNull(result);
        {
            final var results = result.getResults();
            Assert.assertEquals(1, results.size());
            logger.info("checkGetResponse get results: " + results + ", shingles="
                + Arrays.toString(results.get(GeneralShingles.SHINGLE).keySet().toArray()));
            Assert.assertEquals(1, results.get(GeneralShingles.SHINGLE).size()); // different shingles count
            {
                final var shingleInfo = results.get(GeneralShingles.SHINGLE).get(231040157L);
                Assert.assertNotNull(shingleInfo);
                logger.info("checkGetResponse: schemes=" + Arrays.toString(shingleInfo.keySet().toArray()));
                Assert.assertEquals(2, shingleInfo.size());
                {
                    final Map<String, List<Object>> countersInfo = shingleInfo.get(ActivityScheme.ACTIVITY);
                    Assert.assertNotNull(countersInfo);
                    Assert.assertEquals(231040157L, countersInfo.get("uid").get(0));
                    Assert.assertEquals("hound", countersInfo.get("module").get(0));
                    Assert.assertEquals(1593932400L, countersInfo.get("last_dt").get(0));

                    Assert.assertEquals(231040157L, countersInfo.get("uid").get(1));
                    Assert.assertEquals("jsintegration", countersInfo.get("module").get(1));
                    Assert.assertEquals(1593414000L, countersInfo.get("last_dt").get(1));

                    Assert.assertEquals(231040157L, countersInfo.get("uid").get(2));
                    Assert.assertEquals("mailbox_oper", countersInfo.get("module").get(2));
                    Assert.assertEquals(1593932400L, countersInfo.get("last_dt").get(2));

                    Assert.assertEquals(231040157L, countersInfo.get("uid").get(8));
                    Assert.assertEquals("wmi", countersInfo.get("module").get(8));
                    Assert.assertEquals(1534143600L, countersInfo.get("last_dt").get(8));

                    Assert.assertEquals(231040157L, countersInfo.get("uid").get(9));
                    Assert.assertEquals("yserver_imap", countersInfo.get("module").get(9));
                    Assert.assertEquals(1481871600L, countersInfo.get("last_dt").get(9));
                }
                {
                    final Map<String, List<Object>> countersInfo = shingleInfo.get(ActivityScheme.COMPLS);
                    Assert.assertNotNull(countersInfo);
                    Assert.assertEquals(231040157L, countersInfo.get("uid").get(0));
                    Assert.assertEquals(31, countersInfo.get("days_with_complaints").get(0));
                    Assert.assertEquals(6133, countersInfo.get("firsttime").get(0));
                    Assert.assertEquals(7461, countersInfo.get("lasttime").get(0));
                    Assert.assertEquals(27L, countersInfo.get("complaints").get(0));
                }
            }
        }
    }

    @Test
    public void testActivityShinglerGetRequest() throws Exception {
        try (StaticServer shingler = new StaticServer(Configs.baseConfig("ActivityShingler"));
             final SharedConnectingIOReactor reactor =
                 new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final ActivityShinglerClient activityShinglerClient =
                 new ActivityShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            activityShinglerClient.start();

            final ActivityShingles data = new ActivityShingles(TypesafeValueContentHandler.parse(GET_RESPONSE));

            shingler.add(GeneralShinglerClient.URI, GET_RESPONSE);

            final Future<?> future = activityShinglerClient.getShingles(data, EmptyFutureCallback.INSTANCE);
            final ActivityShingles result = (ActivityShingles) future.get();

            checkGetResponse(result);
            waitServerRequests(shingler, GeneralShinglerClient.URI, 1);
        }
    }

    @Test
    public void testActivityShinglerPutRequest() throws Exception {
        try (TestShinglerServer shingler = new TestShinglerServer(Configs.baseConfig("ActivityShingler"));
             final SharedConnectingIOReactor reactor =
                 new SharedConnectingIOReactor(Configs.baseConfig(), Configs.dnsConfig());
             final ActivityShinglerClient activityShinglerClient =
                 new ActivityShinglerClient(reactor, Configs.hostConfig(shingler))
        ) {
            shingler.start();
            reactor.start();
            activityShinglerClient.start();

            final ActivityShingles data = new ActivityShingles(TypesafeValueContentHandler.parse(GET_RESPONSE));

            final String putBody = activityShinglerClient.getPutQuery(data);
            shingler.add(GeneralShinglerClient.URI, putBody, HttpStatus.SC_OK);
            logger.info("PUT Query: " + putBody);

            final String getBody = activityShinglerClient.getGetQuery(data);
            shingler.add(GeneralShinglerClient.URI, getBody, GET_RESPONSE);
            logger.info("GET Query: " + getBody);

            final Future<?> putFuture =
                activityShinglerClient.putShingles(data, AbstractShinglerClient.StringFutureCallback.INSTANCE);
            final String putResult = (String) putFuture.get();

            Assert.assertNotNull(putResult);

            final Future<?> getFuture = activityShinglerClient.getShingles(data, EmptyFutureCallback.INSTANCE);
            final ActivityShingles getResult = (ActivityShingles) getFuture.get();

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
