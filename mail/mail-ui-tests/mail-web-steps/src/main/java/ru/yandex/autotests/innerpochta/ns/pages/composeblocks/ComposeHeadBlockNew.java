package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * User: lanwen
 * Date: 15.11.13
 * Time: 22:53
 */
public interface ComposeHeadBlockNew extends MailElement {

    @Name("Крестик в правом верхнем углу шапки - “Закрыть“")
    @FindByCss(".ns-view-compose-cancel-button")
    MailElement composeCancelBtn();
}
