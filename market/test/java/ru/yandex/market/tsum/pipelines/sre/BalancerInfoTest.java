package ru.yandex.market.tsum.pipelines.sre;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;
import ru.yandex.market.tsum.pipelines.sre.jobs.BalancerPipelineTestFactory;
import ru.yandex.market.tsum.pipelines.sre.resources.BalancerEnvironment;
import ru.yandex.market.tsum.pipelines.sre.resources.balancer.BalancerInfo;

import static org.junit.Assert.assertTrue;
import static ru.yandex.misc.test.Assert.assertContains;

public class BalancerInfoTest {
    private final StartrekTicket startrekTicket = new StartrekTicket("TEST-1");
    private final Properties properties = new Properties();
    private final BalancerInfo balancerInfoTesting = BalancerPipelineTestFactory.getBalancerInfo();
    private final BalancerInfo balancerInfoProduction = BalancerPipelineTestFactory.getBalancerInfoProduction();
    private final BalancerInfo balancerInfoHttp = BalancerPipelineTestFactory.getBalancerInfoHttp();
    private final BalancerInfo balancerInfoGrpc = BalancerPipelineTestFactory.getBalancerInfoGrpc();
    private final BalancerInfo balancerInfoGrpcSsl = BalancerPipelineTestFactory.getBalancerInfoGrpcSsl();

    public BalancerInfoTest() throws IOException {
    }

    private BalancerInfo getTestList(String humanAccess) throws IOException {
        String description =
            "slb.health_check_type = 200-ый код ответа\n" +
                "slb.fqdn = test.tst.vs.market.yandex.net\n" +
                "slb.real_servers = test.yandex.ru\n" +
                "slb.cnames = cname1.market.yandex.net,cname2.market.yandex.net\n" +
                "slb.type_backends = HOST\n";
        if (humanAccess != null) {
            description += String.format("access.human = %s", humanAccess);
        }
        properties.load(new StringReader(description));
        return new BalancerInfo(startrekTicket, properties, BalancerEnvironment.TESTING);
    }

    @Test
    public void parseTestingDomainTest() {
        Assert.assertEquals("test_service_name", balancerInfoTesting.getServiceName());
        Assert.assertEquals("tst.vs.market.yandex.net", balancerInfoTesting.getServiceDomain());
    }

    @Test
    public void monitorParseSettingsTest() {
        Assert.assertFalse(balancerInfoTesting.isJugglerMonitor());
        Assert.assertEquals(Arrays.asList(
            "yandex_market_dev", "yandex_market_admin", "le087"), balancerInfoTesting.getResps());
    }

    @Test
    public void monitorParseSettingsProductionTest() {
        assertTrue(balancerInfoProduction.isJugglerMonitor());
        Assert.assertEquals(Arrays.asList(
            "yandex_market_dev", "yandex_market_admin", "le087"), balancerInfoTesting.getResps());
    }

    @Test
    public void parseProductionDomainTest() {
        Assert.assertEquals("test_service_name", balancerInfoProduction.getServiceName());
        Assert.assertEquals("vs.market.yandex.net", balancerInfoProduction.getServiceDomain());
    }

    @Test
    public void parseEmptyListTest() throws Exception {
        BalancerInfo balancerInfo = getTestList(null);
        Assert.assertNull(balancerInfo.getHumanAccess());
        balancerInfo = getTestList("");
        Assert.assertEquals(Collections.emptyList(), balancerInfo.getHumanAccess());
    }

    @Test
    public void parseOneElementListTest() throws Exception {
        BalancerInfo balancerInfo = getTestList("test abc group");
        Assert.assertEquals(List.of("test abc group"), balancerInfo.getHumanAccess());

        balancerInfo = getTestList("test abc group,   ,  ,,   ");
        Assert.assertEquals(List.of("test abc group"), balancerInfo.getHumanAccess());
    }

    @Test
    public void parseTwoElementListTest() throws Exception {
        BalancerInfo balancerInfo = getTestList("test abc group 1,test abc group 2");
        Assert.assertEquals(Arrays.asList("test abc group 1", "test abc group 2"), balancerInfo.getHumanAccess());

        balancerInfo = getTestList("test abc group 1, test abc group 2");
        Assert.assertEquals(Arrays.asList("test abc group 1", "test abc group 2"), balancerInfo.getHumanAccess());

        balancerInfo = getTestList("test abc group 1, test abc group 2, , ,  ");
        Assert.assertEquals(Arrays.asList("test abc group 1", "test abc group 2"), balancerInfo.getHumanAccess());
    }

    /**
     * Если порт реалов не задан в форме, то он равен httpsPort, если он указан, либо httpPort.
     */
    @Test
    public void testCalculateRealPortRealPort() {
        Assert.assertEquals((Integer) 55556, balancerInfoTesting.calculateRealPort());
    }

