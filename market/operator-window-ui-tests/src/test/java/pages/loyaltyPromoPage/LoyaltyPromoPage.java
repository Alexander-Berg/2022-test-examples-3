package ui_tests.src.test.java.pages.loyaltyPromoPage;

import org.openqa.selenium.WebDriver;
import pages.loyaltyPromoPage.editRecordPage.EditRecordPage;
import pages.loyaltyPromoPage.viewRecordPage.ViewRecordPage;

public class LoyaltyPromoPage {
    private WebDriver webDriver;

    public LoyaltyPromoPage(WebDriver webDriver) {
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
