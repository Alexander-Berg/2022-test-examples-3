package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.abook;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface BlockSetupAbook extends MailElement {

    @Name("Блок «Адреса & Отображение в письмах»")
    @FindByCss(".b-setup__form")
    AbookViewAndExportBlock importExportView();

    @Name("Блок «Группы контактов»")
    @FindByCss(".b-setup__inner:not([class*='submit'])")
    AbookGroupsBlock groupsManage();
}