    @Test
    public void testCalculateRealPortHttp() {
        Assert.assertEquals((Integer) 80, balancerInfoHttp.calculateRealPort());
    }

    @Test
    public void testCalculateRealPortHttpsNoSslBackends() {
        balancerInfoHttp.setHttpsPort(443);
        balancerInfoHttp.setSslBackends(false);
        Assert.assertEquals((Integer) 80, balancerInfoHttp.calculateRealPort());
    }

    @Test
    public void testCalculateRealPortHttpsSslBackends() {
        balancerInfoHttp.setHttpsPort(443);
        balancerInfoHttp.setSslBackends(true);
        Assert.assertEquals((Integer) 443, balancerInfoHttp.calculateRealPort());
    }

    /**
     * Валидатор cname преобразует их в абсолютные
     */
    @Test
    public void testAbsoluteCnames() throws Exception {
        BalancerInfo balancerInfo = getTestList("");
        Assert.assertEquals(
            Arrays.asList("cname1.market.yandex.net.", "cname2.market.yandex.net."),
            balancerInfo.getCnames());
    }

    @Test
    public void testIfBooleanFieldsIsNull() throws IOException {
        BalancerInfo balancerInfo = BalancerPipelineTestFactory.getBalancerInfo();

        balancerInfo.setRedirectToHttps(false);
        balancerInfo.setSslBackends(false);

        String descriptionFalseBoolean = balancerInfo.calculateDescriptionForParser();
        assertContains(descriptionFalseBoolean, "slb.redirect_to_https = Нет");
        assertContains(descriptionFalseBoolean, "slb.ssl_backends = Нет");

        balancerInfo.setRedirectToHttps(true);
        balancerInfo.setSslBackends(true);

        String descriptionTrueBoolean = balancerInfo.calculateDescriptionForParser();
        assertContains(descriptionTrueBoolean, "slb.redirect_to_https = Да");
        assertContains(descriptionTrueBoolean, "slb.ssl_backends = Да");
    }

    @Test
    public void testCalculateBalancerPipelineArgs() throws UnsupportedEncodingException {
        String formData = balancerInfoTesting.calculateBalancerPipelineArgs();
        assertContains(formData, "%7B%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".healthCheckUrl%22%3A%22%2Fping%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".enableHttp2%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".humanAccess%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".httpsPort%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".description%22%3A%22%D0%A7%D1%82%D0%BE-%D1%82%D0%BE+%D0%B4%D0%BB%D1%8F+%D1%82%D0%B5%D1%81%D1%82%D0%B0" +
            "%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".sslExternalCa%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".realServers%22%3A%22market_slb_search-stable%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".rps%22%3A%2220+%D1%80%D0%BF%D1%81%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".enableGrpc%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".sslAltnames%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".enableGrpcSsl%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".realPort%22%3A%2255556%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".balancerType%22%3A%22%D0%92%D0%BD%D1%83%D1%82%D1%80%D0%B5%D0%BD%D0%BD%D0%B8%D0%B9%22%2C%2217952fb1-d0e7" +
            "-4da5-b044-e96025450fd5.healthCheckText%22%3A%220%3Bok%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".typeOfBackends%22%3A%22CONDUCTOR_GROUP%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".jugglerMonitor%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".grpcBackendsPort%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".httpPort%22%3A%2255555%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".grpcOffsetPort%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".startrekTicketKey%22%3A%22TEST-12345%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".offsetPort%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".ipVersion%22%3A%22IPv6-only%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".sslBackends%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".healthCheckType%22%3A%22%D0%A2%D0%B5%D0%BB%D0%BE+%D0%BE%D1%82%D0%B2%D0%B5%D1%82%D0%B0%22%2C%2217952fb1" +
            "-d0e7-4da5-b044-e96025450fd5.grpcSslBackends%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044" +
            "-e96025450fd5.fqdn%22%3A%22test-service.name.tst.vs.market.yandex" +
            ".net%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".cnames%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".redirectToHttps%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".machineAccess%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5" +
            ".resps%22%3A%22yandex_market_dev%2Cyandex_market_admin%2Cle087%22%7D");
    }

    @Test
    public void testEnableGrpc() {
        Assert.assertTrue(balancerInfoGrpc.getEnableGrpc());
        Assert.assertFalse(balancerInfoGrpc.getEnableGrpcSsl());
        Assert.assertTrue(balancerInfoGrpc.getPorts().contains(8080));
    }

    @Test
    public void testEnableGrpcSsl() {
        Assert.assertTrue(balancerInfoGrpcSsl.getEnableGrpc());
        Assert.assertTrue(balancerInfoGrpcSsl.getEnableGrpcSsl());
        Assert.assertTrue(balancerInfoGrpcSsl.getPorts().contains(8443));
    }
}
