package ru.yandex.autotests.innerpochta.rules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.junit.rules.ExternalResource;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
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
import static ru.yandex.autotests.innerpochta.util.props.HostRootProperties.hostrootProps;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;

public class WebDriverRule extends ExternalResource {

    private final static Logger LOGGER = Logger.getLogger(WebDriverRule.class);
    private EventFiringWebDriver eventDriver;
    private WebDriver webDriver;
    private DesiredCapabilities capabilities;
    private RetryRule retry;

    public WebDriverRule(DesiredCapabilities capabilities) {
        this.capabilities = capabilities;
        setAllureReportEnvironment();
    }

    public WebDriverRule() {
        setCapabilities();
        setAllureReportEnvironment();
    }

    protected void before() throws Throwable {
        if (WebDriverProperties.props().driverType().equals("chrome")) {
            setChromeOptions();
        }
        if (urlProps().getVideo() != null) {
            //https://sw.yandex-team.ru/video/<session-id>
            capabilities.setCapability("enableVideo", true);
            //https://sw.yandex-team.ru/novnc/vnc.html?host=sw.yandex-team.ru&port=443&path=v0%2Fvnc%2F<SESSION_ID>&encrypt=true&password=selenoid&autoconnect=true&resize=scale
            capabilities.setCapability("enableVnc", true);
        }
        LOGGER.info(format("Getting browser start: %s", new Date().toString()));
        capabilities.setCapability("acceptInsecureCerts", true);
        this.webDriver = new SurfWaxClient()
            .withUserName(consumerProps().userName())
            .withPassword(consumerProps().password())
            .find(capabilities);
        eventDriver = new EventFiringWebDriver(webDriver);
        EventHandler eventHandler = new EventHandler();
        eventDriver.register(eventHandler);
        LOGGER.info(format("Getting browser end: %s", new Date().toString()));
        LOGGER.info(format("WebDriver session id is: %s", ((RemoteWebDriver) webDriver).getSessionId()));
    }

    protected void after() {
        this.eventDriver.quit();
    }

    private void setCapabilities() {
        capabilities = new DesiredCapabilities(
            WebDriverProperties.props().driverType(),
            WebDriverProperties.props().version(),
            WebDriverProperties.props().platform()
        );
        capabilities.setCapability(SESSION_TIMEOUT, SESSION_TIMEOUT_VALUE);
    }

    private void setChromeOptions() {
        capabilities.setCapability(ChromeOptions.CAPABILITY, getChromeOptions());
    }

    private void setAllureReportEnvironment() {
        Map<String, String> allureParameters = EnvironmentInfoHolder.getInstance().getEnvironmentInfo();
        String experiment = UrlProps.urlProps().getExperiments().toString();
        String url = !hostrootProps().testhost().equals("mail.yandex.ru") ? hostrootProps().testhost() :
            urlProps().getBaseUri();
        allureParameters.put("Browser", WebDriverProperties.props().driverType());
        allureParameters.put("Browser version", WebDriverProperties.props().version());
        allureParameters.put("Test URL", url);
        allureParameters.put("Experiment", (experiment != null) ? experiment : "-");
    }

    private ChromeOptions getChromeOptions() {
        ChromeOptions chromeOptions = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("browser.enable_spellchecking", false);
        prefs.put("spellcheck.use_spelling_service", false);
        prefs.put("profile.content_settings.exceptions.clipboard", getClipBoardSettingsMap(1));
        chromeOptions.setExperimentalOption("w3c", false);
        chromeOptions.setExperimentalOption("prefs", prefs);
        chromeOptions.setAcceptInsecureCerts(true);
//        chromeOptions.addArguments("--kiosk-printing");
        return chromeOptions;
    }

    public EventFiringWebDriver getDriver() {
        return this.eventDriver;
    }

    public RemoteWebDriver getRemoteDriver() {
        return (RemoteWebDriver) this.webDriver;
    }

    public String getBaseUrl() {
        return urlProps().getBaseUri();
    }

    public WebDriverRule withRetry(RetryRule retry) {
        this.retry = retry;
        return this;
    }

    public int getCurrentRetryNum() {
        if (retry != null) {
            return retry.getCurrentCount();
        } else
            return 0;
    }

    private static Map<String,Object> getClipBoardSettingsMap(int settingValue){
        Map<String,Object> map = new HashMap<>();
        map.put("last_modified",String.valueOf(System.currentTimeMillis()));
        map.put("setting", settingValue);
        Map<String,Object> cbPreference = new HashMap<>();
        cbPreference.put("[*.],*",map);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(cbPreference);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to enable clipboard");
        }
        return cbPreference;
    }
}
