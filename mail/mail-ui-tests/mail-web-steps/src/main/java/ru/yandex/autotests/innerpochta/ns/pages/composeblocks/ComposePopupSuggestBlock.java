package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;


/**
 * @author marchart
 */
public interface ComposePopupSuggestBlock extends MailElement {

    @Name("Имя контакта")
    @FindByCss(".ContactsSuggestItemDesktop-Name")
    MailElement contactName();

    @Name("Адрес контакта")
    @FindByCss(".ContactsSuggestItemDesktop-Email")
    MailElement contactEmail();

    @Name("Аватар")
    @FindByCss(".ContactsSuggestItemDesktop-Avatar")
    MailElement avatar();

    @Name("Адрес контакта")
    @FindByCss(".ComposeAddressItem-Email")
    MailElement fromEmail();
}
