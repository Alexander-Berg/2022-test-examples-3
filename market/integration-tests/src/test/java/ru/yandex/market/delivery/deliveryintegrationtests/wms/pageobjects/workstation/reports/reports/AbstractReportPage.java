package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.reports.reports;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import java.util.concurrent.TimeUnit;

import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;

public abstract class AbstractReportPage extends AbstractWsPage {

    protected static final Logger log = LoggerFactory.getLogger(AbstractReportPage.class);

    @Name("Фрейм отчета")
    @FindBy(xpath = "//iframe[@tabindex = '1']")
    protected HtmlElement reportFrameId;

    @Name("Кнопка Запустить отчет")
    @FindBy(xpath = "//button[text()[contains(., 'Запустить отчет')]]")
    protected HtmlElement submitButton;

    @Name("Вернуться к представлению списка")
    @FindBy(xpath = "//*[text()[contains(., 'ВЕРНУТЬСЯ К ПРЕДСТАВЛЕНИЮ СПИСКА')]]")
    protected HtmlElement backToList;

    @Name("Спиннер Загрузка")
    @FindBy(xpath = "//*[@class[contains(., 'busy')]]")
    protected HtmlElement loadingSpinner;

    @Name("Вся страница")
    @FindBy(xpath = "//*")
    protected HtmlElement wholePage;

    public AbstractReportPage(WebDriver driver) {
        super(driver);
    }

    public void waitSpinner() {
        String overlayXpath = "//div[@class = 'busy-indicator-container blocked-ui is-hidden']";

        if (driver.findElements(By.xpath(overlayXpath)).size() != 0) {
            driver.manage().timeouts().implicitlyWait(0, TimeUnit.MILLISECONDS);
            wait.until(invisibilityOfElementLocated(By.xpath(overlayXpath)));
        }

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
    }

    @Step("Жмем кнопку Запустить отчет")
    public void submitButtonClick() {
        driver.switchTo().frame(reportFrameId);
        waitSpinner();
        safeClick(submitButton);
        driver.switchTo().defaultContent();
    }

}
