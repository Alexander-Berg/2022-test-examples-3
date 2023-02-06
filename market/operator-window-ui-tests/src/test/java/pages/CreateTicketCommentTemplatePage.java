package ui_tests.src.test.java.pages;

import entity.Entity;
import org.openqa.selenium.WebDriver;

import java.util.List;

// По аналогии с src/test/java/pages/ticketPage/properties/Properties.java
public class CreateTicketCommentTemplatePage {
    private WebDriver webDriver;

    public CreateTicketCommentTemplatePage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    // Ввести значения полей
    public CreateTicketCommentTemplatePage setName(String name) {
        try {
            Entity.properties(webDriver).setInputField("title", name);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать название шаблона:\n" + t);
        }
    }

    public CreateTicketCommentTemplatePage setCode(String code) {
        try {
            Entity.properties(webDriver).setInputField("code", code);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать код шаблона:\n" + t);
        }
    }

    public CreateTicketCommentTemplatePage setText(String text) {
        try {
            Entity.comments(webDriver).commentsCreation().setTextComment(text);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать текст шаблона:\n" + t);
        }
    }

    public CreateTicketCommentTemplatePage setCategories(List<String> categories) {
        try {
            Entity.properties(webDriver).setPropertiesOfMultiSuggestTypeField("categories", categories);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать категории для шаблона:\n" + t);
        }
    }

    // Нажать "Сохранить"
    public CreateTicketCommentTemplatePage saveButtonClick() {
        Entity.modalWindow(webDriver).controls().clickButton("Добавить");
        return this;
    }
}
