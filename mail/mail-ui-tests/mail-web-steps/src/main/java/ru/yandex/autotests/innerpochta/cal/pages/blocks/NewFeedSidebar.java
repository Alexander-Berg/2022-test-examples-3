package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author mariya-murm
 */
public interface NewFeedSidebar extends MailElement {

    @Name("Адрес подписки для импорта")
    @FindByCss(".qa-LayerUrl .textinput__control")
    MailElement urlInput();

    @Name("Название подписки")
    @FindByCss(".qa-LayerName .textinput__control")
    MailElement nameInput();

    @Name("Цвет")
    @FindByCss(".qa-ColorPicker-Color")
    ElementsCollection<MailElement> colors();

    @Name("Добавить уведомление")
    @FindByCss(".qa-NotificationsField-Add")
    MailElement addNotifyBtn();

    @Name("Создать")
    @FindByCss(".qa-AddFeed-Create")
    MailElement createBtn();
}
