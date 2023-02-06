package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.dropdown;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface ComposePopupYabbleDropdown extends MailElement {
    // TODO: поменять селекторы, как случится задача - https://st.yandex-team.ru/DARIA-67803
    @Name("Связанные email'ы")
    @FindByCss(".ComposeYabbleMenu-Addresses .ComposeYabbleMenu-Item")
    ElementsCollection<MailElement> changeEmail();

    @Name("Скопировать емейл")
    @FindByCss(".ComposeYabbleMenu-Actions > :nth-child(1)")
    MailElement copyEmail();

    @Name("Редактировать адрес")
    @FindByCss(".ComposeYabbleMenu-Actions > :nth-child(2)")
    MailElement editYabble();

    @Name("Написать только этому получателю")
    @FindByCss(".ComposeYabbleMenu-Actions > :nth-child(3)")
    MailElement singleTarget();

    @Name("email выбранного яббла")
    @FindByCss(".ComposeYabbleMenu-Item_active .ComposeAddressItem-Email")
    MailElement yabbleEmail();
}
