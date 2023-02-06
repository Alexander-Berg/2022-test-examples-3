package ui_tests.src.test.java.tools;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public class Scripts {
    private WebDriver webDriver;

    public Scripts(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Проскроллить страницу вниз
     */
    public void scrollToBottom() {
        runScript("window.scrollTo(0, document.body.scrollHeight)");
    }

    /**
     * выполнить скрипт JS
     *
     * @param script скрипт
     */
    public void runScript(String script) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) webDriver;
            js.executeScript(script);
        } catch (Throwable e) {
            throw new Error("Не получилось выполнить скрипт " + script + "\n" + e);
        }
    }

    public void waitForJQueryToEnd(WebDriver webDriver) {
        while ((Boolean) ((JavascriptExecutor) webDriver).executeScript("return jQuery.active!=0")) {
            Tools.waitElement(webDriver).waitTime(2000);
        }
    }
}
