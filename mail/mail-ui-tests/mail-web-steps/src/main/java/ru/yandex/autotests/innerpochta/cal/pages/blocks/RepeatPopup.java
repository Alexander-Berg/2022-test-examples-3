package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author eremin-n-s
 */
public interface RepeatPopup extends MailElement {

    @Name("Инпут поля «Повторять до»")
    @FindByCss("[class*=EventRepetition__dueDateInput] input")
    MailElement repeatUntilInput();

    @Name("Выбранный период повторения")
    @FindByCss(".Select2-Button .Button2-Text")
    MailElement repeatPeriodChosen();

    @Name("Выбранный день недели")
    @FindByCss(".Checkbox_checked label")
    MailElement weekDayChosen();

    @Name("Дни недели")
    @FindByCss(".Checkbox input")
    ElementsCollection<MailElement> weekDay();

    @Name("Результат выбранных опций в футере")
    @FindByCss("[class*=EventRepetition__footer-]")
    MailElement repeatInfo();
}
