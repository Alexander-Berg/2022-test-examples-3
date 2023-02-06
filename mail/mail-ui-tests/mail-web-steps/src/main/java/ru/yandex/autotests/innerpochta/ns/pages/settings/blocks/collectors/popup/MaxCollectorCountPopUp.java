package ru.yandex.autotests.innerpochta.ns.pages.settings.blocks.collectors.popup;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface MaxCollectorCountPopUp extends MailElement {

    String TEXT = "Можно создать не более 10 сборщиков почты." +
            " Чтобы создать новый сборщик, сначала удалите один из старых.";

    @Name("Кнопка «Закрыть»")
    @FindByCss("button[data-dialog-action='dialog.cancel']")
    MailElement closePopUpButton();
}
