package ui_tests.src.test.java.entity.entityTable;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;

import java.util.List;

public final class Footer extends Entity {

    String block = "//div[@data-tid='2c842bbe']";
    private WebDriver webDriver;

    public Footer(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    @FindBy(xpath = "//*[@title='Следующая']")
    private WebElement nextPageButton;

    @FindBy(xpath = "//*[@title='Первая']")
    private WebElement firstPageButton;

    /**
     * Нажать на кнопку ведущую на следующую страницу
     *
     * @return Content
     */
    public void nextPageButtonClick() {
        try {
            buttons(webDriver).clickNavLinkButton("", "Следующая");
            Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//div[@data-tid='a7e4b349']//input[@value!='1']"));
        } catch (Throwable t) {
            throw new Error("Не удалось нажать на кнопку ведущую на след. страницу \n" + t);
        }
    }

    /**
     * Нажать на кнопку ведущую на первую страницу
     *
     * @return Content
     */
    public void openFirstPageButtonClick() {
        try {
            buttons(webDriver).clickNavLinkButton("", "Первая");
        } catch (Throwable t) {
            throw new Error("Не удалось нажать на кнопку ведущую на первую страницу \n" + t);
        }
    }

    /**
     * Получить общее к-во записей в таблице
     *
     * @return к-во записей в таблице
     */
    public int getTotalEntity() {
        try {
            List<WebElement> webElements = Tools.findElement(webDriver).findElements(By.xpath(block + "//*[contains(@class,'jmf-total-items-calc')]/*[text()='Посчитать']"));
            if (webElements.size() > 0) {
                Tools.clickerElement(webDriver).clickElement(webElements.get(0));
            }
            webElements = Tools.findElement(webDriver).findElements(By.xpath(block + "//*[contains(@class,'jmf-total-items-calc')]"));
            String count = webElements.get(0).getText().replace("Объектов в таблице: ", "");
            return Integer.parseInt(count);
        } catch (Throwable t) {
            throw new Error("Не удалось получить к-во записей \n" + t);
        }
    }
}
