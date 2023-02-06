package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author mariya-murm
 */
public interface TouchRoomCard extends MailElement {

    @Name("Кнопка «Выбрать переговорку»")
    @FindByCss("[class*=TouchResource__actionButton]")
    MailElement chooseRoomButton();

    @Name("Кнопка «Показать на карте»")
    @FindByCss("[class*=TouchRoomCard__actionButton]")
    MailElement showMap();
}
