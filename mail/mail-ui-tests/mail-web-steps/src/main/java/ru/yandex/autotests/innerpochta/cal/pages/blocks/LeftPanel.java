package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface LeftPanel extends MailElement {

    @Name("Мини календарь в левой колонке")
    @FindByCss(".qa-AsideDatePicker")
    MiniCalendar miniCalendar();

    @Name("Выбор вида")
    @FindByCss("[class*=AsideView__wrap] button")
    MailElement view();

    @Name("Свернуть/Развернуть календари")
    @FindByCss(".qa-AsideLayers_Layers-Title")
    MailElement expandCalendars();

    @Name("Свернуть/Развернуть подписки")
    @FindByCss(".qa-AsideLayers_Subscriptions-Title")
    MailElement expandSubscriptions();

    @Name("Кнопка «Новый календарь»")
    @FindByCss(".qa-AsideLayers_Layers-Add")
    MailElement addCal();

    @Name("Кнопка «Настроить календарь»")
    @FindByCss(".qa-AsideLayers_Layers-LayerSettings")
    MailElement calSettings();

    @Name("Чекбокс включения/отключения календаря")
    @FindByCss(".qa-AsideLayers_Layers .checkbox__control")
    MailElement layerCalCheckBox();

    @Name("Список календарей")
    @FindByCss(".qa-AsideLayers_Layers-Layer")
    ElementsCollection<MailElement> layersList();

    @Name("Список названий календарей")
    @FindByCss(".qa-AsideLayers_Layers-Layer .AsideLayers__layerName--1qk4j")
    ElementsCollection<MailElement> layersNames();

    @Name("Список владельцев календарей")
    @FindByCss(".qa-AsideLayers_Layers-Layer [class*=AsideLayers__layerOwner]")
    ElementsCollection<MailElement> layersOwners();

    @Name("Список названий подписок")
    @FindByCss(".qa-AsideLayers_Subscriptions-Layer .AsideLayers__layerName--1qk4j")
    ElementsCollection<MailElement> subscriptionLayersNames();

    @Name("Кнопка «Новая подписка»")
    @FindByCss(".qa-AsideLayers_Subscriptions-Add")
    MailElement addSubscription();

    @Name("Чекбокс включения/отключения подписки")
    @FindByCss(".qa-AsideLayers_Subscriptions .checkbox__control")
    MailElement layerSubsCheckBox();

    @Name("Свернуть/Развернуть подписки")
    @FindByCss(".qa-AsideLayers_Subscriptions-Title")
    ElementsCollection<MailElement> expandSubs();

    @Name("Кнопка «Настроить подписки»")
    @FindByCss(".qa-AsideLayers_Subscriptions-LayerSettings")
    MailElement subsSettings();

    @Name("Список подписок")
    @FindByCss(".qa-AsideLayers_Subscriptions")
    ElementsCollection<MailElement> subsList();

    @Name("Кнопка «Поменять таймзону»")
    @FindByCss("[class*=AsideFooter__timezone]")
    MailElement changeTime();

    @Name("Список месяцев в выпадушке")
    @FindByCss(".react-datepicker__month-option")
    ElementsCollection<MailElement> monthList();

    @Name("Список годов в выпадушке")
    @FindByCss(".react-datepicker__year-option")
    ElementsCollection<MailElement> yearList();

    @Name("Кнопка «Свернуть/Развернуть боковую панель»")
    @FindByCss(".Aside__toggleButton--1UYwe")
    MailElement manageLeftPanel();

    @Name("Кнопка «Создать событие»")
    @FindByCss(".qa-AsideCreateEvent")
    MailElement createEvent();

    @Name("Выбор языка в левой колонке")
    @FindByCss(".qa-AsideLang")
    MailElement lang();

    @Name("Фильтры переговорок по параметрам")
    @FindByCss("[class*=AsideResourcesFilter__wrap]")
    FilterList filterList();
}
