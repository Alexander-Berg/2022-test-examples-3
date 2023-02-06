package ru.yandex.calendar.logic.event.web;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventInvitation;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.EventWithRelations;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventInvitationData;
import ru.yandex.calendar.logic.event.model.EventInvitationsData;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.ResourceType;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.user.Group;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractEwsExportedLoginsTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * CAL-6466, CAL-6511
 *
 * @author dbrylev
 */
@AbstractEwsExportedLoginsTest.WantsEws
public class EventWebManagerOccupyApartmentTest extends AbstractEwsExportedLoginsTest {
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private MailSenderMock mailSender;


    private Resource apartment;
    private Email apartmentEmail;

    private TestUserInfo customer;
    private TestUserInfo customerFriend;

    private TestUserInfo apartmentAdmin;
    private TestUserInfo superUser;

    private final Email subscriberEmail = new Email("apartments@yandex-team.ru"); // CAL-6649

    public EventWebManagerOccupyApartmentTest(EwsUsage ewsUsage) {
        super(ewsUsage);
    }

    @Before
    public void setUpAll() {
        super.setUpAll();
        cleanBeforeTest();
        setIsEwserIfNeeded(apartmentAdmin);
    }

    private void cleanBeforeTest() {
        apartment = testManager.cleanAndCreateResourceWithNoExchSync("ap_138", "Квартира #138", ResourceType.APARTMENT);
        apartmentEmail = ResourceRoutines.getResourceEmail(apartment);

        customer = testManager.prepareYandexUser(TestManager.createSsytnik());
        customerFriend = testManager.prepareYandexUser(TestManager.createAkirakozov());

        apartmentAdmin = testManager.prepareYandexUser(TestManager.createHelga(), Group.APARTMENT_ADMIN);
        superUser = testManager.prepareYandexUser(TestManager.createDbrylev(), Group.SUPER_USER);

        testManager.setValue(eventInvitationManager.apartmentSubscribers, Cf.list(subscriberEmail.getEmail()));

        mailSender.clear();
    }

