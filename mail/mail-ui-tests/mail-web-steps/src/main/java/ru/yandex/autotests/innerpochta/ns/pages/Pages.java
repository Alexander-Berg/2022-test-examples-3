package ru.yandex.autotests.innerpochta.ns.pages;

import io.qameta.atlas.core.Atlas;
import io.qameta.atlas.webdriver.WebDriverConfiguration;
import ru.yandex.autotests.innerpochta.atlas.AllureListener;
import ru.yandex.autotests.innerpochta.atlas.MailPage;
import ru.yandex.autotests.innerpochta.atlas.extensions.FindByCssCollectionExtension;
import ru.yandex.autotests.innerpochta.atlas.extensions.FindByCssExtension;
import ru.yandex.autotests.innerpochta.atlas.extensions.HoverMethodExtension;
import ru.yandex.autotests.innerpochta.cal.pages.CalPages;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.touch.pages.TouchPages;

/**
 * @author cosmopanda
 */
public class Pages {

    private static WebDriverRule webDriverRule;

    public Pages(WebDriverRule webDriverRule) {
        Pages.webDriverRule = webDriverRule;
    }

    protected <T extends MailPage> T on(Class<T> pageClass) {
        Atlas atlas = new Atlas(new WebDriverConfiguration(webDriverRule.getDriver()));
        atlas.listener(new AllureListener());
        atlas.extension(new FindByCssExtension());
        atlas.extension(new FindByCssCollectionExtension());
        atlas.extension(new HoverMethodExtension());
        return atlas.create(webDriverRule.getDriver(), pageClass);
    }

    public CalPages cal() { return new CalPages(webDriverRule); }

    public MailPages mail() { return new MailPages(webDriverRule); }

    public TouchPages touch() { return new TouchPages(webDriverRule); }

    public HomerPage homer() { return on(HomerPage.class); }

    public PassportPage passport() {
        return on(PassportPage.class);
    }
}