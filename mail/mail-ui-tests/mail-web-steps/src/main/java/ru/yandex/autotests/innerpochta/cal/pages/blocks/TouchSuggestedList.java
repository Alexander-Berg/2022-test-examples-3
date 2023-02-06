package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author mariya-murm
 */
public interface TouchSuggestedList extends MailElement {

    @Name("Имя участника")
    @FindByCss("[class*=TouchMember__name]")
    MailElement memberName();
}
