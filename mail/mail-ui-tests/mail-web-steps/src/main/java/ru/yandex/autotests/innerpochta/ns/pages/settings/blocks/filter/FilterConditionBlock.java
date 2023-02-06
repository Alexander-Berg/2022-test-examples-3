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

public interface FilterConditionBlock extends MailElement {

    @Name("Кнопка выбора первого условия")
    @FindByCss(".js-condition-field-field1")
    MailElement firstConditionDropDown();

    @Name("Кнопка выбора второго условия")
    @FindByCss(".nb-with-xs-right-gap")
    MailElement secondConditionDropDown();

    @Name("Инпут ввода текста условия")
    @FindByCss(".js-condition-field-field3")
    MailElement inputCondition();

    @Name("Кнопка удаления условия")
    @FindByCss(".js-filters-condition-delete")
    MailElement deleteConditionButton();
}
