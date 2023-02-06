package ru.yandex.autotests.search.meta;

import net.lightbody.bmp.proxy.LegacyProxyServer;
import net.lightbody.bmp.proxy.ProxyServer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.SessionNotFoundException;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.search.meta.data.ReportCreator;
import ru.yandex.autotests.search.meta.data.ReportHandler;
import ru.yandex.autotests.search.meta.data.TestScenario;
import ru.yandex.autotests.search.meta.report.Report;
import ru.yandex.autotests.search.meta.report.Reports;
import ru.yandex.autotests.search.meta.report.Status;
import ru.yandex.autotests.search.meta.rules.RetryRule;
import ru.yandex.autotests.search.meta.search.Meta;
import ru.yandex.autotests.search.meta.search.Service;
import ru.yandex.autotests.search.meta.utils.Browser;
import ru.yandex.autotests.search.meta.utils.TouchUserAgent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import static org.junit.Assert.fail;

/**
 * User: leonsabr, fenice
 * Date: 24.04.13
 */
@Aqua.Test(title = "Meta Test", description = "Приемка meta поисков")
@RunWith(Parameterized.class)
@Feature("AllMetaTests")
public class MetaTest {

    private static final Config CONFIG = Config.INSTANCE;

    private static LegacyProxyServer proxy;

    private static final ReportHandler REPORT =
            new ReportHandler(CONFIG.getMetaType(), CONFIG.getDomain(), CONFIG.getBrowserDescription());

    private static final Meta META = CONFIG.getMeta();

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<TestScenario[]> data() throws IOException {
        return META.getScenarios();
    }

    @Rule
    public RetryRule retry = RetryRule.retry()
            .withReportHandler(REPORT)
            .ifException(WebDriverException.class)
            .ifException(SessionNotCreatedException.class)
            .ifException(SessionNotFoundException.class)
            .times(2);

    @BeforeClass
    public static void addInterceptors() throws Exception {
        proxy = new ProxyServer();
        proxy.start();
        TouchUserAgent.setUserAgent(proxy);
    }

    private final TestScenario scenario;
    private Browser browser;

    public MetaTest(TestScenario scenario) {
        this.scenario = scenario;
    }

    @Before
    public void prepareRequests() throws Exception {
        browser = new Browser(getCapabilities());
    }

    private DesiredCapabilities getCapabilities() throws UnknownHostException {
        DesiredCapabilities caps =
                new DesiredCapabilities(CONFIG.getBrowserName(), CONFIG.getBrowserVersion(), Platform.ANY);
        caps.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        if (isTouch(scenario)) {
            caps.setCapability(CapabilityType.PROXY, createHttpProxy(proxy.getPort()));
        }
        return caps;
    }

    private Proxy createHttpProxy(int port) throws UnknownHostException {
        Proxy proxy = new Proxy();
        proxy.setProxyType(Proxy.ProxyType.MANUAL);
        String proxyStr = String.format("%s:%d", InetAddress.getLocalHost().getCanonicalHostName(), port);
        proxy.setHttpProxy(proxyStr);
        proxy.setSslProxy(proxyStr);
        return proxy;
    }

    private static boolean isTouch(TestScenario scenario) {
        return scenario.getRequest().getService().equals(Service.TOUCHSEARCH);
    }

    @Test
    public void executeActions() {
        scenario.perform(browser, REPORT);
    }

    @After
    public void closeWindows() {
        if (browser != null) {
            browser.quit();
        }
    }

    @AfterClass
    public static void publish() throws Exception {
        Reports bean = REPORT.getBean();
        ReportCreator.saveReport(bean);
        proxy.stop();

        for (Report report : bean.getReport()) {
            if (Status.FAIL.equals(report.getStatus())) {
                fail("Есть отчёты со статусом FAIL.");
                break;
            }
        }
    }

}
