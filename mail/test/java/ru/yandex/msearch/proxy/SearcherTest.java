package ru.yandex.msearch.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.SlowpokeHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.msearch.proxy.MsearchProxyCluster.MproxyClusterContext;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class SearcherTest extends MsearchProxyTestBase {
    private static final String LOCALHOST = "localhost";
    private static final String LUCENE_ALPHA = "lucene-alpha";
    private static final String LUCENE_BETA = "lucene-beta";
    private static final String LUCENE_GAMMA = "lucene-gamma";
    private static final Map<String, String> DNS_HOSTS_MAPPING;

    private static final String FILTER_SEARCH =
        "/filter_search?order=default&mdb=pg"
        + "&uid=0&suid=1&excl_folders=unsubscribe&excl_folders=spam&excl_folders=hidden_trash&mids=";
    private static final String PRODUCER_STATUS =
        "/_status?service=change_log&prefix=0&allow_cached&all&json-type=dollar";

    private static final String LCN_RESP_PREFIX =
        "{\"hitsCount\":1," +
            "\"hitsArray\":[{\"mid\":\"";
    private static final String LCN_RESP_POSTFIX ="\"," +
            "\"clicks_total_count\":\"1\",\"clicks_serp_count\":\"1\"," +
            "\"received_date\":\"1481131770\"}]}";

    static {
        DNS_HOSTS_MAPPING = new HashMap<>();
        DNS_HOSTS_MAPPING.put(LUCENE_ALPHA, LOCALHOST);
        DNS_HOSTS_MAPPING.put(LUCENE_BETA, LOCALHOST);
        DNS_HOSTS_MAPPING.put(LUCENE_GAMMA, LOCALHOST);
    }

    @Test
    public void testBroadcastSearcher() throws Exception {
        try (StaticServer search1 =
                new StaticServer(Configs.baseConfig("Search-1"));
            StaticServer search2 =
                new StaticServer(Configs.baseConfig("Search-2"));
            StaticServer search3 =
                new StaticServer(Configs.baseConfig("Search-3"));
            MsearchProxyCluster cluster =
                new MsearchProxyCluster(
                    this,
                    new MproxyClusterContext().searchMap(
                        searchMapRule(
                            LUCENE_ALPHA,
                            "change_log",
                            search1.port())
                        + searchMapRule(
                            LUCENE_BETA,
                            "change_log",
                            search2.port())
                        + searchMapRule(
                            LUCENE_GAMMA,
                            "change_log",
                            search3.port()))
                        .dnsHostsMapping(DNS_HOSTS_MAPPING));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            search1.start();
            search2.start();
            search3.start();

            String uri = "/search*";
            search1.add(
                uri,
                new StaticHttpResource(new PositionHttpItem("1", 5)));
            search2.add(
                uri,
                new StaticHttpResource(
                    new SlowpokeHttpItem(
                        new PositionHttpItem("2", 7, 200),
                        15000)));
            search3.add(
                uri,
                new StaticHttpResource(new PositionHttpItem("3", 6, 1400)));

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(cluster.proxy().host() + "/ping")))
            {
                CharsetUtils.toString(response.getEntity());
            } catch (Throwable t) {
                // ignore
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(search1.host() + "/ping")))
            {
                CharsetUtils.toString(response.getEntity());
            } catch (Throwable t) {
                // ignore
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(search2.host() + "/ping")))
            {
                CharsetUtils.toString(response.getEntity());
            } catch (Throwable t) {
                // ignore
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(search3.host() + "/ping")))
            {
                CharsetUtils.toString(response.getEntity());
            } catch (Throwable t) {
                // ignore
            }

            filterSearch(cluster.filterSearch(), "1");
            filterSearch(cluster.filterSearch(), "2");
            filterSearch(cluster.filterSearch(), "3");

            HttpGet request = new HttpGet(
                cluster.proxy().host()
                    + "/broadcast/api/async/mail/search?mdb=pg&uid=0&first=0"
                    + "&request=q&nolaf&get=mid");
            try (CloseableHttpResponse response = client.execute(request)) {
                CharsetUtils.toString(response.getEntity());
            } catch (Throwable t) {
                // ignore
            }

            request = new HttpGet(
                cluster.proxy().host()
                    + "/broadcast/api/async/mail/search?mdb=pg&uid=0&first=0"
                    + "&request=q&nolaf&get=mid");
            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String body = CharsetUtils.toString(response.getEntity());
                YandexAssert.assertContains(
                    "lucene-alpha:" + search1.port()
                    + "\n\n"
                    + "{\"details\":{\"crc32\":\"0\","
                    + "\"search-limits\":{\"offset\":0,"
                    + "\"length\":200},"
                    + "\"search-options\":"
                    + "{\"request\":\"q\",\"pure\":true},"
                    + "\"total-found\":1},\"envelopes\":[\"1\"]}"
                    + "\n-------------------\n",
                    body);
                YandexAssert.assertContains(
                    "lucene-beta:" + search2.port()
                    + "\n\n"
                    + "Failed: java.net.SocketTimeoutException",
                    body);
                YandexAssert.assertContains(
                    "\n-------------------\n"
                    + "lucene-gamma:" + search3.port()
                    + "\n\n"
                    + "{\"details\":{\"crc32\":\"0\","
                    + "\"search-limits\":{\"offset\":0,"
                    + "\"length\":200},"
                    + "\"search-options\":"
                    + "{\"request\":\"q\",\"pure\":true},"
                    + "\"total-found\":1},\"envelopes\":[\"3\"]}",
                    body);
            }
        }
    }

    @Test
    public void testSearchmapSearch() throws Exception {
        try (StaticServer search1 =
                new StaticServer(Configs.baseConfig("Search-1"));
            StaticServer search2 =
                new StaticServer(Configs.baseConfig("Search-2"));
            StaticServer search3 =
                new StaticServer(Configs.baseConfig("Search-3"));
            MsearchProxyCluster cluster =
                new MsearchProxyCluster(
                    this,
                    new MproxyClusterContext()
                        .searchMap(
                            searchMapRule(
                                LUCENE_ALPHA,
                                "change_log",
                                search1.port())
                            + searchMapRule(
                                LUCENE_BETA,
                                "change_log",
                                search2.port())
                            + searchMapRule(
                                LUCENE_GAMMA,
                                "change_log",
                                search3.port()))
                        .dnsHostsMapping(DNS_HOSTS_MAPPING));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            search1.start();
            search2.start();
            search3.start();

            final String uri =
                "/search-async-mail-search?user-id-field_disable=uid"
                    + "&user-id-term_disable=0"
                    + "&get=mid,hid,clicks_total_count,clicks_serp_count,"
                    + "received_date&prefix=0&group=mid&merge_func=none&text="
                    + "(((mid_p:q+OR+pure_body:q+OR+attachname:q+OR"
                    + "+attachtype:q+OR+attachments:q+OR+hdr_subject:q+OR"
                    + "+hdr_from:q+OR+hdr_from_normalized:q+OR+reply_to:q+OR"
                    + "+reply_to_normalized:q+OR+hdr_to:q+OR"
                    + "+hdr_to_normalized:q+OR+hdr_cc:q+OR+hdr_cc_normalized"
                    + ":q+OR+hdr_bcc:q+OR+hdr_bcc_normalized:q)))"
                    + "&service=change_log&scope=mid_p,pure_body,hdr_subject,"
                    + "hdr_from,hdr_from_normalized,hdr_to,hdr_to_normalized,"
                    + "hdr_cc,hdr_cc_normalized,hdr_bcc,hdr_bcc_normalized,"
                    + "reply_to,reply_to_normalized,attachname,attachtype,"
                    + "attachments&sort=received_date&collector=pruning"
                    + "(received_day_p)&offset=0&length=400";

            search1.add(uri, new PositionHttpItem("1", 5));
            search2.add(uri, new PositionHttpItem("2", 7, 200));
            search3.add(uri, new PositionHttpItem("3", 6, 400));

            filterSearch(cluster.filterSearch(), "2");

            HttpGet request = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg&uid=0&first=0"
                    + "&request=q&nolaf&get=mid");
            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("2")),
                    CharsetUtils.toString(response.getEntity()));
            }

            search1.add(
                uri,
                new PositionHttpItem(HttpStatus.SC_INTERNAL_SERVER_ERROR));

            search2.add(uri, new PositionHttpItem("2", 7, 200));
            search3.add(uri, new PositionHttpItem("3", 8, 400));

            filterSearch(cluster.filterSearch(), "3");

            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(serp("3")),
                    CharsetUtils.toString(response.getEntity()));
            }

            search2.add(uri, new PositionHttpItem("4", 7));
            search1.add(
                uri,
                new PositionHttpItem(HttpStatus.SC_SERVICE_UNAVAILABLE, 400));
            search3.add(uri, new PositionHttpItem("5", 6, 100));

            filterSearch(cluster.filterSearch(), "4");

            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);

                YandexAssert.check(
                    new JsonChecker(serp("4")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // check NON_RETRIABLE will cause error anyway
            search2.add(uri, new PositionHttpItem("4", 7));
            search1.add(
                uri,
                new PositionHttpItem(HttpStatus.SC_BAD_REQUEST, 400));
            search3.add(uri, new PositionHttpItem("5", 6, 100));

            filterSearch(cluster.filterSearch(), "4");

            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
            }
        }
    }

    @Test
    public void testProducerSearch() throws Exception {
        try (StaticServer search1 =
                new StaticServer(Configs.baseConfig("Search-1"));
            StaticServer search2 =
                new StaticServer(Configs.baseConfig("Search-2"));
            StaticServer search3 =
                new StaticServer(Configs.baseConfig("Search-3"));
            MsearchProxyCluster cluster =
                new MsearchProxyCluster(
                    this,
                    new MproxyClusterContext()
                        .searchMap(
                            searchMapRule(
                                LUCENE_ALPHA,
                                "change_log",
                                search1.port())
                            + searchMapRule(
                                LUCENE_BETA,
                                "change_log",
                                search2.port())
                            + searchMapRule(
                                LUCENE_GAMMA,
                                "change_log",
                                search3.port()))
                        .producer(true, true)
                        .dnsHostsMapping(DNS_HOSTS_MAPPING));
            CloseableHttpClient client = HttpClients.createDefault())
        {
            cluster.start();
            search1.start();
            search2.start();
            search3.start();

            final String uri =
                "/search-async-mail-search?user-id-field_disable"
                    + "=uid&user-id-term_disable=0"
                    + "&get=mid,hid,clicks_total_count,clicks_serp_count,"
                    + "received_date&prefix=0&group=mid&merge_func=none&text="
                    + "(((mid_p:q+OR+pure_body:q+OR+attachname:q+OR"
                    + "+attachtype:q+OR+attachments:q+OR+hdr_subject:q+OR"
                    + "+hdr_from:q+OR+hdr_from_normalized:q+OR+reply_to:q+OR"
                    + "+reply_to_normalized:q+OR+hdr_to:q+OR"
                    + "+hdr_to_normalized:q+OR+hdr_cc:q+OR+hdr_cc_normalized"
                    + ":q+OR+hdr_bcc:q+OR+hdr_bcc_normalized:q)))"
                    + "&service=change_log&scope=mid_p,pure_body,hdr_subject,"
                    + "hdr_from,hdr_from_normalized,hdr_to,hdr_to_normalized,"
                    + "hdr_cc,hdr_cc_normalized,hdr_bcc,hdr_bcc_normalized,"
                    + "reply_to,reply_to_normalized,attachname,attachtype,"
                    + "attachments&sort=received_date&collector=pruning"
                    + "(received_day_p)&offset=0&length=400";

            // test fallback to searchmap searcher
            search1.add(uri, new PositionHttpItem("1", 5));
            search2.add(uri, new PositionHttpItem("2", 7, 200));
            search3.add(uri, new PositionHttpItem("3", 6, 400));

            producer(cluster.producer(), HttpStatus.SC_INTERNAL_SERVER_ERROR);

            filterSearch(cluster.filterSearch(), "2");

            HttpGet request = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg&uid=0&first=0"
                    + "&request=q&nolaf&get=mid");
            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("2")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // return fastest if position over producer position
            producer(
                cluster.producer(),
                Arrays.asList(LUCENE_ALPHA, LUCENE_BETA, LUCENE_GAMMA),
                Arrays.asList(4L, 4L, 4L));
            search1.add(uri, new PositionHttpItem("1", 5));
            search2.add(uri, new PositionHttpItem("2", 7, 200));
            search3.add(uri, new PositionHttpItem("3", 6, 400));

            filterSearch(cluster.filterSearch(), "1");

            request = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg&uid=0&first=0"
                    + "&request=q&nolaf&get=mid");
            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("1")),
                    CharsetUtils.toString(response.getEntity()));
            }

            // return best if all positions less producer position
            producer(
                cluster.producer(),
                Arrays.asList(LUCENE_ALPHA, LUCENE_BETA, LUCENE_GAMMA),
                Arrays.asList(8L, 8L, 8L));
            search1.add(uri, new PositionHttpItem("1", 5));
            search2.add(uri, new PositionHttpItem("2", 7, 300));
            search3.add(uri, new PositionHttpItem("3", 6, 400));

            filterSearch(cluster.filterSearch(), "2");

            request = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg&uid=0&first=0"
                    + "&request=q&nolaf&get=mid");
            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("2")),
                    CharsetUtils.toString(response.getEntity()));
            }

            producer(
                cluster.producer(),
                Arrays.asList(LUCENE_ALPHA, LUCENE_BETA, LUCENE_GAMMA),
                Arrays.asList(6L, 5L, 4L));
            search1.add(uri, new PositionHttpItem("1", 5));
            search2.add(uri, new PositionHttpItem("2", 6));
            search3.add(uri, new PositionHttpItem("3", 6, 400));

            filterSearch(cluster.filterSearch(), "2");

            request = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg&uid=0&first=0"
                    + "&request=q&nolaf&get=mid");
            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("2")),
                    CharsetUtils.toString(response.getEntity()));
            }

            producer(
                cluster.producer(),
                Arrays.asList(LUCENE_ALPHA, LUCENE_BETA, LUCENE_GAMMA),
                Arrays.asList(6L, 5L, 4L));
            search1.add(
                uri,
                new StaticHttpItem(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            search2.add(uri, new StaticHttpItem(HttpStatus.SC_GATEWAY_TIMEOUT));
            search3.add(uri, new PositionHttpItem("3", 4, 1500));

            filterSearch(cluster.filterSearch(), "3");

            request = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg&uid=0&first=0"
                    + "&request=q&nolaf&get=mid");
            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(serp("3")),
                    CharsetUtils.toString(response.getEntity()));
            }

            producer(
                cluster.producer(),
                Arrays.asList(LUCENE_ALPHA, LUCENE_BETA, LUCENE_GAMMA),
                Arrays.asList(6L, 5L, 4L));
            search1.add(
                uri,
                new StaticHttpItem(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            search2.add(
                uri,
                new PositionHttpItem(HttpStatus.SC_BAD_REQUEST, 100));
            search3.add(
                uri,
                new PositionHttpItem(HttpStatus.SC_GATEWAY_TIMEOUT, 200));

            filterSearch(cluster.filterSearch(), "3");

            request = new HttpGet(
                cluster.proxy().host()
                    + "/api/async/mail/search?mdb=pg&uid=0&first=0"
                    + "&request=q&nolaf&get=mid");
            try (CloseableHttpResponse response = client.execute(request)) {
                HttpAssert.assertStatusCode(
                    HttpStatus.SC_BAD_REQUEST,
                    response);
            }
        }

    }

    private final String serp(final String... mids) {
        String[] envelopes = new String[mids.length];
        for (int i = 0; i < mids.length; i++) {
            envelopes[i] = "\"" + mids[i] + "\"";
        }

        return serp("q", true, envelopes);
    }

    private static final void producer(
        final StaticServer server,
        final int statusCode)
    {
        server.add(PRODUCER_STATUS, new StaticHttpItem(statusCode));
    }

    private static final void producer(
        final StaticServer server,
        final List<String> hosts,
        final List<Long> positions)
    {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < hosts.size(); i++) {
            sb.append("{\"" + hosts.get(i) + "\":");
            sb.append(positions.get(i));
            sb.append("},");
        }

        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1);
        }

        sb.append("]");

        server.add(
            PRODUCER_STATUS,
            new StaticHttpItem(HttpStatus.SC_OK, sb.toString()));
    }

    private final void filterSearch(
        final StaticServer server,
        final String... mids)
    {
        String[] envelopes = new String[mids.length];
        for (int i = 0; i < mids.length; i++) {
            envelopes[i] = envelope(mids[i]);
        }

        server.add(
            FILTER_SEARCH + String.join(",", mids),
            envelopes("", envelopes));
    }

    private static String searchMapRule(
        final String hostname,
        final String db,
        final int port)
    {
        return db + " shards:0-65534"
            + ",host:" + hostname
            + ",search_port:" + (port - 1)
            + ",search_port_ng:" + port
            + ",json_indexer_port:" + (port + 1) + '\n';
    }

    private static class PositionHttpItem extends StaticHttpItem {
        private final long zooqueueId;
        private final long delay;

        public PositionHttpItem(
            final int statusCode,
            final long delay)
        {
            super(statusCode);

            assert statusCode != HttpStatus.SC_OK;
            this.zooqueueId = -1;
            this.delay = delay;
        }

        public PositionHttpItem(
            final String mid,
            final long pos,
            final long delay)
        {
            super(HttpStatus.SC_OK, LCN_RESP_PREFIX + mid + LCN_RESP_POSTFIX);

            this.zooqueueId = pos;
            this.delay = delay;
        }

        public PositionHttpItem(final String mid, final long pos) {
            this(mid, pos, 0);
        }

        public PositionHttpItem(final int statusCode) {
            this(statusCode, 0);
        }

        @Override
        public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context)
            throws HttpException, IOException
        {
            super.handle(request, response, context);

            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    throw new HttpException("Was interrupted", ie);
                }
            }

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                response.addHeader(
                    YandexHeaders.ZOO_QUEUE_ID,
                    String.valueOf(zooqueueId));
            }
        }
    }
}
