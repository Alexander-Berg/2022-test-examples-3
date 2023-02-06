package ui_tests.src.test.java.entity.comments;

import Classes.Comment;
import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import tools.Tools;
import unit.Config;

import java.util.ArrayList;
import java.util.List;

public class Comments {

    private WebDriver webDriver;

    public Comments(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * получить Сообщения
     * lite в заказах а обычный в обращениях
     * @return - Массив сообщений
     */
    public List<Comment> getCommentsLite() {
        List<Comment> messagesOfTicket = new ArrayList<>();
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        // Дождаться, пока появится блок комментариев
        Tools.waitElement(webDriver).waitElementToAppearInDOM(
                By.xpath(Entity.properties(webDriver).getXPathElement("commentsLite")));
        //Tools.waitElement(webDriver).waitVisibilityElementTheTime(Tools.findElement(webDriver).findElement(
//                By.xpath("//div[@class='_3vaASLwc' or @class='_1ERc3eCI']")), Config.DEF_TIME_WAIT_LOAD_PAGE);
        // Дождаться, пока в нем пропадет загрузка
        Tools.waitElement(webDriver).waitInvisibilityElementTheTime(
                By.xpath("//div[text()='Комментарии загружаются...']"), Config.DEF_TIME_WAIT_LOAD_PAGE);
        // Получить список комментариев
        List<WebElement> messages = Tools.findElement(webDriver).findElements(
                By.xpath("//*[contains(@data-ow-test-comment,'comment@')]"));
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        for (WebElement webElement : messages) {
            List<String> filesName = new ArrayList<>();
            Comment comment = new Comment();
            comment
                    .setNameAndEmail(webElement.findElement(By.xpath(".//span[contains(@data-tid,'2fd3735c')]")).getText())
                    .setText(webElement.findElement(By.xpath(".//div[contains(@data-tid,'e7195566')]")).getText());

            String classMessage = webElement.findElement(By.xpath(".//div[@data-tid=\"cb22a49\"]")).getAttribute(
                    "style");

            if (classMessage.contains("background: rgb(255, 248, 217)")) {
                comment.setType("internal");
            } else if (classMessage.contains("background: rgb(251, 251, 251)")) {
                comment.setType("public");
            } else {
                comment.setType("contact");
            }

            for (WebElement file : webElement.findElements(By.xpath(".//div[@data-tid='402c6749']//a[@data-tid" +
                    "='2b5112c9']"))) {
                filesName.add(file.getText());
            }
            comment.setFiles(filesName);
            messagesOfTicket.add(comment);
        }
        return messagesOfTicket;
    }

    public List<Comment> getComments() {
        List<Comment> messagesOfTicket = new ArrayList<>();
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        // Дождаться, пока появится блок комментариев
        Tools.waitElement(webDriver).waitElementToAppearInDOM(
                By.xpath(Entity.properties(webDriver).getXPathElement("comments")));
        //Tools.waitElement(webDriver).waitVisibilityElementTheTime(Tools.findElement(webDriver).findElement(
//                By.xpath("//div[@class='_3vaASLwc' or @class='_1ERc3eCI']")), Config.DEF_TIME_WAIT_LOAD_PAGE);
        // Дождаться, пока в нем пропадет загрузка
        Tools.waitElement(webDriver).waitInvisibilityElementTheTime(
                By.xpath("//div[text()='Комментарии загружаются...']"), Config.DEF_TIME_WAIT_LOAD_PAGE);
        // Получить список комментариев
        List<WebElement> messages = Tools.findElement(webDriver).findElements(
                By.xpath("//*[contains(@data-ow-test-comment,'comment@')]"));
        Tools.waitElement(webDriver).waitInvisibleLoadingElement();
        for (WebElement webElement : messages) {
            List<String> filesName = new ArrayList<>();
            Comment comment = new Comment();
            comment
                    .setNameAndEmail(webElement.findElement(By.xpath(".//span[contains(@data-tid,'2fd3735c')]")).getText())
                    .setText(webElement.findElement(By.xpath(".//div[contains(@data-tid,'e7195566')]")).getText());

            String classMessage = webElement.findElement(By.xpath(".//div[@data-tid=\"cb22a49\"]")).getAttribute(
                    "style");

            if (classMessage.contains("background: rgb(255, 248, 217)")) {
                comment.setType("internal");
            } else if (classMessage.contains("background: rgb(251, 251, 251)")) {
                comment.setType("public");
            } else {
                comment.setType("contact");
            }

            for (WebElement file : webElement.findElements(By.xpath(".//div[@data-tid='402c6749']//a[@data-tid" +
                    "='2b5112c9']"))) {
                filesName.add(file.getText());
            }
            comment.setFiles(filesName);
            messagesOfTicket.add(comment);
        }
        return messagesOfTicket;
    }

    public CommentsCreation commentsCreation() {
        return new CommentsCreation(webDriver);
    }
}
