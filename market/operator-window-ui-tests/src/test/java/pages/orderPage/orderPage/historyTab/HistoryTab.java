package ui_tests.src.test.java.pages.orderPage.orderPage.historyTab;

import Classes.order.History;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import tools.Tools;

import java.util.ArrayList;
import java.util.List;

public class HistoryTab {
    private final WebDriver webDriver;

    public HistoryTab(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Получить все записи из таблицы событий
     *
     * @return
     */
    public List<History> getAllHistory() {
        List<History> historiesFromPage = new ArrayList<>();
        By xPathTableRows = By.xpath("//table/tbody/tr");
        Tools.waitElement(webDriver).waitVisibilityElement(xPathTableRows);
        List<WebElement> rows = Tools.findElement(webDriver).findElements(xPathTableRows);

        for (WebElement row : rows) {
            History history = new History();
            history
                    .setTypeEntity(row.findElement(By.xpath("./td[3]")).getText())
                    .setAuthor(row.findElement(By.xpath("./td[1]")).getText())
                    .setDate(row.findElement(By.xpath("./td[2]")).getText())
                    .setStatusAfter(row.findElement(By.xpath("./td[4]")).getText());
            historiesFromPage.add(history);
        }
        return historiesFromPage;
    }
}
