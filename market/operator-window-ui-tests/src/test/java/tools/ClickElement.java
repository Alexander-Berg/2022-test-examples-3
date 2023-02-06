package ui_tests.src.test.java.tools;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ClickElement {

    private WebDriver webDriver;

    public ClickElement(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * клик по элементу
     *
     * @param byTag локатор элемента на который нужно нажать
     */
    public void clickElement(By byTag) {
        try {
            WebElement webElement = Tools.waitElement(webDriver).waitClickableElement(byTag);
            webElement.click();
            Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        } catch (Throwable e) {
            throw new Error("Не получилось нажать на элемент страницы" + "\n" + e);
        }
    }

    /**
     * клик по элементу
     *
     * @param webElement локатор элемента на который нужно нажать
     */
    public void clickElement(WebElement webElement) {
        try {
            Tools.waitElement(webDriver).waitClickableElement(webElement);
            webElement.click();
            Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        } catch (Throwable e) {
            throw new Error("Не получилось нажать на элемент страницы" + "\n" + e);
        }
    }

}
