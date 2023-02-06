package ui_tests.src.test.java.pages.smsTemplatePage.viewRecordPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class Properties {
    private WebDriver webDriver;

    public Properties(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * ПОлучить Название
     *
     * @return Название
     */
    public String getTitle() {
        try {
            return Entity.properties(webDriver).getValueField("title");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Название\n" + throwable);
        }
    }

    /**
     * Получить Код
     *
     * @return Код
     */
    public String getCode() {
        try {
            return Entity.properties(webDriver).getValueField("code");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Код\n" + throwable);
        }
    }

    /**
     * Получить отправителя
     *
     * @return Отправитель
     */
    public String getSender() {
        try {
            return Entity.properties(webDriver).getValueField("sender");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Отправитель\n" + throwable);
        }
    }

    /**
     * Получить Текст шаблона
     *
     * @return Текст шаблона
     */
    public String getText() {
        try {
            return Entity.properties(webDriver).getValueField("text");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить значение поля Текст шаблона\n" + throwable);
        }
    }
}
