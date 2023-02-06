package ru.yandex.autotests.innerpochta.ns.pages.messages.custombuttonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface CustomButtonsCreateButtonBlock extends MailElement {

    @Name("Кнопка для архивации")
    @FindByCss("[data-params='button=archive']:not(.b-mail-button_filter-selected)")
    MailElement archive();

    @Name("Кнопка для архивации (активная)")
    @FindByCss("[data-params='button=archive'].b-mail-button_filter-selected")
    MailElement archiveActive();

    @Name("Кнопка отмены")
    @FindByCss("[data-click-action='dialog.cancel']")
    MailElement cancel();

    @Name("Кнопка «Готово/Сохранить изменения»")
    @FindByCss(".js-toolbar-settings-apply")
    MailElement saveChangesButton();

    @Name("Кнопка для переадресации")
    @FindByCss("[data-params='button=sendon']:not(.b-mail-button_filter-selected)")
    MailElement forward();

    @Name("Кнопка для переадресации")
    @FindByCss("[data-params='button=sendon'].b-mail-button_filter-selected")
    MailElement deleteForwardButton();

    @Name("Кнопка для перемещения в папку")
    @FindByCss(".js-toolbar-item-title-infolder:not(.b-mail-button_filter-selected)")
    MailElement moveToFolder();

    @Name("Кнопка для перемещения в папку")
    @FindByCss("[data-params='button=infolder'].b-mail-button_filter-selected")
    MailElement deleteMoveToFolderButton();

    @Name("Кнопка для установки метки")
    @FindByCss("[data-params='button=label']:not(.b-mail-button_filter-selected)")
    MailElement label();

    @Name("Кнопка для установки метки")
    @FindByCss("[data-params='button=label'].b-mail-button_filter-selected")
    MailElement deleteLabelButton();

    @Name("Кнопка для автоответа")
    @FindByCss("[data-params='button=template']:not(.b-mail-button_filter-selected)")
    MailElement autoReply();

    @Name("Кнопка для автоответа (активная)")
    @FindByCss("[data-params='button=template'].b-mail-button_filter-selected")
    MailElement activeAutoReplyBtn();
}

