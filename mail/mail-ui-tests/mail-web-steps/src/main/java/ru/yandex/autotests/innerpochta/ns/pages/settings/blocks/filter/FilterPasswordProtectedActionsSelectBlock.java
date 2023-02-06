package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.filter;

import io.qameta.atlas.webdriver.extension.Name;

import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 13.09.12
 * Time: 14:14
 */

public interface FilterPasswordProtectedActionsSelectBlock extends MailElement {

    @Name("Получить доступ к закрытым опциям")
    @FindByCss(".b-pseudo-link")
    MailElement accessToAdvancedOptionsLink();

    @Name("Переслать по адресу")
    @FindByCss("[dependence-parent-id='forward_5'] input")
    MailElement forwardToCheckBox();

    @Name("Предупреждение «Нельзя создать правило с пересылкой на тот же адрес, на котором создается правило»")
    @FindByCss(".b-notification.b-notification_error-self-address-forward")
    MailElement sameAddressNotification();

    @Name("Предупреждение о неверном email адресе")
    @FindByCss(".b-notification.b-notification_error-pattern-forward")
    MailElement wrongAddressNotification();

    @Name("Чекбокс сохранения копии")
    @FindByCss("[name='forward_with_store']")
    MailElement saveCopyCheckBox();

    @Name("Уведомить по адресу")
    @FindByCss("[dependence-parent-id='notify_6']")
    MailElement notifyToCheckBox();

    @Name("Ответить следующим текстом")
    @FindByCss("[dependence-parent-id='reply']")
    MailElement replyWithTextCheckBox();

    @Name("Поле ввода адреса для пересылки")
    @FindByCss("[name='forward_address']")
    MailElement forwardToInbox();

    @Name("Поле ввода адреса для уведомления")
    @FindByCss("[name='notify_address']")
    MailElement notifyToInbox();

    @Name("Поле ввода для ответа")
    @FindByCss("[name='autoanswer']")
    MailElement replyWithTextInbox();
}
