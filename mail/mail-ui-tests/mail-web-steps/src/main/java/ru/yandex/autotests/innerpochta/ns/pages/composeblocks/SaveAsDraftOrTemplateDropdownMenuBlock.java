package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface SaveAsDraftOrTemplateDropdownMenuBlock extends MailElement {

    @Name("как Шаблон")
    @FindByCss(".js-template-link ")
    MailElement asTemplateLink();
}
