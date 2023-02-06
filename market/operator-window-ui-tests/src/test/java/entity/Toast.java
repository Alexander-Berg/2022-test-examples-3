package ui_tests.src.test.java.entity;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;

import java.util.ArrayList;
import java.util.List;

public class Toast {
    private WebDriver webDriver;

    public Toast(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    @FindBy(xpath = ".//div[@role='alert']")
    private List<WebElement> toast;

    public List<String> getToastMessages() {
        List<String> messages = new ArrayList<>();
        if (toast.size() == 0) return messages;
        for (WebElement webElement : toast) {
            messages.add(webElement.getText());
        }
        return messages;
    }

    // Скрыть тосты с ошибками
    public void hideNotificationError() {
        try {
            Tools.findElement(webDriver).findVisibleElement(By.xpath("//*[contains(@class,\"Toastify__toast-container\")]"));
            Tools.scripts(webDriver).runScript("var elementNotification = document.getElementsByClassName('Toastify__toast-container'); elementNotification.item(0).setAttribute('style','width:0px;');");
        } catch (Throwable t) {

        }
    }

    // Показать тосты
    public void showNotificationError() {
        try {
            Tools.findElement(webDriver).findVisibleElement(By.xpath("//*[contains(@class,\"Toastify__toast-container\")]"));
            Tools.scripts(webDriver).runScript("var elementNotification = document.getElementsByClassName('Toastify__toast-container'); elementNotification.item(0).setAttribute('style','width:500px;');");
        } catch (Throwable t) {

        }
    }
}
