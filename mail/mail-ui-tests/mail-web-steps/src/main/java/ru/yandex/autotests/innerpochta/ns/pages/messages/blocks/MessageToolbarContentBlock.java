package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * Created by a-zoshchuk
 */
public interface MessageToolbarContentBlock extends MailElement {

    @Name("Кнопка «Ответить»")
    @FindByCss("[title=Ответить]")
    MailElement replyBtn();

    @Name("Кнопка «Удалить»")
    @FindByCss("[title=Удалить]")
    MailElement deleteBtn();

    @Name("Кнопка «Ещё»")
    @FindByCss("[title=Ещё]")
    MailElement moreBtn();

    @Name("Кнопка «Ответить всем»")
    @FindByCss("[title='Ответить всем']")
    MailElement replyToAllBtn();

    @Name("Кнопка «Дописать»")
    @FindByCss("[title=Дописать]")
    MailElement finishDraft();

    @Name("Кнопка «Переслать»")
    @FindByCss("[title=Переслать]")
    MailElement forwardButton();

    @Name("Кнопка «Это спам!»")
    @FindByCss("[title='Это спам!']")
    MailElement spamButton();

    @Name("Кнопка «Не спам!»")
    @FindByCss("[title='Не спам!']")
    MailElement notSpamButton();

    @Name("Все кнопки тулбара")
    @FindByCss(".qa-MessageViewer-ToolbarButton")
    ElementsCollection<MailElement> allItems();
}
