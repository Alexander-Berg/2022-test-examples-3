package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface AlreadyCreatedFilterBlock extends MailElement {

    @Name("Имя правила")
    @FindByCss(".b-filters-filter__action")
    MailElement filterName();

    @Name("Выключить правило")
    @FindByCss(".b-switch-off-img")
    MailElement switchOff();

    @Name("Включить правило")
    @FindByCss(".b-switch-on-img")
    MailElement switchOn();

    @Name("Фильтр ожидает подтверждения")
    @FindByCss(".b-filters-filter_disabled")
    MailElement filterIsDisabled();

    @Name("Изменить правило")
    @FindByCss(".b-filters-filter__controls-link.b-filters-filter__controls-link_edit")
    MailElement refactorFilter();

    @Name("Условие")
    @FindByCss(".b-filters-filter__conditions")
    MailElement сonditionContent();

    @Name("Действия")
    @FindByCss(".b-filters-filter__action")
    ElementsCollection<MailElement> conditionActionsList();

    @Name("Удаление правила")
    @FindByCss(".js-filters-delete")
    MailElement deleteFilterButton();
}