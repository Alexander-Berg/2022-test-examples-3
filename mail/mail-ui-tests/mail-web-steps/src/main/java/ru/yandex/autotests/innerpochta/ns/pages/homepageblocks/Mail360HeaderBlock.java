package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.commonblocks.NewHeaderUserMenuBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;


public interface Mail360HeaderBlock extends MailElement {

    @Name("Иконки сервисов")
    @FindByCss(".PSHeader-ServiceList-MainService")
    ElementsCollection<MailElement> serviceIcons();

    @Name("Кнопка «Еще»/«Другие сервисы» в шапке")
    @FindByCss(".PSHeaderIcon_More")
    MailElement moreServices();

    @Name("Логотип Морды")
    @FindByCss(".PSHeaderLogo360-Ya")
    MailElement yandexLogoMainPage();

    @Name("Кнопка «Улучшить Почту 360»")
    @FindByCss(".PSHeader-Pro")
    MailElement upgradeMail360();

    @Name("Меню за логином в шапке")
    @FindByCss(".user-account")
    MailElement userMenu();

    @Name("Кнопка “Выбрать оформление“")
    @FindByCss(".mail-ThemesButton")
    MailElement changeThemeBtn();

    @Name("Сервисы в шапке")
    @FindByCss(".PSHeaderService")
    ElementsCollection<MailElement> servicesHeader();

    @Name("Выпадающее меню пользователя")
    @FindByCss(".legouser__menu")
    NewHeaderUserMenuBlock userMenuDropdown();

    @Name("Обводка аватара при наличии Плюса")
    @FindByCss(".user-pic_has-plus_yes")
    MailElement plusAvatar();

    @Name("Настройки")
    @FindByCss(".mail-SettingsButton")
    MailElement settings();

    @Name("Кнопка “Найти“ в компактном режиме")
    @FindByCss(".mail-GhostButton.search-input__form-activator")
    MailElement searchBtnCompactMode();

    @Name("Поле поиска по почте")
    @FindByCss(".mail-Search .textinput__control")
    MailElement searchInput();

    @Name("Кнопка “Найти“")
    @FindByCss(".search-input__form-button")
    MailElement searchBtn();

    @Name("Крестик для закрытия поиска")
    @FindByCss(".search-input__narrow-close-button")
    MailElement closeSearch();

    @Name("Нотификация о новых письмах в шапке")
    @FindByCss(".user-pic.is-updated")
    MailElement userMenuNotification();

    @Name("Логотип Почты")
    @FindByCss(".PSHeaderLogo360-360")
    MailElement mailLogo();

    @Name("Кастомный логотип пользователя WS")
    @FindByCss(".PSHeaderLogo360-CustomLogo")
    MailElement wsLogo();

    @Name("Шестерёнка настроек")
    @FindByCss(".mail-SettingsButton")
    MailElement settingsMenu();

    @Name("Баббл в поиске")
    @FindByCss(".search-bubble")
    MailElement searchBubble();

    @Name("Опция “Расширенный поиск“")
    @FindByCss(".search-input__advanced-button")
    MailElement searchOptionsBtn();

    @Name("Поле поиска по контактам")
    @FindByCss(".mail-SearchContainer .textinput__control")
    MailElement searchContactInput();

    @Name("Крестик для очищения поля")
    @FindByCss(".search-input__narrow-close-button")
    MailElement clearContactInput();

    @Name("Крестик для сворачивания поиска")
    @FindByCss(".search-input__narrow-close-button")
    MailElement closeContactInput();

    @Name("Пользовательский аватар")
    @FindByCss(".user-account__pic")
    MailElement userAvatar();

    @Name("Свернутый поиск")
    @FindByCss(".search-input__form_folded")
    MailElement foldedSearch();
}
