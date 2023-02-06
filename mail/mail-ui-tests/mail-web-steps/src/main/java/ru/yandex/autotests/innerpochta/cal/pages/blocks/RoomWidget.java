package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author mariya-murm
 */
public interface RoomWidget extends MailElement {

    @Name("Доступные переговорки, время")
    @FindByCss("[class*=SuggestMeetingTimesItem__wrap]")
    ElementsCollection<MailElement> availableRoom();

    @Name("Кнопка «Еще переговорки»")
    @FindByCss("[class*=SuggestMeetingTimesAnyRoom__viewModeSwitcher]")
    MailElement moreButton();
}
