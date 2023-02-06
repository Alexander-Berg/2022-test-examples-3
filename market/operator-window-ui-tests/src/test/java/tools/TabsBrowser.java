package ui_tests.src.test.java.tools;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import unit.Config;

import java.util.Set;

public class TabsBrowser {
    private WebDriver webDriver;

    public TabsBrowser(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Ожидание появления новой вкладки
     * @param numberNewTab номер новой вкладки
     */
    public void waitOpenNewTab(int numberNewTab){
        try {
            new WebDriverWait(webDriver, 3).until(ExpectedConditions.numberOfWindowsToBe(numberNewTab));
        } catch (Throwable e) {
            throw new Error("Не дождались открытия вкладки " + e);
        }
    }

    /**
     * Передать фокус на новую вкладку
     *
     * @param buttonClick на какую кноку нажать для открытия новой вкладки
     */
    public void takeFocusNewTab(By buttonClick) {
        Set<String> handles = webDriver.getWindowHandles();
        Tools.clickerElement(webDriver).clickElement(buttonClick);
        waitOpenNewTab(handles.size() + 1);
        Set<String> allHandles = webDriver.getWindowHandles();
        for (String winHandle : allHandles) {
            if (!handles.contains(winHandle)) {
                webDriver.switchTo().window(winHandle);
            }
        }
    }

    /**
     * Передать фокус на новую вкладку
     *
     * @param buttonClick на какую кнопку нажать для открытия новой вкладки
     */
    public void takeFocusNewTab(WebElement buttonClick) {
        Set<String> handles = webDriver.getWindowHandles();
        Tools.clickerElement(webDriver).clickElement(buttonClick);
        waitOpenNewTab(handles.size() + 1);
        takeFocusNewTab();
    }

    /**
     * Передать фокус на последнею открытую вкладку
     */
    public void takeFocusNewTab() {
        Set<String> handles = webDriver.getWindowHandles();
        for (String winHandle : handles) {
            webDriver.switchTo().window(winHandle);
        }
        Tools.waitElement(webDriver).waitTime(500);
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
    }

    /**
     * Передать фокус на основную вкладку
     */
    public void takeFocusMainTab() {
        Set<String> handles = webDriver.getWindowHandles();
        Object[] h = handles.toArray();
        webDriver.switchTo().window(h[0].toString());
    }

    /**
     * Закрыть текущую вкладку
     */
    public void closeCurrentTab() {
        webDriver.close();
        try {
            Tools.alerts(webDriver).accept();
        } catch (Throwable ignore) {
            // ничего не делаем, алерта и так нет
        }
    }

    /**
     * Открыть ссылку в новой вкладке
     */
    public void openUrlInNewTab(String url) {
        Tools.scripts(webDriver).runScript(String.format("window.open('%s','_blank');", url));
    }

    /**
     * Закрыть все текущие вкладки и открыть новую вкладку приложения
     */
    public void resetBrowserTabsToOneAppTab() {
        Set<String> handles = webDriver.getWindowHandles();
        try {
            webDriver.switchTo().alert().dismiss();
        } catch (NoAlertPresentException e) {
            // ничего не делаем, алерта и так нет
        } catch (NoSuchWindowException ignore){
            webDriver.switchTo().window(handles.toArray()[0].toString());
        }
        openUrlInNewTab(Config.getProjectURL());
        handles = webDriver.getWindowHandles();

        String mainTab = handles.toArray()[handles.size()-1].toString();
            for (String winHandle : handles) {
                if (!winHandle.equals(mainTab)) {
                    try {
                        webDriver.switchTo().window(winHandle);
                        closeCurrentTab();
                    } catch (NoSuchWindowException ignore) {
                    }
                }
            }
        webDriver.switchTo().window(mainTab);
    }
}
