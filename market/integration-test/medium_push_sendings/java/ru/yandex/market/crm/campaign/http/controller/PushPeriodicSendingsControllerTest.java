package ru.yandex.market.crm.campaign.http.controller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.springframework.http.MediaType;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.campaign.domain.periodic.Event;
import ru.yandex.market.crm.campaign.domain.periodic.EventType;
import ru.yandex.market.crm.campaign.domain.periodic.NotificationSettings;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.PushPeriodicSending;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.sending.periodic.GeneratedEvent;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.dto.sending.PushPeriodicSendingDto;
import ru.yandex.market.crm.campaign.dto.sending.SendRequest;
import ru.yandex.market.crm.campaign.http.response.ErrorResponse;
import ru.yandex.market.crm.campaign.services.converter.sending.PushPeriodicSendingConverter;
import ru.yandex.market.crm.campaign.services.security.ObjectPermissions;
import ru.yandex.market.crm.campaign.services.security.Roles;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.dao.PushPeriodicSendingDAO;
import ru.yandex.market.crm.campaign.services.sending.periodic.PushEndNotificationTask;
import ru.yandex.market.crm.campaign.services.sending.periodic.PushPeriodicSendingService;
import ru.yandex.market.crm.campaign.services.sending.periodic.StartPeriodicPushSendingTask;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumWithoutYtTest;
import ru.yandex.market.crm.campaign.test.tms.TestScheduler;
import ru.yandex.market.crm.campaign.test.utils.AccountsTeslHelper;
import ru.yandex.market.crm.campaign.test.utils.PeriodicEntitiesTestUtils;
import ru.yandex.market.crm.campaign.test.utils.PushPeriodicSendingTestHelper;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.MobileAppsTestHelper;
import ru.yandex.market.crm.core.test.utils.SecurityUtils;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.crm.domain.CompositeUserRole;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.misc.thread.ThreadUtils;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

/**
 * @author apershukov
 */
public class PushPeriodicSendingsControllerTest extends AbstractControllerMediumWithoutYtTest {

    private static final String ACCOUNT_1 = "ACCOUNT_1";
    private static final String ACCOUNT_2 = "ACCOUNT_2";

    private static final String MOBILE_APP_1 = "MOBILE_APP_1";
    private static final String MOBILE_APP_2 = "MOBILE_APP_2";

    @Inject
    private PushPeriodicSendingTestHelper pushPeriodicSendingTestHelper;

    @Inject
    private SegmentService segmentService;

    @Inject
    private PushPeriodicSendingDAO sendingDAO;

    @Inject
    private PushPeriodicSendingConverter periodicSendingConverter;

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private TestScheduler scheduler;

    @Inject
    private PushPeriodicSendingService sendingService;

    @Inject
    private StartPeriodicPushSendingTask task;

    @Inject
    private MobileAppsTestHelper mobileAppsTestHelper;

    @Inject
    private UsersRolesDao usersRolesDao;

    @Inject
    private AccountsTeslHelper accountsTeslHelper;

