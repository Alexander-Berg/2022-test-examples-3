package ui_tests.src.test.java.pages.smsTemplatePage.editRecordPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class Properties {

    private WebDriver webDriver;

    public Properties(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Указать название шаблона
     *
     * @param title название шаблона
     * @return
     */
    public Properties setTitle(String title) {
        try {
            Entity.properties(webDriver).setInputField("title", title);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля Название\n" + throwable);
        }
        return this;
    }

    /**
     * Указать отправителя
     *
     * @param sender Отправитель
     * @return
     */
    public Properties setSender(String sender) {
        try {
            Entity.properties(webDriver).setPropertiesOfSuggestTypeField("sender", sender);
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
    public Properties setText(String text) {
        try {
            Entity.properties(webDriver).setPropertiesOfTextArea("text", text);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение поля Текст шаблона\n" + throwable);
        }
        return this;
    }
}
