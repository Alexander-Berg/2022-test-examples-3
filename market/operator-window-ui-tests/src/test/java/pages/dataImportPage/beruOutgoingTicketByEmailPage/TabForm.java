package ui_tests.src.test.java.pages.dataImportPage.beruOutgoingTicketByEmailPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;
import tools.Tools;

import java.util.List;

public class TabForm {
    private WebDriver webDriver;

    public TabForm(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Указать значение в поле "Очередь"
     *
     * @param service
     */
    public TabForm setService(String service) {
        Entity.properties(webDriver).setPropertiesOfSuggestTypeField("service", service);
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
     * Указать значение в поле "Категория обращения"
     *
     * @param categories
     */
    public TabForm setCategory(List<String> categories) {
        Entity.properties(webDriver).setPropertiesOfTreeSelectTypeField("category", categories);
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        return this;
    }

}
