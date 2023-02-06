package helpers;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

public class CloseNotification {

    private static final String notificationCloseButton = "//div[@class='slds-notification__body']/button";
    private static final String notificationsClearAllButton = "//div[contains(@class, 'clearAll')]";


    // Закрывает все уведомления, сперва ищет кнопку "Закрыть все", если её не находит получает отдельные уведомления и если их число > 0 закрывает их отдельно
    public static void clearAllNotification(WebDriver driver) {

        try {
            new WebDriverWait(driver, 10)
                    .ignoring(NoSuchElementException.class)
                    .until(ExpectedConditions.elementToBeClickable(By.xpath(notificationsClearAllButton))).click();
        } catch (TimeoutException e) {

            List<WebElement> notifications = driver.findElements(By.xpath(notificationCloseButton));
            if (notifications.size() > 0) {
                for (WebElement notification : notifications) {
                    notification.click();
                }
            }
        }
    }
}
