package ru.yandex.autotests.innerpochta.ns.pages.composeblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 03.10.12
 * Time: 18:30
 */
public interface ComposePageFileAttachmentBlock extends MailElement {

    @Name("Имя файла")
    @FindByCss(".b-file__text")
    MailElement attachDescription();

    @Name("Прогрессбар")
    @FindByCss(".js_progress")
    MailElement progressBar();

    @Name("Кнопка Удалить")
    @FindByCss(".js-mark-deleted")
    MailElement deleteBtn();

    @Name("Кнопка Восстановить")
    @FindByCss(".js-unmark-deleted")
    MailElement restoreBtn();
}
