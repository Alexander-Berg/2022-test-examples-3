package ui_tests.src.test.java.pages.orderPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class ModalWindowCancelOrder {
    private final WebDriver webDriver;

    public ModalWindowCancelOrder(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Выбрать причину отмены заказа
     */
    public void setCancellationReason(String cancellationReason) {
        try {

            Entity.modalWindow(webDriver).content().setPropertiesOfSelectField("reason", cancellationReason);

//            Tools.findElement(webDriver).findElement(By.xpath("//*[@data-ow-test-cancel-order-modal='reason']//button")).click();
//            Tools.waitElement(webDriver).waitElementToAppearInDOM(By.xpath(String.format("//div[text()='%s']", cancellationReason)));
//            Tools.findElement(webDriver).findElement(By.xpath(String.format("//div[text()='%s']", cancellationReason))).click();
        } catch (Throwable e) {
            throw new Error("Не удалось выбрать причину отмены в модальном окне:\n" + e);
        }
    }

    /**
     * Ввести комментарий
     */
    public void setComment(String commentText) {
        Entity.modalWindow(webDriver).content().setPropertiesOfTextArea("comment", commentText);

//        Tools.findElement(webDriver).findElement(By.xpath("//*[@data-ow-test-cancel-order-modal='comment']//textarea"))
//                .sendKeys(commentText);
    }

    /**
     * Нажать "Отменить"
     */
    public void cancelButtonClick() {
        Entity.modalWindow(webDriver).controls().clickButton("Отменить");
    }
}
