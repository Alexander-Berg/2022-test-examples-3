package ru.yandex.autotests.innerpochta.cal.pages;

import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import ru.yandex.autotests.innerpochta.atlas.AllureListener;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.atlas.extensions.FindByCssCollectionExtension;
import ru.yandex.autotests.innerpochta.atlas.extensions.FindByCssExtension;
import ru.yandex.autotests.innerpochta.atlas.extensions.HoverMethodExtension;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;

/**
 * @author cosmopanda
 */
public class CalPages {

    private static WebDriverRule webDriverRule;

    public CalPages(WebDriverRule webDriverRule) {
        CalPages.webDriverRule = webDriverRule;
    }

    protected <T extends MailPage> T on(Class<T> pageClass) {
        Atlas atlas = new Atlas(new WebDriverConfiguration(webDriverRule.getDriver()));
        atlas.listener(new AllureListener());
        atlas.extension(new FindByCssExtension());
        atlas.extension(new FindByCssCollectionExtension());
        atlas.extension(new HoverMethodExtension());
        return atlas.create(webDriverRule.getDriver(), pageClass);
    }

    public HomePage home() {
        return on(HomePage.class);
    }

    public TouchHomePage touchHome() {
        return on(TouchHomePage.class);
    }
}
