package ui_tests.src.test.java.entity;

import entity.comments.Comments;
import entity.entityTable.EntityTable;
import entity.modalWindow.ModalWindow;
import org.openqa.selenium.WebDriver;
import pages.ticketPage.PartnerPreviewTab;

public class Entity {

    public static Header header(WebDriver webDriver) {
        return new Header(webDriver);
    }

    public static EntityTable entityTable(WebDriver webDriver) {
        return new EntityTable(webDriver);
    }

    public static Tabs tabs(WebDriver webDriver) {
        return new Tabs(webDriver);
    }

    public static ModalWindow modalWindow(WebDriver webDriver) {
        return new ModalWindow(webDriver);
    }

    public static Buttons buttons(WebDriver webDriver) {
        return new Buttons(webDriver);
    }

    public static Comments comments(WebDriver webDriver) {
        return new Comments(webDriver);
    }

    public static Properties properties(WebDriver webDriver) {
        return new Properties(webDriver);
    }

    public static SimpleTable simpleTable(WebDriver webDriver) {
        return new SimpleTable(webDriver);
    }

    public static Toast toast(WebDriver webDriver) {return new Toast(webDriver);}

    public static PartnerPreviewTab partnerPreviewTab(WebDriver webDriver) {
        return new PartnerPreviewTab(webDriver);
    }
}
