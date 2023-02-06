package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.replenishment;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.TablePreloader;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.market.wms.common.spring.enums.replenishment.ProblemStatus;
import ru.yandex.qatools.htmlelements.annotations.Name;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class OrderReplenishmentPage extends AbstractPage {
    private final TablePreloader tablePreloader;


    @Name("Таб Проблемные заказы")
    @FindBy(xpath = "//*[@data-e2e='tab_problem_orders']")
    private SelenideElement problemOrderTabInput;

    @Name("Поле фильтрации по номеру заказа")
    @FindBy(xpath = "//div[@data-e2e='orderKey_filter']//input")
    private SelenideElement orderKeyField;

    @Name("Кнопка Сбросить фильтры")
    @FindBy(xpath = "//button[@data-e2e='reset_table_filters']")
    private SelenideElement resetFiltersButton;

    String firstResultProblemOrderStatusXpath = "//td[@data-e2e='status_cell_row_0']//span";
    String rowProblemOrderResultXpath = "//tr[contains(@data-e2e, 'row_row')]";

    public OrderReplenishmentPage(WebDriver driver) {
        super(driver);
        this.tablePreloader = new TablePreloader(driver);
    }

    public void clickProblemOrderTab() {
        problemOrderTabInput.click();
        tablePreloader.waitUntilHidden();
    }

    @Step("Вводим {0} в поле фильтрации по номеру заказа")
    public OrderReplenishmentPage inputOrderId(String orderId) {
        orderKeyField.shouldBe(enabled, Duration.ofSeconds(20));
        orderKeyField.sendKeys(orderId);
        orderKeyField.pressEnter();

        wait.until(ExpectedConditions.elementToBeClickable(orderKeyField));

        return this;
    }

    @Step("Жмем кнопку Сбросить фильтры")
    public OrderReplenishmentPage resetFiltersClick() {
        waitTablePreloader();
        resetFiltersButton.shouldBe(enabled).click();
        return this;
    }

    @Step("Считываем статус заказа из результатов")
    public ProblemStatus getOrderStatus() {
        checkResultsNumber(
                1, "Неверное количество результатов в списке проблемных заказов при получении статуса заказа"
        );
        SelenideElement orderStatus = $(byXpath(firstResultProblemOrderStatusXpath));
        return ProblemStatus.of(orderStatus.getText());
    }

    @Step("Проверяем количество результатов в списке заказов")
    private void checkResultsNumber(int expectedResultsNumber, String errorMessage) {
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.MEDIUM_WAIT_TIMEOUT, TimeUnit.SECONDS);
        int actualResultsNumber = $$(byXpath(rowProblemOrderResultXpath)).size();
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);

        Assertions.assertEquals(expectedResultsNumber, actualResultsNumber, errorMessage);
    }

}
