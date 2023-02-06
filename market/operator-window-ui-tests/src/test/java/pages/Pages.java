package ui_tests.src.test.java.pages;

import entity.AlertDanger;
import entity.Toast;
import org.openqa.selenium.WebDriver;
import pages.bonusReasonPage.BonusReasonPage;
import pages.customerPage.CustomerPage;
import pages.dataImportPage.DataImportPage;
import pages.employeePage.EmployeePage;
import pages.importPage.ImportPage;
import pages.importsSummaryPage.ImportsSummaryPage;
import pages.loyaltyPromoPage.LoyaltyPromoPage;
import pages.orderPage.OrderPage;
import pages.searchPage.SearchPage;
import pages.servicePage.createServicePage.CreateServicePage;
import pages.smsTemplatePage.SmsTemplatePage;
import pages.ticketCommentTemplatePage.TicketCommentTemplatePage;
import pages.ticketPage.TicketPage;
import pages.yaDeliveryOrderPage.YaDeliveryOrderPage;

public class Pages {

    public static LoginPage loginPage(WebDriver webDriver) {
        return new LoginPage(webDriver);
    }

    public static MainMenu mainMenuOfPage(WebDriver webDriver) {
        return new MainMenu(webDriver);
    }

    public static TicketPage ticketPage(WebDriver webDriver) {
        return new TicketPage(webDriver);
    }

    public static Navigate navigate(WebDriver webDriver) {
        return new Navigate(webDriver);
    }

    public static AlertDanger alertDanger(WebDriver webDriver) {
        return new AlertDanger(webDriver);
    }

    public static Toast toast(WebDriver webDriver) {
        return new Toast(webDriver);
    }

    public static OrderPage orderPage(WebDriver webDriver) {
        return new OrderPage(webDriver);
    }

    public static LoyaltyPromoPage loyaltyPromoPage(WebDriver webDriver) {
        return new LoyaltyPromoPage(webDriver);
    }

    public static CreateTicketCommentTemplatePage createTicketCommentTemplatePage(WebDriver webDriver) {
        return new CreateTicketCommentTemplatePage(webDriver);
    }

    public static TicketCommentTemplatePage ticketCommentTemplatePage(WebDriver webDriver) {
        return new TicketCommentTemplatePage(webDriver);
    }

    public static BonusReasonPage bonusReasonPage(WebDriver webDriver) {
        return new BonusReasonPage(webDriver);
    }

    public static SmsTemplatePage smsTemplatePage(WebDriver webDriver) {
        return new SmsTemplatePage(webDriver);
    }

    public static EmployeePage employeePage(WebDriver webDriver) {
        return new EmployeePage(webDriver);
    }

    public static YaDeliveryOrderPage yaDeliveryOrderPage(WebDriver webDriver) {
        return new YaDeliveryOrderPage(webDriver);
    }

    public static SecondScreenPage secondScreenPage(WebDriver webDriver) {
        return new SecondScreenPage(webDriver);
    }

    public static CreateServicePage createServicePage(WebDriver webDriver) {
        return new CreateServicePage(webDriver);
    }

    public static DataImportPage dataImportPage(WebDriver webDriver) {
        return new DataImportPage(webDriver);
    }

    public static ImportPage importPage(WebDriver webDriver) {
        return new ImportPage(webDriver);
    }

    public static ImportsSummaryPage importsSummaryPage(WebDriver webDriver) {
        return new ImportsSummaryPage(webDriver);
    }

    public static CustomerPage customerPage(WebDriver webDriver) {
        return new CustomerPage(webDriver);
    }

    public static SearchPage searchPage(WebDriver webDriver) {
        return new SearchPage(webDriver);
    }

}
