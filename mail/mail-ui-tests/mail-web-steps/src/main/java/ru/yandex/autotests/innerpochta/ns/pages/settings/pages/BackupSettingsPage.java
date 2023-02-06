package ru.yandex.autotests.innerpochta.ns.pages.settings.pages;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BackupSettingsPage extends MailPage {

    @Name("Тогглер «Выбрать папки для резервной копии»")
    @FindByCss("[class*=BackupSettings__toggler]")
    MailElement foldersToggler();

    @Name("Блок настройки Енота")
    @FindByCss("[class*=BackupRoot__section]")
    MailElement enotBlock();

    @Name("Кнопка «Создать резервную копию»")
    @FindByCss("[class*=BackupCreate__action]")
    MailElement createBtn();

    @Name("Блок выбора папок")
    @FindByCss("[class*=BackupFolders__scrollRoot]")
    MailElement foldersBlock();

    @Name("Информация о предыдущей копии")
    @FindByCss(".qa-BackupRestore-State")
    MailElement lastCopyInfo();

    @Name("Тизер резервной копии")
    @FindByCss("[class*=BackupNewTeaser__root]")
    MailElement backupTeaser();

    @Name("Кнопка «Подключить»")
    @FindByCss("[class*=BackupNewTeaser__button]")
    MailElement subscribeBtn();

    @Name("Кнопка «Узнать больше»")
    @FindByCss("[class*=BackupNewTeaser__bottomNavLink]")
    MailElement learnMoreBtn();

    @Name("Кнопка подтверждения в попапе")
    @FindByCss("[class*=Modal__button]")
    MailElement applyActionBtn();

    @Name("Баннер процесса создания бэкапа")
    @FindByCss("[class*=BackupInProgressAlert__root]")
    MailElement backupInProgressBanner();

    @Name("Кнопка «Удалить резервную копию»")
    @FindByCss("[class*=BackupData__deleteButton]")
    MailElement deleteBtn();

    @Name("Кнопка «Восстановить письма»")
    @FindByCss("[class*=BackupData__restoreButton]")
    MailElement restoreBtn();

    @Name("Опция восстановления")
    @FindByCss("[class*=ConfirmRestoreModal__radio--]")
    ElementsCollection<MailElement> restoreOptionsBtn();

    @Name("Баннер процесса восстановления бэкапа")
    @FindByCss("[class*=BackupRestoreAlert__root]")
    MailElement backupRestoreBanner();

    @Name("Тумблер включения Енота")
    @FindByCss("[class*=HiddenTrash__tumbler]")
    MailElement enotControl();

    @Name("Кнопка «Посмотреть удаленные письма»")
    @FindByCss("[class*=NavLink__root]")
    MailElement hiddenLettersView();

    @Name("Кнопка подтверждения отключения енота")
    @FindByCss(".Button2_view_danger")
    MailElement enotDisableApplyBtn();

    @Name("Папки для резервной копии")
    @FindByCss("[class*=BackupTreeNode__folderRow]")
    ElementsCollection<MailElement> folders();

    @Name("Кнопка подтверждения выбора папок")
    @FindByCss("[class*=BackupFolders__applyButton]")
    MailElement foldersApplyBtn();

    @Name("Чекбоксы выбора папок для резервной копии")
    @FindByCss("[class*=BackupTreeNode__checkbox]")
    ElementsCollection<MailElement> foldersCheckBoxes();
}
