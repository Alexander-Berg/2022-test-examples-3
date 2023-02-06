package ru.yandex.autotests.innerpochta.ns.pages.lite;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface SettingsLitePage extends MailPage {

    // Создать папку
    @FindByCss("[name = 'add']")
    WebElement addButton();

    // Удалить папку
    @FindByCss("[name = 'remove']")
    WebElement removeButton();

    // Переименовать папку
    @FindByCss("[name = 'rename']")
    WebElement renameButton();

    // Ссылка "Папки"
    @FindByCss("[href='/lite/setup/folders']")
    WebElement foldersLink();

    // Ссылка "Метки"
    @FindByCss("[href='/lite/setup/labels']")
    WebElement labelsLink();

    // Имя папки или метки
    @FindByCss("[id = 'name']")
    WebElement nameInput();

    // Кнопка подтвердить
    @FindByCss(".b-form-button:not([name])")
    MailElement submitButton();

    @FindByCss(".b-form-label")
    ElementsCollection<MailElement> folderNamesList();

    @FindByCss(".b-settings__list .b-label.b-label_rounded")
    ElementsCollection<MailElement> labelNamesList();
}
