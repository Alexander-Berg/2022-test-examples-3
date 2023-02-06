package ui_tests.src.test.java.entity.entityTable;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;

import java.util.ArrayList;
import java.util.List;

public final class Content {
    private WebDriver webDriver;

    public Content(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
        // Tools.waitElement(webDriver).waitVisibilityElementTheTime(By.xpath("//div[@data-tid='618140a9']/div[@class='_1TjDV9Am']//tbody"), Config.DEF_TIME_WAIT_LOAD_PAGE);
    }

    /**
     * Получить заголовки объектов из таблицы
     *
     * @return
     */
    public List<String> getTitlesEntityOnPage() {
        List<String> titles = new ArrayList<>();
        By linkOfTicketPage = By.xpath("//a[contains(@data-tid,'f96568f')]");
        List<WebElement> webElements = Tools.findElement(webDriver).findElements(linkOfTicketPage);
        int x = webElements.size();
        for (int i = 0; i < x; i++) {
            titles.add(webElements.get(i).getText());
        }
        return titles;
    }

    /**
     * Открыть страницу Объекта
     *
     * @param entityTitle - Тема Объекта
     * @return EntityPage
     */
    public void openEntity(String entityTitle) {
        try {
            Tools.waitElement(webDriver).waitInvisibleLoadingElement();
            By linkOfTicketPage = By.xpath("//a[contains(@data-tid,'f96568f') and text()='" + entityTitle + "']");
            Tools.clickerElement(webDriver).clickElement(linkOfTicketPage);
        } catch (Throwable t) {
            throw new Error("Не удалось открыть страницу с названием " + entityTitle + " \n" + t);
        }
    }

    /**
     * Получить количество ссылок на объекты
     *
     * @return
     */
    public int getLinkCountToEntityOnPage() {
        try {

            By linkOfTicketPage = By.xpath("//a[contains(@data-tid,'f96568f') and text()]");
            List<WebElement> webElements = Tools.findElement(webDriver).findElements(linkOfTicketPage);
            return webElements.size();
        } catch (Throwable t) {
            throw new Error("Не удалось получить количество ссылок на объекты  \n" + t);
        }
    }

    /**
     * Открыть рандомную страницу объекта таблицы
     *
     * @return EntityPage
     */
    public void openRandomEntity() {
        openRandomEntity("");
    }

    /**
     * Открыть рандомную страницу объекта таблицы
     *
     * @return EntityPage
     */
    public void openRandomEntity(String block) {
        By linkOnEntityBy = By.xpath(block + "//a[contains(@data-tid, 'f96568f')]");
        try {
            Tools.waitElement(webDriver).waitInvisibleLoadingElement();
            Tools.waitElement(webDriver).waitClickableElement(linkOnEntityBy);
            List<WebElement> linkOfTicketsPage = Tools.findElement(webDriver).findElements(linkOnEntityBy);
            int randomNumber = Tools.other().getRandomNumber(0, linkOfTicketsPage.size() - 1);

            Tools.clickerElement(webDriver).clickElement(linkOfTicketsPage.get(randomNumber));
        } catch (Throwable t) {
            throw new Error("Не удалось открыть рандомную запись \n" + t);
        }
    }

}
