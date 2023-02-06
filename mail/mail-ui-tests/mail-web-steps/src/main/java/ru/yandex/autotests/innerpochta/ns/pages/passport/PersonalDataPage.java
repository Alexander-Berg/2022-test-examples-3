package ru.yandex.autotests.innerpochta.ns.pages.passport;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.passport.passportblocks.PersonalDataPageChangeDataBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 10.10.12
 * Time: 15:44
 */
public interface PersonalDataPage extends MailPage {

    @Name("Блок изменения персональных данных")
    @FindByCss(".l-page__center")
    PersonalDataPageChangeDataBlock personalDataPageChangeDataBlock();

    @Name("Переход на персональные данные")
    @FindByCss("[href*='passport?mode=changereg']")
    MailElement changePersonalDataLink();
}
