package tools;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class Field {

    public static void clearAndFillByXpath(String newText, String xPath, WebDriver driver) {
        WebElement element = Elements.getElementByXpath(xPath, driver);
        String currentText = element.getText();
        //WebElement.clear() не везде срабатывает
        for (int i = 0; i < currentText.length(); i++) {
            element.sendKeys(Keys.BACK_SPACE);
        }
        element.sendKeys(newText);
    }
}
