package ru.yandex.market.pricelabs.tms.services.market_report;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.ConfigurationForTests;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenerationReportServiceTest extends AbstractTmsSpringConfiguration {

    @Autowired
    GenerationReportService service;

    @Autowired
    @Qualifier("mockWebServerMarketReport")
    private ConfigurationForTests.MockWebServerControls mockWebServerControls;

    String adminActionExample;

    @BeforeEach
    void init() {
        mockWebServerControls.cleanup();
        adminActionExample = Utils.readResource("tms/services/market_report/admin-action-example.xml");
    }

    @Test
    void testCorrectGettingXml() {
        mockWebServerControls.enqueue(new MockResponse().setBody(adminActionExample));
        var adminActionResponse = service.getAdminAction();
        var data = adminActionResponse.join();
        assertEquals(data.getReport(), "2021.3.25.0");
        assertEquals(data.getRevision(), 8383680);
        assertEquals(data.getHost(), "sas2-4935-1f5-sas-market-test--b33-17050.gencfg-c.yandex.net");
    }
}
