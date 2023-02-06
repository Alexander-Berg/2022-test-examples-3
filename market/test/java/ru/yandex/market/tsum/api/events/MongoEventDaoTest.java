package ru.yandex.market.tsum.api.events;

import com.google.protobuf.InvalidProtocolBufferException;
import com.googlecode.protobuf.format.JsonFormat;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.event.Bookmark;
import ru.yandex.market.tsum.event.Event;
import ru.yandex.market.tsum.event.EventComment;
import ru.yandex.market.tsum.event.EventCriteria;
import ru.yandex.market.tsum.event.EventFilter;
import ru.yandex.market.tsum.event.EventRequest;
import ru.yandex.market.tsum.event.EventStatus;
import ru.yandex.market.tsum.event.MicroEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 28/09/16
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class, MongoEventDao.class})
public class MongoEventDaoTest {

    @Autowired
    private MongoEventDao mongoEventDao;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void testAdd() throws Exception {

        Event eventBefore = Event.newBuilder().setId("id1").setTitle("атата").addTags("xxx:zzz").build();
        mongoEventDao.addEvent(eventBefore);
        Optional<Event> eventAfter = mongoEventDao.get("id1");
        Assert.assertTrue(eventAfter.isPresent());
        Assert.assertEquals(eventBefore, eventAfter.get());

        System.gc();
    }

