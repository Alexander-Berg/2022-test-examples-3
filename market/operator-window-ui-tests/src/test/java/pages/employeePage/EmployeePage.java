package ui_tests.src.test.java.pages.employeePage;

import org.openqa.selenium.WebDriver;
import pages.employeePage.viewRecordPage.ViewRecordPage;

public class EmployeePage {
    private WebDriver webDriver;

    public EmployeePage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Модальное окно создания и редактирования записи
     *
     * @return
     */
    public ModalWindowCreateOrEditRecords modalWindowCreateOrEditRecords() {
        return new ModalWindowCreateOrEditRecords(webDriver);
    }

    /**
     * Страница просмотара пользователя
     *
     * @return
     */
    public ViewRecordPage viewRecordPage() {
        return new ViewRecordPage(webDriver);
    }
}
