package ru.yandex.market.pers.tms.timer.notification;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.cleanweb.CleanWebClient;
import ru.yandex.market.cleanweb.dto.CleanWebResponseDto;
import ru.yandex.market.cleanweb.dto.VerdictDto;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeVoteService;
import ru.yandex.market.pers.grade.core.model.core.GradeValue;
import ru.yandex.market.pers.grade.core.model.core.ModReason;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.model.notification.NotificationEvent;
import ru.yandex.market.pers.grade.core.model.vote.GradeVoteKind;
import ru.yandex.market.pers.grade.core.moderation.GradeModeratorModificationProxy;
import ru.yandex.market.pers.grade.core.service.GradeCleanWebService;
import ru.yandex.market.pers.grade.core.service.NotificationQueueService;
import ru.yandex.market.pers.grade.core.service.VerifiedGradeService;
import ru.yandex.market.pers.grade.core.util.MarketUtilsService;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.moderation.ModelGradesAutomoderation;
import ru.yandex.market.pers.tms.moderation.ShopGradesAutomoderation;
import ru.yandex.market.util.ListUtils;
import ru.yandex.market.util.RetryService;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.grade.client.model.ModState.APPROVED;
import static ru.yandex.market.pers.grade.client.model.ModState.AUTOMATICALLY_REJECTED;
import static ru.yandex.market.pers.grade.client.model.ModState.REJECTED;
import static ru.yandex.market.pers.grade.client.model.ModState.UNMODERATED;
import static ru.yandex.market.pers.grade.core.model.notification.NotificationEventState.BAD_DATA;
import static ru.yandex.market.pers.grade.core.model.notification.NotificationEventState.CANCELLED;
import static ru.yandex.market.pers.grade.core.model.notification.NotificationEventState.FAILED;
import static ru.yandex.market.pers.grade.core.model.notification.NotificationEventState.PROCESSED;
import static ru.yandex.market.pers.grade.core.model.notification.NotificationEventState.SKIPPED;
import static ru.yandex.market.pers.grade.core.model.notification.NotificationEventType.NOTIFY_GRADE_MOD_USER;
import static ru.yandex.market.pers.grade.core.model.notification.NotificationEventType.NOTIFY_GRADE_VOTED_PUSH;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 29.04.2020
 */
public class NotificationEventsUtilsExecutorTest extends MockedPersTmsTest {
    private static final long MODEL_ID = 234234;
    private static final long SHOP_ID = 42515;
    private static final long USER_ID = 562462;
    private static final long TEST_MODERATOR_ID = 45252;

    @Autowired
    protected CleanWebClient cleanWebClient;
    @Autowired
    protected ConfigurationService configurationService;
    @Autowired
    private NotificationEventsUtilsExecutor executor;
    @Autowired
    private GradeModeratorModificationProxy gradeModeratorModificationProxy;
    @Autowired
    private DbGradeVoteService dbGradeVoteService;
    @Autowired
    private GradeCreator gradeCreator;
    @Autowired
    private NotificationQueueService notificationQueueService;
    @Autowired
    private MarketUtilsService utilsService;
    @Autowired
    private PersNotifyClient notifyClient;
    @Autowired
    private ComplexMonitoring complexMonitoring;
    @Autowired
    private ModelGradesAutomoderation modelGradesAutomoderation;
    @Autowired
    private ShopGradesAutomoderation shopGradesAutomoderation;
    @Autowired
    private VerifiedGradeService verifiedGradeService;

