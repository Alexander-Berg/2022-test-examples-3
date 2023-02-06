package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface ComposeFooterSendBlock extends MailElement {

    @Name("Сообщение «Сохранено как ...»")
    @FindByCss(".ns-view-compose-autosave-status")
    MailElement savedAsDraftMessage();

    @Name("Линк на попап 'как черновик' (чтобы выбрать шаблон)")
    @FindByCss(".ns-view-compose-autosave-status .mail-ui-Link")
    MailElement savedAsDraftLink();

    @Name("Кнопка с часами для отложенной отправки")
    @FindByCss(".js-sendtime")
    MailElement sendTimeBtn();

    @Name("Кнопка «Напомнить» в подвале композа")
    @FindByCss(".js-compose-notify-noreply-button")
    MailElement remindBtn();

    @Name("Чекбокс «Напомнить»")
    @FindByCss(".js-compose-notify-noreply-button input")
    MailElement remindCheckbox();

    @Name("Чекбокс «Уведомить»")
    @FindByCss(".js-compose-remind-button")
    MailElement notifyCheckbox();

    @Name("Кнопка «Отправить» в подвале композа")
    @FindByCss(".js-send")
    MailElement sendBtn();

}