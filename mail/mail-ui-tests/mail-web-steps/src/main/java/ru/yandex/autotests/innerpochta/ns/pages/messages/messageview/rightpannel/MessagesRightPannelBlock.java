package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview.rightpannel;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * Created by a-zoshchuk
 */
public interface MessagesRightPannelBlock extends MailElement {

    @Name("Список писем")
    @FindByCss(".qa-MessageViewer-RelatedMessage")
    ElementsCollection<MailElement> relatedMsgList();

    @Name("Текст выделенного письма (текущее)")
    @FindByCss(".qa-MessageViewer-RelatedMessage-current .qa-MessageViewer-RelatedMessage-Content")
    MailElement selectedMsgInList();

    @Name("Список непрочитанных писем")
    @FindByCss("[class*=RelatedMessage__new]")
    ElementsCollection<MailElement> unreadMsgList();
}
