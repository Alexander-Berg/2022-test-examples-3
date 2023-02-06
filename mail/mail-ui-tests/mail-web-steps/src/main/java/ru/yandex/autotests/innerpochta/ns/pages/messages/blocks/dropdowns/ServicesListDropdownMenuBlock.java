package ru.yandex.autotests.innerpochta.ns.pages.messages.blocks.dropdowns;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface ServicesListDropdownMenuBlock extends MailElement {

    @Name("Ссылка в меню сервисы с текстом «{{ mailText }}")
    @FindBy(value = ".//a[contains(text(), '{{ mailText }}')]")
    MailElement servicesText(@Param("mailText") String mailText);
}
