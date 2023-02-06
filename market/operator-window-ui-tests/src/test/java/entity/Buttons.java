package ui_tests.src.test.java.entity;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import tools.Tools;
import unit.Config;

public class Buttons {

    private WebDriver webDriver;

    public Buttons(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Нажать на кнопку по локатору
     * @param attribute
     */
    public void clickOnButtonOnLocator(String block ,String attribute){
        String xPath = Entity.properties(webDriver).getXPathElement(attribute);
        Tools.clickerElement(webDriver).clickElement(By.xpath(block + xPath+"//button"));
    }

    /**
     * Нажать на кнопку начинающуюся на <button>
     *
     * @param buttonName
     */
    public void clickButton(String block, String buttonName) {
        try {
            By buttonBy;
            if (buttonName != null) {
                buttonBy = By.xpath(block + "//button//*[text()='" + buttonName.toLowerCase() + "' or text()='" + buttonName.substring(0, 1).toUpperCase() + buttonName.substring(1) + "']");
            } else {
                buttonBy = By.xpath(block + "//button//span[2]");
            }
            Tools.clickerElement(webDriver).clickElement(buttonBy);
        } catch (Throwable t) {
            throw new Error("Не удалось нажать на кнопку \"Button\":\n" + t);
        }
    }

    /**
     * Ждем пока не пропадет элемент начинающийся на <button>
     *
     * @param block
     * @param buttonName
     */
    public void waitInvisibleButton(String block, String buttonName) {
        try {
            Tools.waitElement(webDriver).waitInvisibilityElementTheTime(By.xpath(block + buttonName), Config.DEF_TIME_WAIT_LOAD_PAGE + 5);
        } catch (Throwable throwable) {
            throw new Error("Не дождались когда пропадет кнопка Button \n" + throwable);
        }
    }

    /**
     * Нажать на кнопку начинающуюся на <a>
     *
     * @param titleActionButton
     */
    public void clickNavLinkButton(String block, String titleActionButton) {
        try {
            titleActionButton = titleActionButton.toLowerCase();
            String buttonLocatorBy = "//span[@data-ow-test-jmf-card-toolbar-action='goToEdit']/button";
//            Tools.waitElement(webDriver).waitTime(1000);
            Tools.clickerElement(webDriver).clickElement(By.xpath(block + buttonLocatorBy));
            Tools.waitElement(webDriver).waitInvisibilityElementTheTime(By.xpath(block + buttonLocatorBy), Config.DEF_TIME_WAIT_LOAD_PAGE + 5);
        } catch (Throwable t) {
            throw new Error("Не удалось нажать на кнопку \"NavLinkButton\"\n" + t);
        }
    }

    /**
     * Нажать на кнопку с текстом
     *
     * @param buttonName текст кнопки
     * @return
     */
    public void clickCustomButton(String block, String buttonName) {
        try {
            By button = By.xpath(block + "//button[text()='" + buttonName + "' or @title='" + buttonName + "']");
            Tools.clickerElement(webDriver).clickElement(button);
        } catch (Throwable t) {
            throw new Error("Не удалось нажать на кнопку с текстом  " + buttonName + "\n" + t);
        }
    }

      /**
     * Нажать на кнопку с текстом
     *
     * @param buttonName текст кнопки
     * @return
     */
    public void clickCustomButton(WebElement block, String buttonName) {
        try {
            WebElement button = block.findElement(By.xpath(".//button[text()='" + buttonName + "' or @title='" + buttonName + "']"));
            Tools.clickerElement(webDriver).clickElement(button);
        } catch (Throwable t) {
            throw new Error("Не удалось нажать на кнопку с текстом  " + buttonName + "\n" + t);
        }
    }
}
