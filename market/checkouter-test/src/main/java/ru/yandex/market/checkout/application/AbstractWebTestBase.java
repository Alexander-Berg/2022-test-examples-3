package ru.yandex.market.checkout.application;

import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.config.web.WebContextConfig;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.test.config.web.IntTestWebConfig;
import ru.yandex.market.checkout.common.logbroker.LogbrokerJsonEventPublishServiceImpl;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.test.MemCachedAgentMockFactory;
import ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer;
import ru.yandex.market.checkout.util.mstat.MstatAntifraudConfigurer;
import ru.yandex.market.checkout.util.pushApi.PushApiConfigurer;
import ru.yandex.market.checkout.util.report.ReportConfigurer;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;
import ru.yandex.market.metrics.micrometer.PrometheusConfiguration;

@ContextHierarchy({
        @ContextConfiguration(name = "web",
                classes = AbstractWebTestBase.WebConfiguration.class,
                initializers = AbstractWebTestBase.ContextInitializer.class)
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class AbstractWebTestBase extends AbstractServicesTestBase {

    public static final int CIS_REQUIRED_CARGOTYPE_CODE = 980;
    public static final int CIS_DISTINCT_CARGOTYPE_CODE = 985;
    public static final int CIS_OPTIONAL_CARGOTYPE_CODE = 990;

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ReportConfigurer reportConfigurer;
    @Autowired
    @Qualifier("reportConfigurerWhite")
    protected ReportConfigurer reportConfigurerWhite;
    @Autowired
    protected StockStorageConfigurer stockStorageConfigurer;
    @Autowired
    protected PushApiConfigurer pushApiConfigurer;
    @Autowired
    protected LoyaltyConfigurer loyaltyConfigurer;
    @Autowired
    protected MstatAntifraudConfigurer mstatAntifraudConfigurer;
    @Autowired
    protected MemCachedAgentMockFactory memCachedAgentMockFactory;
    @Autowired
    protected MemCachedAgent memCachedAgentMock;
    @Autowired
    protected LogbrokerClientFactory lbkxClientFactory;
    @Autowired
    protected OrderCreateHelper orderCreateHelper;
    @Autowired
    protected OrderStatusHelper orderStatusHelper;
    @Autowired
    private WebApplicationContext wac;
    @Autowired
    private List<WireMockServer> mocks;
    @Autowired
    protected CheckouterAPI client;
    @Autowired
    protected ColorConfig colorConfig;
    @Autowired
    protected PersonalDataService personalDataService;
    @Autowired
    private LogbrokerJsonEventPublishServiceImpl lbkxEventPublishService;


    public WebApplicationContext getWebApplicationContext() {
        return wac;
    }

    public MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    @BeforeEach
    public void setUpBase() {
        super.setUpBase();
        clean();
        mstatAntifraudConfigurer.mockOk();
    }

    @AfterEach
    public void clean() {
        resetWireMocks();
        memCachedAgentMockFactory.resetMemCachedAgentMock(memCachedAgentMock);
        pushApiConfigurer.resetMocks();
    }

    protected void resetWireMocks() {
        for (WireMockServer mock : mocks) {
            mock.resetAll();
        }
    }

    @Configuration
    @ComponentScan(basePackageClasses = {WebContextConfig.class, IntTestWebConfig.class, PrometheusConfiguration.class})
    public static class WebConfiguration {

    }

    public static class ContextInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

        @Override
        public void initialize(GenericApplicationContext context) {
            context.setAllowCircularReferences(false);
        }
    }
}
