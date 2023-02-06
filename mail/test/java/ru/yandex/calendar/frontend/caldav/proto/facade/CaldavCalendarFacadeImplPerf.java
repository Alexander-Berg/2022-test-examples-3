package ru.yandex.calendar.frontend.caldav.proto.facade;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.frontend.caldav.proto.caldav.report.CalendarComponentConditions;
import ru.yandex.calendar.frontend.caldav.proto.tree.CollectionId;
import ru.yandex.calendar.frontend.caldav.userAgent.UserAgentType;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.LayerFields;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.BatchParticipationParameters;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.misc.cache.tl.TlCache;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.log.mlf.ndc.Ndc;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.time.TimeUtils;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class CaldavCalendarFacadeImplPerf extends AbstractCaldavTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private CaldavCalendarFacade caldavCalendarFacade;

    @Test
    public void export10Events() {
        exportEvents(10, true, false, false);
    }

    @Test
    public void export1000Events() {
        exportEvents(1000, true, false, false);
    }


    private void exportEvents(int count, boolean withParticipants, boolean byIds, boolean withIcs) {
        Ndc.Handle h;

        final ListF<TestUserInfo> allUsers;
        h = Ndc.push("preparing");
        try {
            val userCount = withParticipants ? 100 : 1;
            val logins = IntStream.range(0, userCount)
                    .mapToObj(i -> String.format("yandex-team-mm-125%02d", i))
                    .collect(toList());
            allUsers = testManager.prepareUsers(Cf.toList(logins));
        } finally {
            h.popSafely();
        }

        val firstUser = allUsers.first();

        val otherUsers = allUsers.drop(1);
        val otherUserLayerIds = testManager.createLayers(otherUsers.map(TestUserInfo.getUidF())).get1().map(LayerFields.ID.getF());

        val layerId = testManager.createLayer(firstUser.getUid());

        ListF<String> externalIds = Cf.arrayList();

        h = Ndc.push("inserting");
        try {
            Tuple2List<Event, String> insertData = Cf.Tuple2List.arrayList();
            for (int i = 0; i < count; ++i) {
                String externalId = CalendarUtils.generateExternalId();
                Instant start = TestDateTimes.moscow(2011, 2, 13, 1, 0).plus(Duration.standardDays(i));

                Event event = new Event();
                event.setStartTs(start);
                event.setEndTs(start.plus(Duration.standardHours(1)));
                event.setName("event " + i);

                externalIds.add(externalId);

                insertData.add(event, externalId);

            }

            externalIds = insertData.get2();

            if (withParticipants) {
                val events = testManager.batchCreateEvent(firstUser.getUid(), insertData);

                ListF<BatchParticipationParameters> participants = Cf.arrayList();
                for (val event : events) {
                    // organizer
                    participants.add(new BatchParticipationParameters(event.getId(), firstUser, layerId, Decision.YES, true));

                    for (val t : Random2.R.randomElements(otherUsers.zip(otherUserLayerIds), 2)) {
                        TestUserInfo user = t._1;
                        long inviteeLayerId = t._2;
                        participants.add(new BatchParticipationParameters(event.getId(), user, inviteeLayerId, Decision.MAYBE, false));
                    }
                }

                testManager.batchAddUserParticipantsToEvents(participants);

            } else {

                testManager.batchCreateEventOnLayer(firstUser.getUid(), insertData, layerId);

            }

        } finally {
            h.popSafely();
        }


        val h2 = Ndc.push("exporting");
        val cacheHandle = TlCache.push();
        try {

            val start = System.currentTimeMillis();
            final List<CalendarComponent> events;
            if (byIds) {
                events = StreamEx.of(caldavCalendarFacade.getUserCalendarEvents(
                        firstUser.getUid(),
                        CollectionId.events(firstUser.getEmail().getEmail(), Long.toString(layerId), firstUser.getUid()),
                        externalIds,
                        withIcs, UserAgentType.ICAL
                )).map(ComponentGetResult::getEventDescription)
                .flatMap(Optional::stream)
                .toImmutableList();
            } else {
                events = caldavCalendarFacade.getUserCalendarEvents(
                        firstUser.getUid(),
                        CollectionId.events(firstUser.getEmail().getEmail(), Long.toString(layerId), firstUser.getUid()),
                        CalendarComponentConditions.trueCondition(),
                        CalendarComponentConditions.trueCondition(),
                        withIcs, UserAgentType.ICAL
                );
            }
            val end = System.currentTimeMillis();

            assertThat(events).hasSize(count);

            val name = "export " + count + " events"
                + (withParticipants ? " with participants" : "")
                + (byIds ? " by id" : " on layer")
                ;
            log.info(name + ": " + TimeUtils.millisecondsToSecondsString(end - start));

            if (withIcs) {
                val content = StreamEx.of(events)
                        .map(CalendarComponent::getIcal)
                        .joining("");
                new File2("tmp.ics").write(content);
            }
        } finally {
            cacheHandle.popSafely();
            h2.popSafely();
        }
    }
}
