package ui_tests.src.test.java.pages.searchPage.basicSearchPage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import tools.Tools;

public class SearchBar {
    private WebDriver webDriver;

    public SearchBar(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Ввести текст в строку поиска
     */
    public void setTextInSearchBar(String text) {
        try {
            Tools.findElement(webDriver).findElement(By.xpath("//input[contains(@class, 'MuiInputBase')]")).sendKeys(text);
        } catch (Throwable e) {
            throw new Error("Не получилось ввести текст в строку поиска: \n" + e);
        }
    }

    /**
     * Нажать "Найти"
     */
    public void clickSearchButton() {
        try {
            Tools.clickerElement(webDriver).clickElement(By.xpath("//span[text()='Найти']/ancestor::button"));
        } catch (Throwable e) {
            throw new Error("Не получилось нажать на кнопку 'Найти': \n" + e);
        }
    }
}
