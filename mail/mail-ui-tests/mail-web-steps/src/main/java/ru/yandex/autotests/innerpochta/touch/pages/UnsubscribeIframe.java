package ru.yandex.autotests.innerpochta.touch.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.qatools.htmlelements.element.CheckBox;

/**
 * @author oleshko
 */
public interface UnsubscribeIframe extends MailPage {

    String IFRAME_SUBS = "iframe[class=\"subscriptionsFrame\"]";
    String IFRAME_SUBS_LIZA = "iframe[class=\"js-subscriptions-frame\"]";

    @Name("Айфрейм управления рассылками")
    @FindByCss("iframe[class=\"js-subscriptions-frame\"]")
    MailElement subscriptionIframe();

    @Name("Список рассылок")
    @FindByCss(".subscriptionsList:nth-child(1)>div")
    ElementsCollection<MailElement> subscriptions();

    @Name("Крестик для закрытия попапа рассылок")
    @FindByCss(".Close")
    MailElement closeSubs();

    @Name("Таб скрытых рассылок")
    @FindByCss(".listTab[href*='/hidden']")
    MailElement tabHidden();

    @Name("Таб новых рассылок")
    @FindByCss(".listTab[href*='/pending']")
    MailElement tabNew();

    @Name("Таб активных рассылок")
    @FindByCss(".listTab[href*='/active']")
    MailElement tabActive();

    @Name("Кнопка на вкладке Новые у юзера без подписки")
    @FindByCss(".OptinWelcomeButton.isNotSubscribed")
    MailElement subscribeAndEnableBtn();

    @Name("Кнопка на вкладке Новые у юзера c подпиской")
    @FindByCss(".OptinWelcomeButton:not(.isNotSubscribed)")
    MailElement enableBtn();

    @Name("Неактивные таб рассылок")
    @FindByCss(".listTab:not(.isActive)")
    MailElement inactiveTab();

    @Name("Табы попапа рассылок")
    @FindByCss(".listTab")
    ElementsCollection<MailElement> tabs();

    @Name("Кнопка в просмотре рассылки")
    @FindByCss(".Button.newsletterInfoButton")
    MailElement subsViewBtn();

    @Name("Попап «Вы уверены?» при отписке/подписке на рассылку")
    @FindByCss(".modal__inner")
    MailElement confirmPopup();

    @Name("Крестик на попапе «Вы уверены?» при отписке/подписке на рассылку")
    @FindByCss(".modal__close")
    MailElement confirmPopupClose();

    @Name("Кнопка в списке рассылок")
    @FindByCss(".listButtonWrapper .Button")
    MailElement subsListBtn();

    @Name("Чекбоксы рассылок в списке рассылок")
    @FindByCss(".subscriptionTableCheckbox .Checkbox")
    ElementsCollection<MailElement> subsCheckboxes();

    @Name("Выделенные чекбоксы рассылок в списке рассылок")
    @FindByCss(".subscriptionTableCheckbox .Checkbox.CheckboxChecked")
    ElementsCollection<MailElement> subsCheckedCheckboxes();

    @Name("Стрелка возврата из просмотра рассылки")
    @FindByCss(".newsletterInfoBack")
    MailElement backFromSubsView();

    @Name("Заглушка при пустом списке рассылок")
    @FindByCss(".subscriptionsList.isEmpty")
    MailElement emptySubsList();

    @Name("Прыщик непрочитанности в списке писем от рассылки")
    @FindByCss(".messageUnread")
    MailElement unreadToggler();

    @Name("Кнопка на попапе «Вы уверены?» при отписке на рассылку")
    @FindByCss(".modal__unsubscribe .modal__button .Button")
    MailElement confirmPopupBtn();

    @Name("Кнопка на попапе об успешной отписке")
    @FindByCss(".modal__success .modal__inner .modal__button")
    MailElement successPopupBtn();

    @Name("Кнопка на попапе «Вы уверены?» при подписке на рассылку")
    @FindByCss(".modal__subscribe .modal__button .Button")
    MailElement confirmSubscribePopupBtn();

    @Name("Кнопка на попапе об успешной подписке")
    @FindByCss(".modal__successSubscribe .modal__inner .modal__button")
    MailElement successSubscribePopupBtn();

    @Name("Выделенные чекбоксы рассылок в попапе подтверждения")
    @FindByCss(".modal__checkbox .Checkbox")
    MailElement deleteMsgesCheckbox();

    @Name("Лоадер загрузки рассылок")
    @FindByCss(".appLoading")
    MailElement loader();

    @Name("Лоадер попапа «Наводим порядок»")
    @FindByCss(".Loading")
    MailElement loaderUnsubscribing();

    @Name("Каунтер количества рассылок")
    @FindByCss(".listTitleCount")
    MailElement counter();

    @Name("Попап промо отписок")
    @FindByCss(".mail-Promo-subscriptions")
    MailElement unsubscribePromo();

    @Name("Закрыть попап промо отписок")
    @FindByCss(".mail-Promo-subscriptions .button2_theme_clear")
    MailElement closeUnsubscribePromo();

    @Name("Кнопка открытия попапа рассылок из промо")
    @FindByCss(".mail-Promo-subscriptions .button2_theme_action")
    MailElement openPopupFromUnsubscribePromo();

    @Name("Каунтер на вкладке таба")
    @FindByCss(".listTab .count")
    MailElement tabCounter();

    @Name("Кнопка отключения опт-ина")
    @FindByCss(".optinDisableButton")
    MailElement optinDisableBtn();

    @Name("Кнопка подтверждения отключения опт-ина")
    @FindByCss(".OptinDisable .Button")
    MailElement optinDisableConfirm();

    @Name("Поиск")
    @FindByCss(".Search input")
    MailElement search();

    @Name("Чекбокс «Выделить все»")
    @FindByCss(".SelectAll")
    MailElement selectAll();

    @Name("Выбранное «Выделить все»")
    @FindByCss(".SelectAll .CheckboxChecked")
    MailElement selectAllChecked();

    @Name("Ссылка на хелп в футере")
    @FindByCss(".OptinWelcomeFooter a")
    MailElement helpLink();

    @Name("Кнопка «Подробнее» на рассылке")
    @FindByCss(".MoreButton")
    MailElement moreBtn();

}
