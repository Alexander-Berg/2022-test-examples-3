package pageHelpers;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import pages.EditPage;
import pages.LeadPage;

import static tools.Date.getCurrentDateTime;
import static tools.Elements.getElementByLabel;
import static tools.Elements.getElementByXpath;

public class LeadPageHelper {

    public static void createNewLead(WebDriver driver) throws InterruptedException {
        //Генерируем имя компании
        String companyName = "Company " + getCurrentDateTime();
        //Генерируем фамилию для лида
        String leadName = "Lead " + getCurrentDateTime();

        driver.get(LeadPage.createPage);
        getElementByXpath(LeadPage.editCompanyField, driver).sendKeys(companyName);
        Thread.sleep(3000);
        getElementByXpath(LeadPage.editLastNameField, driver).sendKeys(leadName);
        Thread.sleep(3000);
        getElementByXpath(LeadPage.editEmailField, driver).sendKeys("test@yandex.ru");
        Thread.sleep(3000);
        getElementByXpath(EditPage.saveButtonXpath, driver).click();
        Thread.sleep(5000);
        Assertions.assertEquals(companyName, getElementByLabel("Компания",
                        driver).getText(),
                "Что-то пошло не так при создании лида");

    }

    public static String getCurrentStatusOnStatusBar(WebDriver driver) {
        return getElementByXpath(LeadPage.currentStatusOnStatusBar, driver).getText();
    }

    public static String getCurrentStatusFromCard(WebDriver driver) {
        return getElementByLabel("Статус интереса", driver).getText();
    }
}
