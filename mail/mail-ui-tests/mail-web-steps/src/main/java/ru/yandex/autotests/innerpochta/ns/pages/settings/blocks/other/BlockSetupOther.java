package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.other;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * User: lanwen
 * Date: 19.11.13
 * Time: 18:27
 */

public interface BlockSetupOther extends MailElement {

    @Name("Панель верхняя <В списке писем>...")
    @FindByCss("[data-id='other']")
    BlockSetupOtherTop topPanel();

    @Name("Панель нижняя <Редактирование и отправка писем>")
    @FindByCss("[data-id='editing-and-sending-emails']")
    BlockSetupOtherBottom bottomPanel();
}
