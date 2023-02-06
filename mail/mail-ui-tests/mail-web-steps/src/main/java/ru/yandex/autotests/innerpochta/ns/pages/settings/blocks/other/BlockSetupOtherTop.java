package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.other;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

/**
 * User: lanwen
 * Date: 19.11.13
 * Time: 18:31
 */
public interface BlockSetupOtherTop extends MailElement {

    // В списке писем
    @Name("По ... писем на странице")
    @FindByCss("[name = 'messages_per_page']")
    MailElement messagesPerPage();

    @Name("Чекбокс «Показывать первую строчку письма»")
    @FindByCss("[name = 'enable_firstline']")
    MailElement showFirstLine();

    @Name("Чекбокс «Разрешить перетаскивание писем мышкой»")
    @FindByCss("[name = 'dnd_enabled']")
    MailElement dndEnabled();

    @Name("Чекбокс «Открывать письмо в списке писем»")
    @FindByCss("[name = 'open-message-list']")
    MailElement openMsgInList();

    @Name("Чекбокс «показывать в папке «Главное» письма, которые нужно посмотреть в первую очередь, и помечать их звездочкой в списке писем»")
    @FindByCss("[name = 'show_priority_stuff']")
    MailElement priorityTab();

    @Name("Чекбокс «Показывать портреты отправителей»")
    @FindByCss("[name = 'messages_avatars']")
    MailElement messagesAvatars();

    @Name("Чекбокс «Использовать меню по правому клику»")
    @FindByCss("[name = 'contextmenu_disable']")
    MailElement contextMenu();

    @Name("Чекбокс «Объединить чекбоксы с аватарками»")
    @FindByCss("[name = 'show_checkbox_inside_userpic']")
    MailElement showCheckBoxInsideUserpic();

    // В интерфейсе почты
    @Name("показывать... «Рекламу»")
    @FindByCss("[name = 'mute_ad_until']")
    MailElement showAdvertisement();

    @Name("Нотифайка с датой возвращения рекламы")
    @FindByCss(".b-setup-item_info_ad-mute-info")
    MailElement showAdvertisementInfo();

    @Name("Чекбокс «Использовать горячие клавиши»")
    @FindByCss("[name = 'enable_hotkeys']")
    MailElement useHotKeys();

    @Name("Вопросик справа от «Использовать горячие клавиши»")
    @FindByCss(".js-shortcuts-help")
    MailElement hotKeysInfo();

    @Name("Показывать календарь")
    @FindByCss("[name = 'timeline_enable']")
    MailElement timeline();

    @Name("Чекбокс «Показывать меню Яндекс 360»")
    @FindByCss("[name = 'liza_minified_header']")
    MailElement show360Header();

    // На странице письма
    @Name("Чекбокс «Показывать карточку контакта и организации»")
    @FindByCss("[name = 'show_avatars']")
    MailElement showContactsCard();

    @Name("Чекбокс «Использовать моноширинный шрифт»")
    @FindByCss("[name = 'use_monospace_in_text']")
    MailElement useMonospaceInText();

    @Name("Чекбокс «Предлагать перевод писем»")
    @FindByCss("[name = 'translate']")
    MailElement translate();

    @Name("Кнопка «Сохранить изменения»")
    @FindByCss("button[type='submit']")
    MailElement saveButton();

    @Name("Чекбокс «Складывать рассылки и уведомления из соцсетей в отдельные папки»")
    @FindByCss("[name = 'show_folders_tabs']")
    MailElement showFoldersTabs();
}
