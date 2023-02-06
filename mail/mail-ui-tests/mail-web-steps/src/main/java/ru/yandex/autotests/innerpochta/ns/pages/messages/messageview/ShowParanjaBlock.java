package ru.yandex.autotests.innerpochta.ns.pages.messages.messageview;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by kurau on 04.07.14.
 */
public interface ShowParanjaBlock extends MailElement {

    @Name("Паранжа")
    @FindByCss(".mail-message-reply-paranja")
    MailElement paranja();

    @Name("Промо")
    @FindByCss(".mail-message-reply-promo")
    MailElement promo();
}
