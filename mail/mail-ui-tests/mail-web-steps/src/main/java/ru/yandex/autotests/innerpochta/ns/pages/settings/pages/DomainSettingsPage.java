package ru.yandex.autotests.innerpochta.ns.pages.settings.pages;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.MailPage;

public interface DomainSettingsPage extends MailPage {

    @Name("Статус домена «Свободен»")
    @FindByCss("[class*=DomainStatus__Available--]")
    MailElement availableStatus();

    @Name("Статус домена «Недоступен»")
    @FindByCss("[class*=DomainStatus__Unavailable--]")
    MailElement unavailableStatus();

    @Name("Инпут логина и домена")
    @FindByCss("[class*=EmailInputBase__TextInput--] input")
    ElementsCollection<MailElement> domainNameInput();

    @Name("Цена на кнопке «Подключить»")
    @FindByCss("[class*=src__BuyButtonCost--]")
    MailElement farePrice();

    @Name("Варианты в саджесте доменов")
    @FindByCss("[class*=SuggestList__ListElement--]")
    ElementsCollection<MailElement> domainSuggestList();

    @Name("Кнопка «Подключить»")
    @FindByCss("[class*=src__BuyButton--]")
    MailElement enableButton();

    @Name("Кнопки «Изменить/Удалить домен»")
    @FindByCss("[class*=ActionButtons__actionButton--]")
    ElementsCollection<MailElement> actionButtons();

    @Name("Статус домена")
    @FindByCss("[class*=StatusDescription__statusDescription--]")
    MailElement domainStatus();

    @Name("Еще варианты")
    @FindByCss("[class*=SuggestList__More]")
    MailElement moreButton();

}
