package ru.yandex.autotests.innerpochta.ns.pages.homer;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author crafty
 */
public interface HomerFooterBlock extends MailElement {

    @Name("Справка")
    @FindByCss("[href*='support/mail/?from=mail']")
    MailElement helpLink();

    @Name("Ссылка на Условия использования")
    @FindByCss("[href*='legal']")
    MailElement legalLink();

    @Name("Ссылка на Круглосуточную поддержку")
    @FindByCss("[href*='troubleshooting']")
    MailElement troubleshootingLink();
}
