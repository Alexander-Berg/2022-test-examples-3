package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface SettingsPopupBlock extends MailElement {

    @Name("Красивый @ адес в попапе настроек")
    @FindByCss(".beautiful-email-section")
    MailElement domainSettingsPageLink();

    @Name("Промо управления рассылками в попапе настроек")
    @FindByCss("[class*='SettingsPromo__subscribes--']")
    SettingsPopupBlock subsPromoSettings();

    @Name("Кнопка «Все настройки» в попапе настроек")
    @FindByCss("[class*='SettingsHeader__button']")
    MailElement moveToSettingsButton();

    @Name("Промо красивого адреса в попапе настроек")
    @FindByCss("[class*='SettingsPromo__email']")
    MailElement beautifulEmailPromoButton();

    @Name("Промо бекапа в попапе настроек")
    @FindByCss("[class*='SettingsPromo__backup']")
    MailElement backupPromoButton();

    @Name("Промо УР в попапе настроек")
    @FindByCss("[class*='SettingsPromo__subscribes']")
    MailElement subscriptionsPromoButton();

    @Name("Темы в попапе настроек")
    @FindByCss("[class*='ThemeCard__thumbnail']")
    ElementsCollection<MailElement> themesList();

    @Name("Варианты цветной темы в попапе настроек")
    @FindByCss("[class*='ThemeScopeColorful__item']")
    ElementsCollection<MailElement> coloursList();

    @Name("Доп. настройки для сезонной темы")
    @FindByCss("[class*='ThemeScopeSeasons__container']")
    MailElement seasonsSettings();

    @Name("Сезон Зима в теме Настроение")
    @FindByCss(".svgicon-mail--Themes-Seasons_winter")
    MailElement seasonWinter();

    @Name("Смена сезонов в теме Настроение") //TODO: need selector
    @FindByCss("[class*='ThemeScopeItem__item']")
    ElementsCollection<MailElement> rotationSeason();

    @Name("Инпут «Город» погодной темы")
    @FindByCss("[class*='ThemeScopeWeather__input'] .Textinput-Control")
    MailElement cityInput();

    @Name("Кнопки Раскрыть,Свернуть темы")
    @FindByCss("[class*='SettingsSection__button']")
    ElementsCollection<MailElement> themesListButtons();

    @Name("Темные темы в попапе настроек")
    @FindByCss("[class*='ThemeScopeDark__container'] [class*='ThemeScopeItem__item']")
    ElementsCollection<MailElement> darkThemesList();

    @Name("Чекбоксы настроек") //TODO: need selector for every setting
    @FindByCss("[class*='SettingsCheckbox__checkbox'] input")
    ElementsCollection<MailElement> settingsCheckboxes();
}
