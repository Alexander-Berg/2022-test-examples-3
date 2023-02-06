package ui_tests.src.test.java.tools;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SendElement {
    private WebDriver webDriver;

    public SendElement(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * ввести данные в поле
     *
     * @param webElement элемент куда нужно ввести текст
     * @param text       текст который нужно ввести
     */
    public void sendElement(WebElement webElement, CharSequence... text) {
        try {
            Tools.waitElement(webDriver).waitClickableElement(webElement);
            webElement.clear();
            webElement.sendKeys(text);
        } catch (Throwable e) {
            throw new Error("Не получилось ввести данные в поле " + "\n" + e);
        }
    }

    /**
     * ввести данные в поле
     *
     * @param byElement элемент куда нужно ввести текст
     * @param text      текст который нужно ввести
     */
    public void sendElement(By byElement, String text) {
        try {
            WebElement elem = Tools.waitElement(webDriver).waitClickableElement(byElement);
            int ii = elem.getAttribute("value").length();
            for (int i = 0; i < ii; i++) {
                elem.sendKeys(Keys.BACK_SPACE);
            }
            elem.clear();
            elem.sendKeys(text);
        } catch (Throwable e) {
            throw new Error("Не получилось ввести данные в поле " + "\n" + e);
        }
    }
}
