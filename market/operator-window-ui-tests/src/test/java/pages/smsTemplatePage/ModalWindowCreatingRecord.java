package ui_tests.src.test.java.pages.smsTemplatePage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class ModalWindowCreatingRecord {
    private WebDriver webDriver;

    public ModalWindowCreatingRecord(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public void clickSaveRecordButton() {
        Entity.modalWindow(webDriver).controls().clickButton("Добавить");
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
            throw new Error("Не удалось указать значение пполя Код\n" + throwable);
        }
        return this;
    }

    /**
     * Указать Название
     *
     * @param title Название
     */

    public ModalWindowCreatingRecord setTitle(String title) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesInputField("title", title);
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Название\n" + throwable);
        }
        return this;
    }

    /**
     * Указать отправителя
     *
     * @param sender отправитель
     */
    public ModalWindowCreatingRecord setSender(String sender) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesOfSuggestTypeField("sender", sender);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля Отправитель\n" + throwable);
        }
        return this;
    }

    /**
     * Указать текст шаблона
     *
     * @param text текст шаблона
     * @return
     */
    public ModalWindowCreatingRecord setText(String text) {
        try {
            Entity.modalWindow(webDriver).content().setPropertiesOfTextArea("text", text);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать заначение поля Текст шаблона\n" + throwable);
        }
        return this;
    }
}
