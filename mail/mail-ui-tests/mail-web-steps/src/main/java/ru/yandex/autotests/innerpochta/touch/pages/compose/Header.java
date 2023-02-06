package ru.yandex.autotests.innerpochta.touch.pages.compose;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface Header extends MailElement {

    @Name("Кнопка «Отправить»")
    @FindByCss(".ComposeSendButton")
    MailElement sendBtn();

    @Name("Кнопка «Отправить» при отложенной отправке")
    @FindByCss(".ComposeSendButton_delayed")
    MailElement delayedSendingBtn();

    @Name("Скрепочка для добавления аттачей")
    @FindByCss(".ComposeAttachFileButton")
    MailElement clip();

    @Name("Кнопка выхода из композа")
    @FindByCss(".ComposeControlPanel-CloseButton")
    MailElement closeBtn();

    @Name("Колокольчик для включения напоминаний")
    @FindByCss(".ComposeNotificationsButton")
    MailElement reminders();

    @Name("Колокольчик для включения напоминаний с прыщом")
    @FindByCss(".ComposeNotificationsButton.ComposeControlPanelButton_notification")
    MailElement turnedOnReminders();

    @Name("Кнопка для настройки отложенной отправки")
    @FindByCss(".ComposeDelayedSendingButton")
    MailElement delayedSending();

    @Name("Кнопка для настройки отложенной отправки с прыщом")
    @FindByCss(".ComposeDelayedSendingButton.ComposeControlPanelButton_notification")
    MailElement turnedOnDelayedSending();

    @Name("Кнопка «Метки»")
    @FindByCss(".ComposeControlPanel-LabelButton")
    MailElement labels();
}
