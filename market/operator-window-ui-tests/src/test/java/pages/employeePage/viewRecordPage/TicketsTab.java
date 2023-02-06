package ui_tests.src.test.java.pages.employeePage.viewRecordPage;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pages.Pages;
import tools.Tools;

import java.util.ArrayList;
import java.util.List;

public class TicketsTab {
    private WebDriver webDriver;

    public TicketsTab(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Получить список заголовков назначенных на пользователя обращений
     *
     * @return
     */
    public List<String> getTitlesOfTicketInWork() {
        Pages.employeePage(webDriver).viewRecordPage().tabs().openTicketsTab();
        Entity.tabs(webDriver).openTab("Назначенные");
        return Entity.entityTable(webDriver).content().getTitlesEntityOnPage();
    }

    /**
     * Получить список заголовков недавно выполненных обращений
     *
     * @return
     */
    public List<String> getTitlesOfRecentlyCompletedTicket() {
        Pages.employeePage(webDriver).viewRecordPage().tabs().openTicketsTab();
        Entity.tabs(webDriver).openTab("Недавно выполненные");
        List<String> titles = new ArrayList<>();
        By linkOfTicketPage = By.xpath("//*[contains(@data-tid,'f96568f')]/a[contains(@href,'ticket')]");
        List<WebElement> webElements = Tools.findElement(webDriver).findElements(linkOfTicketPage);
        int x = webElements.size();
        for (int i = 0; i < x; i++) {
            titles.add(webElements.get(i).getText());
        }
        return titles;
    }
}
