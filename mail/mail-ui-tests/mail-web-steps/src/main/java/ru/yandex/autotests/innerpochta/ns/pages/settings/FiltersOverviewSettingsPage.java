package ru.yandex.autotests.innerpochta.ns.pages.settings;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.AlreadyCreatedFilterBlock;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.BlockSetupFiltersConfirm;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.SettingsPageCreateSimpleFilterBlock;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.popup.DeleteFilterPopUpBlock;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.popup.HeaderForFilterPopUpBlock;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.popup.NewFilterPopUpBlock;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.popup.PasswordConfirmationBlock;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup.NewFolderPopUp;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.folderslabels.popup.NewLabelPopUp;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 13.09.12
 * Time: 12:55
 */

public interface FiltersOverviewSettingsPage extends MailPage {

    @Name("Кнопка «Создать правило»")
    @FindByCss(".js-filters-create")
    MailElement createNewFilterButton();

    @Name("Ссылка на создание простого фильтра для перемещения писем")
    @FindByCss(".b-messages__placeholder.b-messages_nolabel")
    MailElement messageAboutNewFilterForLabel();

    @Name("Ссылка на создание простого фильтра для перемещения писем")
    @FindByCss(".b-filters__quick-filter:nth-of-type(1)>a")
    MailElement createSimpleFilterForMoving();

    @Name("Ссылка на создание простого фильтра для метки")
    @FindByCss(".b-filters__quick-filter:nth-of-type(2)>a")
    MailElement createSimpleFilterForLabeling();

    @Name("Ссылка на создание простого фильтра для удаления")
    @FindByCss(".b-filters__quick-filter:nth-of-type(3)>a")
    MailElement createSimpleFilterForDeleting();

    @Name("Созданные правила")
    @FindByCss(".js-filters-list-item")
    ElementsCollection<AlreadyCreatedFilterBlock> createdFilterBlocks();

    @Name("Попап «Название заголовка»")
    @FindByCss(".b-popup__box")
    HeaderForFilterPopUpBlock headerForFilterPopUpBlock();

    @Name("Блок создания простого фильтра")
    @FindByCss(".b-form-layout_filters-simple .b-form-layout__block")
    SettingsPageCreateSimpleFilterBlock createSimpleFilter();

    @Name("Окно создания новой папки")
    @FindByCss(".b-popup:not(.g-hidden)")
    NewFolderPopUp newFolderPopUp();

    @Name("Попап «Введите пароль от вашего ящика»")
    @FindByCss(".b-popup__box")
    PasswordConfirmationBlock passwordConfirmationBlock();

    @Name("Окно создания новой метки")
    @FindByCss(".b-popup:not(.g-hidden)")
    NewLabelPopUp newLabelPopUp();

    @Name("Окно создания правила для новой метки")
    @FindByCss(".b-popup__box__content")
    NewFilterPopUpBlock newFilterPopUp();

    @Name("Окно удаления фильтра")
    @FindByCss(".b-popup__box__content")
    DeleteFilterPopUpBlock deleteFilterPopUp();

    @Name("Подтверждение правила (Страничка с кнопкой о включении фильтра)")
    @FindByCss(".forward-confirm-content")
    BlockSetupFiltersConfirm blockConfirmFilter();

    @Name("Ссылка на конструктор правил")
    @FindByCss("a[href='#setup/filters-create']")
    MailElement filtersBuilder();
}
