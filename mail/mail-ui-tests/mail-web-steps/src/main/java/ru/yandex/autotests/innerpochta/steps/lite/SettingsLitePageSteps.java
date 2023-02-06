package ru.yandex.autotests.innerpochta.steps.lite;

import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.qatools.allure.annotations.Step;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;


public class SettingsLitePageSteps {

    private AllureStepStorage user;

    public SettingsLitePageSteps(AllureStepStorage user) {
        this.user = user;
    }

    @Step("Клик по папкам")
    public void clicksOnFoldersLink() {
        assertThat("Элемента нет на странице", user.pages().SettingsLitePage().foldersLink(), isPresent());
        user.pages().SettingsLitePage().foldersLink().click();
    }

    @Step("Клик по меткам")
    public void clicksOnLabelsLink() {
        assertThat("Элемента нет на странице", user.pages().SettingsLitePage().labelsLink(), isPresent());
        user.pages().SettingsLitePage().labelsLink().click();
    }

    @Step("Клик по кнопке добавить")
    public void clicksOnAddButton() {
        assertThat("Элемента нет на странице", user.pages().SettingsLitePage().addButton(), isPresent());
        user.pages().SettingsLitePage().addButton().click();
    }

    @Step("Клик по кнопке переименовать")
    public void clicksOnRenameButton() {
        assertThat("Элемента нет на странице", user.pages().SettingsLitePage().renameButton(), isPresent());
        user.pages().SettingsLitePage().renameButton().click();
    }

    @Step("Вводим имя «{0}»")
    public void entersName(String folderName) {
        user.pages().SettingsLitePage().nameInput().clear();
        user.pages().SettingsLitePage().nameInput().sendKeys(folderName);
    }

    @Step("Клик по кнопке сохранить")
    public void clicksOnSubmitButton() {
        assertThat("Элемента нет на странице", user.pages().SettingsLitePage().submitButton(), isPresent());
        user.pages().SettingsLitePage().submitButton().click();
    }

    @Step("Выбираем метку «{0}»")
    public void selectsOneWithName(String name) {
        user.defaultSteps().clicksOnElementWithText(user.pages().SettingsLitePage().folderNamesList(), name);
    }

    @Step("Клик по кнопке удалить")
    public void clicksOnRemoveButton() {
        assertThat("Элемента нет на странице", user.pages().SettingsLitePage().removeButton(), isPresent());
        user.pages().SettingsLitePage().removeButton().click();
    }

}