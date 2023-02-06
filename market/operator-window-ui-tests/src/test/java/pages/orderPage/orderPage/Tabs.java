package ui_tests.src.test.java.pages.orderPage.orderPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class Tabs {
    private final WebDriver webDriver;

    public Tabs(WebDriver webDriver) {
        this.webDriver = webDriver;
    }


    /**
     * Нажать на вкладку Общая информация
     */
    public void clickGeneralInformationTab() {
        try {
            Entity.tabs(webDriver).openTab("Общая информация");
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на вкладку Общая информация\n" + throwable);
        }
    }

    /**
     * Нажать на вкладку "Бонусы"
     */
    public void clickBonusesTab() {
        try {
            Entity.tabs(webDriver).openTab("Бонусы");
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на вкладку 'Бонусы' \n" + throwable);
        }
    }

    /**
     * Нажать на вкладку "История заказа"
     */
    public void clickHistoryTab() {
        try {
            Entity.tabs(webDriver).openTab("История заказа");
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на вкладку 'История заказа' \n" + throwable);
        }
    }
}
