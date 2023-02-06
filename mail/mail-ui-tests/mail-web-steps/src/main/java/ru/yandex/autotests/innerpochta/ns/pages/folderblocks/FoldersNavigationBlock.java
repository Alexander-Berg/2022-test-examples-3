package ru.yandex.autotests.innerpochta.ns.pages.folderblocks;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface FoldersNavigationBlock extends MailElement {

    @Name("Пользовательские папки")
    @FindByCss(".qa-LeftColumn-Folder")
    ElementsCollection<CustomFolderBlock> customFolders(); //TODO: нужен отдельный селектор

    @Name("Пользовательские папки при включенных табах")
    @FindByCss(".qa-LeftColumn-Folder")
    ElementsCollection<CustomFolderBlock> customUserFolders(); //TODO: нужен селектор

    @Name("Список всех папок")
    @FindByCss(".qa-LeftColumn-Folder")
    ElementsCollection<MailElement> allFolders();

    @Name("Открытая папка")
    @FindByCss(".qa-LeftColumn-Folder_selected")
    MailElement currentFolder();

    @Name("Отправленные")
    @FindByCss("a[href='#sent']")
    MailElement sentFolder();

    @Name("Папка «Шаблоны»")
    @FindByCss("a[href='#template']")
    MailElement templatesFolder();

    @Name("Папка «Шаблоны»")
    @FindByCss("a[href='#draft']")
    MailElement draftFolder();

    @Name("Раскрыть вложенные папки (Стрелка внутри рядом с папкой)")
    @FindByCss(".qa-LeftColumn-FolderExpander:not(.qa-LeftColumn-FolderExpander_expanded)")
    ElementsCollection<MailElement> expandFoldersList();

    @Name("Свернуть вложенные папки (Стрелка внутри рядом с папкой)")
    @FindByCss(".qa-LeftColumn-FolderExpander_expanded")
    ElementsCollection<MailElement> collapseFoldersList();

    @Name("Архив")
    @FindByCss("[href='#archive']")
    MailElement archiveFolder();

    @Name("Счетчик папки Отправленные")
    @FindByCss(".mail-NestedList-Item[href='#sent'] .mail-NestedList-Item-Info span")
    MailElement sentFolderCounter();

    @Name("Счетчик пользовательской папки")
    @FindByCss(".qa-LeftColumn-CountersUnread")
    MailElement customFolderCounter();

    @Name("Входящие «только новые»")
    @FindByCss("[href='#inbox?extra_cond=only_new']")
    MailElement inboxFolder();

    @Name("Входящие")
    @FindByCss("[href='#inbox']")
    MailElement inboxFolderLink();

    @Name("Входящие ВСЕ")
    @FindByCss("[data-react-focusable-id='1'][data-react-focusable='folder'] .qa-LeftColumn-FolderText")
    MailElement inbox();

    @Name("Папка «Удаленные»")
    @FindByCss("[href='#trash']")
    MailElement trashFolder();

    @Name("Папка «Исходящие»")
    @FindByCss("[href='#outbox']")
    MailElement outboxFolder();

    @Name("Очистить папку «Удаленные»")
    @FindByCss(".qa-LeftColumn-Folder_selected .qa-LeftColumn-ClearControl")
    MailElement cleanTrashFolder();

    @Name("Счетчик выделенной папки")
    @FindByCss(".qa-LeftColumn-Folder_selected .qa-LeftColumn-CountersTotal")
    MailElement selectedFolderCounter();

    @Name("Счетчик папки шаблоны")
    @FindByCss("[href='#template']  .mail-NestedList-Item-Info-Extras")
    MailElement templatesFolderCounter();

    @Name("Общий счетчик писем папки")
    @FindByCss(".qa-LeftColumn-CountersTotal")
    ElementsCollection<MailElement> folderTotalCounter();

    @Name("Счетчик непрочитанных писем папки")
    @FindByCss("[href='#inbox?extra_cond=only_new']")
    MailElement inboxUnreadCounter();

    @Name("Папка «Спам»")
    @FindByCss("[href='#spam']")
    MailElement spamFolder();

    @Name("Счетчик папки «Спам»")
    @FindByCss("[href='#spam'] .mail-NestedList-Item-Info-Extras")
    MailElement spamFolderCounter();

    @Name("Счетчик папки «Удаленные»")
    @FindByCss("[href='#trash'] .mail-NestedList-Item-Info-Extras")
    MailElement trashFolderCounter();

    @Name("Раскрыть список подпапок Входящих")
    @FindByCss(".qa-LeftColumn-FolderExpander:not(.qa-LeftColumn-FolderExpander_expanded) ")
    MailElement expandInboxFolders();

    @Name("Свернуть список подпапок Входящих")
    @FindByCss(".qa-LeftColumn-FolderExpander.qa-LeftColumn-FolderExpander_expanded")
    MailElement hideInboxThread();

    @Name("Прыщ непрочитанности")
    @FindByCss(".qa-LeftColumn-UnreadControl")
    MailElement markReadIcon();

    @Name("Очистить папку «Спам»")
    @FindByCss(".qa-LeftColumn-Folder_selected .qa-LeftColumn-ClearControl")
    MailElement cleanSpamFolder();

    @Name("Папка «Напомнить позже»")
    @FindByCss("[href='#reply_later']")
    MailElement replyLaterFolder();
}
