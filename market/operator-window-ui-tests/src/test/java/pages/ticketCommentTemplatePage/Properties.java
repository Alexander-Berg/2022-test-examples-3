package ui_tests.src.test.java.pages.ticketCommentTemplatePage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

import java.util.List;

// По аналогии с pages/ticketPage/properties/Properties.java
public class Properties {
    private WebDriver webDriver;

    public Properties(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Получить код
     *
     * @return
     */
    public String getCode() {
        try {
            return Entity.properties(webDriver).getValueField("code");
        } catch (Throwable t) {
            throw new Error("Не удалось получить код шаблона:\n" + t);
        }
    }

    /**
     * Получить значение поля "Шаблон ответа"
     *
     * @return
     */
    public String getText() {
        try {
            return Entity.properties(webDriver).getValueField("template");
        } catch (Throwable t) {
            throw new Error("Не удалось получить текст шаблона:\n" + t);
        }
    }

    /**
     * Получить Категорию
     *
     * @return
     */
    public List<String> getCategory() {
        try {
            return Entity.properties(webDriver).getValuesField("categories");
        } catch (Throwable t) {
            throw new Error("Не удалось получить категорию обращения:\n" + t);
        }
    }
}
