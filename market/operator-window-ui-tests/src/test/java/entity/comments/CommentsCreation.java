package ui_tests.src.test.java.entity.comments;


import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;

import java.util.List;

public final class CommentsCreation {

    private String block = "//*[contains(@data-tid,'33355149')]";
    private WebDriver webDriver;

    public CommentsCreation(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }


    /**
     * Открыть вкладку
     *
     * @return this
     */
    public void openMailTab(String tabName) {
        try {
            Entity.buttons(webDriver).clickCustomButton(block, tabName);
        } catch (Throwable t) {
            throw new Error("Не удлось открыть вкладку " + tabName + "\n" + t);
        }
    }

    public void clickOutputMailTab() {
        openMailTab("Внешнее письмо");
    }

    /**
     * Нажать на вкладку "Внутренняя заметка"
     *
     * @return
     */
    public void clickInternalMailTab() {
        openMailTab("Внутренняя заметка");
    }

    /**
     * Нажать на вкладку "Письмо партнёру"
     *
     * @return
     */
    public void clickPartnerMailTab() {
        openMailTab("Письмо партнёру");
    }


    /**
     * Нажать на кнопку из блока с созданием комментария
     *
     * @param buttonName - текст кнопки
     * @return this
     */
    public CommentsCreation clickButton(String buttonName) {
        Entity.buttons(webDriver).clickButton(block, buttonName);
        return this;
    }

    /**
     * Нажать на кнопку действия в саджесте кнопки
     *
     * @param actionButton - текст кнопки
     * @return - TicketPage
     */
    public void clickButtonActionOnTicket(String actionButton) {
        try {
            Tools.clickerElement(webDriver).clickElement(By.xpath("//ul[@data-tid='a9e7786b']/li[text()='" + actionButton + "']"));
        } catch (Throwable t) {
            throw new Error("Не удалось нажать на кнопку действия над тикетом \n" + t);
        }
    }

    /**
     * Получить текст введенный в поле комментария
     *
     * @return
     */
    public String getEnteredComment() {
        try {
            StringBuilder textFromPage = new StringBuilder();
            Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//*[@data-contents=\"true\"]/div/div"));
            List<WebElement> textFromWebElement = Tools.findElement(webDriver).findElements(By.xpath("//*[@data-contents=\"true\"]/div/div"));
            for (WebElement element : textFromWebElement) {
                textFromPage.append(element.getText().trim()).append(" ");
            }
            return textFromPage.toString().trim();
        } catch (Throwable t) {
            throw new Error("Не удалось получить текст, введенный в поле комментрия \n" + t);
        }
    }

    /**
     * Получить текст введенный в поле ответа в чат
     *
     * @return
     */
    public String getEnteredChatComment() {
        try {
            Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//textarea[@aria-invalid=\"false\"]"));
            return Tools.findElement(webDriver).findElement(By.xpath("//textarea[@aria-invalid=\"false\"]")).getText();
        } catch (Throwable t) {
            throw new Error("Не удалось получить текст, введенный в поле ответа в чат \n" + t);
        }
    }


    public CommentsCreation setTextComment(CharSequence... textComment) {
        try {
            WebElement webElement = Tools.waitElement(webDriver).waitClickableElement(By.xpath("//*[@contenteditable=\"true\"]"));
            webElement.sendKeys(textComment);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось ввести текст в поле комментария \n" + t);
        }
    }

    public CommentsCreation setChatTextComment(CharSequence... textComment) {
        try {
            WebElement webElement = Tools.waitElement(webDriver).waitClickableElement(By.xpath("//textarea[@aria-invalid=\"false\"]"));
            webElement.sendKeys(textComment);
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось ввести текст в поле ответа в чат \n" + t);
        }
    }
}
