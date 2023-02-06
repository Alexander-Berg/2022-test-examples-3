package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.mailclients;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 18.09.12
 * Time: 17:08
 */

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface ImapAdvantagesBlock extends MailElement {

    String FIRST_ADVANTAGE = "Почта не будет привязана к одному компьютеру\n" +
        "Вы сможете пользоваться привычной почтовой программой где бы вы ни находились. Нужно просто зайти по " +
        "адресу mail.yandex.ru, и вы увидите свою почту в том же виде, что и в почтовой программе.";

    String SECOND_ADVANTAGE = "Ваша почта будет в сохранности\n" +
        "Ваши письма будут надёжно храниться на сервисах Яндекса как минимум в двух экземплярах. " +
        "Даже если что-то случится с вашим компьютером, все письма останутся.\nВыбрать IMAP";

    @Name("Ссылка «Узнать о преимуществах IMAP»")
    @FindByCss(".js-imap-show-advantages")
    MailElement showAdvantagesLink();

    @Name("Ссылка «Выбрать IMAP»")
    @FindByCss(".js-enable-imap-btn")
    MailElement selectIMAPLink();

    @Name("Преимущества IMAP")
    @FindByCss(".b-teaser__liq")
    ElementsCollection<MailElement> advantages();

    @Name("Картинки")
    @FindByCss(".b-teaser__pic")
    ElementsCollection<MailElement> pictures();
}
