package ru.yandex.autotests.innerpochta.touch.pages.messagelistblocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author oleshko
 */

public interface TabsOnboarding extends MailElement{

    @Name("Заголовок слайда")
    @FindByCss(".onboarding-item_isActive .onboarding-slide-title")
    MailElement title();

    @Name("Крестик закрытия")
    @FindByCss(".onboarding-close-button")
    MailElement cross();

    @Name("Кнопка «Не сейчас»")
    @FindByCss(".onboarding-slide-button_secondary")
    MailElement notNowBtn();

    @Name("Кнопка «Далее» или «Включить»")
    @FindByCss(".onboarding-item_isActive .onboarding-slide-button_primary")
    MailElement yesBtn();
}

