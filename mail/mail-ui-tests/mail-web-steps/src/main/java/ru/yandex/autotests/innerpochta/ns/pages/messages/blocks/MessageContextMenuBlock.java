package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface MessageContextMenuBlock extends MailElement {

    @Name("Удалить письмо")
    @FindByCss(".svgicon-mail--MainToolbar-Delete")
    MailElement deleteMsg();

    @Name("Удалить папку/метку")
    @FindByCss(".qa-LeftColumn-ContextMenu-DeleteItem")
    MailElement deleteItem();

    @Name("Ответить")
    @FindByCss(".svgicon-mail--MainToolbar-Reply")
    MailElement reply();

    @Name("Ответить всем")
    @FindByCss(".svgicon-mail--MainToolbar-Replyall")
    MailElement replyAll();

    @Name("Переслать")
    @FindByCss(".svgicon-mail--MainToolbar-Forward")
    MailElement forward();

    @Name("Не прочитано")
    @FindByCss(".svgicon-mail--MainToolbar-Unread")
    MailElement unread();

    @Name("Прочитано")
    @FindByCss(".svgicon-mail--MainToolbar-Read")
    MailElement read();

    @Name("Спам")
    @FindByCss(".svgicon-mail--MainToolbar-Spam")
    MailElement markSpam();

    @Name("Не спам")
    @FindByCss(".svgicon-mail--MainToolbar-Unspam")
    MailElement unSpam();

    @Name("Архив")
    @FindByCss(".svgicon-mail--MainToolbar-Archive")
    MailElement archive();

    @Name("Список элементов")
    @FindByCss("[class*='qa-LeftColumn-ContextMenu-']")
    ElementsCollection<MailElement> itemList();

    @Name("Список элементов в старом контекстном меню")
    @FindByCss(".js-item")
    ElementsCollection<MailElement> itemListInMsgList();

    @Name("Выбранный элемент")
    @FindByCss(".js-item.is-current")
    MailElement currentItem();

    @Name("Переложить в папку")
    @FindByCss(".svgicon-mail--MainToolbar-Folder")
    MailElement moveToFolder();

    @Name("Создать новую папку/метку")
    @FindByCss(".qa-LeftColumn-ContextMenu-CreateNewItem")
    MailElement createFolderLabel();

    @Name("Поставить метку")
    @FindByCss(".svgicon-mail--MainToolbar-Tag")
    MailElement markWithLabel();

    @Name("Первая метка в разделе Снять метку")
    @FindByCss(".is-inactive + .js-item")
    MailElement removeLabel();

    @Name("Очистить папку")
    @FindByCss(".qa-LeftColumn-ContextMenu-ClearFolder")
    MailElement cleanFolder();

    @Name("Редактировать сборщик")
    @FindByCss(".svgicon-mail--Settings")
    MailElement setupCollector();

    @Name("Дописать черновик")
    @FindByCss(".svgicon-mail--MainToolbar-Compose")
    MailElement updateDraft();

    @Name("Создать шаблон")
    @FindByCss(".svgicon-mail--MainToolbar-AddTemplate")
    MailElement addTemplate();

    @Name("Пометить все письма в папке прочитанными")
    @FindByCss(".qa-LeftColumn-ContextMenu-MarkRead")
    MailElement markAllRead();

}