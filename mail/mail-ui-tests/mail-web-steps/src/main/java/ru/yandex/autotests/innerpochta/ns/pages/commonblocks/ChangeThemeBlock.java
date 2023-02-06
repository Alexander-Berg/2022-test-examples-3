package ru.yandex.autotests.innerpochta.ns.pages.commonblocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by cosmopanda on 14.06.2016.
 */
public interface ChangeThemeBlock extends MailElement {

    @Name("Спиcок тем")
    @FindByCss(".js-theme")
    ElementsCollection<MailElement> allThemesList();

    @Name("Активная тема")
    @FindByCss(".js-theme.is-active")
    MailElement activeTheme();

    @Name("Список доп. настроек цветной темы")
    @FindByCss(".ns-view-theme-colorful-selector .js-scope")
    ElementsCollection<MailElement> colorfulSettingsList();

    @Name("Доп. настройки для сезонной темы")
    @FindByCss(".mail-ThemeOverlay-Settings_seasons")
    MailElement seasonsSettings();

    @Name("Сезон зима")
    @FindByCss(".mail-ThemeOverlay-Settings-Item_winter")
    MailElement seasonsWinter();

    @Name("Сезон весна")
    @FindByCss(".mail-ThemeOverlay-Settings-Item_spring")
    MailElement seasonsSpring();

    @Name("Авто-изменение сезонов")
    @FindByCss(".js-rotation")
    MailElement rotationSeason();

    @Name("Ночная тема")
    @FindByCss(".js-theme[data-id='lamp']")
    MailElement lampTheme();

    @Name("Инпут «Город» погодной темы")
    @FindByCss(".mail-ThemeOverlay-Settings-CitySelector-Input")
    MailElement cityInput();

}
