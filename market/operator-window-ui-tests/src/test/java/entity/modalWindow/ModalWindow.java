package ui_tests.src.test.java.entity.modalWindow;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class ModalWindow {
    private WebDriver webDriver;

    public ModalWindow(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    public Controls controls() {
        return new Controls(webDriver);
    }


    public Content content() {
        return new Content(webDriver);
    }


}
