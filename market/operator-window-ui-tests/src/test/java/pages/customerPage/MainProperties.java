package ui_tests.src.test.java.pages.customerPage;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;

public class MainProperties {
    private final WebDriver webDriver;

    public MainProperties(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    @FindBy(xpath = ".//*[@*[starts-with(name(.),'data-ow-test')]='nameAndLogin']")
    private WebElement customerFullName;

    @FindBy(xpath = ".//*[@*[starts-with(name(.),'data-ow-test')]='secondaryInfoEmails']")
    private WebElement customerEmail;

    @FindBy(xpath = ".//*[@*[starts-with(name(.),'data-ow-test')]='secondaryInfoPhone']")
    private WebElement customerPhone;

    @FindBy(xpath = ".//*[@*[starts-with(name(.),'data-ow-test')]='secondaryInfoRegistrationDate']")
    private WebElement customerRegistrationDate;

    @FindBy(xpath = ".//*[@*[starts-with(name(.),'data-ow-test')]='uid']")
    private WebElement customerUID;

    @FindBy(xpath = ".//*[@*[starts-with(name(.),'data-ow-test')]='secondaryCashbackBalance']")
    private WebElement customerCashback;


    /**
     * Получить ФИО и логин клиента
     */
    public String getCustomerFullName() {
        try {
            Tools.waitElement(webDriver).waitVisibilityElement(customerFullName);
            return customerFullName.getText();
        } catch (Exception e) {
            throw new Error("Не удалось получить ФИО и логин клиента \n" + e);
        }
    }

    /**
     * Получить Email клиента
     *
     * @return
     */
    public String getCustomerEmail() {
        try {
            return customerEmail.getText().trim();
        } catch (Throwable t) {
            throw new Error("Не удалось получить Email клиента \n" + t);
        }
    }

    /**
     * Получить Телефон клиента
     *
     * @return
     */
    public String getCustomerPhone() {
        try {
            return customerPhone.getText().trim();
        } catch (Throwable t) {
            throw new Error("Не удалось получить Телефон клиента \n" + t);
        }
    }

    /**
     * Получить дату регистрации клиента
     */
    public String getCustomerRegistrationDate() {
        try {
            return customerRegistrationDate.getText();
        } catch (Throwable t) {
            throw new Error("Не удалось получить дату регистрации клиента \n" + t);
        }
    }

    /**
     * Получить UID клиента
     */
    public String getCustomerUID() {
        try {
            return customerUID.getText();
        } catch (Throwable t) {
            throw new Error("Не удалось получить UID клиента \n" + t);
        }
    }

    /**
     * Получить кэшбек клиента
     */
    public String getCustomerCashback() {
        try {
            return customerCashback.getText();
        } catch (Throwable t) {
            throw new Error("Не удалось получить информацию о кэшбеке клиента \n" + t);
        }
    }
}
