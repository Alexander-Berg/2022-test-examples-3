package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 13.09.12
 * Time: 14:14
 */

public interface FilterCreateConditionsBlock extends MailElement {

    @Name("Блок создания условия")
    @FindByCss(".b-form-layout__field_conditions [class='b-form-layout__line b-form-layout__line_condition']")
    ElementsCollection<FilterConditionBlock> conditionsList();

    @Name("Кнопка выбора логики (хотя бы одно/все одновременно)")
    @FindByCss(".b-form-layout__line_show-after-clone .nb-button")
    MailElement selectLogicButton();

    @Name("Кнопка добавления условия")
    @FindByCss(".js-filters-condition-add")
    MailElement addConditionButton();

    @Name("Нотифайка о валидации поля")
    @FindByCss("[class*='b-notification_error-empty-field'] .b-notification__i")
    MailElement emptyFieldNotification();
}
