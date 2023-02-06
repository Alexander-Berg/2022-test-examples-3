package ru.yandex.mail.so.jrbld;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class JrbldTest extends TestBase {
    private static final long INT_MASK = 0xffffffffL;
    private static final long RBL_TTL = 20000L;
    private static final String EXPIRE_TIMESTAMP =
        "\",\"rbl_expire_timestamp\":";
    private static final String STAT = "/stat";
    private static final String RELOADLISTS = "/reloadlists";
    private static final String URI = "/check?";
    private static final String INFO = "&info=";
    private static final String CHECK = "&check=";
    private static final String IP = "&ip=";
    private static final String GET_VALUES = "&get-values";
    private static final String TRUE = "\":true,\"";
    private static final String FALSE = "\":false,\"";
    private static final String TRUE_END = "\":true}}";
    private static final String FALSE_END = "\":false}}";
    private static final String CHECKS = "{\"infos\":{},\"checks\":{\"";
    private static final String INFOS =
        "{\"infos\":{\"static-info\":"
        + "{\"ip-info\":\"AS119526\"}},\"checks\":{\"";
    private static final String ZERO = Integer.toString(0);
    private static final String ONE = Integer.toString(1);

    private static LongPrefix prefix(final String ip) throws Exception {
        return new LongPrefix(
            (InetAddress.getByName(ip).hashCode() & INT_MASK)
            % JrbldCluster.SHARDS);
    }

    // CSOFF: MethodLength
    @Test
    public void test() throws Exception {
        try (JrbldCluster cluster = new JrbldCluster(this);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            String ip1 = "4.7";
            cluster.lucene().add(
                prefix(ip1),
                "\"id\":\"wl_1\",\"rbl_ip\":{\"function\":\"normalize_ip\","
                + "\"args\":[\"4.7\"]},\"rbl_type\":\""
                + JrbldCluster.WHITELIST_LUCENE_TYPE
                + EXPIRE_TIMESTAMP + (System.currentTimeMillis() + RBL_TTL));

            String ip2 = "4.6";
            cluster.lucene().add(
                prefix(ip2),
                "\"id\":\"bl_1\",\"rbl_ip\":{\"function\":\"normalize_ip\","
                + "\"args\":[\"4.6\"]},\"rbl_type\":\""
                + JrbldCluster.BLACKLIST_LUCENE_TYPE
                + EXPIRE_TIMESTAMP + (System.currentTimeMillis() + RBL_TTL));
            String ip3 = "4.0.8";
            String ip6 = "fe80::0:225:90ff:fec3:be5a";
            String ip7 = "fe80::0:225:90ff:fec3:be4a";
            String ip8 = "fe80::0:225:90ff:fec3:be6a";
            cluster.lucene().add(
                prefix(ip8),
                "\"id\":\"bl_2\",\"rbl_ip\":{\"function\":\"normalize_ip\","
                + "\"args\":[\"fe80::0:225:90ff:fec3:be6a\"]},\"rbl_type\":\""
                + JrbldCluster.BLACKLIST_LUCENE_TYPE
                + EXPIRE_TIMESTAMP + (System.currentTimeMillis() + RBL_TTL));

            String chainedCheckerTrue =
                "ip-checker-ipv4-and-wl-checker-true_ammm";
            String chainedCheckerFalse =
                "ip-checker-ipv4-and-wl-checker-false_ammm";
            String chainedCheckerNull =
                "ip-checker-ipv4-and-wl-checker-null_ammm";
            String ipv4CheckerTrue =
                "ip-checker-static-ipv4-list-true_ammm";
            String ipv4CheckerFalse =
                "ip-checker-static-ipv4-list-false_ammm";
            String ipv4CheckerNull =
                "ip-checker-static-ipv4-list-null_ammm";
            String whitelistCheckerTrue =
                "ip-checker-lucene-wl-true_ammm";
            String whitelistCheckerFalse =
                "ip-checker-lucene-wl-false_ammm";
            String whitelistCheckerNull =
                "ip-checker-lucene-wl-null_ammm";

            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(cluster.jrbld().host() + STAT)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String body = HttpAssert.body(response);
                HttpAssert.assertStat(chainedCheckerTrue, ZERO, body);
                HttpAssert.assertStat(chainedCheckerFalse, ZERO, body);
                HttpAssert.assertStat(chainedCheckerNull, ZERO, body);
                HttpAssert.assertStat(ipv4CheckerTrue, ZERO, body);
                HttpAssert.assertStat(ipv4CheckerFalse, ZERO, body);
                HttpAssert.assertStat(ipv4CheckerNull, ZERO, body);
                HttpAssert.assertStat(whitelistCheckerTrue, ZERO, body);
                HttpAssert.assertStat(whitelistCheckerFalse, ZERO, body);
                HttpAssert.assertStat(whitelistCheckerNull, ZERO, body);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI + INFO + JrbldCluster.STATIC_INFO_PROVIDER
                            + CHECK + JrbldCluster.IPV4_AND_WL_CHECKER
                            + CHECK + JrbldCluster.BLACKLIST_LUCENE_CHECKER
                            + IP + ip1)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        INFOS + JrbldCluster.IPV4_AND_WL_CHECKER + TRUE
                        + JrbldCluster.BLACKLIST_LUCENE_CHECKER
                        + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(cluster.jrbld().host() + STAT)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String body = HttpAssert.body(response);
                HttpAssert.assertStat(chainedCheckerTrue, ONE, body);
                HttpAssert.assertStat(chainedCheckerFalse, ZERO, body);
                HttpAssert.assertStat(chainedCheckerNull, ZERO, body);
                HttpAssert.assertStat(ipv4CheckerTrue, ZERO, body);
                HttpAssert.assertStat(ipv4CheckerFalse, ONE, body);
                HttpAssert.assertStat(ipv4CheckerNull, ZERO, body);
                HttpAssert.assertStat(whitelistCheckerTrue, ONE, body);
                HttpAssert.assertStat(whitelistCheckerFalse, ZERO, body);
                HttpAssert.assertStat(whitelistCheckerNull, ZERO, body);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CHECKER
                            + IP + ip2)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CHECKER + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CHECKER
                            + IP + ip6)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CHECKER + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CHECKER
                            + IP + ip7)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CHECKER + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CHECKER
                            + CHECK + JrbldCluster.BLACKLIST_LUCENE_CHECKER
                            + IP + ip2)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CHECKER + FALSE
                        + JrbldCluster.BLACKLIST_LUCENE_CHECKER + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CHECKER
                            + CHECK + JrbldCluster.BLACKLIST_LUCENE_CHECKER
                            + IP + ip8)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CHECKER + FALSE
                        + JrbldCluster.BLACKLIST_LUCENE_CHECKER + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            cluster.producer().close();

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CHECKER
                            + CHECK + JrbldCluster.IPV4_AND_WL_CHECKER
                            + CHECK + JrbldCluster.BLACKLIST_LUCENE_CHECKER
                            + IP + ip2)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CHECKER + FALSE
                        + JrbldCluster.IPV4_AND_WL_CHECKER + "\":null,\""
                        // Producer is dead, but data already in cache
                        + JrbldCluster.BLACKLIST_LUCENE_CHECKER + "\":true}}"),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(new HttpGet(cluster.jrbld().host() + STAT)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String body = HttpAssert.body(response);
                HttpAssert.assertStat(chainedCheckerTrue, ONE, body);
                HttpAssert.assertStat(chainedCheckerFalse, ZERO, body);
                HttpAssert.assertStat(chainedCheckerNull, ONE, body);
                HttpAssert.assertStat(ipv4CheckerTrue, ZERO, body);
                HttpAssert.assertStat(ipv4CheckerFalse, "8", body);
                HttpAssert.assertStat(ipv4CheckerNull, ZERO, body);
                HttpAssert.assertStat(whitelistCheckerTrue, ONE, body);
                HttpAssert.assertStat(whitelistCheckerFalse, ZERO, body);
                HttpAssert.assertStat(whitelistCheckerNull, ONE, body);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI + INFO + JrbldCluster.STATIC_INFO_PROVIDER
                            + CHECK + JrbldCluster.PROXY_IPV4_AND_WL_CHECKER
                            + IP + ip3)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        INFOS + JrbldCluster.PROXY_IPV4_AND_WL_CHECKER
                        + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            String cidrIp1 = "77.88.45.124";
            String cidrIp2 = "77.88.46.125";
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CIDR_LIST
                            + IP + cidrIp1)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CIDR_LIST + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CIDR_LIST
                            + IP + cidrIp2)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CIDR_LIST + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CIDR_LIST
                            + IP + "77.88.47.126")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CIDR_LIST + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CIDR_LIST
                            + IP + "77.88.48.127")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CIDR_LIST + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CIDR_LIST
                            + IP + "123.44.33.22")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CIDR_LIST + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CIDR_LIST
                            + IP + "123.44.33.23")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CIDR_LIST + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CIDR_LIST
                            + IP + "38.32.01.15")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CIDR_LIST + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CIDR_LIST
                            + IP + "2620:10f:d000::")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CIDR_LIST + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "125.0.0.1")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "125.0.0.2")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "125.0.0.3")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "125.0.0.4")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "125.0.0.5")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "125.0.0.7")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "125.0.0.8")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "125.4.255.255")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "125.5.0.0")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "125.5.2.12")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "125.5.1.12")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "125.5.255.255")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "125.6.0.0")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + ip6)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "128.1.0.0")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "128.2.0.0")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_IPV4_RANGE
                            + IP + "128.4.0.0")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_IPV4_RANGE + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            Files.write(
                cluster.cidrList(),
                Collections.singletonList(cidrIp1),
                StandardCharsets.UTF_8);
            long cidrLastModified = cluster.cidrList().toFile().lastModified();

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(cluster.jrbld().host() + RELOADLISTS)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CIDR_LIST
                            + IP + cidrIp1)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CIDR_LIST + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CIDR_LIST
                            + IP + cidrIp2)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CIDR_LIST + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            Files.write(
                cluster.cidrList(),
                Collections.singletonList(cidrIp2),
                StandardCharsets.UTF_8);
            cluster.cidrList().toFile().setLastModified(cidrLastModified);

            // Reload will be skipped due to last modified reset
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(cluster.jrbld().host() + RELOADLISTS)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CIDR_LIST
                            + IP + cidrIp1)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CIDR_LIST + TRUE_END),
                    CharsetUtils.toString(response.getEntity()));
            }

            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + URI
                            + CHECK + JrbldCluster.STATIC_CIDR_LIST
                            + IP + cidrIp2)))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        CHECKS + JrbldCluster.STATIC_CIDR_LIST + FALSE_END),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
    // CSON: MethodLength

    @Test
    public void testBatch() throws Exception {
        try (JrbldCluster cluster = new JrbldCluster(this, false);
            CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(
                            cluster.jrbld().host()
                            + "/check-all?check="
                            + JrbldCluster.STATIC_CIDR_LIST
                            + GET_VALUES
                            + INFO + JrbldCluster.STATIC_INFO_PROVIDER
                            + IP + "77.88.47.125"
                            + IP + "77.88.48.128")))
            {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(
                        "{\"77.88.47.125\":"
                        + INFOS + JrbldCluster.STATIC_CIDR_LIST
                        + "\":4}},\"77.88.48.128\":"
                        + INFOS + JrbldCluster.STATIC_CIDR_LIST + "\":0}}}"),
                    CharsetUtils.toString(response.getEntity()));
            }
        }
    }
}

