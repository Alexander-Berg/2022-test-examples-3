package ru.yandex.autotests.innerpochta.ns.pages.passport;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 10.10.12
 * Time: 12:57
 */
public interface PassportPage extends MailPage{

    @Name("Переход на персональные данные")
    @FindByCss("[href*='passport?mode=passport']")
    MailElement personalDataLink();
}
