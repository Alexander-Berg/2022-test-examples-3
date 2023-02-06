package ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface MessagePageInboxPagerBlock extends MailElement {

    @Name("Стрелка влево на пэйджере")
    @FindByCss(".b-link_scroll-left")
    MailElement scrollLeft();

    @Name("Года")
    @FindByCss(".js-year a[data-params*='год']")
    ElementsCollection<MailElement> years();

    @Name("Месяца")
    @FindByCss(".js-year a[data-params*='месяц']")
    ElementsCollection<MailElement> months();

    @Name("Предыдущий год")
    @FindByCss(".b-mail-paginator__group.js-year:nth-last-child(2) a[data-params*='год']")
    MailElement prevYear();

    @Name("Текущий год")
    @FindByCss(".b-mail-paginator__group.js-year:last-child a[data-params*='год']")
    MailElement currentYear();
}
