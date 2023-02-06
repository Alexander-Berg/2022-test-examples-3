package ru.yandex.autotests.innerpochta.touch.pages.compose;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface DiskAttachmentsPage extends MailElement {

    @Name("Блок аттача на странице дисковых аттачей")
    @FindByCss(".DiskResourcesListingItem")
    MailElement attachment();

    @Name("Чекбоксы для выбора аттачей")
    @FindByCss(".DiskResourcesListingItem-Checkbox")
    ElementsCollection<MailElement> checkbox();

    @Name("Кнопка «Прикрепить» в окнах дисковых аттачей")
    @FindByCss(".DiskResourcesFooterMobile [type=\"button\"]")
    MailElement attachBtn();

    @Name("Кнопка закрыть в окнах дисковых аттачей")
    @FindByCss(".DiskResourcesBreadcrumbsMobile-Close")
    MailElement closeBtn();

    @Name("Список аттачей на странице дисковых аттачей")
    @FindByCss(".browseDisk-Listing-Item")
    ElementsCollection<MailElement> attachments();

    @Name("Кнопка назад в окнах дисковых аттачей")
    @FindByCss(".DiskResourcesBreadcrumbsMobile-Back")
    MailElement backBtn();
}
