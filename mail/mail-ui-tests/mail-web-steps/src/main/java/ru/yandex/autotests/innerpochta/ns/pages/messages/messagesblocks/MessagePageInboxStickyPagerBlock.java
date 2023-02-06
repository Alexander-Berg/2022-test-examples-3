package ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 12.11.12
 * Time: 15:20
 */
public interface MessagePageInboxStickyPagerBlock extends MailElement {

    @Name("Стрелка влево на пэйджере")
    @FindByCss(".b-link_scroll-left .b-link__i")
    MailElement scrollLeft();

    @Name("Стрелка вправо на пэйджере")
    @FindByCss(".b-link_scroll-right .b-link__i")
    MailElement scrollRight();

    @Name("Месяца")
    @FindByCss(".js-year a[data-params*='месяц']")
    ElementsCollection<MailElement> months();

    @Name("Предыдущий год")
    @FindByCss(".b-mail-paginator__group.js-year:nth-last-child(2) a[data-params*='год']")
    MailElement prevYear();
}
