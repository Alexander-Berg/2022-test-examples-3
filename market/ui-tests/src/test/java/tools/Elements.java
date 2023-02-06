package tools;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Elements {

    private static final String activeTabXpath = "//div[contains(@class,'oneWorkspaceTabWrapper')]//div[contains(@class,'active')]";
    private static final String modalWindowXpath = "//div[contains(@class,'modal-body')]";

    public static WebElement getElementByXpath(String xPath, WebDriver driver) {
        WebElement element = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(activeTabXpath + xPath)));
        return element;
    }

    public static WebElement getElementInModalWindowByXpath(String xPath, WebDriver driver) {
        WebElement element = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(modalWindowXpath + xPath)));
        return element;
    }

    public static WebElement getElementById(String id, WebDriver driver) {
        WebElement element = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));
        return element;
    }

    //label и текст имеют разный путь, поэтому сперва поднимаемся от label вверх, а потом идем по верному пути вниз
    public static WebElement getElementByLabel(String label, WebDriver driver) {
        String xPath = activeTabXpath + "//*[@class='test-id__field-label'][contains(text(),'" + label + "')]/../../div[2]/*[1]";
        WebElement element = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xPath)));
        return element;
    }


    public static void clickElementByXpath(String xPath, WebDriver driver) {
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.elementToBeClickable(By.xpath(activeTabXpath + xPath))).click();
    }

    public static void clickElementByXpathFromAllPages(String xPath, WebDriver driver) {
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.elementToBeClickable(By.xpath(xPath))).click();
    }

    public static void clickLinkByTitle(String title, WebDriver driver) {
        String xPath = activeTabXpath + "//a[@title='" + title + "']";
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.elementToBeClickable(By.xpath(xPath))).click();
    }

    public static void clickElementInModalWindowByTitle(String title, WebDriver driver) {
        String xPath = modalWindowXpath + "//*[@title='" + title + "']";
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.elementToBeClickable(By.xpath(xPath))).click();
    }

    public static void clickElementInModalWindowByXpath(String xPath, WebDriver driver) {
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.elementToBeClickable(By.xpath(modalWindowXpath + xPath))).click();
    }

    // Используется если основной клик попадает не туда
    public static void moveToElementAndClickByXpath(String xPath, WebDriver driver) {
        WebElement element = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.elementToBeClickable(By.xpath(activeTabXpath + xPath)));
        Action.moveToElementAndClick(element, driver);
    }

    public static void moveToLinkAndClickByTitle(String title, WebDriver driver) {
        String xPath = activeTabXpath + "//a[@title='" + title + "']";
        WebElement element = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.elementToBeClickable(By.xpath(xPath)));
        Action.moveToElementAndClick(element, driver);
    }

    public static void moveToElementAndClickByXpathFromAllPages(String xPath, WebDriver driver) {
        WebElement element = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.elementToBeClickable(By.xpath(xPath)));
        Action.moveToElementAndClick(element, driver);
    }


    public static void selectFromPickList(String xPathPickList, String xPathElement, WebDriver driver) {
        clickElementByXpath(xPathPickList, driver);
        clickElementByXpathFromAllPages(xPathElement, driver);
    }

    public static void moveAndSelectFromPickList(String xPathPickList, String xPathElement, WebDriver driver) {
        moveToElementAndClickByXpath(xPathPickList, driver);
        moveToElementAndClickByXpathFromAllPages(xPathElement, driver);
    }


}
