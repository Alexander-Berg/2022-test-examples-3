package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author pavponn 
 */
public interface EventBigDialogPopup extends MailElement {

    @Name("Крестик закрытия")
    @FindByCss("[class*=TouchModal__close]")
    MailElement close();
    
    @Name("Кнопка отмены")
    @FindByCss("[class*=TouchEventDecision__modalButton]:nth-child(1)")
    MailElement cancelBtn();

    @Name("Кнопка выбора текущего события")
    @FindByCss("[class*=TouchEventDecision__modalButton]:nth-child(2)")
    MailElement onlyCurrentEventBtn();

    @Name("Кнопка выбора всей серии")
    @FindByCss("[class*=TouchEventDecision__modalButton]:nth-child(3)")
    MailElement allEventsBtn();

    @Name("Текстовое поле для комментария")
    @FindByCss("[class*=modalTextArea] textarea")
    MailElement textCommentArea();
}
