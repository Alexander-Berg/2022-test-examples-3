package ui_tests.src.test.java.pageHelpers;


import org.openqa.selenium.WebDriver;

public class PageHelper {
    public static TicketPageHelper ticketPageHelper(WebDriver webDriver) {
        return new TicketPageHelper(webDriver);
    }

    public static TableHelper tableHelper(WebDriver webDriver) {
        return new TableHelper(webDriver);
    }

    public static CreateTicketPageHelper createTicketPageHelper(WebDriver webDriver) {
        return new CreateTicketPageHelper(webDriver);
    }

    public static CustomerPageHelper customerPageHelper(WebDriver webDriver) {
        return new CustomerPageHelper(webDriver);
    }

    public static MainMenuHelper mainMenuHelper(WebDriver webDriver){
        return new MainMenuHelper(webDriver);
    }

    public static LoyaltyPromoHelper loyaltyPromoHelper(WebDriver webDriver){return new LoyaltyPromoHelper(webDriver); }

    public static OtherHelper otherHelper(WebDriver webDriver){return  new OtherHelper(webDriver);}

    public static BonusReasonHelper bonusReasonHelper(WebDriver webDriver) {return new BonusReasonHelper(webDriver);}

    public static SmsTemplatePageHelper smsTemplatePageHelper(WebDriver webDriver){return new SmsTemplatePageHelper(webDriver);}

    public static EmployeeHelper employeeHelper(WebDriver webDriver){return new EmployeeHelper(webDriver);}

    public static OrderHelper orderHelper(WebDriver webDriver){return new OrderHelper(webDriver);}

    public static DataImportHelper dataImportHelper(WebDriver webDriver){return new DataImportHelper(webDriver);}

    public static SearchHelper searchHelper(WebDriver webDriver){return new SearchHelper(webDriver);}
}
