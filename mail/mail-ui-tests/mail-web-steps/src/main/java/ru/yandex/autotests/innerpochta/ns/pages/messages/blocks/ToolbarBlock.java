package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface ToolbarBlock extends MailElement {

    @Name("Кнопка «В папку»")
    @FindByCss(".ns-view-toolbar-button-folders-actions")
    MailElement moveMessageBtn();

    @Name("Кнопка «Архив»")
    @FindByCss(".js-toolbar-item-archive:not(.is-hidden)")
    MailElement archiveButton();

    @Name("Кнопка «Удалить»")
    @FindByCss(".js-toolbar-item-delete:not(.is-hidden)")
    MailElement deleteButton();

    @Name("Кнопка «Спам»")
    @FindByCss(".js-toolbar-item-spam:not(.is-hidden)")
    MailElement spamButton();

    @Name("Кнопка «Не спам»")
    @FindByCss(".js-toolbar-item-not-spam:not(.is-hidden)")
    MailElement notSpamButton();

    @Name("Кнопка «Отметить прочитанным»")
    @FindByCss(".js-toolbar-item-mark-as-read:not(.b-toolbar__item_disabled)")
    MailElement markAsReadButton();

    @Name("Кнопка «Переслать»")
    @FindByCss(".js-toolbar-item-forward:not(.is-hidden)")
    MailElement forwardButton();

    @Name("Кнопка создания шаблона")
    @FindByCss(".js-toolbar-item-add-template")
    MailElement createTemplateButton();

    @Name("Шестеренка редактирования пользовательских кнопок")
    @FindByCss(".js-toolbar-item-toolbar-settings")
    MailElement configureCustomButtons();

    @Name("Добавить пользовательскую кнопку")
    @FindByCss(".ns-view-toolbar-button-add-button:not(.g-hidden) .js-toolbar-item-add-button")
    MailElement addCustomButton();

    @Name("Иконка на кнопке для переадресации")
    @FindByCss(".js-toolbar-item-sendon:not(.is-hidden) .mail-ui-Icon")
    MailElement sendOnButtonIcon();

    @Name("Иконка на кнопке удалить письмо")
    @FindByCss(".js-toolbar-item-delete.mail-ui-Icon")
    MailElement deleteButtonIcon();

    @Name("Кнопка для переадресации")
    @FindByCss(".js-toolbar-item-sendon:not(.is-hidden)")
    MailElement sendOnButton();

    @Name("Кнопка «Переслать» на странице просмотра письма")
    @FindByCss(".js-toolbar-item-forward")
    MailElement forwardMessageButton();

    @Name("Кнопка «Отметить непрочитанным»")
    @FindByCss(".js-toolbar-item-mark-as-unread:not(.is-hidden)")
    MailElement markAsUnreadButton();

    @Name("Кнопка “Ответить всем“")
    @FindByCss(".js-toolbar-item-reply-all:not(.b-toolbar__item_disabled)")
    MailElement replyToAllButton();

    @Name("Кнопка “Ответить“")
    @FindByCss(".js-toolbar-item-reply:not(.b-toolbar__item_disabled)")
    MailElement replyButton();

    @Name("Иконка пользовательской кнопки для установки метки")
    @FindByCss(".ns-view-toolbar-button-label:not(.is-hidden) .mail-ui-Icon")
    MailElement autoLabelButtonIcon();

    @Name("Иконка пользовательской кнопки для перемещения в папку")
    @FindByCss(".js-toolbar-item-infolder:not(.is-hidden) .mail-ui-Icon")
    MailElement autoMoveButtonIcon();

    @Name("Пользовательская кнопка для перемещения в папку")
    @FindByCss(".js-toolbar-item-infolder:not(.is-hidden)")
    MailElement autoMoveButton();

    @Name("Пользовательская кнопка для установки метки")
    @FindByCss(".js-toolbar-item-label:not(.is-hidden)")
    MailElement autoLabelButton();

    @Name("Пользовательская кнопка автоответа")
    @FindByCss(".js-toolbar-item-template:not(.is-hidden)")
    MailElement autoReplyButton();

    @Name("Иконка на кнопке для автоответа")
    @FindByCss(".js-toolbar-item-template:not(.is-hidden) .mail-ui-Icon")
    MailElement autoReplyButtonIcon();

    @Name("Выбор метки")
    @FindByCss(".js-toolbar-item-labels-actions:not(.is-hidden)")
    MailElement markMessageDropDown();

    @Name("Ссылка в тулбаре «Переложить в папку» (2pane)")
    @FindByCss(".js-toolbar-item-folders-actions:not(.is-hidden) .mail-ui-Icon")
    MailElement moveMessageDropDown();

    @Name("Кнопка вызова выпадушки  “Вид")
    @FindByCss(".js-content-toolbar-layout-switch")
    MailElement layoutSwitchBtn();

    @Name("Кнопка-чекбокс “Выделить все письма на странице“")
    @FindByCss(".ns-view-toolbar-button-main-select-all")
    MailElement selectAllMessages();

    @Name("Кнопка “Еще“")
    @FindByCss(".js-toolbar-item-more")
    MailElement moreBtn();

    @Name("Кнопка “Изменить“ для черновика")
    @FindByCss(".ns-view-toolbar-button-edit")
    MailElement editDraftBtn();

    @Name("Кнопка “Закрепить“")
    @FindByCss(".js-toolbar-item-pin:not(.is-hidden)")
    MailElement pinBtn();

    @Name("Кнопка «Наверх»")
    @FindByCss(".js-toolbar-button-top")
    MailElement topBtn();

    @Name("Кнопка «Отписаться»")
    @FindByCss(".ns-view-toolbar-button-unsubscribe-one")
    MailElement unsubscribeButton();

    @Name("Иконка на кнопке «Отписаться»")
    @FindByCss(".ns-view-toolbar-button-unsubscribe-one .mail-ui-Icon")
    MailElement unsubscribeButtonIcon();

    @Name("Кнопка «Напомнить позже»")
    @FindByCss(".js-toolbar-item-reply-later:not(.is-disabled)")
    MailElement replyLaterBtn();
}