    @Test
    public void createByNotOrganizer() {
        EventData data = testManager.createDefaultEventData(apartmentAdmin.getUid(), "Проживание");
        data.setInvData(Option.of(apartmentAdmin.getEmail()), apartmentEmail, customer.getEmail());

        EventWithRelations event = createEvent(superUser, data);

        Assert.isEmpty(event.getEventLayers());

        Assert.some(Decision.NO, event.findUserEventUserDecision(apartmentAdmin.getUid()));
        Assert.some(Decision.NO, event.findUserEventUserDecision(customer.getUid()));
        Assert.none(event.findUserEventUser(superUser.getUid()));

        Assert.some(apartment.getId(), event.getResourceIds().singleO());

        Assert.hasSize(3, mailSender.getEventMailTypes());
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(apartmentAdmin.getEmail()));
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(customer.getEmail()));
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(subscriberEmail));
    }

    @Test
    public void actorIsCustomer() {
        EventData data = testManager.createDefaultEventData(apartmentAdmin.getUid(), "Проживание");
        data.setInvData(Option.of(superUser.getEmail()), apartmentEmail, apartmentAdmin.getEmail());

        EventWithRelations event = createEvent(apartmentAdmin, data);

        Assert.isEmpty(event.getEventLayers());

        Assert.hasSize(3, mailSender.getEventMailTypes());
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(superUser.getEmail()));
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(apartmentAdmin.getEmail()));
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(subscriberEmail));
    }

    @Test
    public void changeCustomerByNotOrganizer() {
        EventData data = testManager.createDefaultEventData(apartmentAdmin.getUid(), "Проживание");

        data.setInvData(Option.of(apartmentAdmin.getEmail()), apartmentEmail, customer.getEmail());
        EventWithRelations event = createEvent(apartmentAdmin, data);

        Assert.hasSize(3, mailSender.getEventMailTypes());
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(apartmentAdmin.getEmail()));
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(customer.getEmail()));
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(subscriberEmail));

        mailSender.clear();
        data.setInvData(Option.of(apartmentAdmin.getEmail()), apartmentEmail, customerFriend.getEmail());
        EventWithRelations updated = updateEvent(superUser, event, data);

        Assert.isEmpty(updated.getEventLayers());

        Assert.some(false, updated.findUserEventUser(customer.getUid()).map(EventUser.getIsAttendeeF()));
        Assert.some(Decision.NO, updated.findUserEventUserDecision(apartmentAdmin.getUid()));
        Assert.some(Decision.NO, updated.findUserEventUserDecision(customerFriend.getUid()));
        Assert.none(updated.findUserEventUser(superUser.getUid()));

        Assert.some(apartment.getId(), updated.getResourceIds().singleO());

        Assert.hasSize(4, mailSender.getEventMailTypes());
        Assert.some(MailType.APARTMENT_UPDATE, mailSender.findEventMailType(apartmentAdmin.getEmail()));
        Assert.some(MailType.APARTMENT_CANCEL, mailSender.findEventMailType(customer.getEmail()));
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(customerFriend.getEmail()));
        Assert.some(MailType.APARTMENT_UPDATE, mailSender.findEventMailType(subscriberEmail));
    }

    @Test
    public void changeExternalCustomer() {
        Email externalCustomer1 = new Email("customer1@somewhere.com");
        Email externalCustomer2 = new Email("customer2@somewhere.com");

        EventData data = testManager.createDefaultEventData(apartmentAdmin.getUid(), "Проживание");
        data.setInvData(Option.of(apartmentAdmin.getEmail()), apartmentEmail, externalCustomer1);

        EventWithRelations event = createEvent(apartmentAdmin, data);
        Assert.hasSize(1, event.getInvitations().map(EventInvitation.getEmailF()));

        Assert.hasSize(3, mailSender.getEventMailTypes());
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(apartmentAdmin.getEmail()));
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(externalCustomer1));
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(subscriberEmail));

        mailSender.clear();
        data.setInvData(Option.of(apartmentAdmin.getEmail()), apartmentEmail, externalCustomer2);
        EventWithRelations updated = updateEvent(apartmentAdmin, event, data);

        Assert.hasSize(1, updated.getInvitations().map(EventInvitation.getEmailF()));
        Assert.equals(externalCustomer2, updated.getInvitations().single().getEmail());

        Assert.hasSize(4, mailSender.getEventMailTypes());
        Assert.some(MailType.APARTMENT_UPDATE, mailSender.findEventMailType(apartmentAdmin.getEmail()));
        Assert.some(MailType.APARTMENT_CANCEL, mailSender.findEventMailType(externalCustomer1));
        Assert.some(MailType.APARTMENT_INVITATION, mailSender.findEventMailType(externalCustomer2));
        Assert.some(MailType.APARTMENT_UPDATE, mailSender.findEventMailType(subscriberEmail));
    }

    @Test
    public void changeCustomerName() {
        Email externalEmail = new Email("customer@somewhere.com");
        String nameBefore = "Name Before";
        String nameAfter = "Name After";

        EventData data = testManager.createDefaultEventData(apartmentAdmin.getUid(), "Проживание");
        data.setInvData(new EventInvitationsData(Option.of(apartmentAdmin.getEmail()),
                Cf.list(new EventInvitationData(externalEmail, Option.of(nameBefore), false))).plusEmail(apartmentEmail));

        EventWithRelations event = createEvent(apartmentAdmin, data);
        Assert.some(nameBefore, event.findInvitation(externalEmail).map(EventInvitation.getNameF()));

        mailSender.clear();
        data.setInvData(new EventInvitationsData(Option.of(apartmentAdmin.getEmail()),
                Cf.list(new EventInvitationData(externalEmail, Option.of(nameAfter), false))).plusEmail(apartmentEmail));

        EventWithRelations updated = updateEvent(apartmentAdmin, event, data);
        Assert.some(nameAfter, updated.findInvitation(externalEmail).map(EventInvitation.getNameF()));

        Assert.hasSize(2, mailSender.getEventMailTypes());
        Assert.some(MailType.APARTMENT_UPDATE, mailSender.findEventMailType(apartmentAdmin.getEmail()));
        Assert.some(MailType.APARTMENT_UPDATE, mailSender.findEventMailType(subscriberEmail));
    }

    @Test
    public void cancelOccupation() {
        EventData data = testManager.createDefaultEventData(apartmentAdmin.getUid(), "Проживание");

        data.setInvData(Option.of(apartmentAdmin.getEmail()), apartmentEmail, customer.getEmail());
        EventWithRelations event = createEvent(apartmentAdmin, data);

        mailSender.clear();
        deleteEvent(apartmentAdmin, event);

        Assert.hasSize(3, mailSender.getEventMailTypes());
        Assert.some(MailType.APARTMENT_CANCEL, mailSender.findEventMailType(apartmentAdmin.getEmail()));
        Assert.some(MailType.APARTMENT_CANCEL, mailSender.findEventMailType(customer.getEmail()));
        Assert.some(MailType.APARTMENT_CANCEL, mailSender.findEventMailType(subscriberEmail));
    }

    private EventWithRelations createEvent(TestUserInfo actor, EventData eventData) {
        eventData.setTimeZone(MoscowTime.TZ);
        Event event = eventWebManager.createUserEvent(
                actor.getUid(), eventData, InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest()).getEvent();
        setIsExportedWithEwsIfNeeded(event);
        return eventDbManager.getEventWithRelationsByEvent(event);
    }

    private EventWithRelations updateEvent(TestUserInfo actor, EventWithRelations event, EventData updateData) {
        updateData.getEvent().setId(event.getId());
        updateData.setExternalId(Option.empty());

        eventWebManager.update(actor.getUserInfo(), updateData, false, ActionInfo.webTest());

        return eventDbManager.getEventWithRelationsById(event.getId());
    }

    private void deleteEvent(TestUserInfo actor, EventWithRelations event) {
        eventWebManager.deleteEvent(
                actor.getUserInfo(), event.getId(), Option.<Instant>empty(), false, ActionInfo.webTest());
    }
}
