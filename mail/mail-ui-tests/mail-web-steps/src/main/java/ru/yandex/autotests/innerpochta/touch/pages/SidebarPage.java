package ru.yandex.autotests.innerpochta.touch.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

import ru.yandex.autotests.innerpochta.touch.pages.blocks.ClearFolderPopup;
import ru.yandex.autotests.innerpochta.touch.pages.folderlistblocks.FolderBlock;
import ru.yandex.autotests.innerpochta.touch.pages.folderlistblocks.LabelsBlockSidebar;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author puffyfloof
 */

public interface SidebarPage extends MailPage {

    @Name("Кнопка «+»")
    @FindByCss(".multiauthorization-account-add")
    MailElement plusButton();

    @Name("e-mail активного пользователя")
    @FindByCss(".multiauthorization-account-email")
    MailElement userEmail();

    @Name("Неактивный МА пользователь")
    @FindByCss(".multiauthorization-account")
    ElementsCollection<MailElement> inactiveMAAccount();

    @Name("Аватарка пользователя")
    @FindByCss(".multiauthorization-account-avatar")
    MailElement sidebarAvatar();

    @Name("Блок папки")
    @FindByCss(".leftPanel-item")
    ElementsCollection<FolderBlock> folderBlocks();

    @Name("Футер списка папок")
    @FindByCss(".leftPanel-footer")
    MailElement leftPanelFooter();

    @Name("Кнопка «Полная версия»")
    @FindByCss(".leftPanel-footer span:nth-child(1)")
    MailElement fullVersion();

    @Name("Левая колонка")
    @FindByCss(".leftPanel-wrapper")
    MailElement leftPanelBox();

    @Name("Стрелочка раскрытия подпапок")
    @FindByCss(".leftPanelItem-right .ico_arrow-mini")
    ElementsCollection<MailElement> subfoldertoggler();

    @Name("Желтый каунтер непрочитанных рядом с папкой")
    @FindByCss(".leftPanelItem-counter.recent")
    MailElement recentToggler();

    @Name("Блок кнопок «Помощь» и «Обратную связь»")
    @FindByCss(".leftPanel-item")
    ElementsCollection<MailElement> leftPanelItems();

    @Name("Метёлка очистки папок спама и удалённых")
    @FindByCss(".leftPanelItem-right .folder-clear")
    MailElement clearToggler();

    @Name("Попап очистки папки")
    @FindByCss(".confirm-popup")
    ClearFolderPopup clearFolderPopup();

    @Name("Прыщик на аватарке в карусели пользователей")
    @FindByCss(".multiauthorization-account-fresh")
    MailElement userToggler();

    @Name("Блок с табами в левой колонке")
    @FindByCss(".leftPanel-group_tabs")
    MailElement tabsBlock();

    @Name("Список табов")
    @FindByCss(".leftPanel-group_tabs .leftPanelItem-link")
    ElementsCollection<MailElement> tabs();

    @Name("Промо в сайдбаре")
    @FindByCss(".sidebarPromo")
    MailElement sidebarPromo();

    @Name("Блок пользовательских меток")
    @FindByCss(".leftPanel-group_labels")
    LabelsBlockSidebar labelsBlockSidebar();

    @Name("Каунтер непрочитанных")
    @FindByCss(".leftPanelItem-counter")
    MailElement counter();

    @Name("Блок системных меток")
    @FindByCss(".leftPanel-group_system")
    ElementsCollection<FolderBlock> systemLabelsBlock();

    @Name("Папка «Входящие»")
    @FindByCss(".leftPanel-item.fid-1")
    FolderBlock inboxFolder();

    @Name("Папка «Отправленные»")
    @FindByCss(".leftPanel-item.fid-4")
    FolderBlock sentFolder();
}
