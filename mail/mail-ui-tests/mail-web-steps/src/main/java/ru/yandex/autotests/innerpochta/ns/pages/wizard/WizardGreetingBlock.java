package ru.yandex.autotests.innerpochta.ns.pages.wizard;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

public interface WizardGreetingBlock extends MailElement {

    @Name("Узнать")
    @FindByCss(".mail-WelcomeWizard-Modern-Step-Action .button2")
    MailElement learnMoreBtn();

    @Name("Закрыть")
    @FindByCss(".mail-WelcomeWizard-Modern-Close")
    MailElement close();
}
