package ui_tests.src.test.java.entity;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import pages.Pages;
import tools.Tools;

import java.util.ArrayList;
import java.util.List;

public final class Tabs extends Entity {
    private WebDriver webDriver;

    private String xpathTabs = "//*[contains(@data-tid,'a4ed0d05')]//*[@*[starts-with(name(.),'data-ow-test')]='tabsWrapper']";

    public Tabs(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    public Tabs(WebDriver webDriver, String xpathTabs) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
        this.xpathTabs = xpathTabs + this.xpathTabs;
    }


    /**
     * Получить список названий вкладок
     *
     * @return список названий вкладок
     */
    public List<String> getListTabsName() {
        List<String> tabsName = new ArrayList<>();
        try {
            List<WebElement> tabs = Tools.findElement(webDriver).findElements(By.xpath(xpathTabs + "//button"));

            for (WebElement element : tabs) {
                tabsName.add(element.getText());
            }
        } catch (Throwable ignored) {

        }
        return tabsName;
    }

    /**
     * Открыть вкладку с именем {tabName}
     *
     * @param tabName Имя вкладки
     */
    public void openTab(String tabName) {
        try {
            Pages.ticketPage(webDriver).toast().hideNotificationError();
            Entity.buttons(webDriver).clickCustomButton(xpathTabs, tabName);
        } catch (Throwable t) {
            try {
                Entity.buttons(webDriver).clickCustomButton("//*[contains(@data-tid,'a4ed0d05')]", tabName);
            } catch (Throwable throwable) {
                throw new Error("Не удалось открыть вкладку " + tabName + ":\n" + t);
            }
        }
    }

    /**
     * Возвращает название активной вкладки
     *
     * @return
     */
    public String whichTabActive() {
        int left = 0;
        Tools.waitElement(webDriver).waitTime(1500);

        try {
            Tools.waitElement(webDriver).waitVisibilityElement(By.xpath(xpathTabs + "//button"));
            WebElement highlighting = Tools.findElement(webDriver).findVisibleElement(By.xpath(xpathTabs)).findElement(By.xpath("./..//div[@class='_0XvbkZG']"));
            String style = highlighting.getAttribute("style");

            List<WebElement> buttons = Tools.findElement(webDriver).findVisibleElement(By.xpath(xpathTabs)).findElements(By.xpath(".//button"));
            for (WebElement button : buttons) {
                if (style.contains("left: " + left + "px;")) {
                    return button.getText();
                } else {
                    left += button.getSize().width;
                }
            }
            return "";
        } catch (Throwable t) {
            throw new Error("Не удалось получить название активной вкладки\n" + t);
        }
    }
}