    @Test
    public void testAppend() throws Exception {
        Event step1 = Event.newBuilder().setId("id2").setTitle("атата").addTags("xxx:zzz").build();
        Event step2 = Event.newBuilder().setId("id2").setTitle("title").build();
        Event step3 = Event.newBuilder().setId("id2").setTitle("title").addTags("a:b:c").build();

        Event expected = Event.newBuilder().setId("id2").setTitle("title").addTags("a:b:c").addTags("xxx:zzz").build();

        mongoEventDao.append(step1);
        mongoEventDao.append(step2);
        mongoEventDao.append(step3);

        Event actual = mongoEventDao.get("id2").get();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAppendMicroEvents() throws Exception {
        Event step1 = Event.newBuilder().setId("id3")
            .setStartTimeSeconds(10)
            .setEndTimeSeconds(12)
            .addMicroEvents(MicroEvent.newBuilder().setProject("report").setType("restart").setSource("host1"))
            .addMicroEvents(MicroEvent.newBuilder().setProject("report").setType("restart").setSource("host2"))
            .build();

        Event step2 = Event.newBuilder().setId("id3")
            .addMicroEvents(MicroEvent.newBuilder().setProject("report").setType("restart").setSource("host3").setTimeSeconds(0))
            .addMicroEvents(MicroEvent.newBuilder().setProject("report").setType("reload").setSource("host1").setTimeSeconds(42))
            .build();

        Event expected = Event.newBuilder().setId("id3")
            .addMicroEvents(MicroEvent.newBuilder().setProject("report").setType("restart").setSource("host1"))
            .addMicroEvents(MicroEvent.newBuilder().setProject("report").setType("restart").setSource("host2"))
            .addMicroEvents(MicroEvent.newBuilder().setProject("report").setType("restart").setSource("host3").setTimeSeconds(0))
            .addMicroEvents(MicroEvent.newBuilder().setProject("report").setType("reload").setSource("host1").setTimeSeconds(42))
            .setStartTimeSeconds(0)
            .setEndTimeSeconds(42)
            .build();

        mongoEventDao.append(step1);
        mongoEventDao.append(step2);

        Event actual = mongoEventDao.get("id3").get();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetFilteredEvents() {
        Event invalidEvent = Event.newBuilder().setId("filterTestInvalid").setStartTimeSeconds(0)
            .setEndTimeSeconds(10).setProject("project0").setType("type0")
            .setStatus(EventStatus.INFO).addTags("tag0").addTags("tag1:tag2:tag3")
            .setAuthor("author0")
            .build();
        Event event0 = Event.newBuilder().setId("filterTest0").setStartTimeSeconds(1)
            .setEndTimeSeconds(10).setProject("project0").setType("type0")
            .setStatus(EventStatus.INFO).addTags("tag0").addTags("tag1:tag2:tag3")
            .setAuthor("author0")
            .build();
        Event event1 = Event.newBuilder().setId("filterTest1").setStartTimeSeconds(10)
            .setEndTimeSeconds(20).setProject("project1").setType("type1")
            .setStatus(EventStatus.ERROR).addTags("tag1:tag2").addTags("tag3")
            .setAuthor("author1")
            .build();
        Event event2 = Event.newBuilder().setId("filterTest2").setStartTimeSeconds(20)
            .setEndTimeSeconds(30).setProject("project1").setType("type2")
            .setStatus(EventStatus.INFO).addTags("tag2:tag3").addTags("tag3")
            .setAuthor("author2")
            .build();
        Event event3 = Event.newBuilder().setId("filterTest3").setStartTimeSeconds(30)
            .setEndTimeSeconds(40).setProject("project3").setType("type1")
            .setStatus(EventStatus.ERROR).addTags("tag0:tag3").addTags("tag2")
            .setAuthor("author3")
            .build();
        Event event4 = Event.newBuilder().setId("filterTest4").setStartTimeSeconds(40)
            .setEndTimeSeconds(0).setProject("project4").setType("type4")
            .setStatus(EventStatus.INFO).addTags("tag0:tag1:tag2:tag3")
            .setAuthor("author1")
            .build();
        Event event5 = Event.newBuilder().setId("filterTest5").setStartTimeSeconds(50)
            .setEndTimeSeconds(60).setProject("project5").setType("type5")
            .setStatus(EventStatus.ERROR).addTags("tag5")
            .setAuthor("author1")
            .build();
        mongoTemplate.dropCollection("timeline.events");
        mongoEventDao.addEvents(Arrays.asList(invalidEvent, event0, event1, event2, event3, event4, event5));

        // endTimeSeconds
        checkRequest(
            EventRequest.newBuilder().setEndTimeSeconds(5).build(),
            event0
        );
        checkRequest(
            EventRequest.newBuilder().setEndTimeSeconds(31).build(),
            event0, event1, event2, event3
        );

        // startTimeSeconds
        checkRequest(
            EventRequest.newBuilder().setStartTimeSeconds(40).build(),
            event3, event4, event5
        );

        // limit
        checkRequest(
            EventRequest.newBuilder().setLimit(3).build(),
            event0, event1, event2
        );

        // skip
        checkRequest(
            EventRequest.newBuilder().setSkip(2).build(),
            event2, event3, event4, event5
        );

        // projects
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addProjects("project0").addProjects("project1").build()
            ).build(),
            event0, event1, event2
        );
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addProjects("project0").addProjects("project1").setInverted(true).build()
            ).build(),
            event3, event4, event5
        );

        // types
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addTypes("type0").addTypes("type1").build()
            ).build(),
            event0, event1, event3
        );
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addTypes("type0").addTypes("type1").setInverted(true).build()
            ).build(),
            event2, event4, event5
        );

        // statuses
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addStatuses(EventStatus.ERROR).build()
            ).build(),
            event1, event3, event5
        );
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addStatuses(EventStatus.ERROR).setInverted(true).build()
            ).build(),
            event0, event2, event4
        );

        // tags
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addTags("tag1:tag2").build()
            ).build(),
            event0, event1
        );
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addTags("tag1:tag2:tag3").build()
            ).build(),
            event0
        );
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addTags("tag0").build()
            ).build(),
            event0, event3, event4
        );
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addTags("tag1:tag2").setInverted(true).build()
            ).build(),
            event2, event3, event4, event5
        );

        // authors
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addAuthors("author1").build()
            ).build(),
            event1, event4, event5
        );
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addAuthors("author1").setInverted(true).build()
            ).build(),
            event0, event2, event3
        );
    }

    @Test
    public void testEventCriteria() {
        Event eventInvalid = Event.newBuilder().setId("filterTestInvalid").setStartTimeSeconds(0)
            .setEndTimeSeconds(10).setProject("project0").setType("type0")
            .setStatus(EventStatus.INFO).addTags("tag0").addTags("tag1:tag2:tag3")
            .setAuthor("author0")
            .build();
        Event event0 = Event.newBuilder().setId("filterTest0").setStartTimeSeconds(1)
            .setEndTimeSeconds(10).setProject("project0").setType("type0")
            .setStatus(EventStatus.INFO).addTags("tag0").addTags("tag1:tag2:tag3")
            .setAuthor("author0")
            .build();
        Event event1 = Event.newBuilder().setId("filterTest1").setStartTimeSeconds(10)
            .setEndTimeSeconds(20).setProject("project0").setType("type1")
            .setStatus(EventStatus.ERROR).addTags("tag1:tag2").addTags("tag4")
            .setAuthor("author1")
            .build();
        Event event2 = Event.newBuilder().setId("filterTest2").setStartTimeSeconds(20)
            .setEndTimeSeconds(30).setProject("project1").setType("type4")
            .setStatus(EventStatus.INFO).addTags("tag1:tag2").addTags("tag6")
            .setAuthor("author2")
            .build();
        Event event3 = Event.newBuilder().setId("filterTest3").setStartTimeSeconds(30)
            .setEndTimeSeconds(40).setProject("project3").setType("type1")
            .setStatus(EventStatus.ERROR).addTags("tag2").addTags("tag6:tag7")
            .setAuthor("author3")
            .build();
        Event event4 = Event.newBuilder().setId("filterTest4").setStartTimeSeconds(40)
            .setEndTimeSeconds(50).setProject("project4").setType("type4")
            .setStatus(EventStatus.INFO).addTags("tag1:tag2").addTags("tag6:tag7:tag8:tag9")
            .setAuthor("author1")
            .build();
        Event event5 = Event.newBuilder().setId("filterTest5").setStartTimeSeconds(50)
            .setEndTimeSeconds(60).setProject("project5").setType("type5")
            .setStatus(EventStatus.ERROR).addTags("tag0").addTags("tag6:tag7:tag8")
            .setAuthor("author1")
            .build();
        mongoTemplate.dropCollection("timeline.events");
        mongoEventDao.addEvents(Arrays.asList(eventInvalid, event0, event1, event2, event3, event4, event5));

        // test AND
        checkRequest(
            EventRequest.newBuilder().setEventCriteria(
                EventCriteria.newBuilder().setType(EventCriteria.LogicType.AND).addFilters(
                    EventFilter.newBuilder().addProjects("project1").build()
                ).addFilters(
                    EventFilter.newBuilder().addProjects("project0").build()
                ).build()
            ).build()
        );
        checkRequest(
            EventRequest.newBuilder().setEventCriteria(
                EventCriteria.newBuilder().setType(EventCriteria.LogicType.AND).addFilters(
                    EventFilter.newBuilder().addTags("tag6:tag7").build()
                ).addFilters(
                    EventFilter.newBuilder().addTags("tag0").build()
                ).build()
            ).build(),
            event5
        );
        // test OR
        checkRequest(
            EventRequest.newBuilder().setEventCriteria(
                EventCriteria.newBuilder().setType(EventCriteria.LogicType.OR).addFilters(
                    EventFilter.newBuilder().addTags("tag6:tag7").build()
                ).addFilters(
                    EventFilter.newBuilder().addTags("tag0").build()
                ).build()
            ).build(),
            event0, event3, event4, event5
        );
        // AND OR
        checkRequest(
            EventRequest.newBuilder().setEventCriteria(
                EventCriteria.newBuilder().setType(EventCriteria.LogicType.AND).addCriterias(
                    EventCriteria.newBuilder().setType(EventCriteria.LogicType.OR).addFilters(
                        EventFilter.newBuilder().addTags("tag6:tag7").build()
                    ).addFilters(
                        EventFilter.newBuilder().addTags("tag0").build()
                    ).build()
                ).addCriterias(
                    EventCriteria.newBuilder().setType(EventCriteria.LogicType.OR).addFilters(
                        EventFilter.newBuilder().addProjects("project0").build()
                    ).addFilters(
                        EventFilter.newBuilder().addTypes("type4").build()
                    ).build()
                ).build()
            ).build(),
            event0, event4
        );

        // filters and criterias in one criteria
        checkRequest(
            EventRequest.newBuilder().setEventCriteria(
                EventCriteria.newBuilder().setType(EventCriteria.LogicType.AND).addCriterias(
                    EventCriteria.newBuilder().setType(EventCriteria.LogicType.OR).addFilters(
                        EventFilter.newBuilder().addTags("tag6:tag7").build()
                    ).addFilters(
                        EventFilter.newBuilder().addTags("tag0").build()
                    ).build()
                ).addFilters(
                    EventFilter.newBuilder().addProjects("project0").build()
                ).build()
            ).build(),
            event0
        );

        // many ANDs
        checkRequest(
            EventRequest.newBuilder().setEventCriteria(
                EventCriteria.newBuilder().setType(EventCriteria.LogicType.AND).addFilters(
                    EventFilter.newBuilder().addTags("tag1").addProjects("project0").build()
                ).addCriterias(
                    EventCriteria.newBuilder().setType(EventCriteria.LogicType.AND).addFilters(
                        EventFilter.newBuilder().addAuthors("author1").addTypes("type1").build()
                    ).build()
                ).build()
            ).build(),
            event1
        );



        // OR tags
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addTags("tag1:tag2").addTags("tag0").build()
            ).build(),
            event0, event1, event2, event4, event5
        );
        // OR projects
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addProjects("project4").addProjects("project5").build()
            ).build(),
            event4, event5
        );
        // OR types
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addTypes("type4").addTypes("type5").build()
            ).build(),
            event2, event4, event5
        );
        // OR authors
        checkRequest(
            EventRequest.newBuilder().setEventFilter(
                EventFilter.newBuilder().addAuthors("author2").addAuthors("author3").build()
            ).build(),
            event2, event3
        );
    }

    @Test
    public void testToEventComment() {
        EventComment eventComment0 = EventComment.newBuilder().setText("text0").build();

        Event event = Event.newBuilder().setId("testId").build();
        mongoEventDao.addEvent(event);
        mongoEventDao.addComment(eventComment0, "testId");
        Event gotEvent = mongoEventDao.get("testId").get();

        EventComment gotEventComment0 = gotEvent.getComments(0);
        eventComment0 = EventComment.newBuilder(eventComment0).setId(gotEventComment0.getId()).build();
        Assert.assertEquals(eventComment0, gotEventComment0);

        EventComment eventComment1 = EventComment.newBuilder().setParentId(eventComment0.getId()).setText("text1")
                                                              .setAuthor("author1").setDeleted(false)
                                                              .setTimeSeconds((int)(System.currentTimeMillis() / 1000))
                                                              .build();
        EventComment eventComment2 = EventComment.newBuilder().setParentId(eventComment0.getId()).setText("text2")
                                                              .setAuthor("author2").setDeleted(false)
                                                              .setTimeSeconds((int)(System.currentTimeMillis()/ 1000 + 1))
                                                              .build();
        mongoEventDao.addComment(eventComment1, "testId");
        mongoEventDao.addComment(eventComment2, "testId");

        gotEvent = mongoEventDao.get("testId").get();

        List<EventComment> gotSubcomments = gotEvent.getComments(0).getSubCommentsList();
        List<EventComment> subcomments = new ArrayList<>();
        subcomments.add(EventComment.newBuilder(eventComment1)
                                    .setId(gotEvent.getComments(0).getSubComments(0).getId()).build());
        subcomments.add(EventComment.newBuilder(eventComment2)
                                    .setId(gotEvent.getComments(0).getSubComments(1).getId()).build());
        Assert.assertEquals(subcomments, gotSubcomments);

        EventComment editedEventComment0 = EventComment.newBuilder()
            .setId(eventComment0.getId()).setText("editedText0")
            .setAuthor("editedAuthor0").setDeleted(false)
            .setTimeSeconds((int)(System.currentTimeMillis() / 1000))
            .build();
        mongoEventDao.editComment(editedEventComment0, "testId");
        gotEvent = mongoEventDao.get("testId").get();
        gotEventComment0 = gotEvent.getComments(0);
        editedEventComment0 = EventComment.newBuilder(editedEventComment0)
            .addAllSubComments(gotEventComment0.getSubCommentsList()).build();
        Assert.assertEquals(editedEventComment0, gotEventComment0);

        editedEventComment0 = EventComment.newBuilder(editedEventComment0).setId("notPresent").build();
        mongoEventDao.editComment(editedEventComment0, "testId");
        Assert.assertTrue(mongoEventDao.get("testId").get().getCommentsList().size() == 1);

    }

    @Test
    public void testAddBookmark() throws Exception {
        Bookmark bookmark = getFullBookmark();
        mongoTemplate.dropCollection("timeline.bookmarks");

        Bookmark emptyBookmark = Bookmark.newBuilder()
            .setAuthor("user43")
            .setCreatedTimeMillis(43)
            .setTitle("Bookmark43")
            .setCriteria(
                EventCriteria.newBuilder()
                    .addFilters(
                            EventFilter.newBuilder()
                                .build()
                    )
                    .addCriterias(
                        EventCriteria.newBuilder()
                            .build()
                    )
                    .build()
            )
            .build();

        String fullBookmarkId = mongoEventDao.addBookmark(bookmark);
        String emptyBookmarkId = mongoEventDao.addBookmark(emptyBookmark);

        Optional<Bookmark> gotBookmark = mongoEventDao.getBookmark(fullBookmarkId);
        Optional<Bookmark> gotEmptyBookmark = mongoEventDao.getBookmark(emptyBookmarkId);

        Assert.assertTrue(gotBookmark.isPresent());
        checkBookmarks(bookmark, gotBookmark.get());

        Assert.assertTrue(gotEmptyBookmark.isPresent());
        checkBookmarks(emptyBookmark, gotEmptyBookmark.get());
    }

    @Test
    public void upsertBookmark() throws JsonFormat.ParseException, InvalidProtocolBufferException {
        Bookmark bookmark = getFullBookmark();
        mongoTemplate.dropCollection("timeline.bookmarks");
        mongoEventDao.addBookmark(bookmark);
        bookmark = Bookmark.newBuilder(bookmark)
            .setAuthor("user43")
            .setCreatedTimeMillis(43)
            .setTitle("Bookmark42")
            .setCriteria(
                EventCriteria.newBuilder()
                    .setType(EventCriteria.LogicType.OR)
                    .addAllFilters(
                        Arrays.asList(
                            EventFilter.newBuilder()
                                .addProjects("project3")
                                .addTypes("type1")
                                .addStatuses(EventStatus.INFO)
                                .addTags("tag3")
                                .setInverted(false)
                                .build()
                        )
                    )
                    .build()
            )
            .build();
        mongoEventDao.upsertBookmark(bookmark);
        Optional<Bookmark> gotBookmark = mongoEventDao.getBookmark("id-one");
        Assert.assertTrue(gotBookmark.isPresent());
        Assert.assertEquals(bookmark, gotBookmark.get());
    }

    private Bookmark getFullBookmark() {
        return Bookmark.newBuilder()
            .setId("id-one")
            .setAuthor("user42")
            .setCreatedTimeMillis(42)
            .setTitle("Bookmark42")
            .setCriteria(
                EventCriteria.newBuilder()
                    .setType(EventCriteria.LogicType.AND)
                    .addAllFilters(
                        Arrays.asList(
                            EventFilter.newBuilder()
                                .addProjects("project1").addProjects("project2")
                                .addTypes("type1").addTypes("type2")
                                .addStatuses(EventStatus.ERROR)
                                .addTags("tag1")
                                .addAuthors("author1")
                                .setInverted(true)
                                .build(),
                            EventFilter.newBuilder()
                                .addProjects("project3")
                                .addTypes("type3")
                                .addStatuses(EventStatus.INFO).addStatuses(EventStatus.WARN)
                                .addTags("tag3").addTags("tag4")
                                .addAuthors("author3").addAuthors("author4")
                                .setInverted(false)
                                .build()
                        )
                    )
                    .addCriterias(
                        EventCriteria.newBuilder()
                            .setType(EventCriteria.LogicType.OR)
                            .addAllFilters(
                                Arrays.asList(
                                    EventFilter.newBuilder()
                                        .addProjects("project5").addProjects("project6")
                                        .addTypes("type5").addTypes("type6")
                                        .addStatuses(EventStatus.ERROR)
                                        .addTags("tag5")
                                        .addAuthors("author5")
                                        .setInverted(true)
                                        .build(),
                                    EventFilter.newBuilder()
                                        .addProjects("project7")
                                        .addTypes("type7")
                                        .addStatuses(EventStatus.INFO).addStatuses(EventStatus.WARN)
                                        .addTags("tag7").addTags("tag8")
                                        .addAuthors("author7").addAuthors("author8")
                                        .setInverted(false)
                                        .build()
                                )
                            )
                            .build()
                    )
                    .build()
            )
            .build();
    }

    private void checkRequest(EventRequest request, Event... expectedEvents) {
        Assert.assertEquals(toSet(expectedEvents), toSet(mongoEventDao.getFilteredEvents(request)));
    }

    private void checkBookmarks(Bookmark expected, Bookmark actual) {
        Assert.assertEquals(expected.getAuthor(), actual.getAuthor());
        Assert.assertEquals(expected.getCreatedTimeMillis(), actual.getCreatedTimeMillis());
        Assert.assertEquals(expected.getTitle(), actual.getTitle());
        Assert.assertEquals(expected.getCriteria(), actual.getCriteria());
    }

    private Set<Event> toSet(Event... events) {
        return new HashSet<>(Arrays.asList(events));
    }

    private Set<Event> toSet(List<Event> events) {
        return new HashSet<>(events);
    }
}