package ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author puffyfloof
 */

public interface MessageListHeaderBlock extends MailElement {

    @Name("Кнопка бокового меню")
    @FindByCss(".topBar-item_left")
    MailElement sidebar();

    @Name("Счетчик непрочитанных писем")
    @FindByCss(".topBar-item_messageCounter-counter")
    MailElement unreadCounter();

    @Name("Кнопка поиска")
    @FindByCss(".topBar-item .ico_toolbar-search")
    MailElement search();

    @Name("Имя папки")
    @FindByCss(".topBar-item_messageCounter-title")
    MailElement folderName();

    @Name("Кнопка композа")
    @FindByCss(".topBar-item .ico_toolbar-compose")
    MailElement compose();

    @Name("Область, разворачивающая фильтры")
    @FindByCss(".topBar-item_messageCounter")
    MailElement filterName();

    @Name("Иконка календаря в шапке")
    @FindByCss(".topBar-item .ico_toolbar-calendar")
    MailElement calendar();
}
