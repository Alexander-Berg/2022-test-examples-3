package ru.yandex.market.pers.grade.core.moderation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.MockedTest;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.model.core.ModReason;
import ru.yandex.market.pers.grade.core.model.notification.NotificationEvent;
import ru.yandex.market.pers.grade.core.service.GradeQueueService;
import ru.yandex.market.pers.grade.core.service.NotificationQueueService;
import ru.yandex.market.pers.grade.core.util.MarketUtilsService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.pers.grade.client.model.ModState.APPROVED;
import static ru.yandex.market.pers.grade.client.model.ModState.DELAYED;
import static ru.yandex.market.pers.grade.client.model.ModState.REJECTED;
import static ru.yandex.market.pers.grade.client.model.ModState.UNMODERATED;
import static ru.yandex.market.pers.grade.core.model.notification.NotificationEventType.NOTIFY_GRADE_MOD_SHOP;
import static ru.yandex.market.pers.grade.core.model.notification.NotificationEventType.NOTIFY_GRADE_MOD_USER;

/**
 * @author Vladimir Gorovoy vgorovoy@yandex-team.ru
 */
public class GradeProxyTest extends MockedTest {
    public static final long TEST_MODERATOR_ID = 1L;
    public static final long TEST_AUTHOR_ID = 1L;
    public static final long TEST_MODEL_ID_APPROVED = 1L;
    public static final long TEST_MODEL_ID_DELAYED = 2L;
    public static final long TEST_MODEL_ID_UNMODERATED = 3L;

    @Autowired
    public GradeModeratorModificationProxy gradeModeratorModificationProxy;

    @Autowired
    DbGradeService dbGradeService;

    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private NotificationQueueService notificationQueueService;

    @Autowired
    private GradeQueueService gradeQueueService;

    /**
     * Тест отправки в очередь индексации отзывов, которые ранее имели статус APPROVED.
     */
    @Test
    public void testModeratingPreviouslyApprovedGrades() {
        //given:
        Long approvedGradeId = gradeCreator.createModelGrade(TEST_MODEL_ID_APPROVED, TEST_AUTHOR_ID, APPROVED);
        Long delayedGradeId = gradeCreator.createModelGrade(TEST_MODEL_ID_DELAYED, TEST_AUTHOR_ID, DELAYED);
        Long unmoderatedGradeId = gradeCreator.createModelGrade(TEST_MODEL_ID_UNMODERATED, TEST_AUTHOR_ID, UNMODERATED);

        List<Long> gradeIds = List.of(approvedGradeId, delayedGradeId, unmoderatedGradeId);

        //when:
        gradeModeratorModificationProxy.moderateGradeReplies(gradeIds, Collections.emptyList(), TEST_MODERATOR_ID,
            REJECTED);

        //then:
        assertTrue(gradeQueueService.isInQueue(approvedGradeId));
        assertFalse(gradeQueueService.isInQueue(delayedGradeId));
        assertFalse(gradeQueueService.isInQueue(unmoderatedGradeId));
    }

    @Test
    public void testApproveGradeNotification() {
        long gradeId = gradeCreator.createModelGradeUnmoderated(TEST_MODEL_ID_UNMODERATED, TEST_AUTHOR_ID);

        gradeModeratorModificationProxy.moderateGradeReplies(
            Collections.singletonList(gradeId),
            Collections.emptyList(), TEST_MODERATOR_ID,
            APPROVED);

        // no notifications since they are send after grade is indexed (by job)
        assertEquals(0, notificationQueueService.getNewEventsCount());
    }

    @Test
    public void testDelayedGradeNotification() {
        long gradeId = gradeCreator.createModelGradeUnmoderated(TEST_MODEL_ID_UNMODERATED, TEST_AUTHOR_ID);

        gradeModeratorModificationProxy.moderateGradeReplies(
            Collections.singletonList(gradeId),
            Collections.emptyList(), TEST_MODERATOR_ID,
            DELAYED);

        // 0 notification - for shop only. Other notifications only after saas indexing
        assertEquals(0, notificationQueueService.getNewEventsCount());

        List<NotificationEvent> events = notificationQueueService.getNewEvents(NOTIFY_GRADE_MOD_SHOP, 10);
        assertEquals(0, events.size());
    }

    @Test
    public void testRejectGradeNotification() {
        long gradeId = gradeCreator.createModelGradeUnmoderated(TEST_MODEL_ID_UNMODERATED, TEST_AUTHOR_ID);

        gradeModeratorModificationProxy.moderateGradeReplies(
            Map.of(gradeId, ModReason.UNINFORMATIVE.forShop()),
            Collections.emptyMap(),
            TEST_MODERATOR_ID,
            REJECTED);

        // all three notifications send on rejection
        assertEquals(1, notificationQueueService.getNewEventsCount());

        // shop mail
        List<NotificationEvent> events = notificationQueueService.getNewEvents(NOTIFY_GRADE_MOD_SHOP, 10);
        assertEquals(0, events.size());

        // use mail
        events = notificationQueueService.getNewEvents(NOTIFY_GRADE_MOD_USER, 10);
        assertEquals(1, events.size());

        NotificationEvent event = events.get(0);
        assertEquals(gradeId, event.getDataLong(MarketUtilsService.KEY_GRADE_ID));
        assertEquals(REJECTED.value(), event.getDataLong(MarketUtilsService.KEY_MOD_STATE));
    }

}
