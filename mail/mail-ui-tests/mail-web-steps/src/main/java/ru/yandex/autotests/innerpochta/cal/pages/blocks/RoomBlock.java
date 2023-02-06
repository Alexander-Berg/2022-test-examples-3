package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author marchart
 */
public interface RoomBlock extends MailElement {

    @Name("Выпадушка городов переговорок")
    @FindByCss("[class*=EventResourcesFieldItem__office]")
    MailElement officeResource();

    @Name("Выбранная переговорка")
    @FindByCss("[class*=EventResourcesFieldItem__resource] .qa-Picker-Item")
    MailElement selectedRoom();

    @Name("Инпут «Переговорки»")
    @FindByCss(".qa-ResourcesField input")
    MailElement roomInput();

    @Name("Ошибка в инпуте")
    @FindByCss(".qa-Picker-Error")
    MailElement errorMessage();
}
