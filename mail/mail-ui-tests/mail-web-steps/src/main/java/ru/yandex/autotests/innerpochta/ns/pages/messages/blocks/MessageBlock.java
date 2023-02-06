package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.AttachmentsWidget;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns.AttachmentsWidgetInMessageList;

public interface MessageBlock extends MailElement {

    @Name("Свернуть/развернуть тред")
    @FindByCss(".js-thread-toggle")
    MailElement expandThread();

    @Name("Свернуть тред")
    @FindByCss(".js-thread-toggle:not(.is-folded)")
    MailElement onlyCollapseThread();

    @Name("Развернуть тред")
    @FindByCss(".js-thread-toggle.is-folded")
    MailElement onlyExpandThread();

    @Name("Значок «Отвечено»")
    @FindByCss(".mail-MessageSnippet-Item_status_replied")
    MailElement repliedArrow();

    @Name("Значок «Отложено»")
    @FindByCss(".mail-MessageSnippet-Item_status_delayed")
    MailElement delayedBadge();

    @Name("Выбрать сообщение")
    @FindByCss(".js-messages-item-checkbox-visual")
    MailElement checkBox();

    @Name("Флаг «Важное» не активен")
    @FindByCss(".js-message-snippet-importance:not(.is-active)")
    MailElement importanceLabel();

    @Name("Флаг «Важное» активен")
    @FindByCss(".js-message-snippet-importance.is-active")
    MailElement isImportance();

    @Name("Прыщ: замьютить тред")
    @FindByCss(".mail-Icon-Mute")
    MailElement threadMute();

    @Name("Прыщ: письмо прочитано.")
    @FindByCss(".mail-Icon-Read:not(.is-active)")
    MailElement messageRead();

    @Name("Прыщ: письмо не прочитано.")
    @FindByCss(".mail-Icon-Read.is-active")
    MailElement messageUnread();

    @Name("Аватар - чекбокс")
    @FindByCss(".mail-MessageSnippet-Checkbox")
    MailElement avatarAndCheckBox();

    @Name("Аватар не объединенный с чекбоксом")
    @FindByCss(".mail-User-Avatar[href*='search']")
    MailElement avatarImg();

    @Name("Отправитель")
    @FindByCss(".js-message-snippet-sender .mail-MessageSnippet-FromText")
    MailElement sender();

    @Name("Метка письма")
    @FindByCss(".mail-Label")
    ElementsCollection<MailElement> labels();

    @Name("Префикс")
    @FindByCss(".mail-MessageSnippet-Item_prefix")
    MailElement prefix();

    @Name("Тема письма")
    @FindByCss(".mail-MessageSnippet-Item_subject")
    MailElement subject();

    @Name("Счетчик треда")
    @FindByCss(".js-thread-toggle")
    MailElement threadCounter();

    @Name("Первая строка письма")
    @FindByCss(".mail-MessageSnippet-Item_firstline")
    MailElement firstLine();

    @Name("Скрепка")
    @FindByCss(".mail-MessageSnippet-Item_attachment")
    MailElement paperClip();

    @Name("Виджет с аттачами в списке писем")
    @FindByCss(".ns-view-attachments-widget")
    AttachmentsWidgetInMessageList attachments();

    @Name("Дата письма")
    @FindByCss(".mail-MessageSnippet-Item_dateText")
    MailElement date();

    @Name("Метка «Главное»")
    @FindByCss(".mail-MessageSnippet-Item_priority:not(.is-active)")
    MailElement priorityMark();

    @Name("Активная метка «Главное»")
    @FindByCss(".mail-MessageSnippet-Item_priority")
    MailElement priorityMarkActive();

    @Name("Значок «Переслано»")
    @FindByCss(".mail-MessageSnippet-Item_status_forwarded")
    MailElement forwardedArrow();

    @Name("Папка письма")
    @FindByCss(".mail-MessageSnippet-Item_folder")
    MailElement folder();

    @Name("Виджеты в письмах")
    @FindByCss(".ns-view-messages-item-widget")
    WidgetBlock widget();

    @Name("Виджеты в письмах")
    @FindByCss(".ns-view-messages-item-ticket")
    WidgetBlock widgetTicket();

    @Name("Письма развернутого треда")
    @FindByCss(".ns-view-messages-item-inner .ns-view-messages-item-wrap")
    ElementsCollection<MessageBlock> expandedThreadMessageList();

    @Name("Кнопка «Посмотреть встречу»")
    @FindByCss(".mail-MessageSnippet-WidgetEvents_actions_button")
    MailElement icsButton();
}


