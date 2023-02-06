package ui_tests.src.test.java.pages.ticketPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class Header {
    private WebDriver webDriver;

    public Header(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Нажать на кнопку изменения обращения
     */
    public void clickOnEditTicketButton() {
        Entity.header(webDriver).clickButtonEditForm("изменить");
    }

    /**
     * Нажать на кнопку сохранения обращения
     */
    public void clickOnSaveTicketButton() {
        Entity.header(webDriver).clickButtonSaveForm("сохранить");
    }

    /**
     * Нажать на кнопку "Добавить связь"
     */
    public void clickLinkTicketButton() {
        Entity.buttons(webDriver).clickOnButtonOnLocator("", "addRelation-Добавить связь");
    }

    /**
     * Получить тему обращения
     *
     * @return
     */
    public String getSubject() {
        return Entity.header(webDriver).getSubject("default");
    }

    /**
     * Нажать на кнопку "Задача к смежникам"
     */
    public void clickCreateOuterTicketButton() {
        Entity.buttons(webDriver).clickButton("",
                "Задача к смежникам");
    }

}
