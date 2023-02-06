package ru.yandex.autotests.innerpochta.ns.pages.messages.messagesblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 12.11.12
 * Time: 15:20
 */
public interface MessagePageDoneBlock extends MailElement {

    @Name("Кнопка 'Сохранить письмо' при отправке Резюме")
    @FindByCss(".nb-button")
    MailElement saveSummaryButton();
}
