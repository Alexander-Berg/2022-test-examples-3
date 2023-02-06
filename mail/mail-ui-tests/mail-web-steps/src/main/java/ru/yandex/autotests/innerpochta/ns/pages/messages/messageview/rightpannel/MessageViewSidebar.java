package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.rightpannel;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface MessageViewSidebar extends MailElement {

    @Name("«Письма на тему»")
    @FindByCss(".qa-MessageViewer-MessagesOnSubject-Title")
    MailElement messagesBySubjLink();

    @Name("Блок «Письма на тему»")
    @FindByCss(".qa-MessageViewer-MessagesOnSubject-Content")
    MessagesRightPannelBlock messagesBySubjList();

    @Name("«Письма от отправителя»")
    @FindByCss(".qa-MessageViewer-MessagesFromSender")
    MailElement messagesBySenderLink();

    @Name("Блок «Письма от отправителя»")
    @FindByCss(".qa-MessageViewer-MessagesFromSender .qa-MessageViewer-Cut-Content")
    MessagesRightPannelBlock messagesBySenderList();

    @Name("Кнопка «след.» письмо в правой колонке")
    @FindByCss(".qa-MessageViewer-Next .Button2")
    MailElement nextBtn();

    @Name("Кнопка «пред.» письмо в правой колонке")
    @FindByCss(".qa-MessageViewer-Prev .Button2")
    MailElement prevBtn();

    @Name("«Свернуть/Развернуть»")
    @FindByCss(".qa-MessageViewer-FloatingColumn-Button")
    MailElement expandBtn();
}
