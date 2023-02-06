package ui_tests.src.test.java.entity;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;


public final class Header extends Entity {
    private WebDriver webDriver;

    public Header(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(webDriver, this);
    }


    public String block = "//div[@class=\"jmf-card-toolbar\"]/../..";

    @FindBy(xpath = "//div[@data-tid='10ec7ed2']/div")
    private WebElement subjectTicket;

    /**
     * Нажать на кнопку изменения страницы (перейти на страницу редактирования или сохранить страницу)
     *
     * @param titleActionButton
     */
    public void clickButtonEditForm(String titleActionButton) {
        try {
            Entity.buttons(webDriver).clickButton(block, titleActionButton);
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на кнопку изменения/сохранения страницы\n" + throwable);
        }
    }

    /**
     * Нажать на кнопку сохранения страницы (перейти на страницу редактирования или сохранить страницу)
     *
     * @param titleActionButton
     */
    public void clickButtonSaveForm(String titleActionButton) {
        try {
            Entity.buttons(webDriver).clickButton(block, titleActionButton);
            Entity.buttons(webDriver).waitInvisibleButton(block, titleActionButton);
            // Tools.waitElement(webDriver).waitTime(2000);
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на кнопку изменения/сохранения страницы\n" + throwable);
        }
    }


    /**
     * Получить заголовок Entity
     *
     * @return
     */
    public String getSubject(String attributeValue) {
        try {
            String xpath = Entity.properties(webDriver).getXPathElement(attributeValue)+"//*[text()][1]";
            Tools.waitElement(webDriver).waitVisibilityElement(By.xpath(xpath));
            return Tools.findElement(webDriver).findVisibleElement(By.xpath(xpath)).getText().split("\"")[1];
        } catch (Throwable t) {
            throw new Error("НЕ удалось получить заголовок сущьности:\n" + t);
        }
    }

    /**
     * Нажать на кнопку
     *
     * @param buttonName
     */
    public void clickButtonEditStatus(String buttonName) {
        Entity.buttons(webDriver).clickButton(block, buttonName);
    }


}
