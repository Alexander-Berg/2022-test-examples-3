package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface ContactsSuggest extends MailElement {

    @Name("Имя элемента саджеста")
    @FindByCss("[class*=MemberSuggestion__name]")
    MailElement contactName();

    @Name("Аватар элемента саджеста")
    @FindByCss("[class*=MemberSuggestion__avatar]")
    MailElement contactAvatar();

    @Name("Занятый участник в саджесте")
    @FindByCss("[class*=MemberSuggestion__wrap_busy]")
    MailElement busyContact();
}
