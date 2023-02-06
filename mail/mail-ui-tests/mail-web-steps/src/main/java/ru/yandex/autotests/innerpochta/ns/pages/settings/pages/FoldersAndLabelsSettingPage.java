package ru.yandex.autotests.innerpochta.ns.pages.settings.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.BlockSetupFoldersAndLabels;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup.CleanFolderPopUp;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup.DeleteFolderPopUp;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup.DeleteFolderPopUpOld;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup.DeleteLabelPopUp;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup.NewFolderPopUp;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup.NewLabelPopUp;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 04.09.12
 * Time: 15:45
 * To change this template use File | settings | File Templates.
 */
public interface FoldersAndLabelsSettingPage extends MailPage {

    @Name("Все настройки → Папки и метки")
    @FindByCss(".ns-view-setup-folders")
    BlockSetupFoldersAndLabels setupBlock();

    @Name("Попап «Создаём папку»")
    @FindByCss(".b-popup:not(.g-hidden)")
    NewFolderPopUp newFolderPopUp();

    @Name("Попап «Создаём метку»")
    @FindByCss(".b-popup:not(.g-hidden)")
    NewLabelPopUp newLabelPopUp();

    @Name("Попап «Удаление папки»")
    @FindByCss(".qa-LeftColumn-ConfirmDeleteFolder")
    DeleteFolderPopUp deleteFolderPopUp();

    @Name("[old] Попап «Удаление папки»")
    @FindByCss(".b-popup:not(.g-hidden)")
    DeleteFolderPopUpOld deleteFolderPopUpOld();

    @Name("Попап «Удаление метки»")
    @FindByCss(".qa-LeftColumn-ConfirmDeleteLabel")
    DeleteLabelPopUp deleteLabelPopUp();

    @Name("Попап «Удаление метки»")
    @FindByCss(".b-popup")
    DeleteLabelPopUp deleteLabelPopUpOld();

    @Name("Попап «Очистка папки»")
    @FindByCss(".b-popup:not(.g-hidden)")
    CleanFolderPopUp cleanFolderPopUp();

    @Name("Выпадушка «Вложить в другую папку»")
    @FindByCss(".b-mail-dropdown__box .b-mail-dropdown__box__content")
    MailElement putInFolderSelect();

    @Name("Кнопка для открытия формы простого фильтра")
    @FindByCss(".b-popup .js-filter-open")
    MailElement openFilterLink();

    @Name("Форма создания простого фильтра")
    @FindByCss(".b-popup .b-form-layout_filters-simple")
    MailElement simpleFilter();
}
