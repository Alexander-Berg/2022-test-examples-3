package ru.yandex.calendar.frontend.webNew;

import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.ReadableInstant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.frontend.bender.WebDate;
import ru.yandex.calendar.frontend.bender.WebDateUtils;
import ru.yandex.calendar.frontend.webNew.actions.AvailabilityActions;
import ru.yandex.calendar.frontend.webNew.actions.ResourcesActions;
import ru.yandex.calendar.frontend.webNew.dto.in.AvailParameters;
import ru.yandex.calendar.frontend.webNew.dto.in.IntervalAndRepetitionData;
import ru.yandex.calendar.frontend.webNew.dto.out.MoveResourceEventsIds;
import ru.yandex.calendar.frontend.webNew.dto.out.ResourcesInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.ResourcesSchedule;
import ru.yandex.calendar.logic.beans.generated.*;
import ru.yandex.calendar.logic.event.dao.EventResourceDao;
import ru.yandex.calendar.logic.resource.ResourceDao;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.SpecialResources;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.util.dates.DateInterval;
import ru.yandex.calendar.util.dates.DateOrDateTime;
import ru.yandex.commune.dynproperties.DynamicPropertyRegistry;
import ru.yandex.commune.mapObject.MapField;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox.PassportDomain;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.time.MoscowTime;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class WebNewResourcesManagerTest extends WebNewTestWithResourcesBase {
    @Autowired
    private AvailabilityActions availabilityActions;
    @Autowired
    private ResourcesActions resourcesActions;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private DynamicPropertyRegistry dynamicPropertyRegistry;
    @Autowired
    private WebNewResourcesManager webNewResourcesManager;
    @Autowired
    private EventResourceDao eventResourceDao;

    private static final DateTime NOW = MoscowTime.dateTime(2017, 12, 21, 18, 0);

    @Override
    protected ReadableInstant now() {
        return NOW;
    }

    @Test
    public void findResources() {
        assertThat(resourcesActions.findResources(
                uid, Option.of(office.getId()), Option.empty(), Option.empty(), Option.empty()).getEmails())
                .containsExactly(resource1Email, resource2Email);

        val rs = testManager.cleanAndCreateResource("resource_3", "Переговорная", office);

        assertThat(resourcesActions.findResources(
                uid, Option.of(office.getId()), Option.empty(), Option.of("gthtujdjh"), Option.empty()).getEmails())
                .containsExactly(ResourceRoutines.getResourceEmail(rs));

        val uuid = "x" + UUID.randomUUID().toString();

        val rs1 = testManager.cleanAndCreateResource("resource_x_1", "1. First " + uuid);
        val rs2 = testManager.cleanAndCreateResource("resource_x_2", uuid + " Another");

        assertThat(resourcesActions.findResources(
                uid, Option.empty(), Option.empty(), Option.of(uuid.substring(0, 5)), Option.empty()).getEmails())
                .containsExactlyElementsOf(StreamEx.of(rs1, rs2).map(ResourceRoutines::getResourceEmail));
    }

    @Test
    public void findAvailableResourcesSingle() {
        createResourceEvent(NOW, resource1);

        val interval = new IntervalAndRepetitionData(NOW.toLocalDateTime(), NOW.plusHours(1).toLocalDateTime(), Option.empty());

        assertThat(findAvailResources(
                "", 10, Cf.list(office.getId(), nextOfficeResource.getOfficeId()), interval).getEmails())
                .containsExactly(resource2Email, nextOfficeResourceEmail);

        assertThat(findAvailResources(
                "", 10, Cf.list(nextOfficeResource.getOfficeId(), office.getId()), interval).getEmails())
                .containsExactly(nextOfficeResourceEmail, resource2Email);
    }

    @Test
    public void findAvailableResourcesRepeating() {
        createResourceEvent(NOW.plusDays(3), resource2);

        val interval = new IntervalAndRepetitionData(
                NOW.toLocalDateTime(), NOW.plusHours(1).toLocalDateTime(), Option.of(consDailyRepetitionData()));

        val result1 = findAvailResources("Resource", 10, Cf.list(office.getId()), interval);
        val result2 = findAvailResources("Resource 2", 10, Cf.list(office.getId()), interval);

        assertThat(result1.getEmails()).containsExactly(resource1Email, resource2Email);
        assertThat(result1.find(resource1Email).get().getAvailability().getAvailability()).isEmpty();
        assertThat(result1.find(resource2Email).get().getAvailability().getAvailability()).isEmpty();

        assertThat(result2.getEmails()).containsExactly(resource2Email);
        assertThat(result2.find(resource2Email).get().getAvailability().getAvailability().toOptional()).hasValue("available");

        assertThat(result2.find(resource2Email).get().getAvailability().getAvailableRepetitions().toOptional()).hasValue(3);
        assertThat(result2.find(resource2Email).get().getAvailability().getDueDate().toOptional()).hasValue(NOW.plusDays(2).toLocalDate());
    }

    private static DateOrDateTime convert(DateTime dateTime) {
        return DateOrDateTime.dateTime(dateTime.toLocalDateTime());
    }

    @Test
    public void checkRestrictionsForNonAdminUsers() {
        val resource = testManager.cleanAndCreateResource("resource_3", "Resource 3", office);
        val oldList = SpecialResources.dateRestrictedRooms.get();

        try {
            val restriction = new SpecialResources.RoomsDates(
                    new SpecialResources.Rooms(Cf.list("resource_3"), Cf.list(), Cf.list(), Cf.list()),
                    new DateInterval(Option.of(convert(NOW.minusDays(3))),
                            Option.of(convert(NOW.plusDays(3))))
            );

            dynamicPropertyRegistry.setValue(SpecialResources.dateRestrictedRooms, Cf.list(restriction));
            val email = ResourceRoutines.getResourceEmail(resource);

            val schedule = resourcesActions.getResourcesSchedule(uid, Cf.list(office.getId()),
                    Cf.list(), Option.empty(), Cf.list(email), Option.of(WebDate.localDate(NOW.minusDays(5).toLocalDate())),
                    Option.of(WebDate.localDate(NOW.toLocalDate())), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty());
            assertThat(schedule.getResources()).hasSize(1);
            val restrictions = StreamEx.of(schedule.getResources()).toFlatList(ResourcesSchedule.Resource::getRestrictions);
            assertThat(restrictions).hasSize(1);
        } finally {
            dynamicPropertyRegistry.setValue(SpecialResources.dateRestrictedRooms, oldList);
        }
    }

    @Test
    public void findAvailableResourcesLimit() {
        val otherOffice = testManager.createDefaultOffice();

        val resource3 = testManager.cleanAndCreateResource("resource_3", "Resource 3", otherOffice);
        val resource4 = testManager.cleanAndCreateResource("resource_4", "Resource 4", otherOffice);
        val resource5 = testManager.cleanAndCreateResource("resource_5", "Resource 5", otherOffice);

        val interval = new IntervalAndRepetitionData(NOW.toLocalDateTime(), NOW.plusHours(1).toLocalDateTime(), Option.empty());

        assertThat(findAvailResources("", 10, Cf.list(office.getId(), otherOffice.getId()), interval).getEmails())
                .containsExactlyElementsOf(StreamEx.of(resource1, resource2, resource3, resource4, resource5).map(ResourceRoutines::getResourceEmail));

        assertThat(findAvailResources("", 2, Cf.list(office.getId(), otherOffice.getId()), interval).getEmails())
                .containsExactlyElementsOf(StreamEx.of(resource1, resource2, resource3, resource4).map(ResourceRoutines::getResourceEmail));

        assertThat(findAvailResources("", 1, Cf.list(office.getId(), otherOffice.getId()), interval).getEmails())
                .containsExactlyElementsOf(StreamEx.of(resource1, resource3).map(ResourceRoutines::getResourceEmail));
    }

    private ResourcesInfo findAvailResources(
            String query, int limit, ListF<Long> officeIds, IntervalAndRepetitionData interval)
    {
        return resourcesActions.findAvailableResources(
                uid, Option.of(query), officeIds, Cf.list(), Cf.list(),
                interval, AvailParameters.empty(), Option.of(limit), Option.empty());
    }

    @Test
    public void findFilteredResources() {
        val domain = PassportDomain.YANDEX_TEAM_RU.getDomain();

        val firstOffice = testManager.createOffice(domain, "First Office");
        val secondOffice = testManager.createOffice(domain, "Second Office");

        val resource1 = testManager.createResource("r1","r1", firstOffice);
        val resource2 = testManager.createResource("r2","r2", firstOffice);
        val resource3 = testManager.createResource("r3","r3", secondOffice);
        val resource4 = testManager.createResource("r4","r4", secondOffice);

        setResourcesField(Cf.list(resource1, resource3), ResourceFields.DESK, true);
        setResourcesField(Cf.list(resource2, resource4), ResourceFields.MARKER_BOARD, true);

        val interval = new IntervalAndRepetitionData(NOW.toLocalDateTime(), NOW.plusHours(1).toLocalDateTime(), Option.empty());

        val info = resourcesActions.findAvailableResources(
                uid, Option.empty(), Cf.list(firstOffice, secondOffice).map(Office::getId),
                Cf.list("desk", "marker_board"), Cf.list(),
                interval, AvailParameters.empty(), Option.empty(), Option.empty());

        assertThat(info.getFoundTotal()).isEqualTo(2);
        assertThat(info.getEmails().map(Email::getLocalPart).unique())
                .containsExactlyElementsOf(Cf.list(resource1, resource4).filterMap(Resource::getExchangeName).unique());
    }

    private <T> void setResourcesField(ListF<Resource> resources, MapField<T> field, T value) {
        resources.forEach(resource -> {
            final Resource modified = resource.copy();
            modified.setFieldValue(field, value);
            resourceDao.updateResource(modified);
        });
    }

    @Test
    public void getResourcesScheduleOrder() {
        createResourceEvent(NOW.plusHours(1), resource2);
        createResourceEvent(NOW.minusHours(1), resource2, nextOfficeResource);

        var schedule = getResourcesSchedule(uid, Cf.list(office.getId(), nextOfficeResource.getOfficeId()), Option.empty());

        assertThat(schedule.find(nextOfficeResourceEmail).get().getEventStarts())
                .containsExactly(NOW.minusHours(1).toLocalDateTime());

        assertThat(schedule.find(resource2Email).get().getEventStarts())
                .containsExactlyElementsOf(StreamEx.of(NOW.minusHours(1), NOW.plusHours(1)).map(DateTime::toLocalDateTime));

        assertThat(schedule.getEmails()).containsExactly(resource1Email, resource2Email, nextOfficeResourceEmail);

        schedule = getResourcesSchedule(
                uid, Cf.list(nextOfficeResource.getOfficeId(), office.getId()), Option.empty());

        assertThat(schedule.getEmails()).containsExactly(nextOfficeResourceEmail, resource1Email, resource2Email);
    }

    @Test
    public void moveResourcesSchedules() {
        final Event resourceEvent1 = createResourceEvent(NOW, resource1);
        final Event resourceEvent2 = createResourceEvent(NOW.plusHours(1), resource1);
        createResourceEvent(NOW.plusHours(1), resource2);
        createResourceEvent(NOW.minusHours(1), resource2, nextOfficeResource);
        final Event resourceEvent3 = createResourceEvent(NOW.plusDays(2), resource1);

        {
            final ListF<EventResource> eventResourcesByEventId =
                    eventResourceDao.findEventResourcesByEventId(resourceEvent1.getId());
            assertThat(eventResourcesByEventId.size()).isEqualTo(1);
            assertThat(eventResourcesByEventId.first().getResourceId()).isEqualTo(resource1.getId());
        }

        final ListF<MoveResourceEventsIds> moveResourceEventsIds = webNewResourcesManager.moveResourcesSchedules(
                resource1.getId(), resource2.getId(),
                WebDateUtils.serialize(NOW.toLocalDate()), 10);
        assertThat(moveResourceEventsIds.size()).isEqualTo(2);
        assertThat(moveResourceEventsIds.first().getSourceEventId())
                .isEqualTo(resourceEvent1.getId().longValue());

        {
            final ListF<EventResource> eventResourcesByEventId =
                    eventResourceDao.findEventResourcesByEventId(resourceEvent1.getId());
            assertThat(eventResourcesByEventId.size()).isEqualTo(1);
            assertThat(eventResourcesByEventId.first().getResourceId()).isEqualTo(resource2.getId());
        }
        {
            final ListF<EventResource> eventResourcesByEventId =
                    eventResourceDao.findEventResourcesByEventId(resourceEvent2.getId());
            assertThat(eventResourcesByEventId.size()).isEqualTo(1);
            assertThat(eventResourcesByEventId.first().getResourceId()).isEqualTo(resource1.getId());
        }
        {
            final ListF<EventResource> eventResourcesByEventId =
                    eventResourceDao.findEventResourcesByEventId(resourceEvent3.getId());
            assertThat(eventResourcesByEventId.size()).isEqualTo(1);
            assertThat(eventResourcesByEventId.first().getResourceId()).isEqualTo(resource2.getId());
        }

    }

    @Test
    public void getResourcesScheduleReservation() {
        val event = createResourceEvent(NOW.minusHours(1), nextOfficeResource);

        WebNewAvailabilityManagerTest.reserveResources(
                availabilityActions, 1, uid2, Cf.list(nextOfficeResourceEmail),
                NOW.plusHours(1), Option.empty(), Option.empty());

        var schedule = getResourcesSchedule(
                uid, Cf.list(nextOfficeResource.getOfficeId()), Option.empty());

        assertThat(schedule.find(nextOfficeResourceEmail).get().getEvents().first().getEventId().toOptional()).isPresent();
        assertThat(schedule.find(nextOfficeResourceEmail).get().getEvents().last().getReservationId().toOptional()).isPresent();
        assertThat(schedule.find(nextOfficeResourceEmail).get().getEvents().last().getAuthorInfo().toOptional()).isPresent();

        schedule = getResourcesSchedule(
                uid2, Cf.list(nextOfficeResource.getOfficeId()), Option.of(event.getId()));

        assertThat(schedule.find(nextOfficeResourceEmail).get().getEvents()).isEmpty();
    }

    private ResourcesSchedule getResourcesSchedule(
            PassportUid uid, ListF<Long> officeIds, Option<Long> exceptEventId)
    {
        return resourcesActions.getResourcesSchedule(
                uid, officeIds, Cf.list(), Option.empty(), Cf.list(), Option.empty(),
                Option.empty(), Option.of(NOW.toLocalDate()),
                Option.empty(), exceptEventId, Option.empty(), Option.empty());
    }

    @Test
    public void getSelectedResourcesSchedule() {
        Function<ListF<Email>, ResourcesSchedule> getSchedule = emails ->
        resourcesActions.getResourcesSchedule(uid, Cf.list(office.getId()),
                Cf.list(), Option.empty(), emails, Option.empty(), Option.empty(),
                Option.of(NOW.toLocalDate()), Option.empty(), Option.empty(),
                Option.empty(), Option.empty());

        assertThat(getSchedule.apply(Cf.list()).getResources()).hasSize(2);

        assertThat(getSchedule.apply(Cf.list(resource1Email)).getResources().single().getInfo().getId())
                .isEqualTo(resource1.getId());
    }

    @Test
    public void findUsersAndResources() {
        Function<String, ListF<Email>> find = query ->
                resourcesActions.findUsersAndResources(uid, Option.of(query), Option.empty(), Option.empty()).getEmails();

        val external = new Email("xxx@yyy");

        assertThat(find.apply(user.getLoginRaw() + "@;" + user2.getLoginRaw()))
                .containsExactly(user.getEmail(), user2.getEmail());

        assertThat(find.apply("User #1 <" + user.getEmail() + ">, <" + user2.getEmail() + ">, " + resource1Email))
                .containsExactly(user.getEmail(), user2.getEmail(), resource1Email);

        assertThat(find.apply(user.getEmail() + "," + resource2Email + "," + external + ","))
                .containsExactly(user.getEmail(), external, resource2Email);

        assertThat(resourcesActions.findUsersAndResources(uid, Option.of(resource1Email.getLocalPart()), Option.empty(), Option.empty()).getNotFound())
                .containsExactly(resource1Email.getLocalPart());
    }

    @Test
    public void getOffices() {
        val data = new Office();

        data.setId(nextOfficeResource.getOfficeId());
        data.setTimezoneId("+05:00");

        data.setName("Office name");
        data.setCityName("Yekaterinburg");

        resourceDao.updateOffices(Cf.list(data));

        val offices = resourcesActions.getOffices(uid, Option.empty(), Option.empty(), Option.empty());

        assertThat(offices.find(data.getId()).get().getTzOffset()).isEqualTo(2 * DateTimeConstants.MILLIS_PER_HOUR);
        assertThat(offices.find(data.getId()).get().getName()).isEqualTo(data.getName());
        assertThat(offices.find(data.getId()).get().getCityName()).isEqualTo(data.getCityName().get());

        assertThat(offices.find(office.getId()).get().getTzOffset()).isEqualTo(0);
    }

    @Test
    public void getOfficesTzOffsets() {
        val officeUpdate = new Office();
        val nextOfficeUpdate = new Office();

        officeUpdate.setId(office.getId());
        officeUpdate.setTimezoneId("Asia/Yekaterinburg");

        nextOfficeUpdate.setId(nextOfficeResource.getOfficeId());
        nextOfficeUpdate.setTimezoneId("Europe/Kiev");

        resourceDao.updateOffices(Cf.list(officeUpdate, nextOfficeUpdate));

        var info = resourcesActions.getOfficesTzOffsets(uid, NOW.toLocalDateTime(), Option.of(MoscowTime.TZ));

        assertThat(info.findOffset(nextOfficeUpdate.getId()).toOptional()).hasValue(-1 * DateTimeConstants.MILLIS_PER_HOUR);
        assertThat(info.findOffset(officeUpdate.getId()).toOptional()).hasValue(2 * DateTimeConstants.MILLIS_PER_HOUR);

        info = resourcesActions.getOfficesTzOffsets(uid, NOW.toLocalDateTime().plusMonths(6), Option.empty());

        assertThat(info.findOffset(nextOfficeUpdate.getId()).toOptional()).hasValue(0);
        assertThat(info.findOffset(officeUpdate.getId()).toOptional()).hasValue(2 * DateTimeConstants.MILLIS_PER_HOUR);
    }

    @Test
    public void sortOfficesInPreferredOrder() {
        val offices = Cf.list(
                createOffice(1, "Москва", "Морозов"),
                createOffice(2, "Москва", "Мамонтов"),
                createOffice(3, "Санкт-Петербург", "Бенуа"),
                createOffice(4, "Санкт-Петербург", "Рубма"),
                createOffice(5, "Екатеринбург", "Екб"),
                createOffice(6, "• Парковка", "Прк"));

        assertSortingInOrder(offices, Option.of("Морозов"), "Морозов", "Бенуа", "Екб", "Прк", "Мамонтов", "Рубма");
        assertSortingInOrder(offices, Option.of("Мамонтов"), "Мамонтов", "Бенуа", "Екб", "Прк", "Морозов", "Рубма");

        assertSortingInOrder(offices, Option.of("Бенуа"), "Бенуа", "Морозов", "Екб", "Прк", "Рубма", "Мамонтов");
        assertSortingInOrder(offices, Option.of("Рубма"), "Рубма", "Морозов", "Екб", "Прк", "Бенуа", "Мамонтов");

        assertSortingInOrder(offices, Option.of("Екб"), "Екб", "Морозов", "Бенуа", "Прк", "Мамонтов", "Рубма");
        assertSortingInOrder(offices, Option.empty(), "Морозов", "Бенуа", "Екб", "Прк", "Мамонтов", "Рубма");
    }

    private static void assertSortingInOrder(
            ListF<Office> offices, Option<String> userOfficeName, String ... expectedOfficeNamesOrder)
    {
        final Function<String, Office> byName = offices.toMapMappingToKey(Office::getName)::getOrThrow;

        ListF<Office> sorted = WebNewResourcesManager.sortOfficesInPreferredOrder(
                offices, userOfficeName.map(byName), Language.RUSSIAN);

        assertThat(sorted.map(Office.getNameF())).containsExactly(expectedOfficeNamesOrder);
    }

    private static Office createOffice(long id, String cityName, String officeName) {
        val o = new Office();
        o.setId(id);
        o.setCityName(cityName);
        o.setCityNameEn(cityName);

        o.setName(officeName);
        o.setNameEn(officeName);

        return o;
    }
}
