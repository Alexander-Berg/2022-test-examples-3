package ru.yandex.autotests.innerpochta.ns.pages.lite;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 30.05.12
 * <p> Time: 15:51
 */
public interface ComposeLitePage extends MailPage {

    // Кому
    @FindByCss("[name = 'to']")
    WebElement toInput();

    // Текст
    @FindByCss("[name = 'send']")
    WebElement sendInput();

    // Кнопка "Отправить"
    @FindByCss("[name = 'doit']")
    WebElement submitButton();

    // Кнопка "Сохранить"
    @FindByCss("[name = 'nosend']")
    WebElement saveButton();

    // Тема
    @FindByCss("[name = 'subj']")
    WebElement subjInput();

    // Вложения
    @FindByCss(".b-message-attach__i")
    ElementsCollection<MailElement> attachesList();

    // Галочки для вложений
    @FindByCss("[name = 'ids']")
    ElementsCollection<MailElement> attachesCheckboxList();
}
