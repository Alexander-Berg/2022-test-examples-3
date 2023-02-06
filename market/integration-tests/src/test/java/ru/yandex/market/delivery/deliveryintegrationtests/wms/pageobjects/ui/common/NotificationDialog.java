package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common;

import java.util.concurrent.TimeUnit;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.HtmlElementsCommon;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.page;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;

public class NotificationDialog extends HtmlElementsCommon {

    @FindBy(xpath = "//span[@data-e2e='notification__text']")
    private SelenideElement notificationText;

    @FindBy(xpath = "//span[@data-e2e='notification__title']")
    private SelenideElement notificationTitle;

    public NotificationDialog(WebDriver driver) {
        super(driver);
        page(this);
    }

    private static final String DIALOG_XPATH = "//div[@data-e2e='notification']";

    public boolean isPresent() {
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.MEDIUM_WAIT_TIMEOUT, TimeUnit.SECONDS);
        Boolean result = $$(byXpath(DIALOG_XPATH)).size() != 0;

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
        return result;
    }

    public String getMessage() {
        return notificationText.getText();
    }

    public String getTitle() {
        return notificationTitle.getText();
    }

    /**
     * Если надо точно знать, что за timeout секунд не появится ли характерных ошибок.
     * Для предпроверки на непредвиденные ошибки использовать быстрый getRemoteErrorBreakerCondition()
     */
    @Step("Смотрим, что нет backend-ошибки")
    public void assertNoRemoteErrorsShown(long timeout) {
        WebDriverWait wait = new WebDriverWait(getWebDriver(), timeout);
        wait.until(invisibilityOfElementLocated(getRemoteErrorLocator()));
    }

    private static By getRemoteErrorLocator() {
        return byXpath(DIALOG_XPATH +
                "//span[@data-e2e='notification__title' and (" +
                " contains(text(),'INTERNAL_SERVER_ERROR')" +
                " or contains(text(),'Что-то пошло не так')" +
                " or contains(text(),'Ошибка сети')" +
                " or contains(text(),'Превышено время ожидания запроса')" +
                ")]");
    }

    public static ExpectedCondition<Boolean> getRemoteErrorBreakerCondition() {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
                try {
                    $$(getRemoteErrorLocator()).stream().forEach(selenideElement -> {
                        if (selenideElement.isDisplayed()) {
                            throw new IllegalStateException("В процессе работы wms возникла неожиданная ошибка");
                        }
                    });
                } finally {
                    driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
                }
                return true;
            }
        };
    }

    public Boolean isPresentWithTitle(String title) {
        return isPresentWithTitleCustomTimeout(title, WebDriverTimeout.LONG_WAIT_TIMEOUT);
    }

    public Boolean isPresentWithTitleCustomTimeout(String title, long timeout) {
        driver.manage().timeouts().implicitlyWait(timeout, TimeUnit.SECONDS);

        String searchString = "//span[@data-e2e='notification__title']" +
                "[text()[contains(., 'title_text_placeholder')]]"
                        .replaceFirst("title_text_placeholder", title);
        Boolean result = $$(byXpath(searchString))
                .size() != 0;

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
        return result;
    }

    public Boolean IsPresentWithMessage(String message) {
        return IsPresentWithMessageWithCustomTimeout(message, WebDriverTimeout.MEDIUM_WAIT_TIMEOUT);
    }

    public Boolean IsPresentWithMessageWithCustomTimeout(String message, long timeout) {
        driver.manage().timeouts().implicitlyWait(timeout, TimeUnit.SECONDS);

        String searchString = "//span[@data-e2e='notification__text']" +
                "[text()[contains(., 'message_text_placeholder')]]"
                        .replaceFirst("message_text_placeholder", message);
        Boolean result = $$(byXpath(searchString))
                .size() != 0;

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
        return result;
    }

    public void waitUntilHidden() {
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.SMALL_WAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);

        if ($$(byXpath(DIALOG_XPATH)).size() != 0) {
            driver.manage().timeouts().implicitlyWait(0, TimeUnit.MILLISECONDS);
            wait.until(invisibilityOfElementLocated(byXpath(DIALOG_XPATH)));
        }

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
    }
}
