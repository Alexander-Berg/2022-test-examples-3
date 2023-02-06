package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.openqa.selenium.WebElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author a-zoshchuk
 */
public interface EventDialogPopup extends MailElement {

    @Name("Крестик закрытия")
    @FindByCss("[class*=TouchModal__close]")
    MailElement close();

    @Name("Кнопка подтверждения / выбора одного события")
    @FindByCss(".button2_theme_action")
    MailElement confirmOrOneEventBtn();

    @Name("Кнопка отмены / выбора всей серии")
    @FindByCss(".button2_theme_normal")
    MailElement refuseOrAllEventsBtn();
}
