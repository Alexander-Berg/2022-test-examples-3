package ru.yandex.autotests.innerpochta.rules;

import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.events.EventFiringWebDriver;
import ru.yandex.autotests.innerpochta.rules.enviroment.EnvironmentInfoHolder;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.autotests.innerpochta.util.surfwax.SurfWaxClient;
import ru.yandex.autotests.webcommon.util.prop.WebDriverProperties;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static ru.yandex.autotests.innerpochta.util.ConsumerProps.consumerProps;
import static ru.yandex.autotests.innerpochta.util.ProxyServerConstants.SESSION_TIMEOUT;
import static ru.yandex.autotests.innerpochta.util.ProxyServerConstants.SESSION_TIMEOUT_VALUE;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;

public class TouchWebDriverRule extends WebDriverRule {

    private EventFiringWebDriver eventDriver;
    private WebDriver webDriver;

    private final static Logger LOGGER = Logger.getLogger(TouchWebDriverRule.class);

    public TouchWebDriverRule() {
    }

    private Map<String, Object> getChromeOptions(String deviceType) {
        Map<String, Object> deviceMetrics = new HashMap<>();
        Map<String, Object> mobileEmulation = new HashMap<>();

        if (deviceType.contains("Nexus 10")) {
            deviceMetrics.put("width", 1280);
            deviceMetrics.put("height", 800);
            deviceMetrics.put("pixelRatio", 2.0);

            mobileEmulation.put("deviceMetrics", deviceMetrics);
            mobileEmulation.put("userAgent", "Mozilla/5.0 (Linux; Android 6.1; Nexus 10 Build/JWR66Y) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.1547.72 Safari/537.36");
        }

        if (deviceType.contains("Nexus 5")) {
            deviceMetrics.put("width", 360);
            deviceMetrics.put("height", 640);
            deviceMetrics.put("pixelRatio", 3.0);

            mobileEmulation.put("deviceMetrics", deviceMetrics);
            mobileEmulation.put("userAgent", "Mozilla/5.0 (Linux; Android 6.1; Nexus 5 Build/JWR66Y) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.1547.72 Mobile Safari/537.36");
        }

        if (deviceType.contains("Apple iPad Pro")) {
            deviceMetrics.put("width", 1366);
            deviceMetrics.put("height", 1024);
            deviceMetrics.put("pixelRatio", 2.0);

            mobileEmulation.put("deviceMetrics", deviceMetrics);
            mobileEmulation.put("userAgent", "Mozilla/5.0 (iPad; CPU OS 12_3_1 like Mac OS X) " +
                "AppleWebKit/603.1.30 (KHTML, like Gecko) Version/12.0 Mobile/14E304 Safari/602.1");
        }

        if (deviceType.contains("Apple iPhone 6 Plus")) {
            deviceMetrics.put("width", 414);
            deviceMetrics.put("height", 736);
            deviceMetrics.put("pixelRatio", 3.0);

            mobileEmulation.put("deviceMetrics", deviceMetrics);
            mobileEmulation.put("userAgent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_3_1 like Mac OS X) " +
                "AppleWebKit/603.1.30 (KHTML, like Gecko) Version/12.0 Mobile/14E304 Safari/602.1");
        }

        Map<String, Object> chromeOptions = new HashMap<>();
        chromeOptions.put("mobileEmulation", mobileEmulation);
        chromeOptions.put("w3c", false);
        return chromeOptions;
    }

    private void setAllureReportEnvironment() {
        Map<String, String> allureParameters = EnvironmentInfoHolder.getInstance().getEnvironmentInfo();
        String experiment = UrlProps.urlProps().getExperiments().toString();
        allureParameters.put("Device type", urlProps().getDeviceType());
        allureParameters.put("Test URL", UrlProps.urlProps().getBaseUri());
        allureParameters.put("Experiment", (experiment != null) ? experiment : "-");
    }

    @Override
    protected void before() throws Throwable {
        String deviceType = urlProps().getDeviceType();
        DesiredCapabilities capabilities = new DesiredCapabilities(
            WebDriverProperties.props().driverType(),
            WebDriverProperties.props().version(),
            WebDriverProperties.props().platform()
        );
        capabilities.setCapability(SESSION_TIMEOUT, SESSION_TIMEOUT_VALUE);
        Map<String, Object> chromeOptions = getChromeOptions(deviceType);
        capabilities.setCapability("chromeOptions", chromeOptions);
        if (urlProps().getVideo() != null) {
            capabilities.setCapability("enableVideo", true);
        }
        LOGGER.info(format("Getting browser start: %s", new Date().toString()));
        this.webDriver = new SurfWaxClient()
            .withUserName(consumerProps().userName())
            .withPassword(consumerProps().password())
            .find(capabilities);
        eventDriver = new EventFiringWebDriver(webDriver);
        EventHandler eventHandler = new EventHandler();
        eventDriver.register(eventHandler);
        LOGGER.info(format("Getting browser end: %s", new Date().toString()));
        LOGGER.info(format("WebDriver session id is: %s", ((RemoteWebDriver) webDriver).getSessionId()));
        setAllureReportEnvironment();
    }

    protected void after() {
        this.eventDriver.quit();
    }

    public EventFiringWebDriver getDriver() {
        return this.eventDriver;
    }

    public RemoteWebDriver getRemoteDriver() {
        return (RemoteWebDriver) this.webDriver;
    }
}
