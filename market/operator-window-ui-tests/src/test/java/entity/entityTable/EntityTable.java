package ui_tests.src.test.java.entity.entityTable;


import org.openqa.selenium.WebDriver;

public class EntityTable {
    private WebDriver webDriver;

    public EntityTable(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Таблица с записями
     *
     * @return Content()
     */
    public Content content() {
        return new Content(webDriver);
    }

    /**
     * Подвал таблицы с записями
     *
     * @return Footer()
     */
    public Footer footer() {
        return new Footer(webDriver);
    }

    /**
     * Тублар таблицы с записями
     *
     * @return ToolBar ()
     */
    public ToolBar toolBar() {
        return new ToolBar(webDriver);
    }


}
