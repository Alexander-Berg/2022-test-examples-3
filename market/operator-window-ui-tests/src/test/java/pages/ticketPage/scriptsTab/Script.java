package ui_tests.src.test.java.pages.ticketPage.scriptsTab;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;
import unit.Config;

public class Script {
    private final WebDriver webDriver;

    public Script(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
        Tools.waitElement(webDriver).waitVisibilityElementTheTime(By.xpath("//div[@data-tid=\"7ef8b06d\"]"), Config.DEF_TIME_WAIT_LOAD_PAGE);
    }

    @FindBy(xpath = "//div[@data-tid='7ef8b06d']/iframe")
    private WebElement iframeElement;

    /**
     * Получить URL отображаемого скрипта
     *
     * @return
     */
    public String getURLScript() {
        return iframeElement.getAttribute("src");
    }

    /**
     * Получить ширину блока со скриптом
     *
     * @return
     */
    public int getWidthOfTagScript() {
        return iframeElement.getSize().width;
    }

    /**
     * Получить высоту блока со скриптом
     *
     * @return
     */
    public int getHeightOfTagScript() {
        return iframeElement.getSize().height;
    }
}
