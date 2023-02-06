package ui_tests.src.test.java.pages.orderPage.orderPage.generalInformationTab;

import Classes.Comment;
import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import tools.Tools;

import java.util.List;


public class Comments {

    private final WebDriver webDriver;

    private String getBlock(){
       return Entity.properties(webDriver).getXPathElement("commentsLite");
    }

    public Comments(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Указать текст в поле создания комментария
     *
     * @param value
     */
    public void setTextComment(String value) {
        try {
            Tools.sendElement(webDriver).sendElement(By.xpath(getBlock() + "//textarea[contains(@aria-invalid,'false')]"), value);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение в поле ввода комментария \n" + throwable);
        }
    }

    /**
     * Нажать на кнопку Добавить
     */
    public void clickAddButton() {
        try {
            Entity.buttons(webDriver).clickButton(getBlock(), "Добавить");
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на кнопку Добавить\n" + throwable);
        }
    }

    /**
     * Получить комментарии заказа
     *
     * @return
     */
    public List<Comment> getComments() {
        return Entity.comments(webDriver).getCommentsLite();
    }

    /**
     * Получить текст введенный в поле ввода комментария
     * @return
     */
    public String getEnteredComment(){
        return Entity.properties(webDriver).getEnteredTextInTextArea("");
    }
}
