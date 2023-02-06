package ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks;

import io.qameta.atlas.webdriver.extension.Name;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 04.06.15.
 */
public interface ForwardedMsgAttachBlock extends MailElement {

    @Name("Чекбокс у аттача")
    @FindByCss(".js-message-checkbox input")
    MailElement fileCheckBox();

    @Name("Лэйбл у чекбокса - “Отправить письмо в виде вложения“")
    @FindByCss("._nb-checkbox-label")
    MailElement fileText();

    @Name("Ссылка на письмо в лэйбле")
    @FindByCss("a")
    MailElement messageLink();
}
