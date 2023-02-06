package ru.yandex.autotests.innerpochta.ns.pages.commonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.DropDownLegoUserBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface NewHeaderUserMenuBlock extends MailElement {
    @Name("Текущий юзер в дропдауне меню залогина")
    @FindByCss(".legouser__menu-header")
    MailElement currentUser();

    @Name("Список аккаунтов в выпадушке")
    @FindByCss(".legouser__accounts .legouser__account")
    ElementsCollection<DropDownLegoUserBlock> accs();

    @Name("Аватарка в выпадающем меню пользователя (с плюсом)")
    @FindByCss(".legouser__menu .user-pic_has-plus_yes")
    MailElement userMenuDropdownAvatar();

    @Name("Управление аккаунтом")
    @FindByCss(".legouser__menu-item_action_passport")
    MailElement userProfileLink();

    @Name("Помощь")
    @FindByCss(".legouser__footer-link")
    MailElement userHelpLink();

    @Name("Переход на Плюс")
    @FindByCss(".legouser__menu-item_action_plus")
    MailElement userPlusLink();

    @Name("Выйти")
    @FindByCss(".legouser__menu-item_action_exit")
    MailElement userLogOutLink();
}
