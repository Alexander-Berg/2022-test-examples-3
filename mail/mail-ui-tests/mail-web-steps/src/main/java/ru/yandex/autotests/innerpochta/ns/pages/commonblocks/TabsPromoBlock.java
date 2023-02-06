package ru.yandex.autotests.innerpochta.ns.pages.commonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author a-zoshchuk
 */
public interface TabsPromoBlock extends MailElement {

    @Name("Кнопка перехода в таб")
    @FindByCss("[name = '.js-promo-tabs-button']")
    MailElement goToTab();

    @Name("Крестик закрытия")
    @FindByCss(".js-promo-tabs-closer")
    MailElement closeBtn();

    @Name("Остановившаяся лупа промки")
    @FindByCss(".js-promo-tabs-magnifier[style*='transition: none;']")
    MailElement magnifier();
}
