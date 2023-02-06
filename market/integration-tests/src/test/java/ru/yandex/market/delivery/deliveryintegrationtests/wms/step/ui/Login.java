package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import io.qameta.allure.Step;
import static com.codeborne.selenide.Selectors.byXpath;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.LoginPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.SeleniumUtil.clearSession;

@Resource.Classpath({"wms/infor.properties"})
public class Login {
    private WebDriver driver;
    private WebDriverWait wait;

    private final Logger log = LoggerFactory.getLogger(Login.class);

    private final String baseUrl;

    @Property("infor.host")
    private String host;

    @Property("infor.ui")
    private String ui;

    @Property("infor.ui.login.page.path")
    private String loginPagePath;

    @Property("infor.ui.query")
    private List<String> query;

    private final User defaultUser;

    public Login(WebDriver driver, User defaultUser) {
        this.driver = driver;
        this.wait = new WebDriverWait(this.driver, WebDriverTimeout.MEDIUM_WAIT_TIMEOUT);
        this.defaultUser = defaultUser;

        PropertyLoader.newInstance().populate(this);

        String urlQuery = String.join("&", this.query);
        baseUrl = host + ui + loginPagePath + "?" + urlQuery;
    }

    public MenuPage PerformLogin() {
        return PerformLogin(baseUrl);
    }

    public MenuPage PerformLogin(String url) {
        return PerformLogin(defaultUser, url);
    }
    /**
     * Алгоритм логина:
     * 1. Открыть страницу логина
     * 2. Проверить, что виден инпут, если виден, то url уже точно не изменится
     * 3. По url определить залогинен ли пользователь
     * 4. Если пользователь залогинен вернуть MenuPage
     * 5. Если пользователь не залогинен, то авторизоваться
     * 6. Если логин не удался, то повторить залогин до успеха либо до превышения попыток Retrier.RETRIES_SMALL
     * @param user
     * @return MenuPage
     */
    private MenuPage PerformLogin(User user, String url) {
        return PerformLogin(user.getLogin(), user.getPass(), url);
    }

    @Step("Логин в новый ui")
    private MenuPage PerformLogin(String username, String password, String url) {
        final String mainMenuRegexp = ".*[^=]/ui/($|\\?)";



        MenuPage menu = Retrier.retry(() -> {
            log.info("Performing login to: {} with username: {}", url, username);

            clearSession(driver);
            open(url);
            $(byXpath("//div/input")).shouldBe(visible);
            log.info("Trying to log in");
            MenuPage menuPage =  new LoginPage(driver).login(username, password);
            wait.until(urlMatches(mainMenuRegexp));
            return menuPage;
        }, Retrier.RETRIES_SMALL, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);

        log.info("UI logon successful");
        return menu;
    }
}
