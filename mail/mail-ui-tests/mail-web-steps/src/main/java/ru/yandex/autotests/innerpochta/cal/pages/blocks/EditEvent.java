package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author eremin-n-s
 */
public interface EditEvent extends MailElement {

    @Name("Поле «Название»")
    @FindByCss("[class*=EventFormField__field] input")
    MailElement nameField();

    @Name("Поле «Описание»")
    @FindByCss("[class*=EventDescriptionField__textArea] textarea")
    MailElement descriptionField();

    @Name("Яббл из поля «Добавили в календарь»")
    @FindByCss("[class*=EventSubscribersField__value] [class*=YabbleList__item]")
    ElementsCollection<MailElement> subscriberYabble();

    @Name("Яббл из поля «Участники»")
    @FindByCss("[class*=EventMembersField__value] [class*=YabbleList__item]")
    ElementsCollection<MailElement> participantYabble();

    @Name("Яббл «Ещё N человек»")
    @FindByCss("[class*=EventSubscribersField__toggle]")
    MailElement moreSubscribersYabble();

    @Name("Яббл «Ещё N человек» в поле «Участники»")
    @FindByCss("[class*=EventMembersField__toggle]")
    MailElement moreParticipantsYabble();

    @Name("Кнопка «Написать участникам»")
    @FindByCss(".qa-EventFormWriteInvitees")
    MailElement writeMail();

    @Name("Кнопка «Написать участникам» в фокусе")
    @FindByCss(".qa-EventFormWriteInvitees.button2_hovered_yes")
    MailElement writeMailHovered();

    @Name("Кнопка «Сохранить» на странице")
    @FindByCss(".qa-EventForm-SaveButton")
    MailElement saveChangesBtn();

    @Name("Кнопка «Закрыть» на странице")
    @FindByCss(".qa-EventForm-CancelButton")
    MailElement closeBtn();

    @Name("Поле занятости")
    @FindByCss("[class*=TimelineInterval__transformable]")
    ElementsCollection<MailElement> busyTime();

    @Name("Чекбокс «Повторять событие»")
    @FindByCss(".qa-DatesField-Repetition input")
    MailElement repeatEventCheckBox();

    @Name("Занятый участник")
    @FindByCss("[class*=Yabble__wrap_busy]")
    MailElement busyMember();

    @Name("Список стандартных уведомлений")
    @FindByCss("[class*=NotificationsFieldItem__wrap]")
    ElementsCollection <Notifications> notifyList();

    @Name("Кнопка «Пойду» на странице")
    @FindByCss(".qa-EventDecision-ButtonYes")
    MailElement acceptEventBtn();

    @Name("Кнопка «Не пойду» на странице")
    @FindByCss(".qa-EventDecision-ButtonNo")
    MailElement declineEventBtn();

    @Name("Кнопка решения о присутствии на встрече")
    @FindByCss(".qa-EventDecision-ButtonDecision")
    MailElement decisionEventBtn();

    @Name("Инпут «Время события. Начало»")
    @FindByCss(".qa-DatesField_Start-TimePicker input")
    MailElement timeStart();

    @Name("Инпут «Время события. Конец»")
    @FindByCss(".qa-DatesField_End-TimePicker input")
    MailElement timeEnd();

    @Name("Чекбокс «Весь день»")
    @FindByCss(".qa-DatesField-AllDay input")
    MailElement allDayCheckBox();

    @Name("Инпут «Место»")
    @FindByCss(".qa-LocationField input")
    MailElement locationInput();

    @Name("Инпут «Участники»")
    @FindByCss(".qa-MembersField input")
    MailElement membersInput();

    @Name("Инпут «Организатор»")
    @FindByCss(".qa-OrganizerField input")
    MailElement orgInput();
}
