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

public interface FilterApplyConditionBlock extends MailElement {

    @Name("Выпадушка выбора писем касательно спам/не спам (первый)")
    @FindByCss(".nb-select.js-letter_type_select")
    MailElement letterTypeConditionDropdown();

    @Name("Выпадушка выбора писем касательно с аттачами/без аттачей (второй)")
    @FindByCss(".nb-select:not(.js-letter_type_select)")
    MailElement withAttachConditionDropdown();
}
