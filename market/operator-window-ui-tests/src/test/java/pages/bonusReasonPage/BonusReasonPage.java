package ui_tests.src.test.java.pages.bonusReasonPage;

import org.openqa.selenium.WebDriver;
import pages.bonusReasonPage.editRecordPage.EditRecordPage;
import pages.bonusReasonPage.viewRecordPage.ViewRecordPage;

public class BonusReasonPage {
    private WebDriver webDriver;

    public BonusReasonPage(WebDriver webDriver) {
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
