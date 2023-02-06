package ru.yandex.market.olap2.controller;

import org.junit.Test;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

public class MonitorDiskUsageControllerTest {
    @Test
    public void testGetUsed() throws Exception {
        System.out.println("Used: " + MonitorDiskUsageController.getUsed("/"));
        assertTrue(MonitorDiskUsageController.getUsed("/") > 0);
    }

    @Test
    public void testCheckQuota() throws Exception {
        assertTrue(MonitorDiskUsageController.checkQuota(0.1)
            .startsWith(JugglerConstants.OK));
        assertTrue(MonitorDiskUsageController.checkQuota(0.99)
            .startsWith(JugglerConstants.CRIT));
    }

}
