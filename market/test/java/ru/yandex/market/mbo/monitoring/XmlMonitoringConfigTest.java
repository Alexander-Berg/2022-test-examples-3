package ru.yandex.market.mbo.monitoring;


import org.junit.Assert;
import org.junit.Test;

/**
 * @author Andrew Moiseev (moisandrew@yandex-team.ru)
 * @date 01.02.2019
 */

public class XmlMonitoringConfigTest {

    private static String pathToXml = "ru/yandex/market/mbo/monitoring/monitoring-config-testing.xml";

    private static String testingMboHostName = "market_mbo.testing";
    private static int testingMboCountOfGroups = 2;
    private static int tastingMboCountOfHosts = 3;
    private static int testingMboCountOfService = 2;


    @Test
    public void readXml() {
        XmlMonitoringConfig config = new XmlMonitoringConfig(pathToXml);

        Assert.assertFalse("Group is empty", config.getGroups().isEmpty());
        Assert.assertTrue(config.getGroups().size() == testingMboCountOfGroups);

        Assert.assertFalse("Host is empty", config.getHosts().isEmpty());
        Assert.assertTrue(config.getHosts().size() == tastingMboCountOfHosts);

        Assert.assertTrue("Count of service is invalid",
            config.getServices(testingMboHostName).size() == testingMboCountOfService);
    }
}
