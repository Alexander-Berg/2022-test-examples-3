package ru.yandex.market.hrms.e2etests.pageobjects.hrms;

import java.time.ZonedDateTime;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.hrms.e2etests.tools.DateUtil;

public class OvertimesCreateShiftPopup extends AbstractHrmsPage {

    private static final String createShiftDialogTitleXpath = "//div[@role='dialog']//div[text()='Создание смены']";
    @FindBy(xpath = createShiftDialogTitleXpath)
    private SelenideElement createShiftDialogTitle;

    @FindBy(xpath = "//label[text()='с']/..//input[@type='tel']")
    private SelenideElement fromDateTimeInput;

    @FindBy(xpath = "//label[text()='по']/..//input[@type='tel']")
    private SelenideElement toDateTimeInput;

    @FindBy(xpath = "//label[text()='Причина создания']/..")
    private SelenideElement overtimeReasonDropdown;

    private static final String overtimeReasonsXpathPattern = "//li[@role='option' " +
            "and @data-value='OVERTIME_REASON_PLACEHOLDER']";

    @FindBy(xpath = "//button/span[text()='Готово']")
    private SelenideElement readyButton;

    @Override
    protected void checkPageElements() {
        createShiftDialogTitle.shouldBe(Condition.visible);
    }

    @Override
    protected String urlCheckRegexp() {
        return "hrms/overtimes";
    }

    public enum OvertimeReason {
        PROCESS_CONTRACTS, PROCESS_URGENT_TASKS, PROCESS_URGENT_TICKETS, REPLACE_ABSENT_COLLEAGUE
    }

    @Step("Вводим дату начала смены")
    public OvertimesCreateShiftPopup inputShiftStart(ZonedDateTime dateTime) {
        String dateTimeString = dateTime.format(DateUtil.DEFAULT_DATE_TIME);
        fromDateTimeInput.setValue(dateTimeString);
        return this;
    }

    @Step("Вводим дату конца смены")
    public OvertimesCreateShiftPopup inputShiftEnd(ZonedDateTime dateTime) {
        String dateTimeString = dateTime.format(DateUtil.DEFAULT_DATE_TIME);
        toDateTimeInput.setValue(dateTimeString);
        return this;
    }

    private SelenideElement findDropdownOptionByReason(OvertimeReason reason) {
        String reasonXpath = overtimeReasonsXpathPattern
                .replaceAll("OVERTIME_REASON_PLACEHOLDER", reason.toString());
        return Selenide.$(By.xpath(reasonXpath));
    }

    @Step("Выбираем причину создания")
    public OvertimesCreateShiftPopup selectOvertimeReason(OvertimeReason reason) {
        overtimeReasonDropdown.click();
        findDropdownOptionByReason(reason).click();
        return this;
    }

    @Step("Жмем Готово")
    public OvertimesPage clickReadyButton() {
        readyButton.click();
        waitElementHidden(By.xpath(createShiftDialogTitleXpath));
        return Selenide.page(OvertimesPage.class);
    }
}
