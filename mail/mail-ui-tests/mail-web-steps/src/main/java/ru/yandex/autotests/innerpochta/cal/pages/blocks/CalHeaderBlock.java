package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface CalHeaderBlock extends MailElement {

    @Name("Кнопка «Сегодня»")
    @FindByCss(".qa-HeaderGridNav-Today")
    MailElement todayBtn();

    @Name("Кнопка «Следующий период»")
    @FindByCss(".qa-HeaderGridNav-Next")
    MailElement nextPeriodBtn();

    @Name("Кнопка «Предыдущий период»")
    @FindByCss(".qa-HeaderGridNav-Prev")
    MailElement pastPeriodBtn();

    @Name("Сервисы в шапке")
    @FindByCss(".PSHeaderService")
    ElementsCollection<MailElement> services();

    @Name("Бургер в шапке")
    @FindByCss(".HeaderLogo__burgerWrap--1eOnl")
    MailElement burger();

    @Name("Кнопка «Календарь» в шапке")
    @FindByCss(".PSHeaderIcon_Calendar")
    MailElement calLink();

    @Name("Кнопка «Ещё» в шапке")
    @FindByCss(".PSHeaderIcon_More")
    MailElement more();

    @Name("Сервисы в попапе «Ещё»")
    @FindByCss(".PSHeader-MorePopupServices > .PSHeaderService")
    ElementsCollection<MailElement> moreItem();

    @Name("Шестеренка настроек")
    @FindByCss(".qa-Header-Settings")
    MailElement settingsButton();

    @Name("Аватар пользователя")
    @FindByCss(".user-account__pic")
    MailElement userAvatar();

    @Name("Имя текущего пользователя")
    @FindByCss(".user-account__name")
    MailElement userName();

    @Name("Кнопка «Календарь» в старой шапке")
    @FindByCss("[class*=HeaderLogo__service]")
    MailElement oldCalLink();

    @Name("Аватар пользователя в старой шапке")
    @FindByCss("[class*=Avatar__wrap]")
    MailElement oldUserAvatar();

    @Name("Имя текущего пользователя в старой шапке")
    @FindByCss("[class*=HeaderUser__name]")
    MailElement oldUserName();
}
