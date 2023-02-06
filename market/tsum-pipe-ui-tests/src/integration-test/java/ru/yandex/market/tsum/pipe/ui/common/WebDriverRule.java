package ru.yandex.market.tsum.pipe.ui.common;

import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.hamcrest.Matcher;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.PageFactory;
import ru.yandex.qatools.htmlelements.element.Select;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementDecorator;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementLocatorFactory;
import ru.yandex.qatools.htmlelements.matchers.MatcherDecorators;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.pageRefresh;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.should;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.timeoutHasExpired;

/**
 * Этот класс выполняет следующие задачи:
 * 1. Создаёт и убивает WebDriver в нужные моменты
 * 2. Логинится под тестовым пользователем
 * 3. Позволяет передавать себя в PageObject'ы до инициализации WebDriver'а, это позволяет не выносить инициализацию
 * PageObject'ов в @Before
 * 4. Логирует в Allure-отчёт переходы на страницы и скриншоты
 * 5. Делает скриншот если тест упал
 *
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 13.02.2018
 */
public class WebDriverRule extends TestWatcher implements WebDriver {
    private final String initialUrl;
    private WebDriver webDriver;

    public WebDriverRule(String initialUrl) {
        this.initialUrl = initialUrl;
    }


    public <T> T createPageObject(Supplier<T> supplier) {
        T result = supplier.get();
        PageFactory.initElements(new HtmlElementDecorator(new HtmlElementLocatorFactory(this)), result);
        return result;
    }


    public void click(WebElement webElement) {
        click(getCurrentUrl(), webElement);
    }

    public void submit(WebElement webElement) {
        submit(getCurrentUrl(), webElement);
    }

    public void sendKeys(WebElement webElement, String keysToSend) {
        sendKeys(getCurrentUrl(), webElement, keysToSend);
    }

    public void selectByValue(Select select, String value) {
        selectByValue(getCurrentUrl(), select, value);
    }

    public void selectByValueInReactSelect(WebElement element, String value) {
        element.sendKeys(value + Keys.ENTER);
    }

    public void clear(WebElement webElement) {
        clear(getCurrentUrl(), webElement);
    }

