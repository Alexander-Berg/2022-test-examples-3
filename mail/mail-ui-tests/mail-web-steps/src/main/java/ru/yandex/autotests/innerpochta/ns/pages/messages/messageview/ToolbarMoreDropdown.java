package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by cosmopanda on 28.04.2016.
 */
public interface ToolbarMoreDropdown extends MailElement {

    @Name("Создать правило")
    @FindByCss("[data-id='filters-create']")
    MailElement createFilter();

    @Name("Свойства письма")
    @FindByCss("[data-id='message-source']")
    MailElement messageInfo();

    @Name("Перевести")
    @FindByCss("[data-id='translate']")
    MailElement translateBtn();

    @Name("Распечатать")
    @FindByCss("[data-id='print']")
    MailElement printBtn();
}
