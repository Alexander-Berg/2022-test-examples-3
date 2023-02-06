package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * @author oleshko
 */
public interface OptInLine extends MailElement {

    @Name("Крестик")
    @FindByCss("[class*='OptInSubscriptions__close--']")
    MailElement closeBtn();

    @Name("Кнопка «Позже»")
    @FindByCss("[class*='OptInSubscriptions__cancelButton--']")
    MailElement laterBtn();

    @Name("Кнопка «Разобрать»")
    @FindByCss("[class*='OptInSubscriptions__actionButton--']")
    MailElement sortBtn();
}
