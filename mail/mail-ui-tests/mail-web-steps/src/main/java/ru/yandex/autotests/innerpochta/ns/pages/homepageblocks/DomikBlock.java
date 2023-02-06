package ru.yandex.autotests.innerpochta.ns.pages.homepageblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface DomikBlock extends MailElement {

    @Name("Кнопка «Почта»")
    @FindByCss("[href='https://mail.yandex.ru/']")
    MailElement inbox();

    @Name("Кнопка «Написать»")
    @FindByCss("[href='https://mail.yandex.ru//compose']")
    MailElement compose();
}
