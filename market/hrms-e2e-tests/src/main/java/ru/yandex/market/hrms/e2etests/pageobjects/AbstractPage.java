package ru.yandex.market.hrms.e2etests.pageobjects;

import java.util.concurrent.TimeUnit;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import ru.yandex.market.hrms.e2etests.selenium.WebDriverTimeout;

import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public abstract class AbstractPage {
    protected WebDriverWait wait;

    public AbstractPage() {
        super();
        this.wait = new WebDriverWait(WebDriverRunner.getWebDriver(), WebDriverTimeout.MEDIUM_WAIT_TIMEOUT);
        String urlRegexp = urlCheckRegexp();

        try {
            wait.until(urlMatches(urlRegexp));
        } catch (Exception e) {
            throw new NotFoundException("Ошибка при открытии страницы. " +
                    "Ожидалось, что адрес будет содержать " + urlRegexp, e);
        }

        //Нужно, чтобы в checkPageElements можно было обращаться к элементам заданным по @FingBy
        Selenide.page(this);

        checkPageElements();
    }

    /**
     * Метод, в котором нужно отдавать строку
     * по которой при загрузке страницы будет проверяться,
     * что открылся правильный url
     */
    protected abstract String urlCheckRegexp();

    /**
     * Метод, в котором нужно указать, какие элементы должны провериться
     * на наличие автоматически при загрузке страницы
     *
     * Если наличие элементов для страницы проверять излишне, то можно реализовать пустым
     */
    protected abstract void checkPageElements();

    /**
     * Подсвечивает красным элемент на странице
     * Бывает удобно при отладке
     */
    public void highlightElement(SelenideElement element) {
        Selenide.executeJavaScript("arguments[0].style.border='2px solid red'", element);
    }

    /**
     * Ждём, когда пропадет с экрана элемент:
     *
     * optional = true - когда элемент может уже не отображаться на момент вызова
     *
     * optional = false - когда нужно обязательно убедиться в наличии элемента
     * перед тем как ждать его изчезновения (default)
     */
    @Step("Проверяем, что элемент скрылся {by}")
    public void waitElementHidden(By by, boolean optional) {

        WebDriver driver = Selenide.webdriver().object();

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.SMALL_WAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);

        if (driver.findElements(by).size() != 0) {
            do {
                driver.manage().timeouts().implicitlyWait(0, TimeUnit.MILLISECONDS);
                wait.until(invisibilityOfElementLocated(by));
                driver.manage().timeouts().implicitlyWait(WebDriverTimeout.SMALL_WAIT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
            } while (driver.findElements(by).size() > 0);
        } else if (!optional) {
            throw new ElementNotVisibleException("Element not found: " + by.toString());
        }

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
    }

    public void waitElementHidden(By by) {
        waitElementHidden(by, true);
    }
}
