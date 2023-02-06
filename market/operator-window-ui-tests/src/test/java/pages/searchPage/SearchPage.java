package ui_tests.src.test.java.pages.searchPage;

import org.openqa.selenium.WebDriver;
import pages.searchPage.basicSearchPage.SearchBar;

public class SearchPage {
    private WebDriver webDriver;

    public SearchPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Строка поиска
     */
    public SearchBar searchBar() {
        return new SearchBar(webDriver);
    }
}
