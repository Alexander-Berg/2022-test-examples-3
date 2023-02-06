package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 18.09.12
 * Time: 17:08
 */

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.elements.CreatedFoldersListBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockFolders extends MailElement {

    @Name("Пользовательские подпапки для папки «Входящие»")
    @FindByCss(".b-folders.b-folder-list_user.b-folders__nesting:nth-of-type(2)")
    CreatedFoldersListBlock blockCreatedFolders();

    @Name("Счетчик справа от «Входящие»")
    @FindByCss(".b-manager__left-i>div>div:nth-of-type(1) .b-folders__folder__counters__total")
    MailElement inboxFolderCounter();

    @Name("Счетчик справа от «Отправленные»")
    @FindByCss(".b-manager__left-i>div>div:nth-of-type(1)+div+div .b-folders__folder__counters__total")
    MailElement sentFolder();

    @Name("Кнопка «Новая папка»")
    @FindByCss("[id = 'setup-folders-add']")
    MailElement newFolderButton();

    @Name("Кнопка «Вложенная папка»")
    @FindByCss("[id = 'setup-folders-subfolder']")
    MailElement createSubFolderButton();

    @Name("Кнопка для папки «Переименовать»")
    @FindByCss("[id = 'setup-folders-edit']")
    MailElement renameFolderButton();

    @Name("Кнопка для папки «Очистить»")
    @FindByCss("[id = 'setup-folders-empty']")
    MailElement clearCustomFolder();

    @Name("Кнопка для папки «Удалить»")
    @FindByCss("[id = 'setup-folders-delete']")
    MailElement deleteCustomFolder();

    @Name("Кнопка для папки «Создать правило»")
    @FindByCss("[id = 'setup-folders-filter']")
    MailElement createFilterButton();

    @Name("Активная кнопка для папки «Создать правило»")
    @FindByCss("#setup-folders-filter:not([disabled])")
    MailElement activeCreateFilterButton();

    @Name("Ссылка «пометить все письма в папке 'как прочитанные'»")
    @FindByCss("[class='b-pseudo-link']")
    MailElement markAsReadAllMail();

    @Name("Список всех папок")
    @FindByCss(".js-setup-folders-list .b-folders__folder__link")
    ElementsCollection<MailElement> foldersNames();
}
