package ui_tests.src.test.java.pages.orderPage;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import tools.Tools;

public class ModalWindowCreateOrderYandexForm {
    private final WebDriver webDriver;

    public ModalWindowCreateOrderYandexForm(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Выбрать ФОС по названию
     */
    public void chooseForm(String formName) {
        String xpath = "//li[text()='"+formName+"']";
        try {
            Tools.findElement(webDriver).findElement(By.xpath("//*[@placeholder='Выберите тип формы']")).click();
            Tools.waitElement(webDriver).waitElementToAppearInDOM(By.xpath(xpath));
            Tools.findElement(webDriver).findElement(By.xpath(xpath)).click();
        } catch (Throwable e) {
            throw new Error("Не удалось выбрать ФОС в модальном окне:\n" + e);
        }
    }

    /**
     * Проверить, что подгрузилось содержимое ФОС
     */
    public void waitFormContent() {
        try {
            Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//div[@class='safe-content']"));
        } catch (Throwable e) {
            throw new Error("В модальном окне не отобразилось содержимое ФОС:\n " + e);
        }
    }

    /**
     * Нажать на кнопку отправки формы
     */
    public void submitButtonClick() {
        try {
            Tools.clickerElement(webDriver).clickElement(By.xpath("//div[@class='survey__submit-button']"));
        } catch (Throwable e) {
            throw new Error("Не удалось нажать на кнопку отправки ФОС \n" + e);
        }
    }

    /**
     * Проверить наличие сообщения от трекера
     */
    public boolean trackerResponcePrecense() {
        for (int i = 0; i < 10; i++) {
            if (Tools.findElement(webDriver).findElements(By.xpath("//div[text()='Трекер']")).size() == 1) {
                return true;
            }
            Tools.waitElement(webDriver).waitTime(1000);
        }
        return false;
    }

    /**
     * Проверить наличие нужной фос в списке
     */
    public boolean findForm(String formName) {
        String xpath = "//li[text()='"+formName+"']";
        Tools.findElement(webDriver).findElement(By.xpath("//*[@placeholder='Выберите тип формы']")).click();
        for (int i = 0; i < 5; i++) {
            if (Tools.findElement(webDriver).findElements(By.xpath(xpath)).size() == 1) {
                return true;
            }
            Tools.waitElement(webDriver).waitTime(1000);
        }
        return false;
    }
}
