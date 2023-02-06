package ui_tests.src.test.java.pages.orderPage;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import tools.Tools;

public class ModalWindowGiveCoupon {
    private final WebDriver webDriver;

    public ModalWindowGiveCoupon(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    private final By discountAmountInput = By.xpath("//input[contains(@class, 'MuiOutlinedInput-input MuiInputBase-input MuiInputBase-inputAdornedEnd')]");

    /**
     * Выбрать причину начисления
     */
    public void setReason(String reason) {
        try {
            /* Раскоммениторовать 16 строку и удалить все строки Tools. в методе после
            решения тикета https://st.yandex-team.ru/OCRM-6091
            Entity.modalWindow(webDriver).content().setPropertiesOfSelectField("reason", reason);
             */
            String xpath = Entity.properties(webDriver).getXPathElement(reason);
            Tools.findElement(webDriver).findElement(
                    By.xpath("//span[text()='Выберите причину начисления']")).click();
            Tools.waitElement(webDriver).waitElementToAppearInDOM(By.xpath(xpath));
            Tools.findElement(webDriver).findElement(By.xpath(xpath+"/..")).click();

        } catch (Throwable e) {
            throw new Error("Не удалось выбрать причину выдачи купона в модальном окне:\n" + e);
        }
    }

    /**
     * Установить значение в поле "Сумма начисления"
     */
    public void setCouponDiscountAmount(String discountAmountText, String expectedDiscountAmount) {
        try {
            // Очистить поле (.clear не срабатывает)
            Tools.findElement(webDriver).findElement(discountAmountInput).sendKeys(Keys.chord(Keys.CONTROL,"a", Keys.DELETE));
            // Ввести значение
            Tools.findElement(webDriver).findElement(discountAmountInput).sendKeys(discountAmountText);
            Tools.waitElement(webDriver).waitInvisibleLoadingElement();
            // Выбрать предложенный вариант
            Tools.clickerElement(webDriver).clickElement(By.xpath("//*[@id='ow-popper-portal']//*[ text()='" + expectedDiscountAmount + "']"));
        }catch (Throwable e) {
            throw new Error("Не удалось указать сумму начисления в модальном окне:\n" + e);
        }
    }

    /**
     * Получить значение из поля "Сумма начисления"
     */
    public String getCouponDiscountAmount() {
        try {
            // Переписать под вызов через Entity после https://st.yandex-team.ru/OCRM-6091
            return Tools.findElement(webDriver).findElement(discountAmountInput).getAttribute("value");
        } catch (Throwable e) {
            throw new Error("Не удалось получить сумму начисления в модальном окне:\n" + e);
        }
    }

    /**
     * Нажать "Сохранить"
     */
    public void saveButtonClick() {
        Entity.modalWindow(webDriver).controls().clickButton("Сохранить");
    }
}
