package ui_tests.src.test.java.pages.orderPage;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pages.Pages;
import tools.Tools;

import javax.lang.model.element.Element;
import java.util.ArrayList;
import java.util.List;

public class Header {
    private final WebDriver webDriver;

    public Header(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Получить маркеры заказа
     *
     * @return
     */
    public List<String> getMarkers() {
        List<String> listMarkersFromPage = new ArrayList<>();
        List<WebElement> webElements = Tools.findElement(webDriver).findElements(By.xpath(Entity.header(webDriver).block + "//*[text()='маркеры:']/*/*/*"));

        try {
            for (WebElement webElement : webElements) {
                listMarkersFromPage.add(webElement.getText());
            }
        } finally {

            return listMarkersFromPage;
        }
    }

    /**
     * Получить статус заказа
     *
     * @return
     */
    public String getOrderStatus() {
        return Entity.properties(webDriver).getValueField(Entity.header(webDriver).block, "status").replace(":", "");
    }


    /**
     * Получить номер заказа
     */
    public String getOrderNumber() {
        try {
            return Tools.findElement(webDriver).findElement(By.xpath("//span[contains(text(),'№')]")).getText().substring(1);
        } catch (Exception e) {
            throw new Error("Не удалось получить номер заказа из шапки на странице заказа:\n" + e);
        }
    }

    /**
     * Получить тип маркета из шапки на странице заказа
     */
    public String getTypeMarket() {
        try {
            // Дождаться загрузки шапки
            Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//span[text()='Заказ']"));
            return Tools.findElement(webDriver).findElement(By.xpath("//span[contains(text(),'Заказ')]/../../div[2]")).getText();
        } catch (Exception e) {
            throw new Error("Не удалось получить тип маркета из шапки на странице заказа:\n" + e);
        }
    }

    /**
     * Получить тип маркета из шапки на странице заказа
     */
    public String getDateCreate() {
        try {
            return Tools.findElement(webDriver).findElement(By.xpath("//div[contains(text(),'от ')]")).getText();
        } catch (Exception e) {
            throw new Error("Не удалось получить тип маркета из шапки на странице заказа:\n" + e);
        }
    }

    /**
     * Нажать на кнопку "Подтвердить заказ"
     */
    public void clickConfirmOrderButton() {

        try {
            Entity.buttons(webDriver).clickButton(Entity.header(webDriver).block, "Подтвердить");
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на кнопку подтверждения заказа\n" + throwable);
        }
    }

    /**
     * Нажать на кнопку "Отменить"
     */
    public void clickCancelOrderButton() {

        try {
            Entity.buttons(webDriver).clickButton(Entity.header(webDriver).block, "Отменить");
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на кнопку отмены заказа\n" + throwable);
        }
    }

    /**
     * Нажать на кнопку "Выдать купон"
     */
    public void clickGiveCouponButton() {
        try {
            Pages.ticketPage(webDriver).toast().hideNotificationError();
            Entity.buttons(webDriver).clickButton(Entity.header(webDriver).block, "Выдать купон");
        } catch (Throwable e) {
            throw new Error("Не удалось открыть форму выдачи купона \n" + e);
        }
    }

    /**
     * Нажать на кнопку "Создать задачу в СТ"
     */
    public void clickCreateSTTicketButton() {
        try {
            Entity.buttons(webDriver).clickButton(Entity.header(webDriver).block, "Создать задачу в СТ");
            Tools.waitElement(webDriver).waitVisibilityElementTheTime
                    (By.xpath("//div[text()='Создание задачи в стартрек']"), 10);
        } catch (Throwable e) {
            throw new Error("Не удалось открыть форму создания задачи в СТ \n" + e);
        }
    }

    /**
     * Получить название платформы с которой был создан заказ
     *
     * @return
     */
    public String getNameOfDistributionPlatform() {
        try {
            WebElement name = Tools.findElement(webDriver).findVisibleElement(By.xpath(Entity.header(webDriver).block + "//*[name()='svg']"));
            return name.findElement(By.xpath(".//*[name()='title']")).getAttribute("innerHTML");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить название платформы с которой был создан заказ\n" + throwable);
        }
    }

    /**
     * Получить значние атрибута href у кнопки "показать заказ в AБО"
     */
    public String getABOButtonHref() {
        try {
            return Tools.findElement(webDriver).findElement(By.xpath("//a[text()='показать заказ в AБО']")).getAttribute("href");
        } catch (Throwable e) {
            throw new Error("Не удалось получить атрибута href у кнопки 'показать заказ в AБО' \n" + e);
        }
    }

    /**
     * Нажать на кнопку "показать заказ в АБО"
     */
    public void clickABOButton() {
        try {
            Tools.tabsBrowser(webDriver).takeFocusNewTab(By.xpath("//a[text()='показать заказ в AБО']"));
        } catch (Throwable e) {
            throw new Error("Не удалось нажать на кнопку 'показать заказ в AБО' \n" + e);
        }
    }

    /**
     * Получить сабстатус заказа
     *
     * @return
     */
    public String getSubStatus() {
        try {
            return Entity.properties(webDriver).getValueField("substatus");
        } catch (Throwable e) {
            throw new Error("Не удалось получить сабстатус заказа' \n" + e);
        }
    }
}
