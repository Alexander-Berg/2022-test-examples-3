package ui_tests.src.test.java.pages.orderPage.orderPage;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.ArrayList;
import java.util.List;

public class MainProperties {
    private final WebDriver webDriver;

    public MainProperties(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    @FindBy(xpath = "//*[text()='маркеры:']/*/*/*")
    List<WebElement> markersWebElements;

    /**
     * Получить маркеры заказа
     *
     * @return
     */
    public List<String> getMarkers() {
        List<String> listMarkersFromPage = new ArrayList<>();

        try {
            for (WebElement webElement : markersWebElements) {
                listMarkersFromPage.add(webElement.getText());
            }
        } finally {

            return listMarkersFromPage;
        }
    }
}
