package ru.yandex.autotests.innerpochta.ns.pages.lite.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by mabelpines on 17.11.15.
 */
public interface FooterLiteBlock extends MailElement{

    @Name("Ссылка - “Полная версия“")
    @FindByCss(".b-footer__link[href*='full']")
    MailElement fullVersionBtn();
}
