package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * Created by mabelpines
 */
public interface LayoutSwitchDropdown extends MailElement {

    @Name("Чекбокс «Группировать по теме»")
    @FindByCss(".js-threaded-switch ._nb-checkbox-input")
    MailElement thredSwitchCheckBox();

    @Name("Чекбокс «Крупный шрифт»")
    @FindByCss(".js-bigger-text-switch input")
    MailElement biggerTextSwitch();

    @Name("Чекбокс «Компактные письма»")
    @FindByCss(".js-compact-mode-switch input")
    MailElement compactModeSwitch();

    @Name("Чекбокс «Компактная шапка»")
    @FindByCss(".js-compact-header-mode-switch input")
    MailElement compactHeaderSwitch();

    @Name("Чекбокс «Компактная левая колонка»")
    @FindByCss(".js-compact-left-column-switch input")
    MailElement compactLeftColumnSwitch();
}
