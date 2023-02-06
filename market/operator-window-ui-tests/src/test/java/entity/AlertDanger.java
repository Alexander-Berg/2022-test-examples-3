package ui_tests.src.test.java.entity;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.ArrayList;
import java.util.List;

public class AlertDanger {

    private WebDriver webDriver;

    public AlertDanger(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    @FindBy(xpath = "(//*[@data-ow-test-global-request-error]//*[text()])[1]")
    private List<WebElement> alertDanger;

    public List<String> getAlertDangerMessages() {
        List<String> messages = new ArrayList<>();
        if (alertDanger.size() == 0) return messages;
        for (WebElement webElement : alertDanger) {
            messages.add(webElement.getText());
        }
        return messages;
    }
}
