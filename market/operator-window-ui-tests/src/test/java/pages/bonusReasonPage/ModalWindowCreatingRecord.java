package ui_tests.src.test.java.pages.bonusReasonPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class ModalWindowCreatingRecord {

    private WebDriver webDriver;

    public ModalWindowCreatingRecord(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Нажать на кнопку Сохранить
     */
    public void clickSaveRecordButton() {
        try {
            Entity.modalWindow(webDriver).controls().clickButton("Добавить");
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на кнопку Сохранить\n" + throwable);
        }
    }

    /**
     * Указать Причину
     *
     * @param title Причину
     */
    public ModalWindowCreatingRecord setTitle(String title) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesInputField("title", title);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля название\n" + throwable);
        }
        return this;
    }

    /**
     * Указать код акции
     *
     * @param code код акции
     */
    public ModalWindowCreatingRecord setCode(String code) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesInputField("code", code);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать знпчение поля Код\n" + throwable);
        }
        return this;
    }

    /**
     * Указать акцию лояльности по умолчанию
     *
     * @param nameDefaultPromo акция лояльности по умолчанию
     */
    public ModalWindowCreatingRecord setDefaultPromoValue(String nameDefaultPromo) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesOfSuggestTypeField("defaultPromo", nameDefaultPromo);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля Акция лояльности\n" + throwable);
        }
        return this;
    }
}
