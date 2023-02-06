package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.android;

import java.util.concurrent.TimeUnit;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.support.ui.WebDriverWait;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.LoginPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.SeleniumUtil.clearSession;

@Slf4j
public class Login {
    private User defaultUser;
    private WebDriverWait wait = new WebDriverWait(getWebDriver(), WebDriverTimeout.LONG_WAIT_TIMEOUT);

    public Login(User user) {
        this.defaultUser = user;
    }

    public void PerformLogin() {
        PerformLogin(defaultUser);
    }

    public void PerformLogin(User user) {
        PerformLogin(user.getLogin(), user.getPass());
    }

    @Step("Логин в новый ui")
    private void PerformLogin(String username, String password) {
        final String mainMenuRegexp = ".*[^=]/ui/($|\\?)";

        log.info("Performing login to Android with username: {}", username);

        clearSession(getWebDriver());
        $(byXpath("//div/input")).shouldBe(visible);

        Retrier.retry(() -> {
            log.info("Trying to log in");
            new LoginPage(getWebDriver()).login(username, password);
            wait.until(urlMatches(mainMenuRegexp));
        }, Retrier.RETRIES_SMALL, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);

        log.info("UI logon successful");
    }
}
