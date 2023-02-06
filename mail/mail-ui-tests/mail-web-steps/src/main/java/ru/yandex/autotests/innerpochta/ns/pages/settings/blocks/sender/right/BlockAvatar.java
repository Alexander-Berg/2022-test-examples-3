package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.sender.right;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 04.04.13
 * Time: 16:06
 */
public interface BlockAvatar extends MailElement {

    @Name("Кнопка «Изменить портрет»")
    @FindByCss(".js-avatar-load-link")
    MailElement loadLink();

    @Name("Кнопка «Справка»")
    @FindByCss(".b-link_help")
    MailElement helpButton();

    @Name("Аватар")
    @FindByCss(".js-avatar-img")
    MailElement avatarImg();

    }
