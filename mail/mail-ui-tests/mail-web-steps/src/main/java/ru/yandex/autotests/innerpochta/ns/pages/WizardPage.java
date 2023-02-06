package ru.yandex.autotests.innerpochta.ns.pages;

import ru.yandex.autotests.innerpochta.atlas.MailPage;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.ns.pages.wizard.NewWizardThemeBlock;
import ru.yandex.autotests.innerpochta.ns.pages.wizard.WizardGreetingBlock;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * Created by cosmopanda on 20.05.2016.
 */
public interface WizardPage extends MailPage {

    @Name("Кнопка «Следующий шаг»")
    @FindByCss(".mail-WelcomeWizard-Modern-Footer-NextStep")
    MailElement continueBtn();

    @Name("Попап визарда")
    @FindByCss(".mail-WelcomeWizard-Modern-Content")
    MailElement newWelcomeWizard();

    @Name("Приветственный экран")
    @FindByCss(".mail-WelcomeWizard-Modern-EdgeStep-Greeting")
    WizardGreetingBlock greetingStep();

    @Name("Экран выбора тем")
    @FindByCss(".mail-WelcomeWizard-Modern-Step_SelectTheme")
    NewWizardThemeBlock newThemeStep();

    @Name("Крестик для закрытия визарда")
    @FindByCss(".js-welcome-wizard-close")
    MailElement closeWizard();
}
