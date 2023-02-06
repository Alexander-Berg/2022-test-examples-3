package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface Notifications extends MailElement {

    @Name("Кнопка удалить уведомление")
    @FindByCss("[class*=NotificationsFieldItem__wrap] > button")
    MailElement deleteNotifyBtn();

    @Name("Кнопка добавить уведомление")
    @FindByCss(".qa-NotificationsField-Add")
    MailElement addNotifyBtn();

    @Name("Инпут времени")
    @FindByCss(".qa-NotificationsFieldItem-Offset input")
    MailElement offsetNotifyInput();

    @Name("Селект единицы измерения времени")
    @FindByCss(".qa-NotificationsFieldItem-Unit button")
    MailElement unitNotifySelect();

    @Name("Текст единицы измерения времени")
    @FindByCss(".qa-NotificationsFieldItem-Unit button .Button2-Text")
    MailElement unitNotifyText();

    @Name("Селект выбора канала оповещения")
    @FindByCss(".qa-NotificationsFieldItem-Channel button")
    MailElement channelNotifySelect();
}
