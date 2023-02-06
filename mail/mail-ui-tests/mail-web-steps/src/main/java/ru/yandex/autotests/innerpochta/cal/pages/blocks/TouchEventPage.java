package ru.yandex.autotests.innerpochta.cal.pages.blocks;

import io.qameta.atlas.webdriver.extension.Name;

import io.qameta.atlas.webdriver.ElementsCollection;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.atlas.FindByCss;

/**
 * @author a-zoshchuk
 */
public interface TouchEventPage extends MailElement {

    @Name("Карандаш редактирования")
    @FindByCss("[class*=TouchHeader__control]:last-child")
    MailElement edit();

    @Name("Отмена редактирования события")
    @FindByCss("[class*=TouchHeader__control]:first-child")
    MailElement cancelEdit();

    @Name("Галочка сохранения изменний")
    @FindByCss("[class*=TouchEventFormSubmit__wrap]")
    MailElement submitForm();

    @Name("Заголовок редактируемого события")
    @FindByCss("[class*=TouchEventFormField__wrap]:nth-child(1) input")
    MailElement editableTitle();

    @Name("Описание редактируемого события")
    @FindByCss("[class*=TouchEventFormField__wrap]:nth-child(2) textarea")
    MailElement editableDescription();

    @Name("Заголовок события")
    @FindByCss("[class*=TouchEventFormField__wrap]:nth-child(1)")
    MailElement title();

    @Name("Описание события")
    @FindByCss("[class*=TouchEventFormField__wrap]:nth-child(2)")
    MailElement description();

    @Name("Ворнинг периодичности редактируемого события")
    @FindByCss("[class*=TouchEventRepetitionWarning__wrap]")
    MailElement repetitionWarning();

    @Name("Ссылка перехода к редактированию одного события серии / всей серии")
    @FindByCss("[class*=TouchEventRepetitionWarning__text] span span")
    MailElement changeEditTypeLink();

    @Name("«Указать место» на страничке события")
    @FindByCss("[class*=TouchEventLocationField]")
    MailElement changePlace();

    @Name("Кнопка добавления участника")
    @FindByCss("[class*=TouchEventMembersField__modalOpener]")
    MailElement changeParticipants();

    @Name("Кнопка изменения слоя")
    @FindByCss("[class*=TouchEventLayerField__value]")
    MailElement editLayer();

    @Name("Кнопка изменения занятости")
    @FindByCss("[class*=TouchEventAvailabilityField__value]")
    MailElement editAvailability();

    @Name("Измененение повтора события")
    @FindByCss("[class*=TouchEventRepetition]")
    MailElement editEventRepetition();

    @Name("Место события")
    @FindByCss("[class*=TouchEventLocationField__text]")
    MailElement place();

    @Name("Список участников события")
    @FindByCss("[class*=TouchMember__wrap]")
    ElementsCollection<MailElement> members();

    @Name("Слой события")
    @FindByCss("[class*=TouchEventLayerField__name]")
    MailElement layer();

    @Name("Занятость во время события")
    @FindByCss("[class*=TouchEventAvailabilityField__value]")
    MailElement availability();

    @Name("Занятость во время события")
    @FindByCss("[class*=TouchEventDatesField__subfield]:nth-child(4) input")
    MailElement allDayCheckBox();

    @Name("Время события")
    @FindByCss("[class*=TouchEventDatesField__value]")
    MailElement eventDate();

    @Name("Повторение события")
    @FindByCss("[class*=TouchEventDatesField__repetitionValue]")
    MailElement eventRepetition();

    @Name("Время начала")
    @FindByCss("[class*=TouchEventDatesField__subfield]:nth-child(2) [class*=TouchEventDatesField__date]")
    MailElement startDate();

    @Name("Кнопка «Ещё n участников» / Скрыть участников")
    @FindByCss("[class*=TouchEventMembersField__buttonContainer] button")
    MailElement showMoreParticipants();

    @Name("Кнопка удаления события")
    @FindByCss("[class*=TouchEventFormDelete__button]")
    MailElement delete();

    @Name("Плашка «Событие прошло»")
    @FindByCss("[class*=TouchEventPast__pastEvent]")
    MailElement eventPastInformer();

    @Name("Кнопка «Написать участникам»")
    @FindByCss("[class*=TouchEventFormWriteInvitees__button]")
    MailElement writeToAllParticipants();

    @Name("Тумблер «Участники могут редактировать событие»")
    @FindByCss("[class*=TouchEventAccessField__subfield]:nth-child(2) input")
    MailElement canEditEventTumbler();

    @Name("Тумблер «Участники могут приглашать других»")
    @FindByCss("[class*=TouchEventAccessField__subfield]:nth-child(3) input")
    MailElement canInviteOthersTumbler();

    @Name("Кнопки принятия/отклонения события")
    @FindByCss("[class*=TouchEventDecision__wrap] button")
    ElementsCollection<MailElement> eventDecisionButtons();

    @Name("Принятое решение на кнопках принятия события")
    @FindByCss("[class*=TouchEventDecision__yes]")
    MailElement checkedEventDecision();

    @Name("Кнопка «Добавить переговорку»")
    @FindByCss("[class*=TouchEventResourcesField__addButton]")
    MailElement addRoomButton();

    @Name("Кнопка «Изменить организатора»")
    @FindByCss("[class*=TouchMember__wrap]")
    MailElement addOrganizatorButton();

    @Name("Список переговорок")
    @FindByCss("[class*=TouchResource__wrap]")
    ElementsCollection<RoomSuggestBlock> roomsList();

    @Name("Список занятых переговорок")
    @FindByCss("[class*=TouchResource__busy]")
    ElementsCollection<MailElement> busyRoomsList();

    @Name("Крестик для удаления переговорок")
    @FindByCss("[class*=TouchResource] svg")
    MailElement remove();

    @Name("Дата начала события")
    @FindByCss("[class*=TouchEventDatesField__subfield]:nth-child(2) input")
    MailElement startDateInput();

    @Name("Дата окончания события")
    @FindByCss("[class*=TouchEventDatesField__subfield]:nth-child(3) input")
    MailElement finishDateInput();

    @Name("Занятый участник")
    @FindByCss("[class*=TouchMember__busy]")
    MailElement busyMember();

    @Name("Свободный участник")
    @FindByCss("[class*=TouchMember__free]")
    MailElement freeMember();
}
