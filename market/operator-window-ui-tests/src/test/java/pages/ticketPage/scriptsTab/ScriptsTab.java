package ui_tests.src.test.java.pages.ticketPage.scriptsTab;

import entity.Tabs;
import org.openqa.selenium.WebDriver;

public class ScriptsTab {

    private final WebDriver webDriver;

    public ScriptsTab(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public Script script() {
        return new Script(webDriver);
    }

    public Tabs tabs() {
        return new Tabs(webDriver);
    }

}
