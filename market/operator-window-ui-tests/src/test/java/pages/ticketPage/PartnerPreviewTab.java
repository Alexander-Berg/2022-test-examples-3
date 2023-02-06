package ui_tests.src.test.java.pages.ticketPage;


import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import tools.Tools;

public class PartnerPreviewTab {
    private WebDriver webDriver;

    public PartnerPreviewTab(WebDriver webDriver) {
        this.webDriver = webDriver;
    }


    /**
     * Нажать на кнопку поиска партнёра
     */
    public void clickSearchButton() {
        try {
            Entity.buttons(webDriver).clickCustomButton("", "поиск");
        } catch (Throwable e) {
            throw new Error("Не удалось нажать на кнопку поиска партнёра \n" + e);
        }
    }

    /**
     * Перейти на таб "Все партнёры"
     */
    public void gotoAllPartners() {
        try {
            Entity.buttons(webDriver).clickCustomButton("", "Все партнеры");
        } catch (Throwable e) {
            throw new Error("Не удалось перейти на таб все партнёры \n" + e);
        }
    }

    /**
     * Указать партнёра
     *
     * @param partner название партнёра
     * @return
     */
    public PartnerPreviewTab setPartner(String partner) {
        try {
            Entity.properties(webDriver).setInputField("search-input", partner);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать партнёра в обращении:\n" + t);
        }
    }

    /**
     * Проверить наличие нужного партнёра в списке
     */
    public boolean findPartner(String partnerName) {
        for (int i = 0; i < 5; i++) {
            if (Tools.findElement(webDriver).findElements(By.xpath(String.format("//a[text()='%s']",
                    partnerName))).size() >= 1) {
                return true;
            }
            Tools.waitElement(webDriver).waitTime(1000);
        }
        return false;
    }

}
