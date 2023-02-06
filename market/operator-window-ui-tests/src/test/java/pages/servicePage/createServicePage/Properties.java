package ui_tests.src.test.java.pages.servicePage.createServicePage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;

public class Properties {
    private WebDriver webDriver;

    public Properties(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    /**
     * Нажать на селект по data-ow-test-attribute-container
     */
    public void clickSelectTypeField(String attributeCode) {
        Tools.clickerElement(webDriver).clickElement(By.xpath(String.format(
                "//*[@*[starts-with(name(.),'data-ow-test')]='%s']//button", attributeCode)));
    }
}
