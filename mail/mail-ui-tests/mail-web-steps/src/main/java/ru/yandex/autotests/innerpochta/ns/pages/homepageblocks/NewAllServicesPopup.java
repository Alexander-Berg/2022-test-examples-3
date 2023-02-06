package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;
import ru.yandex.autotests.innerpochta.atlas.MailElement;

public interface NewAllServicesPopup extends MailElement {

    @Name("Кнопка «Все сервисы» 360")
    @FindByCss(".qa-PSHeader-MorePopup-all-services")
    MailElement allServices360();

    @Name("Сервисы в попапе")
    @FindByCss(".mail-HeaderServicesPopup__item")
    ElementsCollection<MailElement> services();

    @Name("Иконки сервисов 360")
    @FindByCss(".PSHeader-ServiceList-PopupService")
    ElementsCollection<MailElement> serviceIcons();

    @Name("Сервис «Этушка» в списке сервисов 360")
    @FindByCss("[href^='https://my.at.yandex-team.ru']")
    MailElement serviceMyAt();

    @Name("Сервис «Рассылки» в списке сервисов 360")
    @FindByCss("[href^='https://ml.yandex-team.ru']")
    MailElement serviceMl();
}
