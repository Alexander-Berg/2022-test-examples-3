package ru.yandex.autotests.innerpochta.ns.pages.commonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.DropDownUserBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.MultiAuthDropdownPromo;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface UserMenuBlock extends MailElement {

    String EXIT = "Выйти из сервисов Яндекса";
    String PASSPORT = "Паспорт";
    String ABOOK = "Контакты";
    String DISK = "Диск";
    String LETTERS = "Письма";
    String CALENDAR = "Календарь";

    @Name("Ссылки в меню")
    @FindByCss(".b-mail-dropdown__item")
    ElementsCollection<MailElement> links();

    @Name("Добавить пользователя")
    @FindByCss(".legouser__add-account")
    MailElement addUserButton();

    @Name("Список залогиненных пользователей")
    @FindByCss(".legouser__accounts .user-account_has-subname_yes")
    ElementsCollection<DropDownUserBlock> userList();

    @Name("Выход на всех устройствах")
    @FindByCss("[data-click-action='common.exitAll']")
    MailElement exitAll();

    @Name("Имя текущего юзера в дропдауне меню залогина")
    @FindByCss(".user-account__subname")
    MailElement currentUserName();

    @Name("Промо")
    @FindByCss(".dropdown-promo")
    MultiAuthDropdownPromo promo();

    @Name("Выйти")
    @FindByCss(".legouser__menu-item_action_exit")
    MailElement userLogOutLink();
}
