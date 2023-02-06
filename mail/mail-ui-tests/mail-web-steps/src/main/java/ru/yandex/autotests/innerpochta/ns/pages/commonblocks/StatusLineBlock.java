package ru.yandex.autotests.innerpochta.ns.pages.commonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 13.09.12
 * Time: 14:14
 */

public interface StatusLineBlock extends MailElement {

    @Name("Текстовое содержимое уведомления ксивы")
    @FindByCss(".tooltip__description")
    MailElement textBox();

    @Name("Кнопка «Перекладывать»")
    @FindByCss(".button2_theme_action")
    MailElement createFilterBtn();

    @Name("Кнопка «Больше не предлагать»")
    @FindByCss(".button2_theme_clear")
    MailElement refuseBtn();

    @Name("Кнопка «Изменить правило»")
    @FindByCss(".button2_theme_clear")
    MailElement editFilterBtn();

    @Name("Кнопка «Переложить»")
    @FindByCss(".button2_theme_action")
    MailElement moveMessagesBtn();

    @Name("Кнопка «Отменить»")
    @FindByCss(".mail-StatuslineProgress_Action")
    MailElement unDoBtn();

    @Name("Кнопка «Точно не спам»")
    @FindByCss(".button2_theme_action")
    MailElement notSpamBtn();
}


