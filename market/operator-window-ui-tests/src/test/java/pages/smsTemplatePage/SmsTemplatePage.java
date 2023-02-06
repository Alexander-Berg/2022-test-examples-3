package ui_tests.src.test.java.pages.smsTemplatePage;

import org.openqa.selenium.WebDriver;
import pages.smsTemplatePage.editRecordPage.EditRecordPage;
import pages.smsTemplatePage.viewRecordPage.ViewRecordPage;

public class SmsTemplatePage {
    private WebDriver webDriver;

    public SmsTemplatePage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public ModalWindowCreatingRecord modalWindowCreatingRecord() {
        return new ModalWindowCreatingRecord(webDriver);
    }

    public ViewRecordPage viewRecordPage() {
        return new ViewRecordPage(webDriver);
    }

    public EditRecordPage editRecordPage() {
        return new EditRecordPage(webDriver);
    }
}
