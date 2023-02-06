package ru.yandex.calendar.frontend.ews.imp;

import javax.xml.datatype.DatatypeConfigurationException;

import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.ResponseTypeType;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.hook.EwsFirewallTestConfiguration;
import ru.yandex.calendar.frontend.ews.hook.EwsNtfContextConfiguration;
import ru.yandex.calendar.logic.beans.GenericBeanDao;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventHelper;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.SubjectType;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.TimeUtils;

import static ru.yandex.calendar.test.auto.db.util.TestManager.NEXT_YEAR;

@ContextConfiguration(classes = {
        EwsNtfContextConfiguration.class,
        EwsFirewallTestConfiguration.class
})
public class EwsImporterRemoveEventTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private UserManager userManager;
    @Autowired
    protected EwsImporter ewsImporter;
    @Autowired
    protected GenericBeanDao genericBeanDao;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventUserDao eventUserDao;

    @Test
    public void removeOfMeetingByUserOrganizer() throws DatatypeConfigurationException {
        removeOfMeeting(SubjectType.USER_ORGANIZER);
    }

    @Test
    public void removeOfMeetingByUserAttedee() throws DatatypeConfigurationException {
        removeOfMeeting(SubjectType.USER_ATTENDEE);
    }

    private void removeOfMeeting(SubjectType removingSubjectType) throws DatatypeConfigurationException {
        TestUserInfo attendeeUser = testManager.prepareRandomYaTeamUser(11);

        Instant now = TestDateTimes.moscow(NEXT_YEAR, 11, 21, 21, 28);

        final UidOrResourceId organizerSubject;
        final Email organizerEmail;
        final UidOrResourceId removingSubject;
        if (removingSubjectType.isResource()) {
            userManager.registerYandexUserForTest(TestManager.createResourceMaster());
            Resource r = testManager.cleanAndCreateThreeLittlePigs();
            organizerEmail = ResourceRoutines.getResourceEmail(r);
            organizerSubject = UidOrResourceId.resource(r.getId());
            removingSubject = organizerSubject;
        } else {
            TestUserInfo organizerUser = testManager.prepareRandomYaTeamUser(10);
            organizerEmail = organizerUser.getEmail();
            organizerSubject = UidOrResourceId.user(organizerUser.getUid());
            TestUserInfo removingUser = removingSubjectType.isAttendee() ? attendeeUser : organizerUser;
            removingSubject = UidOrResourceId.user(removingUser.getUid());
            testManager
                .createDefaultLayerForUser(organizerUser.getUid(), now);
        }

        // Create new event
        DateTime dateTime = new DateTime(NEXT_YEAR +1, 6, 7, 10, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        CalendarItemType calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                dateTime, "removeOfMeeting(" + removingSubjectType + ")");
        TestCalItemFactory.setOrganizer(calItem, organizerEmail);
        TestCalItemFactory.addAttendee(calItem, attendeeUser.getEmail(), ResponseTypeType.ACCEPT);

        ewsImporter.createOrUpdateEventForTest(
                organizerSubject, calItem, ActionInfo.exchangeTest(now), false);

        final String exchangeId = calItem.getItemId().getId();
        Event event = eventRoutines.findEventByExchangeId(exchangeId).get();
        ewsImporter.removeEvent(removingSubject, exchangeId, ActionInfo.exchangeTest());

        Option<Event> eventO = genericBeanDao.findBeanById(EventHelper.INSTANCE, event.getId());
        if (removingSubjectType.isAttendee()) {
            Assert.assertSome(eventO);
        } else {
            Assert.assertNone(eventO);
        }
    }

    @Test
    public void exchangeIdIsErasedFromEventUser() throws DatatypeConfigurationException {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(13);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(14);

        DateTime start = new DateTime(NEXT_YEAR, 5, 8, 19, 30, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        CalendarItemType calItem = TestCalItemFactory.createDefaultCalendarItemForImport(
                start, "exchangeIdIsErasedFromEventUser", start.toInstant());
        TestCalItemFactory.setOrganizer(calItem, creator.getEmail());
        TestCalItemFactory.addAttendee(calItem, attendee.getEmail(), ResponseTypeType.ACCEPT);

        UidOrResourceId subjectId = UidOrResourceId.user(attendee.getUid());
        ewsImporter.createOrUpdateEventForTest(subjectId, calItem, ActionInfo.exchangeTest(), false);

        String exchangeId = calItem.getItemId().getId();
        Assert.A.equals(attendee.getUid(), eventUserDao.findEventUserByExchangeId(exchangeId).get().getUid());

        ewsImporter.removeEvent(subjectId, exchangeId, ActionInfo.exchangeTest());

        Assert.A.none(eventUserDao.findEventUserByExchangeId(exchangeId));
    }

    @Test // CAL-9251
    public void removeOneOfMultipleUsersFromNonMeeting() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(15);
        TestUserInfo user = testManager.prepareRandomYaTeamUser(16);

        String userExchangeId = "userExchangeId";

        EventUser overrides = new EventUser();
        overrides.setExchangeId(userExchangeId);

        Event event = testManager.createDefaultEvent(creator.getUid(), "notMeetingStrict");
        testManager.createEventUser(creator.getUid(), event.getId(), Decision.YES, Option.empty());
        testManager.createEventUser(user.getUid(), event.getId(), Decision.YES, Option.empty(), overrides);

        ewsImporter.removeEvent(UidOrResourceId.user(user.getUid()), userExchangeId, ActionInfo.exchangeTest());

        Option<Decision> creatorDecision =
                eventUserDao.findEventUserByEventIdAndUid(event.getId(), creator.getUid()).map(EventUser::getDecision);

        Option<Decision> userDecision =
                eventUserDao.findEventUserByEventIdAndUid(event.getId(), user.getUid()).map(EventUser::getDecision);

        Assert.isTrue(creatorDecision.isSome(Decision.YES));
        Assert.isTrue(userDecision.isSome(Decision.NO));
    }
}
