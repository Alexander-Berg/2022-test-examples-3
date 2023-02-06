package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;


/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 03.10.12
 * Time: 18:30
 */
public interface ComposePageSuggestBlock extends MailElement {

    @Name("Имя контакта")
    @FindByCss(".mail-Suggest-Item-Name")
    MailElement contactName();

    @Name("Адрес контакта")
    @FindByCss(".mail-Suggest-Item-Email")
    MailElement contactEmail();

    @Name("Кнопка “копия в SMS“")
    @FindByCss(".js-compose-add-phone")
    MailElement smsCopyBtn();

    @Name("Аватар")
    @FindByCss(".mail-Suggest-Item-Avatar")
    MailElement avatar();
}
