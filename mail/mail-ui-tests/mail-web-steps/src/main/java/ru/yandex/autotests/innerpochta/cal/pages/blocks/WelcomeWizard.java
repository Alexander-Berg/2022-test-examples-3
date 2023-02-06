package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface WelcomeWizard extends MailElement {

    @Name("Крестик")
    @FindByCss(".qa-WelcomeWizard-Closer")
    MailElement close();

    @Name("Создать встречу")
    @FindByCss(".qa-WelcomeWizard-CreateEvent")
    MailElement createEvent();

    @Name("Создать дело")
    @FindByCss(".qa-WelcomeWizard-CreateTodo")
    MailElement createTodo();

}
