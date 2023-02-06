package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter;

import io.qameta.atlas.webdriver.extension.Name;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 13.09.12
 * Time: 14:14
 */

public interface FilterActionsSelectBlock extends MailElement {

    @Name("Имя выбранной папки")
    @FindByCss(".js-move_folder")
    MailElement selectFolderDropdown();

    @Name("Имя выбранной метки")
    @FindByCss(".js-labels-select")
    MailElement selectLabelDropdown();

    @Name("Удалить письмо")
    @FindByCss("[value='delete']")
    MailElement deleteCheckBox();

    @Name("Пометить прочитанным")
    @FindByCss("[dependence-parent-id='movel_2'] input")
    MailElement markAsReadCheckBox();

    @Name("Положить в папку")
    @FindByCss("[value='move']")
    MailElement moveToFolderCheckBox();

    @Name("Поставить метку")
    @FindByCss("[dependence-child-id='delete'] [value='movel']")
    MailElement markAsCheckBox();
}
