package ui_tests.src.test.java.pages.dataImportPage.orderTicketsManualCreation;

import entity.Entity;
import org.openqa.selenium.WebDriver;
import tools.Tools;

public class TabForm {
    private WebDriver webDriver;

    public TabForm(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Указать значение в поле "Текст комментария"
     *
     * @param textComment
     */
    public TabForm setTextComment(String textComment) {
        Entity.properties(webDriver).setRichTextEditor("commentText", textComment);
        return this;
    }

    /**
     * Указать значение в поле "Файл с данными"
     *
     * @param filePath
     */
    public TabForm setImportFile(String filePath) {
        Entity.properties(webDriver).setPropertiesFileField("", "sources", filePath);
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        return this;
    }

    /**
     * Указать значение поля очередь
     *
     * @return
     */
    public TabForm setService(String value) {
        Entity.properties(webDriver).setPropertiesOfSuggestTypeField("service", value);
        return this;
    }
}
