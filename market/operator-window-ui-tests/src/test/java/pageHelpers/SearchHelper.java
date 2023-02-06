package ui_tests.src.test.java.pageHelpers;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pages.Pages;
import tools.Tools;

import java.util.ArrayList;
import java.util.List;

public class SearchHelper {

    private WebDriver webDriver;

    public SearchHelper(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Ввести текст в поисковую строку и нажать "Найти"
     */
    public void useBasicSearch(String text) {
        Pages.searchPage(webDriver).searchBar().setTextInSearchBar(text);
        Pages.searchPage(webDriver).searchBar().clickSearchButton();
    }

    /**
     * Получить результаты со страницы
     */
    public List<String> getResults() {
        List<String> res = new ArrayList<>();
        // Если отображается надпись "Ничего не найдено" - вернуть пустой список, т.е. результатов нет
        if (Tools.findElement(webDriver).findVisibleElement(By.xpath("//div[text()='Ничего не найдено']")).isDisplayed()) {
            return res;
        }

        // Если отобразились результаты - вернуть их
            // Тут описать логику получения результатов. String в результате заменить на класс результатов поиска.
        throw new Error("В результатах была не пустая выдача и не блоки с результатами.");
    }
}
