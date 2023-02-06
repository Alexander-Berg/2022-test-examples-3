package ru.yandex.market.tsum.clients.dynamic_dns;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Resources;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xbill.DNS.Type;

import ru.yandex.market.request.netty.WrongStatusCodeException;
import ru.yandex.market.tsum.clients.dynamic_dns.model.BaseDnsRecord;
import ru.yandex.market.tsum.clients.dynamic_dns.model.SoaDnsRecord;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class DynamicDnsApiClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    DynamicDnsApiClient client;

    DnsRecord testRecord;

    private static String getResourceAsString(String resourceName) throws IOException {
        URL resultFile = Resources.getResource(resourceName);
        return Resources.toString(resultFile, Charset.defaultCharset());
    }

    @Before
    public void before() throws Exception {
        testRecord = new DnsRecord(
            "testhost.market.yandex.net",
            Type.A,
            "87.250.250.99",
            3600
        );

        client = new DynamicDnsApiClient(
            "testaccount", "http://localhost:" + wireMockRule.port(), "testtoken", null);
    }

    /**
     * Adding a DNS record doesn't return anything on success.
     */
    @Test
    public void testAddRecord() throws Exception {
        wireMockRule.stubFor(put(urlEqualTo("/v2.3/testaccount/primitives"))
            .withHeader("Content-Type", containing("application/json"))
            .withHeader("X-Auth-Token", equalTo("testtoken"))
            .withRequestBody(equalToJson("{ \"primitives\" : [ { \"operation\": \"add\", \"name\" : \"testhost.market" +
                ".yandex.net\", \"type\" : \"A\", \"data\" : \"87.250.250.99\", \"ttl\" : 3600 } ] }"))
            .willReturn(aResponse()));
        client.addRecord(testRecord);
    }

    /**
     * Deleteing a DNS record doesn't return anything on success.
     */
    @Test
    public void testDeleteRecord() throws Exception {
        wireMockRule.stubFor(put(urlEqualTo("/v2.3/testaccount/primitives"))
            .withHeader("Content-Type", containing("application/json"))
            .withHeader("X-Auth-Token", equalTo("testtoken"))
            .withRequestBody(equalToJson("{ \"primitives\" : [ { \"operation\": \"delete\", \"name\" : \"testhost" +
                ".market.yandex.net\", \"type\" : \"A\", \"data\" : \"87.250.250.99\", \"ttl\" : 3600 } ] }"))
            .willReturn(aResponse()));
        client.deleteRecord(testRecord);
    }

    /**
     * DNS API request with invalid OAuth token throws 401 unauthorized
     */
    @Test(expected = WrongStatusCodeException.class)
    public void testInvalidToken() throws Exception {
        wireMockRule.stubFor(put(urlEqualTo("/v2.3/testaccount/primitives"))
            .withHeader("Content-Type", containing("application/json"))
            .withHeader("X-Auth-Token", equalTo("invalidtoken"))
            .willReturn(aResponse().withStatus(HttpStatus.SC_UNAUTHORIZED)));
        DynamicDnsApiClient badclient = new DynamicDnsApiClient(
            "testaccount", "http://localhost:" + wireMockRule.port(), "invalidtoken",
            null);
        badclient.addRecord(testRecord);
    }

    /**
     * Get all zones request
     */
    @Test
    public void testGetAllZones() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/v2.0/testaccount/zones"))
            .withHeader("Accept", containing("application/json"))
            .withHeader("X-Auth-Token", equalTo("testtoken"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\n" +
                    "  \"zones\": [\n" +
                    "    {\n" +
                    "      \"file\": \"190.158.93.in-addr.arpa\",\n" +
                    "      \"keys\": \"ROBOT_KEY(\\\"wgwr-zhv8-cwib-3ybk\\\");\",\n" +
                    "      \"master\": \"cold\",\n" +
                    "      \"notify\": \"_NS_DYN_CLUSTER_\",\n" +
                    "      \"realms\": \"realm-noc\",\n" +
                    "      \"uuid\": \"000650f8-af7b-11e3-81b1-1a46ddab69a1\",\n" +
                    "      \"xfer\": \"_FW_ROUTERLOOPBACKS_ _NOC_TRANSFER_SERVERS_\",\n" +
                    "      \"zone\": \"190.158.93.in-addr.arpa\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}")));
        List<DnsZoneRecord> allZones = client.getAllZones();
        Assert.assertEquals(1, allZones.size());
        Assert.assertEquals("000650f8-af7b-11e3-81b1-1a46ddab69a1", allZones.get(0).getUuid());
    }


    /**
     * Get all records by zone id
     */
    @Test
    public void testGetRecords() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/v2.0/testaccount/zones/000650f8-af7b-11e3-81b1-1a46ddab69a1/records" +
            "?limit=100&offset=0"))
            .withHeader("Accept", containing("application/json"))
            .withHeader("X-Auth-Token", equalTo("testtoken"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\n" +
                    "  \"zones\": [\n" +
                    "    {\n" +
                    "      \"acl-list\": \"ROBOT_KEY(\\\"wgwr-zhv8-cwib-3ybk\\\");\",\n" +
                    "      \"arpa\": \"190.158.93.in-addr.arpa\",\n" +
                    "      \"cidr\": \"93.158.190.0/24\",\n" +
                    "      \"desc\": \"десктопная сеть, Бенуа\",\n" +
                    "      \"location\": \"spb\",\n" +
                    "      \"range_start\": \"93.158.190.0\",\n" +
                    "      \"range_stop\": \"93.158.190.255\",\n" +
                    "      \"realms\": \"realm-noc\",\n" +
                    "      \"recordsList\": {\n" +
                    "        \"records\": [\n" +
                    "          {\n" +
                    "            \"expire\": \"3600000\",\n" +
                    "            \"hostmaster\": \"sysadmin.yandex.ru.\",\n" +
                    "            \"left-side\": \"190.158.93.in-addr.arpa.\",\n" +
                    "            \"nttl\": \"300\",\n" +
                    "            \"refresh\": \"900\",\n" +
                    "            \"retry\": \"600\",\n" +
                    "            \"right-side\": \"ns3.yandex.ru.\",\n" +
                    "            \"serial\": \"2013126479\",\n" +
                    "            \"ttl\": \"600\",\n" +
                    "            \"type\": \"SOA\",\n" +
                    "            \"metadata\": {\n" +
                    "              \"create-time\": \"Tue Oct 20 11:54:58 2015\",\n" +
                    "              \"fingerprint\": \"e1e0d03d76676e0782a5f71c41002910\",\n" +
                    "              \"id\": \"TYPE-B-DIRECT-825e313f98b8fb8058c354bc39cfe80d\",\n" +
                    "              \"managed-by\": \"authority-dns-monkey\",\n" +
                    "              \"mgmt-type\": \"dynamic\",\n" +
                    "              \"realm\": \"realm-dns-api-testing\",\n" +
                    "              \"zone\": \"dns-api-testing.yandex.net\"\n" +
                    "            }\n" +
                    "          }\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}")));
        List<BaseDnsRecord> records = client.getRecords("000650f8-af7b-11e3-81b1-1a46ddab69a1");
        Assert.assertEquals(1, records.size());
        Assert.assertEquals("ns3.yandex.ru.", records.get(0).getRightSide());
        Assert.assertEquals("authority-dns-monkey", records.get(0).getMetadata().getManagedBy());
        Assert.assertTrue(records.get(0) instanceof SoaDnsRecord);
    }

    /**
     * Get all records by zone id with paging
     */
    @Test
    public void testGetRecords2() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/v2.0/testaccount/zones/000650f8-af7b-11e3-81b1-1a46ddab69a1/records" +
            "?limit=100&offset=0"))
            .withHeader("Accept", containing("application/json"))
            .withHeader("X-Auth-Token", equalTo("testtoken"))
            .willReturn(aResponse().withStatus(200).withBody(getResourceAsString("dynamic_dns/rec_list_1.json"))));

        wireMockRule.stubFor(get(urlEqualTo("/v2.0/testaccount/zones/000650f8-af7b-11e3-81b1-1a46ddab69a1/records" +
            "?limit=100&offset=100"))
            .withHeader("Accept", containing("application/json"))
            .withHeader("X-Auth-Token", equalTo("testtoken"))
            .willReturn(aResponse().withStatus(200).withBody(getResourceAsString("dynamic_dns/rec_list_2.json"))));

        wireMockRule.stubFor(get(urlEqualTo("/v2.0/testaccount/zones/000650f8-af7b-11e3-81b1-1a46ddab69a1/records" +
            "?limit=100&offset=200"))
            .withHeader("Accept", containing("application/json"))
            .withHeader("X-Auth-Token", equalTo("testtoken"))
            .willReturn(aResponse().withStatus(200).withBody(getResourceAsString("dynamic_dns/rec_list_3.json"))));

        List<BaseDnsRecord> records = client.getRecords("000650f8-af7b-11e3-81b1-1a46ddab69a1");
        Assert.assertEquals(260, records.size());
    }

}
