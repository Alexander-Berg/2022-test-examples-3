package ru.yandex.autotests.innerpochta.ns.pages.settings.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.BlockBlackList;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.BlockSetupFiltersCreate;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter.BlockWhiteList;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 04.09.12
 * Time: 15:45
 * To change this template use File | settings | File Templates.
 */
public interface FiltersCreationSettingsPage extends MailPage {

    @Name("Правила обработки входящей почты → Создать правило")
    @FindByCss(".ns-view-setup-filters-create")
    BlockSetupFiltersCreate setupFiltersCreate();

    @Name("Черный список")
    @FindByCss(".b-filters-list_black")
    BlockBlackList blackListBlock();

    @Name("Белый список")
    @FindByCss(".b-filters-list_white")
    BlockWhiteList whiteListBlock();
}