package ru.yandex.autotests.innerpochta.ns.pages.passport.passportblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.Select;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 10.10.12
 * Time: 15:47
 */
public interface PersonalDataPageChangeDataBlock extends MailElement {

    @Name("Выбор страны")
    @FindByCss("[name='xcountry']")
    Select countryDropBox();

    @Name("Выбор страны")
    @FindByCss(" [type='submit']")
    MailElement saveChangesButton();

    @Name("Выбор часового пояса")
    @FindByCss(".b-hangover-content>select")
    Select timeZoneSelect();
}
