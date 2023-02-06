package ui_tests.src.test.java.pages.ticketPage.messageTab;


import entity.Entity;
import org.openqa.selenium.WebDriver;
import pages.Pages;

public class CommentCreation {
    private final WebDriver webDriver;

    public CommentCreation(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Нажать на кнопку "Завершить"
     *
     * @return
     */
    public CommentCreation clickCloseButton() {
        Pages.ticketPage(webDriver).toast().hideNotificationError();
        Entity.comments(webDriver).commentsCreation().clickButton("Завершить");
        return this;
    }

    /**
     * Нажать на кнопку
     *
     * @return
     */
    public CommentCreation clickSaveANoteButton() {
        Entity.comments(webDriver).commentsCreation().clickButton("Сохранить заметку");
        return this;
    }

    /**
     * нажать на действие "Отправить ответ"
     *
     * @return
     */
    public CommentCreation clickSendAResponseActionButton() {
        Entity.comments(webDriver).commentsCreation().clickButtonActionOnTicket("Отправить ответ");
        return this;
    }

    /**
     * нажать на действие "Отправить комментарий"
     *
     * @return
     */
    public void clickSendACommentActionButton() {
        Entity.comments(webDriver).commentsCreation().clickButtonActionOnTicket("Отправить комментарий");
    }

    /**
     * нажать на действие "Не требует ответа"
     *
     * @return
     */
    public CommentCreation clickNoResponseRequiredActionButton() {
        Entity.comments(webDriver).commentsCreation().clickButtonActionOnTicket("Не требует ответа");
        return this;
    }

    /**
     * нажать на действие "Пометить спамом"
     *
     * @return
     */
    public CommentCreation clickSpamActionButton() {
        Entity.comments(webDriver).commentsCreation().clickButtonActionOnTicket("Пометить спамом");
        return this;
    }

    /**
     * нажать на действие "Переложить"
     *
     * @return
     */
    public CommentCreation clickShiftActionButton() {
        try {
        Entity.comments(webDriver).commentsCreation().clickButtonActionOnTicket("Переложить");}
        catch (Throwable exception){
            clickShiftActionButtonV2();
        }
        return this;
    }

    /**
     * нажать на действие "Переложить"
     *
     * @return
     */
    public CommentCreation clickShiftActionButtonV2() {
        Entity.comments(webDriver).commentsCreation().clickButtonActionOnTicket("Переложено");
        return this;
    }

    /**
     * Нажать на кнопку "Выбрать ответ из шаблона"
     *
     * @return
     */
    public CommentCreation clickMessageTemplateButton() {
        Entity.comments(webDriver).commentsCreation().clickButton("Выбрать ответ из шаблонов");
        return this;
    }

    /**
     * Нажать на кнопку "В работу"
     *
     * @return
     */
    public CommentCreation clickInWorkButton() {
        Entity.comments(webDriver).commentsCreation().clickButton("В работу");
        return this;
    }

    /**
     * Нажать на вкладку "Сообщение в чат"
     *
     * @return
     */
    public CommentCreation clickChatTab() {
        Entity.comments(webDriver).commentsCreation().openMailTab("Сообщение в чат");
        return this;
    }

    /**
     * Нажать на вкладку "Внешнее письмо"
     *
     * @return
     */
    public CommentCreation clickOutputMailTab() {
        Entity.comments(webDriver).commentsCreation().openMailTab("Внешнее письмо");
        return this;
    }

    /**
     * Нажать на вкладку "Письмо партнёру"
     *
     * @return
     */
    public CommentCreation clickPartnerMailTab() {
        Entity.comments(webDriver).commentsCreation().openMailTab("Письмо партнёру");
        return this;
    }

    /**
     * Нажать на вкладку "Внутренняя заметка"
     *
     * @return
     */
    public CommentCreation clickInternalMailTab() {
        Entity.comments(webDriver).commentsCreation().openMailTab("Внутренняя заметка");
        return this;
    }

    /**
     * Ввести текст в поле комментария
     *
     * @param textComment текст комментария
     * @return
     */
    public CommentCreation setTextComment(String textComment) {
        Entity.comments(webDriver).commentsCreation().setTextComment(textComment);
        return this;
    }

    /**
     * Ввести текст в поле ответа в чат
     *
     * @param textComment текст комментария
     * @return
     */
    public CommentCreation setChatTextComment(String textComment) {
        Entity.comments(webDriver).commentsCreation().setChatTextComment(textComment);
        return this;
    }

    /**
     * Получить текст введенный в поле для ввода комментария
     *
     * @return
     */
    public String getEnteredComment() {
        return Entity.comments(webDriver).commentsCreation().getEnteredComment();
    }

    /**
     * Получить текст введенный в поле для ввода ответа в чат
     */
    public String getEnteredChatComment() {
        return Entity.comments(webDriver).commentsCreation().getEnteredChatComment();
    }


    public CommentCreation clickTackInWorkActionButton() {
        Entity.comments(webDriver).commentsCreation().clickButtonActionOnTicket("В работу");
        return this;
    }
}