    @Test
    public void testApproveModelGradeNotification() throws Exception {
        long gradeId = gradeCreator.createModelGradeUnmoderated(MODEL_ID, USER_ID);
        approve(gradeId);
        imitateSaasIndexed(gradeId);

        executor.runTmsJob();

        checkModelGradeEventsProcessed();

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(1);
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_MODEL_GRADE));
    }

    @Test
    public void testNotification4ApprovedModelGradeByAutomoderation()  throws Exception {
        ModelGrade modelGrade = GradeCreator.constructModelGradeNoText(MODEL_ID, USER_ID, UNMODERATED);
        modelGrade.setText("Честный отзыв без лишних прикрас, без нареканий");
        long gradeId = gradeCreator.createGrade(modelGrade);
        verifiedGradeService.setCpaInDB(List.of(gradeId), false); // any value for automod
        updateCrTimeForAutomoderation(gradeId);

        modelGradesAutomoderation.process();
        assertEquals(APPROVED, getGradeModState(gradeId));
        assertNull(getGradeModReason(gradeId));

        verifyNotifyEvents(0);

        imitateSaasIndexed(gradeId);
        executor.runTmsJob();

        checkModelGradeEventsProcessed();

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(1);
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_MODEL_GRADE));
    }

    @Test
    public void testRejectModelGradeNotification() throws Exception {
        long gradeId = gradeCreator.createModelGradeUnmoderated(MODEL_ID, USER_ID);
        reject(gradeId, REJECTED, ModReason.UNINFORMATIVE.forModel());

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(0, notificationQueueService.getEventsCount(SKIPPED));
        assertEquals(1, notificationQueueService.getEventsCount(PROCESSED));

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(1);
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_MODEL_GRADE));
    }

    @Test
    public void testAutomaticallyRejectModelGradeNotification() throws Exception {
        long gradeId = gradeCreator.createModelGradeUnmoderated(MODEL_ID, USER_ID);
        reject(gradeId, AUTOMATICALLY_REJECTED, ModReason.RUDE.forModel());

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(0, notificationQueueService.getEventsCount(SKIPPED));
        assertEquals(1, notificationQueueService.getEventsCount(PROCESSED));

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(1);
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_MODEL_GRADE));
    }

    @Test
    public void testNotification4AutoRejectedModelMarkByAutomoderation()  throws Exception {
        long gradeId = gradeCreator.createModelGrade(MODEL_ID, USER_ID, ModState.UNMODERATED, "");
        updateCrTimeForAutomoderation(gradeId);

        modelGradesAutomoderation.process();

        assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeId));
        assertNull(getGradeModReason(gradeId));

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(1, notificationQueueService.getEventsCount(SKIPPED));

        verifyNotifyEvents(0);
    }

    @Test
    public void testNotification4AutoRejectedModelGradeByAutomoderation()  throws Exception {
        long gradeId = gradeCreator.createModelGrade(MODEL_ID, USER_ID, ModState.UNMODERATED,
                "ЭТОТ ОТЗЫВ СОДЕРЖИТ СЛИШКОМ МНОГО КАПСА");
        updateCrTimeForAutomoderation(gradeId);

        modelGradesAutomoderation.process();

        assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeId));
        assertEquals(ModReason.CAPS.forModel(), getGradeModReason(gradeId));

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(0, notificationQueueService.getEventsCount(SKIPPED));
        assertEquals(1, notificationQueueService.getEventsCount(PROCESSED));

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(1);
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_MODEL_GRADE));
    }

    @Test
    public void testNotification4AutoRejectedGoodShopGradeByAutomoderation()  throws Exception {
        ShopGrade shopGrade = GradeCreator.constructShopGrade(SHOP_ID, USER_ID);
        shopGrade.setModState(UNMODERATED);
        shopGrade.setText("ЭТОТ ОТЗЫВ СОДЕРЖИТ СЛИШКОМ МНОГО КАПСА");
        shopGrade.setAverageGrade(GradeValue.GOOD.toAvgGrade());
        long gradeId = gradeCreator.createGrade(shopGrade);
        verifiedGradeService.setCpaInDB(List.of(gradeId), anyBoolean());
        updateCrTimeForAutomoderation(gradeId);

        shopGradesAutomoderation.process();

        assertEquals(ModState.AUTOMATICALLY_REJECTED, getGradeModState(gradeId));
        assertEquals(ModReason.CAPS.forShop(), getGradeModReason(gradeId));

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(1, notificationQueueService.getEventsCount(PROCESSED));

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(1);
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_SHOP_GRADE));
    }

    @Test
    public void testAutomaticallyRejectModelGradeNotificationWithoutText() throws Exception {
        long gradeId = gradeCreator.createModelGradeUnmoderated(MODEL_ID, USER_ID);
        reject(gradeId, AUTOMATICALLY_REJECTED, null);

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(1, notificationQueueService.getEventsCount(SKIPPED));

        verifyNotifyEvents(0);
    }

    @Test
    public void testRejectSpamModelGradeNotification() throws Exception {
        long gradeId = gradeCreator.createModelGradeUnmoderated(MODEL_ID, USER_ID);
        reject(gradeId, REJECTED, ModReason.SPAM.forModel());

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(1, notificationQueueService.getEventsCount(SKIPPED));

        verifyNotifyEvents(0);
    }

    @Test
    public void testAutomaticallyRejectGradeOnExcludeModelNotification() throws Exception {
        long gradeId = gradeCreator.createModelGradeUnmoderated(MODEL_ID + 100, USER_ID);
        updateCrTimeForAutomoderation(gradeId);

        pgJdbcTemplate.update("insert into EXCLUDE_MODEL(MODEL_ID,DESCRIPTION) values (?, ?)", MODEL_ID + 100, "test spam");

        modelGradesAutomoderation.process();

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(1, notificationQueueService.getEventsCount(SKIPPED));

        verifyNotifyEvents(0);
    }

    @Test
    public void testRejectModelGradeNotificationCancelled() throws Exception {
        long gradeId = gradeCreator.createModelGradeUnmoderated(MODEL_ID, USER_ID);
        reject(gradeId, REJECTED, ModReason.UNINFORMATIVE.forModel());

        checkNotificationCancelled();
    }

    @Test
    public void testAutomaticallyRejectModelGradeNotificationCancelled() throws Exception {
        long gradeId = gradeCreator.createModelGradeUnmoderated(MODEL_ID, USER_ID);
        reject(gradeId, AUTOMATICALLY_REJECTED, ModReason.RUDE.forModel());

        checkNotificationCancelled();
    }

    @Test
    public void testShopGradeNotificationMultiDataException() throws Exception {
        long shopGrade = gradeCreator.createShopGrade(USER_ID, SHOP_ID, GradeValue.BAD.toAvgGrade());
        utilsService.notifyAboutApprovedGrades(Collections.singletonList(shopGrade));
        Mockito.doAnswer(invocation -> {
            NotificationEventSource notification = invocation.getArgument(0);
            if (notification.getNotificationSubtype().getType() == NotificationType.SHOP_GRADE) {
                throw new PersNotifyClientException(
                    "Could not add multi data to plain data");
            }
            return null;
        }).when(notifyClient).createEvent(any());

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(1, notificationQueueService.getEventsCount(CANCELLED));
        assertEquals(1, notificationQueueService.getEventsCount(PROCESSED));

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(2);
        assertTrue(notifications.containsKey(NotificationSubtype.NEGATIVE_SHOP_GRADE));
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_SHOP_GRADE));
    }

    private void checkNotificationCancelled() throws Exception {
        Mockito.doAnswer(invocation -> {
            NotificationEventSource notification = invocation.getArgument(0);
            if (notification.getNotificationSubtype() == NotificationSubtype.SUCCESSFUL_MODEL_GRADE) {
                throw new PersNotifyClientException(
                        "Failed to find or add subscriber for event source NotificationEventSource");
            }
            return null;
        }).when(notifyClient).createEvent(any());

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(0, notificationQueueService.getEventsCount(SKIPPED));
        assertEquals(1, notificationQueueService.getEventsCount(CANCELLED));
        assertEquals(0, notificationQueueService.getEventsCount(PROCESSED));

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(1);
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_MODEL_GRADE));
    }

    @Test
    public void testApproveShopGradeBadNotification() throws Exception {
        ShopGrade shopGrade = GradeCreator.constructShopGrade(SHOP_ID, USER_ID);
        shopGrade.setModState(UNMODERATED);
        shopGrade.setAverageGrade(GradeValue.NORMAL.toAvgGrade());

        long gradeId = gradeCreator.createGrade(shopGrade);
        approve(gradeId);
        imitateSaasIndexed(gradeId);

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(2, notificationQueueService.getEventsCount(PROCESSED));

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(2);
        assertTrue(notifications.containsKey(NotificationSubtype.NEGATIVE_SHOP_GRADE));
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_SHOP_GRADE));
    }

    @Test
    public void testApproveShopGradeGoodNotification() throws Exception {
        ShopGrade shopGrade = GradeCreator.constructShopGrade(SHOP_ID, USER_ID);
        shopGrade.setModState(UNMODERATED);
        shopGrade.setAverageGrade(GradeValue.GOOD.toAvgGrade());

        long gradeId = gradeCreator.createGrade(shopGrade);
        approve(gradeId);
        imitateSaasIndexed(gradeId);

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(2, notificationQueueService.getEventsCount(PROCESSED));

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(2);
        assertTrue(notifications.containsKey(NotificationSubtype.SHOP_GRADE));
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_SHOP_GRADE));
    }

    @Test
    public void testRejectShopGradeBadNotification() throws Exception {
        ShopGrade shopGrade = GradeCreator.constructShopGrade(SHOP_ID, USER_ID);
        shopGrade.setModState(UNMODERATED);
        shopGrade.setAverageGrade(GradeValue.NORMAL.toAvgGrade());

        long gradeId = gradeCreator.createGrade(shopGrade);
        reject(gradeId, REJECTED, ModReason.UNINFORMATIVE.forShop());

        executor.runTmsJob();

        checkShopGradeEventsProcessed();

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(1);
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_SHOP_GRADE));
    }

    @Test
    public void testAutomaticallyRejectShopGradeBadNotification() throws Exception {
        ShopGrade shopGrade = GradeCreator.constructShopGrade(SHOP_ID, USER_ID);
        shopGrade.setModState(UNMODERATED);
        shopGrade.setAverageGrade(GradeValue.NORMAL.toAvgGrade());

        long gradeId = gradeCreator.createGrade(shopGrade);
        reject(gradeId, AUTOMATICALLY_REJECTED, ModReason.CAPS.forShop());

        executor.runTmsJob();

        checkShopGradeEventsProcessed();

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(1);
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_SHOP_GRADE));
    }

    @Test
    public void testRejectShopGradeGoodNotification() throws Exception {
        checkGoodNotificationForRejectedShopGrade(REJECTED);
    }

    @Test
    public void testAutomaticallyRejectShopGradeGoodNotification() throws Exception {
        checkGoodNotificationForRejectedShopGrade(AUTOMATICALLY_REJECTED);
    }

    private void checkGoodNotificationForRejectedShopGrade(ModState modState) throws Exception {
        ShopGrade shopGrade = GradeCreator.constructShopGrade(SHOP_ID, USER_ID);
        shopGrade.setModState(UNMODERATED);
        shopGrade.setAverageGrade(GradeValue.GOOD.toAvgGrade());

        long gradeId = gradeCreator.createGrade(shopGrade);
        reject(gradeId, modState, ModReason.RUDE.forShop());

        executor.runTmsJob();

        checkShopGradeEventsProcessed();

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(1);
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_SHOP_GRADE));
    }

    @Test
    public void testGradeVoteNotification() throws Exception {
        long gradeId = gradeCreator.createModelGradeUnmoderated(MODEL_ID, USER_ID);
        approve(gradeId);

        Long voteId = dbGradeVoteService.createVote(gradeId, USER_ID + 1, GradeVoteKind.agree, null);

        // do it manually, contract is checked in controller
        utilsService.notifyGradeVoted(gradeId, voteId);

        assertEquals(1, notificationQueueService.getNewEventsCount());
        assertEquals(0, notificationQueueService.getEventsCount(PROCESSED));

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(1, notificationQueueService.getEventsCount(PROCESSED));

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(1);
        assertTrue(notifications.containsKey(NotificationSubtype.PUSH_GRADE_VOTED));
    }

    @Test
    public void testFailSafeNotification() throws Exception {
        long gradeId = gradeCreator.createModelGradeUnmoderated(MODEL_ID, USER_ID);
        reject(gradeId, REJECTED, ModReason.UNINFORMATIVE.forModel());

        doThrow(RetryService.NotRetryException.class).when(notifyClient).createEvent(any());
        executor.runTmsJob();

        // check failed
        assertEquals(1, notificationQueueService.getNewEventsCount());
        assertEquals(MonitoringStatus.OK, complexMonitoring.getResult().getStatus());

        // check attempts increased
        List<NotificationEvent> event = notificationQueueService.getNewEvents(NOTIFY_GRADE_MOD_USER, 1);
        assertEquals(1, event.get(0).getAttempts());

        // try re-run and end just fine
        resetMonitoring();
        initMocks();
        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(0, notificationQueueService.getEventsCount(SKIPPED));
        assertEquals(1, notificationQueueService.getEventsCount(PROCESSED));

        Map<NotificationSubtype, NotificationEventSource> notifications = verifyNotifyEvents(1);
        assertTrue(notifications.containsKey(NotificationSubtype.SUCCESSFUL_MODEL_GRADE));
    }

    @Test
    public void testFailNotification() throws Exception {
        long gradeId = gradeCreator.createModelGradeUnmoderated(MODEL_ID, USER_ID);
        reject(gradeId, REJECTED, ModReason.UNINFORMATIVE.forModel());

        long maxAttempts = NotificationEventsUtilsExecutor.MAX_ATTEMPTS;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            boolean isLastAttempt = attempt == maxAttempts - 1;

            resetMonitoring();
            initMocks();
            doThrow(RetryService.NotRetryException.class).when(notifyClient).createEvent(any());
            executor.runTmsJob();

            // check failed
            assertEquals(isLastAttempt? 0 : 1, notificationQueueService.getNewEventsCount());

            assertEquals(isLastAttempt ? MonitoringStatus.WARNING : MonitoringStatus.OK,
                complexMonitoring.getResult().getStatus());

            // check attempts increased
            if (!isLastAttempt) {
                List<NotificationEvent> event = notificationQueueService.getNewEvents(NOTIFY_GRADE_MOD_USER, 1);
                assertEquals(attempt + 1, event.get(0).getAttempts());
            }
        }

        // check all attempts failed (expept for shop, with no utils call)
        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(0, notificationQueueService.getEventsCount(SKIPPED));
        assertEquals(1, notificationQueueService.getEventsCount(FAILED));
    }

    @Test
    public void testInvalidNotification() throws Exception {
        long gradeId = gradeCreator.createModelGradeUnmoderated(MODEL_ID, USER_ID);
        approve(gradeId);

        Long voteId = dbGradeVoteService.createVote(gradeId, USER_ID + 1, GradeVoteKind.agree, null);

        // add invalid notification
        notificationQueueService.addEvent(NOTIFY_GRADE_VOTED_PUSH, Map.of("grade_id", "invalid"));

        assertEquals(1, notificationQueueService.getNewEventsCount());
        assertEquals(0, notificationQueueService.getEventsCount(PROCESSED));

        executor.runTmsJob();

        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(0, notificationQueueService.getEventsCount(PROCESSED));
        assertEquals(0, notificationQueueService.getEventsCount(FAILED));
        assertEquals(1, notificationQueueService.getEventsCount(BAD_DATA));

        assertEquals(MonitoringStatus.WARNING, complexMonitoring.getResult().getStatus());
    }

    private void approve(long gradeId) {
        gradeModeratorModificationProxy.moderateGradeReplies(
            Collections.singletonList(gradeId),
            Collections.emptyList(), TEST_MODERATOR_ID,
            APPROVED);
        assertEquals(0, notificationQueueService.getNewEventsCount());
    }

    private void imitateSaasIndexed(long gradeId) {
        // do it manually, contract is checked in indexing job
        utilsService.notifyAboutApprovedGrades(Collections.singletonList(gradeId));
        assertEquals(2, notificationQueueService.getNewEventsCount());
        assertEquals(0, notificationQueueService.getEventsCount(PROCESSED));
    }

    private void checkModelGradeEventsProcessed() {
        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(1, notificationQueueService.getEventsCount(SKIPPED));
        assertEquals(1, notificationQueueService.getEventsCount(PROCESSED));
    }

    private void checkShopGradeEventsProcessed() {
        assertEquals(0, notificationQueueService.getNewEventsCount());
        assertEquals(1, notificationQueueService.getEventsCount(PROCESSED));
    }

    private void reject(long gradeId, ModState modState, Long modReason) {

        gradeModeratorModificationProxy.moderateGradeReplies(
            Collections.singletonMap(gradeId,  modReason),
            Collections.emptyMap(),
            TEST_MODERATOR_ID,
            modState);

        assertEquals(1, notificationQueueService.getNewEventsCount());
        assertEquals(0, notificationQueueService.getEventsCount(PROCESSED));
    }

    private Map<NotificationSubtype, NotificationEventSource> verifyNotifyEvents(int times) throws Exception {
        ArgumentCaptor<NotificationEventSource> eventCaptor = ArgumentCaptor.forClass(NotificationEventSource.class);
        verify(notifyClient, times(times)).createEvent(eventCaptor.capture());

        return ListUtils.toMap(eventCaptor.getAllValues(), NotificationEventSource::getNotificationSubtype);
    }

    private ModState getGradeModState(long gradeId) {
        return ModState.byValue(pgJdbcTemplate.queryForObject("SELECT mod_state FROM grade WHERE id=" + gradeId, Integer.class));
    }

    private void mockCleanWebBadVerdict(long gradeId) {
        VerdictDto verdictDto = new VerdictDto();
        verdictDto.setKey(String.valueOf(gradeId));
        verdictDto.setName("text_auto_obscene");
        verdictDto.setValue("true");

        CleanWebResponseDto dto = new CleanWebResponseDto(String.valueOf(gradeId), new VerdictDto[]{verdictDto});
        configurationService.tryGetOrMergeVal(GradeCleanWebService.ENABLE_KEY, Boolean.class, true);
        when(cleanWebClient.sendContent(anyList(), anyBoolean())).thenReturn(new CleanWebResponseDto[]{dto});
    }

    private Long getGradeModReason(long gradeId) {
        return pgJdbcTemplate.queryForList(
                "SELECT mod_reason FROM grade WHERE id =" + gradeId, Long.class)
                .stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void updateCrTimeForAutomoderation(long gradeId) {
        pgJdbcTemplate.update("UPDATE grade SET cr_time = now() - interval '1' day WHERE id = " + gradeId);
    }

}
