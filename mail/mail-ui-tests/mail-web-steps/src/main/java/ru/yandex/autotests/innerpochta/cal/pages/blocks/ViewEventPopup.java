package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author cosmopanda
 */
public interface ViewEventPopup extends MailElement {

    @Name("Название")
    @FindByCss(".qa-EventFormPreview-Title")
    MailElement name();

    @Name("Поле «Место»")
    @FindByCss(".qa-LocationField")
    MailElement location();

    @Name("Поле «Описание»")
    @FindByCss(".qa-DescriptionField")
    MailElement description();

    @Name("Кнопка «Редактировать»")
    @FindByCss(".qa-EventFormPreview-EditButton")
    MailElement editEventBtn();

    @Name("Кнопка «Удалить»")
    @FindByCss(".qa-EventFormPreview-DeleteButton")
    MailElement deleteEventBtn();

    @Name("Кнопка «Убрать из календаря»")
    @FindByCss(".qa-EventFormPreview-DetachDetach")
    MailElement removeEventBtn();

    @Name("Кнопка «Клонировать»")
    @FindByCss(".qa-CloneEvent")
    MailElement cloneEventBtn();

    @Name("Кнопка «Копировать событие»")
    @FindByCss("[class*=EventCopyLinkButton_]")
    MailElement copyEventBtn();

    @Name("Кнопка «Написать участникам»")
    @FindByCss(".qa-EventFormWriteInvitees")
    MailElement writeMail();

    @Name("Кнопки-решения")
    @FindByCss(".qa-EventDecision")
    MailElement solutionsBtns();

    @Name("Кнопка Развернуть/Скрыть список участников встречи")
    @FindByCss(".qa-MembersField-MembersListToggler")
    MailElement membersListToggler();

    @Name("Кнопка «Закрыть» попап встречи")
    @FindByCss(".qa-EventFormPreview-Closer")
    MailElement closeEventBtn();

    @Name("Поле «Календарь»")
    @FindByCss(".qa-LayerField")
    MailElement layerField();

    @Name("Список участников встречи")
    @FindByCss("[class*=YabbleList__item]")
    ElementsCollection<MailElement> membersList();

    @Name("Поле «Переговорка»")
    @FindByCss(".qa-ResourcesField [class*=EventResourcesField__value] [class*=YabbleList__item]")
    MailElement roomYabble();

    @Name("Кнопка «Не пойду»")
    @FindByCss(".qa-EventDecision-ButtonNo")
    MailElement buttonNo();

    @Name("Кнопка «Пойду»")
    @FindByCss(".qa-EventDecision-ButtonYes")
    MailElement buttonYes();

    @Name("Кнопка решения в попапе принятой встречи")
    @FindByCss(".qa-EventDecision-ButtonDecision")
    MailElement buttonDecision();

    @Name("Зеленая галочка у участника")
    @FindByCss("[*|href='#accept']")
    MailElement acceptMark();

    @Name("Красный круг у участника")
    @FindByCss("[*|href='#busy']")
    MailElement declineMark();

    @Name("Знак вопроса у участника")
    @FindByCss("[*|href='#question']")
    MailElement questionMark();

    @Name("Время и дата")
    @FindByCss("[class*=EventDatesField__value]")
    MailElement timeAndDate();

    @Name("Символ повторения события")
    @FindByCss(".EventRepetition__value--1i69g")
    MailElement repeatSymbol();
}