    @Step("Нажимаем OK в алерте")
    public void acceptAlert() {
        webDriver.findElement(By.cssSelector(".modal-open .btn-primary")).click();

        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException e) {
            throw new RuntimeException("", e);
        }
    }

    public <T> void assertStep(T actual, Matcher<? super T> matcher) {
        assertStep(getCurrentUrl(), actual, matcher);
    }

    public <T> void assertWaitStep(T actual, Matcher<? super T> matcher) {
        assertWaitStep(getCurrentUrl(), actual, matcher);
    }

    public <T> void refreshUntil(T actual, Matcher<? super T> matcher) {
        assertStep(
            actual,
            MatcherDecorators.should(matcher)
                .after(pageRefresh(webDriver))
                .whileWaitingUntil(MatcherDecorators.timeoutHasExpired(TimeUnit.SECONDS.toMillis(120)))
        );
    }


    @Step("Кликаем по {webElement}")
    private void click(String currentUrlForAllureReport, WebElement webElement) {
        takeScreenshot("Скриншот до");
        webElement.click();
    }

    @Step("Сабмитим {webElement}")
    private void submit(String currentUrlForAllureReport, WebElement webElement) {
        takeScreenshot("Скриншот до");
        webElement.submit();
    }

    @Step("Вводим \"{keysToSend}\" в {webElement}")
    private void sendKeys(String currentUrlForAllureReport, WebElement webElement, String keysToSend) {
        takeScreenshot("Скриншот до");
        webElement.sendKeys(keysToSend);
    }

    @Step("Выбираем значение \"{value}\" в {select}")
    private void selectByValue(String currentUrlForAllureReport, Select select, String value) {
        takeScreenshot("Скриншот до");
        select.selectByValue(value);
    }

    @Step("Очищаем {webElement}")
    private void clear(String currentUrlForAllureReport, WebElement webElement) {
        takeScreenshot("Скриншот до");
        webElement.clear();
    }

    @Step("Проверяем что {actual} {matcher}")
    private <T> void assertStep(String currentUrlForAllureReport, T actual, Matcher<? super T> matcher) {
        takeScreenshot("Скриншот до");
        assertThat(actual, matcher);
    }

    private <T> void assertWaitStep(String currentUrlForAllureReport, T actual, Matcher<? super T> matcher) {
        assertStep(actual, should(matcher).whileWaitingUntil(timeoutHasExpired(TimeUnit.SECONDS.toMillis(120))));
    }


    @Override
    protected void starting(Description description) {
        DesiredCapabilities desiredCapabilities = getDesiredCapabilities();
        attach("Браузер (что попросили)", desiredCapabilities);
        RemoteWebDriver remoteWebDriver = new RemoteWebDriver(getSeleniumUrl(), desiredCapabilities);
        attach("Браузер (что получили)", remoteWebDriver.getCapabilities());
        attach("Id селениумной сессии", remoteWebDriver.getSessionId());
        this.webDriver = remoteWebDriver;
        takeScreenshot("Скриншот до начала работы с браузером");
        loginAndGoTo(initialUrl);
    }

    @Attachment("{description}")
    private static <T> T attach(String description, T o) {
        return o;
    }

    private static URL getSeleniumUrl() {
        try {
            return new URL(TsumPipeUiTestsProperties.SELENIUM_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static DesiredCapabilities getDesiredCapabilities() {
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities(
            TsumPipeUiTestsProperties.BROWSER_NAME,
            TsumPipeUiTestsProperties.BROWSER_VERSION,
            Platform.ANY
        );

        // Чтобы браузер не ругался на YandexInternalCA
        desiredCapabilities.setAcceptInsecureCerts(true);

        // хак, что бы выставить правильную версию браузера
        desiredCapabilities.setCapability("browserVersion", TsumPipeUiTestsProperties.BROWSER_VERSION);

        desiredCapabilities.setCapability("sessionTimeout", "2m");

        // Чтобы при запуске над локальным Selenoid'ом можно было смотреть что в браузере происходит
        if (TsumPipeUiTestsProperties.ENABLE_VNC) {
            desiredCapabilities.setCapability("enableVNC", true);
        }

        // Настраиваем подробность логов, в конце они будут прикреплены к отчёту, бывает полезно для отладки
        desiredCapabilities.setCapability(CapabilityType.LOGGING_PREFS, getLoggingPreferences());

        return desiredCapabilities;
    }

    private static LoggingPreferences getLoggingPreferences() {
        LoggingPreferences logs = new LoggingPreferences();
        logs.enable(LogType.BROWSER, Level.ALL);
        logs.enable(LogType.PERFORMANCE, Level.INFO);
        return logs;
    }

    @Step("Логинимся и переходим на страницу {url}")
    private void loginAndGoTo(String url) {
        webDriver.get(TsumPipeUiTestsProperties.TSUM_TEST_USER_LOGIN == null ? url : getLoginUrl(url));

        // http://aqua.yandex-team.ru/auth.html редиректит джаваскриптом, поэтому то, что webDriver.get() закончился,
        // не значит что мы перешли на конечную страницу. Если здесь не подождать и если тест сразу переходит на другую
        // страницу, то куки не проставятся.
        assertWaitStep(
            webDriver,
            hasProperty("currentUrl", not(containsString("https://aqua.yandex-team.ru")))
        );
    }

    private static String getLoginUrl(String retpath) {
        try {
            // Логинимся через акву, там есть ручка, логинящая простым GET-запросом. Логиниться через форму долго и
            // ненадёжно. Форма работает через POST-запрос, WebDriver POST не умеет.
            return String.format(
                "https://aqua.yandex-team.ru/auth-html" +
                    "?mode=auth" +
                    "&login=%s" +
                    "&secretId=%s" +
                    "&retpath=%s",
                TsumPipeUiTestsProperties.TSUM_TEST_USER_LOGIN,
                URLEncoder.encode(TsumPipeUiTestsProperties.TSUM_TEST_USER_SECRET_ID, "UTF-8"),
                URLEncoder.encode(retpath, "UTF-8")
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void finished(Description description) {
        takeScreenshot("Скриншот в конце");
        attachWebDriverLogs();
        webDriver.quit();
    }

    private void attachWebDriverLogs() {
        attachWebDriverLog(LogType.BROWSER);
        attachWebDriverLog(LogType.CLIENT);
        attachWebDriverLog(LogType.DRIVER);
        attachWebDriverLog(LogType.PERFORMANCE);
        attachWebDriverLog(LogType.PROFILER);
        attachWebDriverLog(LogType.SERVER);
    }

    private void attachWebDriverLog(String logType) {
        try {
            List<LogEntry> logEntries = webDriver.manage().logs().get(logType).getAll();
            if (!logEntries.isEmpty()) {
                attachWebDriverLog(logType, logEntries);
            }
        } catch (RuntimeException ignored) {
            // Не все браузеры поддерживают все логи
        }
    }

    @Attachment(value = "Лог Selenium'а logType={logType}", type = "text/plain")
    private String attachWebDriverLog(String logType, List<LogEntry> logEntries) {
        return logEntries.stream()
            .map(LogEntry::toString)
            .collect(Collectors.joining("\n"));
    }


    @Step("Переходим на страницу {url}")
    @Override
    public void get(String url) {
        takeScreenshot("Скриншот до");
        webDriver.get(url);
    }

    @Override
    public String getCurrentUrl() {
        return webDriver.getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return webDriver.getTitle();
    }

    @Override
    public List<WebElement> findElements(By by) {
        return webDriver.findElements(by);
    }

    @Override
    public WebElement findElement(By by) {
        return webDriver.findElement(by);
    }

    @Override
    public String getPageSource() {
        return webDriver.getPageSource();
    }

    @Override
    public void close() {
        webDriver.close();
    }

    @Override
    public void quit() {
        webDriver.quit();
    }

    @Override
    public Set<String> getWindowHandles() {
        return webDriver.getWindowHandles();
    }

    @Override
    public String getWindowHandle() {
        return webDriver.getWindowHandle();
    }

    @Override
    public TargetLocator switchTo() {
        return webDriver.switchTo();
    }

    @Override
    public Navigation navigate() {
        return webDriver.navigate();
    }

    @Override
    public Options manage() {
        return webDriver.manage();
    }

    public byte[] takeScreenshot() {
        return takeScreenshot("Скриншот");
    }

    @Attachment("{name}")
    public byte[] takeScreenshot(@SuppressWarnings("unused") String name) {
        return ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
    }
}
