package ui_tests.src.test.java.pages.dataImportPage.beruOutgoingCallTicketByOrderIdPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;
import tools.Tools;

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
}