    /**
     * Конфигурацию и название рассылки можно отредактировать, сделав запрос к ручке
     * PUT /api/periodic_sendings/push/by_key/{key}
     * <p>
     * При этом если рассылка ранее не отправлялась, новая версия не создается
     */
    @Test
    public void testUpdateSending() throws Exception {
        PushPeriodicSending sending = pushPeriodicSendingTestHelper.addSending();
        PushSendingConf config = sending.getConfig();

        Segment segment = segmentService.addSegment(segment(plusFilter()));
        TargetAudience target = new TargetAudience(LinkingMode.NONE, segment.getId());
        config.setTarget(target);

        PushPeriodicSendingDto dto = new PushPeriodicSendingDto();
        dto.setName("New Name");
        dto.setApplication(mobileAppsTestHelper.getMarketApp());
        dto.setConfig(config);

        mockMvc.perform(put("/api/periodic_sendings/push/by_key/{key}", sending.getKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSerializer.writeObjectAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk());

        sending = sendingDAO.getByKey(sending.getKey()).orElseThrow();

        Assertions.assertEquals(target, sending.getConfig().getTarget());
        Assertions.assertEquals(dto.getName(), sending.getName());
        Assertions.assertEquals(1, sending.getVersion());
    }

    /**
     * При активации периодической рассылки:
     * <p>
     * 1. Создается quartz-джоба с cron-выражением, соответствующим расписанию, указанным пользователем
     * 2. Название джобы (её уникальный идентификатор) сохраняется в рассылке
     * 3. Сохраняется событие об акивации рассылки
     * 4. В случае если настроки оповещения не заданы quartz-джоба оповещения об окончании действия
     * рассылки не создается
     */
    @Test
    public void testEnableSending() throws Exception {
        PushPeriodicSending sending = prepareSending();

        mockMvc.perform(post("/api/periodic_sendings/push/by_id/{id}/enable", sending.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        List<JobDetail> jobs = scheduler.getJobs();
        Assertions.assertEquals(1, jobs.size());

        JobDetail job = jobs.get(0);

        String expectedJobName = "start_PushPeriodicSending#" + sending.getId();
        Assertions.assertEquals(new JobKey(expectedJobName, "PERIODIC_SENDINGS"), job.getKey());

        JobDataMap jobDataMap = job.getJobDataMap();
        Assertions.assertEquals(StartPeriodicPushSendingTask.BEAN_NAME, jobDataMap.getString("executorBeanName"));
        Assertions.assertEquals(sending.getId(), jobDataMap.getString("entity_id"));

        List<Trigger> triggers = scheduler.getTriggers();
        Assertions.assertEquals(1, triggers.size());

        Trigger trigger = triggers.get(0);
        Assertions.assertEquals(job.getKey(), trigger.getJobKey());

        Assertions.assertTrue(
                trigger instanceof CronTrigger,
                "Trigger has invalid type " + trigger.getClass().getSimpleName());

        CronTrigger cronTrigger = (CronTrigger) trigger;
        String expression = cronTrigger.getCronExpression();
        Assertions.assertTrue("0 30 13 ? * SAT,MON *".equals(expression) || "0 30 13 ? * MON,SAT *".equals(expression));

        sending = sendingService.getById(sending.getId());
        Assertions.assertEquals(expectedJobName, sending.getSendJobName());

        List<Event> events = sendingService.getEvents(sending.getKey(), 0);
        Assertions.assertNotNull(events);
        Assertions.assertEquals(Set.of(EventType.ENABLED),
                events.stream().map(Event::getType).collect(Collectors.toSet()));

        Assertions.assertNull(sending.getEndNotificationJobName());
    }

    /**
     * При деактивации рассылки:
     * <p>
     * 1. Удаляется связанная с ней джоба отправки
     * 2. Идентификатор удаленной джобы удаляется из рассылки
     * 3. Сохраняется событие об деактивации рассылки
     */
    @Test
    public void testDisableSending() throws Exception {
        PushPeriodicSending sending = prepareSending();
        sendingService.enable(sending.getId());
        PeriodicEntitiesTestUtils.startTask(task, sending.getId());

        mockMvc.perform(post("/api/periodic_sendings/push/by_id/{id}/disable", sending.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        Assertions.assertTrue(scheduler.getJobs().isEmpty());
        Assertions.assertTrue(scheduler.getTriggers().isEmpty());

        sending = sendingService.getById(sending.getId());
        Assertions.assertNull(sending.getSendJobName());
        Assertions.assertNull(sending.getEndNotificationJobName());

        //На всякий случай ждём отмены 5 сек, поскольку одновременно может начаться выполнение подшага и остановка таски
        ThreadUtils.sleep(5, TimeUnit.SECONDS);

        List<Event> events = sendingService.getEvents(sending.getKey(), 0);
        MatcherAssert.assertThat(events, not(empty()));
        Assertions.assertEquals(Set.of(EventType.DISABLED, EventType.GENERATED, EventType.ENABLED),
                events.stream().map(Event::getType).collect(Collectors.toSet()));
        Assertions.assertTrue(events.stream()
                .filter(event -> event instanceof GeneratedEvent)
                .map(event -> (GeneratedEvent) event)
                .allMatch(event -> event.getStatus() != StageStatus.IN_PROGRESS));
    }

    @Test
    public void testCopyPeriodicPushSendingChangeUtmCampaignMiddle() throws Exception {
        var action = "yamarket://test?referrer=appmetrica_tracking_id%3D1035348116234076871%26utm_source" +
                "%3Dpush_andr%26reattribution%3D1%26utm_campaign%3Dtest_utm_campaign123%26clid%3D621%26utm_referrer" +
                "%3D621%26utm_medium%3Dmassmail";

        String expectedAction =
                """
                        yamarket://test?referrer=\
                        appmetrica_tracking_id%%3D1035348116234076871%%26\
                        utm_source%%3Dpush_andr%%26\
                        reattribution%%3D1%%26\
                        utm_campaign%%3D%s%%26\
                        clid%%3D621%%26\
                        utm_referrer%%3D621%%26\
                        utm_medium%%3Dmassmail\
                        """;
        testCopyPeriodicPushSendingChangeUtmCampaign(action, expectedAction);
    }

    @Test
    public void testCopyPeriodicPushSendingChangeUtmCampaignEnd() throws Exception {

        var action = "yamarket://test?referrer=appmetrica_tracking_id%3D1035348116234076871%26utm_source" +
                "%3Dpush_andr%26reattribution%3D1%26clid%3D621%26utm_referrer" +
                "%3D621%26utm_medium%3Dmassmail%26utm_campaign%3Dtest_utm_campaign123";

        String expectedAction =
                """
                        yamarket://test?referrer=\
                        appmetrica_tracking_id%%3D1035348116234076871%%26\
                        utm_source%%3Dpush_andr%%26\
                        reattribution%%3D1%%26\
                        clid%%3D621%%26\
                        utm_referrer%%3D621%%26\
                        utm_medium%%3Dmassmail%%26\
                        utm_campaign%%3D%s\
                        """;
        testCopyPeriodicPushSendingChangeUtmCampaign(action, expectedAction);
    }

    /**
     * Редактирование настроек оповещения для активной рассылки для которой оповещение еще
     * не было настроено приводит к планированию специальной джобы оповещения об окончании
     * действия push-рассылки
     */
    @Test
    public void testSetNotificationSettings() throws Exception {
        PushPeriodicSending sending = prepareSending();
        sendingService.enable(sending.getId());

        NotificationSettings settings = new NotificationSettings()
                .setDaysBeforeEnd(3)
                .setEmails(Set.of("manager@yandex-team.ru"));

        mockMvc.perform(put(
                        "/api/periodic_sendings/push/by_id/{sendingId}/nf_settings",
                        sending.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSerializer.writeObjectAsString(settings)))
                .andDo(print())
                .andExpect(status().isOk());

        sending = sendingService.getById(sending.getId());

        NotificationSettings savedSettings = sending.getNotificationSettings();
        Assertions.assertEquals(settings.getDaysBeforeEnd(), savedSettings.getDaysBeforeEnd());
        Assertions.assertEquals(settings.getEmails(), savedSettings.getEmails());

        String expectedJobName = "end_PushPeriodicSending_notification#" + sending.getId();
        Assertions.assertEquals(expectedJobName, sending.getEndNotificationJobName());

        JobKey jobKey = new JobKey(expectedJobName, "PERIODIC_SENDINGS");

        JobDetail jobDetail = scheduler.getJobs().stream()
                .filter(job -> jobKey.equals(job.getKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No notification job"));

        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        Assertions.assertEquals(PushEndNotificationTask.BEAN_NAME, jobDataMap.getString("executorBeanName"));
        Assertions.assertEquals(sending.getId(), jobDataMap.getString("entity_id"));
    }

    /**
     * Рассылку прошлая версия которой была отправлена нельзя удалить
     */
    @Test
    public void testPushHavingPreviousVersionThatHasBeenSentCannotBeDeleted() throws Exception {
        PushPeriodicSending sending = prepareSending();
        sendingService.setSentFlag(sending.getId());

        // Create a new version of sending
        sending.setName("New sending name");
        PushPeriodicSending newVersion = sendingService.update(sending);
        Assertions.assertEquals(2, newVersion.getVersion());

        mockMvc.perform(delete("/api/periodic_sendings/push/by_key/{id}", sending.getKey()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    /**
     * Позволяем переключать активную рассылку
     */
    @Test
    public void testActivateThrottleEvenInEnabledSending() throws Exception {
        PushPeriodicSending sending = prepareSending();
        Assertions.assertFalse(sending.getConfig().isFrequencyThrottleEnabled());

        mockMvc.perform(post("/api/periodic_sendings/push/by_id/{id}/enable", sending.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/periodic_sendings/push/by_key/{key}/activateThrottle", sending.getKey()))
                .andDo(print())
                .andExpect(status().isOk());

        sending = sendingService.getByKey(sending.getKey());
        Assertions.assertTrue(sending.getConfig().isFrequencyThrottleEnabled());
    }

    /**
     * Позволяем переключать рассылку, которая хотя бы раз уже выполнялась, без создания новой версии.
     * <p>
     * Обычные модификации таких рассылок приводят к появлению новой версии, а в данном случае так делать не следует.
     */
    @Test
    public void testActivateThrottleForExecutedSending() throws Exception {
        PushPeriodicSending sending = prepareSending();
        sendingDAO.setExecuted(sending.getId());
        int previousVersion = sendingService.getById(sending.getId()).getVersion();

        mockMvc.perform(post("/api/periodic_sendings/push/by_id/{id}/enable", sending.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/periodic_sendings/push/by_key/{key}/activateThrottle", sending.getKey()))
                .andDo(print())
                .andExpect(status().isOk());

        var resultingSending = sendingService.getByKey(sending.getKey());
        Assertions.assertAll(
                () -> Assertions.assertTrue(resultingSending.getConfig().isFrequencyThrottleEnabled()),
                () -> Assertions.assertEquals(previousVersion, resultingSending.getVersion())
        );
    }

    /**
     * Пользователь с ролью оператор не может активировать/деактивировать регулярные push-рассылки,
     * которые сконфигурированы на отправку пушей в мобильные приложения, отсутствующие в аккаунте пользователя
     */
    @Test
    public void testOperatorCantActivatePushPromoToMobileAppNotFromHisAccount() throws Exception {
        mobileAppsTestHelper.insertApplication(MOBILE_APP_1, 111222333, YPath.cypressRoot(), List.of());
        mobileAppsTestHelper.insertApplication(MOBILE_APP_2, 444555666, YPath.cypressRoot(), List.of());

        accountsTeslHelper.prepareAccount(ACCOUNT_1, Set.of(MOBILE_APP_1));
        accountsTeslHelper.prepareAccount(ACCOUNT_2, Set.of(MOBILE_APP_2));

        //TODO: uid нужен, чтобы тесты работали. Убрать после выпиливания ограничений
        var user = SecurityUtils.profile("operator_profile", 1120000000039960L);
        usersRolesDao.addRole(user.getUid(), new CompositeUserRole(ACCOUNT_1, Roles.OPERATOR));

        PushPeriodicSending sending = prepareSending();

        SecurityUtils.setAuthentication(user);

        var response1 = mockMvc.perform(
                post("/api/periodic_sendings/push/by_id/{id}/enable", sending.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSerializer.writeObjectAsBytes(new SendRequest())))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andReturn().getResponse();

        var errorResponse1 = jsonDeserializer.readObject(
                ErrorResponse.class,
                response1.getContentAsString()
        );

        assertEquals(
                "Access denied for object. Required permission: " + ObjectPermissions.SEND,
                errorResponse1.getMessage()
        );

        sending = sendingService.getByKey(sending.getKey());
        Assertions.assertFalse(sending.isEnabled());

        sendingService.enable(sending.getId());

        var response2 = mockMvc.perform(
                post("/api/periodic_sendings/push/by_id/{id}/disable", sending.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSerializer.writeObjectAsBytes(new SendRequest())))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andReturn().getResponse();

        var errorResponse2 = jsonDeserializer.readObject(
                ErrorResponse.class,
                response2.getContentAsString()
        );

        assertEquals(
                "Access denied for object. Required permission: " + ObjectPermissions.SEND,
                errorResponse2.getMessage()
        );

        sending = sendingService.getByKey(sending.getKey());
        Assertions.assertTrue(sending.isEnabled());
    }

    /**
     * Пользователь с ролью оператор может активировать/деактивировать регулярные push-рассылки,
     * которые сконфигурированы на отправку пушей в мобильные приложения, присутствующие в аккаунте пользователя
     */
    @Test
    public void testOperatorCanActivatePushPromoToMobileAppFromHisAccount() throws Exception {
        mobileAppsTestHelper.insertApplication(MOBILE_APP_1, 111222333, YPath.cypressRoot(), List.of());

        accountsTeslHelper.prepareAccount(ACCOUNT_1, Set.of(MOBILE_APP_1));

        //TODO: uid нужен, чтобы тесты работали. Убрать после выпиливания ограничений
        var user = SecurityUtils.profile("operator_profile", 1120000000039960L);
        usersRolesDao.addRole(user.getUid(), new CompositeUserRole(ACCOUNT_1, Roles.OPERATOR));

        PushPeriodicSending sending = prepareSending();

        mockMvc.perform(post("/api/periodic_sendings/push/by_id/{id}/enable", sending.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        sending = sendingService.getByKey(sending.getKey());
        Assertions.assertTrue(sending.isEnabled());

        mockMvc.perform(post("/api/periodic_sendings/push/by_id/{id}/disable", sending.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        sending = sendingService.getByKey(sending.getKey());
        Assertions.assertFalse(sending.isEnabled());
    }

    private PushPeriodicSendingDto requestCopy(PushPeriodicSending sending) throws Exception {
        sending.setKey(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) + "_" + UUID.randomUUID());
        return jsonDeserializer.readObject(
                PushPeriodicSendingDto.class,
                mockMvc.perform(post("/api/periodic_sendings/push/by_id/{id}/copy", sending.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonSerializer.writeObjectAsString(periodicSendingConverter.convert(sending))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        );
    }

    private PushPeriodicSending prepareSending() {
        Segment segment = segmentService.addSegment(segment(plusFilter()));
        return pushPeriodicSendingTestHelper.prepareSending(segment);
    }

    private PushPeriodicSending prepareSending(String action) {
        Segment segment = segmentService.addSegment(segment(plusFilter()));
        return pushPeriodicSendingTestHelper.prepareSending(segment, action);
    }

    private void testCopyPeriodicPushSendingChangeUtmCampaign(String action, String expectedAction) throws Exception {

        var sending = prepareSending(action);

        var copy = requestCopy(sending);

        Assertions.assertEquals(1, copy.getConfig().getVariants().size());

        PushSendingVariantConf variantCopy = copy.getConfig().getVariants().get(0);
        for (var platformConfig : variantCopy.getPushConfigs().values()) {
            Assertions.assertEquals(String.format(expectedAction, variantCopy.getId()), platformConfig.getAction());
        }
    }
}
