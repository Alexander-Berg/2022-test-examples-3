package ui_tests.src.test.java.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import tools.Tools;
import unit.Config;

public class SecondScreenPage {

    private WebDriver webDriver;

    public SecondScreenPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Проверить наличие надписи "Нет связанных данных" на втором экране.
     */
    public boolean checkRelatedData() {
        try {
            Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//div[@class='main-window']"));
        } catch (Throwable e) {
            throw new Error("Не дождались загрузки второго экрана");
        }
        try {
            Tools.waitElement(webDriver).waitInvisibilityElementTheTime(By.xpath("//span[text()='Нет связанных данных']"), Config.DEF_TIME_WAIT_LOAD_PAGE);
        }catch (Error error){
         if (error.getMessage().contains("не пропал со страницы за")){
             return false;
         }else {
             throw new Error(error);
         }
        }
        return true;
    }

    /**
     * Дождаться отображения заказа на втором мониторе
     */
    public boolean waitForOrder() {
        // Проверять наличие заказа раз в секунду
        for (int i = 0; i < 10; i++) {
            if (checkRelatedData()) {
                return true;
            } else {
                Tools.waitElement(webDriver).waitTime(1000);
            }
        }
        return false;
    }
}
