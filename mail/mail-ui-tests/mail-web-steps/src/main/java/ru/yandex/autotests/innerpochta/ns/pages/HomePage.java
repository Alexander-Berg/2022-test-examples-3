package ru.yandex.autotests.innerpochta.ns.pages;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.ChangeLabelFolderPopup;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.UserMenuBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.DomikBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.ExitAllPopup;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.Mail360HeaderBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.TodoItemEditBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.TodoItemsBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.TodoListSentBlock;
import ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.TodoListsBlock;
import ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.popup.BlockSetupMailRuCollector;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 30.05.12
 * <p> Time: 15:51
 */
public interface HomePage extends MailPage {

    @Name("Выпадающее меню пользователя")
    @FindByCss(".legouser__menu")
    UserMenuBlock userMenuDropdown();

    @Name("Домик на морде")
    @FindByCss(".desk-notif-card__card")
    DomikBlock domikBlock();

    @Name("Кнопка «Завести почту» на морде")
    @FindByCss(".desk-notif-card__login-mail-promo")
    MailElement registerMordaBtn();

    @Name("Кнопка «Войти в почту» на морде")
    @FindByCss(".desk-notif-card__login-new-item_mail")
    MailElement logInMordaBtn();

    @Name("Шапка Почта 360")
    @FindByCss(".mail-Header-Wrapper")
    Mail360HeaderBlock mail360HeaderBlock();

    @Name("Кнопка «Ставить метку автоматически»")
    @FindByCss("[class*=messages-empty__button]")
    MailElement putMarkAutomaticallyButton();

    @Name("Подсказка о хоткеях, текст")
    @FindByCss(".b-shortcuts__list.b-shortcuts__list-id-0")
    MailElement hotKeysHelp();

    @Name("Окошко которое всем мешает. closePhoneSecurity")
    @FindByCss(".b-mail-icon.b-mail-icon_remove.daria-action[data-action='phone.close']")
    MailElement closePhoneSecurity();

    @Name("Вся страница почты полностью")
    @FindByCss(".mail-Page-Body")
    MailElement pageContent();

    @Name("Вся страница календаря полностью")
    @FindByCss("body")
    MailElement pageCalContent();

    @Name("Рекламный баннер")
    @FindByCss(".b-banner")
    MailElement advertiseBanner();

    @Name("Полоса директа в почте под тулбаром")
    @FindByCss("[id = 'js-messages-direct']")
    MailElement directAd();

    @FindByCss(".js-todo-placeholder-wrap")
    @Name("Кнопка “Добавить дело“ внизу страницы")
    MailElement toDoWindow();

    @Name("Развернутый блок Тудушки со списками дел")
    @FindByCss(".ns-view-todo-lists-box")
    TodoListsBlock todoListBlock();

    @Name("Развернутый блок Тудушки со списками дел")
    @FindByCss(".ns-view-todo-items-box")
    TodoItemsBlock todoItemsBlock();

    @Name("Блок редактирования Дела")
    @FindByCss(".ns-view-todo-item-edit-box")
    TodoItemEditBlock todoItemEditBlock();

    @Name("Блок отправки Списка дел по email")
    @FindByCss(".ns-view-todo-email-box")
    TodoListSentBlock todoListSentBlock();

    @Name("Кнопка “Восстановить“")
    @FindByCss(".js-todo-splash .js-todo-restore")
    MailElement todoListRestore();

    @Name("Попап выхода на всех устройствах")
    @FindByCss(".b-popup__box__content")
    ExitAllPopup exitAllPopup();

    @Name("Попап переименования папки/метки")
    @FindByCss(".b-popup__box")
    ChangeLabelFolderPopup changeLabelFolderPopup();

    @Name("Кнопка разворачивания домика в полную форму")
    @FindByCss(".b-ico-arrow_down")
    MailElement arrowDown();

    @Name("Статуслайн")
    @FindByCss(".mail-Statusline")
    MailElement notification();

    @Name("Нотификация в темной теме")
    @FindByCss(".tooltip_tone_dark")
    MailElement notificationDark();

    @Name("Нотификация в цветной теме")
    @FindByCss(".tooltip_tone_default")
    MailElement notificationColourful();

    @Name("Крестик закрытия нотификации")
    @FindByCss(".tooltip__close")
    MailElement closeNotification();

    @Name("Кнопка «Написать»")
    @FindByCss(".qa-LeftColumn-ComposeButton")
    MailElement composeButton();

    @Name("Обновить")
    @FindByCss(".qa-LeftColumn-SyncButton")
    MailElement checkMailButton();

    @Name("Спиннер обновления списка писем")
    @FindByCss(".mail-Loader.is-active")
    MailElement mailLoader();

    @Name("Кнопка «Отмена» в попапе функциональных нотификаций")
    @FindByCss(".button2_theme_clear")
    MailElement cancelButton();

    @Name("Попап подключения сборщика mail.ru")
    @FindByCss("[data-test-id='login-app-ready']")
    BlockSetupMailRuCollector mailRuOauthPopup();
}
