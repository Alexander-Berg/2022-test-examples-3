package ru.yandex.autotests.innerpochta.ns.pages.lite;

import ru.yandex.autotests.innerpochta.atlas.MailPage;

import io.qameta.atlas.webdriver.ElementsCollection;
import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 30.05.12
 * <p> Time: 15:51
 */
public interface MailboxListLitePage extends MailPage {

    // Блоки сообщений
    @FindByCss(".b-messages__message")
    ElementsCollection<WebElement> messages();

    // Блок непрочитанного сообщения
    @FindByCss(".b-messages__message_unread")
    ElementsCollection<WebElement> unreadMessages();

    // Флажки "важно"
    @FindByCss(".b-messages__message span>img")
    ElementsCollection<WebElement> importantFlags();

    // Галка "Выделить все"
    @FindByCss("[id = 'check-all']")
    MailElement checkAllCheckbox();

    //Кнопка "Еще..."
    @FindByCss("[name = 'more")
    WebElement moreButton();

    //Кнопка "Написать"
    @FindByCss("[href='/lite/compose/retpath=%2Finbox']")
    WebElement composeButton();

    //Кнопка "Проверить"
    @FindByCss(".b-toolbar__but[href='/lite/inbox']")
    WebElement checkmailButton();

    //Кнопка "Удалить"
    @FindByCss("[name = 'delete")
    WebElement deleteButton();

    //Кнопка "Переслать"
    @FindByCss("[name = 'forward")
    WebElement forwardButton();

    //Кнопка "Спам"
    @FindByCss("[name = 'tospam")
    WebElement tospamButton();

    //Кнопка "Не Спам"
    @FindByCss("[name = 'notspam")
    WebElement notspamButton();

    @FindByCss(".b-folders__folder__link[href='/lite/draft']")
    WebElement draftsLink();

    @FindByCss(".b-folders__folder__link[href='/lite/spam']")
    WebElement spamLink();

    @FindByCss(".b-header__link.b-header__link_setup")
    WebElement settingsLink();
}
