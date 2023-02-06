package ui_tests.src.test.java.pages.orderPage.customer;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;

import java.util.ArrayList;
import java.util.List;

public class MainProperties {
    private final WebDriver webDriver;

    public MainProperties(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    @FindBy(xpath = "//a[contains(@href,'customer/')]")
    private WebElement nameCustomer;

    @FindBy(xpath = "//*[text()='Emails']/../div[2]")
    private WebElement emailCustomer;

    @FindBy(xpath = "//*[text()='Телефон']/../div[1]")
    private WebElement phoneCustomer;

    @FindBy(xpath = "//*[contains(text(),'статус аккаунта')]/following-sibling::*[1]")
    private WebElement statusAuthorisation;

    /**
     * Нажать на номер телефона клиента
     */
    public void clickPhoneNumberButton(){
        Tools.clickerElement(webDriver).clickElement(By.xpath(Entity.properties(webDriver).getXPathElement("secondaryInfoPhone")+"//button"));
    }

    /**
     * Получить значение поля Статус аккаунта
     *
     * @return
     */
    public String getStatusAuthorisation() {
        try {
            Tools.waitElement(webDriver).waitVisibilityElement(statusAuthorisation);
            return statusAuthorisation.getText().trim();
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить статус авторизации \n" + throwable);
        }
    }

    /**
     * Получить ФИО клиента
     */
    public String getFullName() {
        try {
            Tools.waitElement(webDriver).waitVisibilityElement(nameCustomer);
            return nameCustomer.getText();
        } catch (Exception e) {
            throw new Error("Не удалось получить ФИО клиента \n" + e);
        }
    }

    /**
     * Получить маркеры Клиента
     *
     * @return
     */
    public List<String> getMarkers() {
        List<String> listMarkersFromPage = new ArrayList<>();
        List<WebElement> markersWebElements = Tools.findElement(webDriver).findElementsWithAnExpectationToAppearInDOM(By.xpath(Entity.properties(webDriver).getXPathElement("nameAndLogin")+"/following-sibling::*[1]//*[text()]"));

        try {
            for (WebElement webElement : markersWebElements) {
                listMarkersFromPage.add(webElement.getText());
            }
        } finally {

            return listMarkersFromPage;
        }
    }

    /**
     * Получить Email клиента
     *
     * @return
     */
    public String getEmailCustomer() {
        try {
            return Entity.properties(webDriver).getValueField("secondaryInfoEmails");
        } catch (Throwable t) {
            throw new Error("Не удалось получить Email клиента \n" + t);
        }
    }

    /**
     * Получить Телефон клиента
     *
     * @return
     */
    public String getPhoneCustomer() {
        try {
            return phoneCustomer.getText();
        } catch (Throwable t) {
            throw new Error("Не удалось получить Телефон клиента \n" + t);
        }
    }
}
