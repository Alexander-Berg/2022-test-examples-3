package ui_tests.src.test.java.pages.ticketPage;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import tools.Tools;

public class ModalWindowLinkTicket {
    private WebDriver webDriver;

    public ModalWindowLinkTicket(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Выбрать тип связи
     */
    public ModalWindowLinkTicket setRelationType(String relationType) {
        // Data-атрибутов пока нет, будут сделаны в https://st.yandex-team.ru/OCRM-6329
        String xpath = Entity.properties(webDriver).getXPathElement(relationType);
        Tools.findElement(webDriver).findElement(
                By.xpath("(//span[text()='выберите тип связи'])")).click();
        Tools.waitElement(webDriver).waitElementToAppearInDOM(By.xpath(xpath));
        Tools.findElement(webDriver).findElement(By.xpath(xpath+"/..")).click();
        return this;
    }

    /**
     * Выбрать тип объекта
     */
    public ModalWindowLinkTicket setObjectType(String objectType) {
        // Data-атрибутов пока нет, будут сделаны в https://st.yandex-team.ru/OCRM-6329
        String xpath = Entity.properties(webDriver).getXPathElement(objectType);
        Tools.findElement(webDriver).findElement(
                By.xpath("(//span[text()='выберите тип объекта'])")).click();
        Tools.waitElement(webDriver).waitElementToAppearInDOM(By.xpath(xpath));
        Tools.findElement(webDriver).findElement(By.xpath(xpath+"/..")).click();
        return this;

    }

    /**
     * Выбрать тип объекта
     */
    public ModalWindowLinkTicket setObject(String object, String expectedSuggest) {
        // Data-атрибутов пока нет, будут сделаны в https://st.yandex-team.ru/OCRM-6329
        Tools.sendElement(webDriver).sendElement(By.xpath("//input[@placeholder='Начните ввод']"), object);
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        Tools.waitElement(webDriver).waitElementToAppearInDOM(By.xpath(String.format("//li[text()='%s']", expectedSuggest)));
        Tools.findElement(webDriver).findElement(By.xpath(String.format("//li[text()='%s']", expectedSuggest))).click();
        return this;
    }

    /**
     * Нажать на кнопку "Добавить связь"
     */
    public void linkTicketButtonClick() {
        Entity.modalWindow(webDriver).controls().clickButton("Добавить связь");
    }
}
