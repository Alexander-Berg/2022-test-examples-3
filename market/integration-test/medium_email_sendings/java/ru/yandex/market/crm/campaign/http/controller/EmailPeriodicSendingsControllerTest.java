package ru.yandex.market.crm.campaign.http.controller;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.periodic.DefaultEvent;
import ru.yandex.market.crm.campaign.domain.periodic.Event;
import ru.yandex.market.crm.campaign.domain.periodic.EventType;
import ru.yandex.market.crm.campaign.domain.periodic.NotificationSettings;
import ru.yandex.market.crm.campaign.domain.periodic.PeriodicEntityUpdateResponse;
import ru.yandex.market.crm.campaign.domain.periodic.Schedule;
import ru.yandex.market.crm.campaign.domain.periodic.Schedule.DateTimeInterval;
import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.EmailPeriodicSending;
import ru.yandex.market.crm.campaign.domain.sending.TestEmail;
import ru.yandex.market.crm.campaign.domain.sending.TestEmailsGroup;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.sending.periodic.GeneratedEvent;
import ru.yandex.market.crm.campaign.domain.utils.NamedEntity;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.dto.segment.SegmentDto;
import ru.yandex.market.crm.campaign.dto.sending.EmailPeriodicSendingDto;
import ru.yandex.market.crm.campaign.services.grouping.campaign.CampaignDAO;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.PluggableTablesInEmailPeriodicSendingDAO;
import ru.yandex.market.crm.campaign.services.sending.UpdateResponse;
import ru.yandex.market.crm.campaign.services.sending.dao.EmailPeriodicSendingDAO;
import ru.yandex.market.crm.campaign.services.sending.periodic.EmailPeriodicSendingService;
import ru.yandex.market.crm.campaign.services.sending.periodic.StartPeriodicEmailSendingTask;
import ru.yandex.market.crm.campaign.services.sql.TestEmailsDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.campaign.test.tms.TestScheduler;
import ru.yandex.market.crm.campaign.test.utils.EmailPeriodicSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PeriodicEntitiesTestUtils;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.core.domain.messages.PluggedTable;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.sending.UploadedImage;
import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType;
import ru.yandex.market.crm.core.domain.yasender.YaSenderCampaign;
import ru.yandex.market.crm.core.services.image.ImageOwnerType;
import ru.yandex.market.crm.core.services.image.UploadedImageDAO;
import ru.yandex.market.crm.core.test.utils.YaSenderHelper;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderDataRow;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

/**
 * @author apershukov
 */
public class EmailPeriodicSendingsControllerTest extends AbstractControllerMediumTest {

    @NotNull
    private static String nfJobName(EmailPeriodicSending sending) {
        return "end_EmailPeriodicSending_notification#" + sending.getId();
    }

    @Inject
    private EmailPeriodicSendingService sendingService;

    @Inject
    private EmailPeriodicSendingDAO emailPeriodicSendingDAO;

    @Inject
    private CampaignDAO campaignDAO;

    @Inject
    private SegmentService segmentService;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private UploadedImageDAO uploadedImageDAO;

    @Inject
    private PluggableTablesInEmailPeriodicSendingDAO pluggableTablesInSendingDAO;

    @Inject
    private TestScheduler scheduler;

    @Inject
    private PluggableTablesTestHelper pluggableTablesTestHelper;

    @Inject
    private EmailPeriodicSendingTestHelper emailPeriodicSendingTestHelper;

    @Inject
    private YaSenderHelper yaSenderHelper;

    @Inject
    private YtClient ytClient;

    @Inject
    private TestEmailsDAO testEmailsDAO;

    @Inject
    StartPeriodicEmailSendingTask task;

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
    public void testEnableEmailSending() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();

        requestEnable(sending);

        List<JobDetail> jobs = scheduler.getJobs();
        Assertions.assertEquals(1, jobs.size());

        JobDetail job = jobs.get(0);

        String expectedJobName = "start_EmailPeriodicSending#" + sending.getId();
        Assertions.assertEquals(new JobKey(expectedJobName, "PERIODIC_SENDINGS"), job.getKey());

        JobDataMap jobDataMap = job.getJobDataMap();
        Assertions.assertEquals(StartPeriodicEmailSendingTask.BEAN_NAME, jobDataMap.getString("executorBeanName"));
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
    public void testDisableEmailSending() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();
        activateSending(sending, false);

        PeriodicEntitiesTestUtils.startTask(task, sending.getId());

        requestDisable(sending);

        assertNoScheduledJobs();

        sending = sendingService.getById(sending.getId());
        Assertions.assertNull(sending.getSendJobName());
        Assertions.assertNull(sending.getEndNotificationJobName());

        List<Event> events = sendingService.getEvents(sending.getKey(), 0);
        Assertions.assertNotNull(events);
        Assertions.assertEquals(Set.of(EventType.DISABLED, EventType.GENERATED),
                events.stream().map(Event::getType).collect(Collectors.toSet()));
        Assertions.assertTrue(events.stream()
                .filter(event -> event instanceof GeneratedEvent)
                .map(event -> (GeneratedEvent) event)
                .allMatch(event -> event.getStatus() != StageStatus.IN_PROGRESS));
    }

    /**
     * При деактивации рассылки с запланированной джобой оповещения джоба оповещения
     * удаляется
     */
    @Test
    public void testDisableSendingWithScheduledNfJob() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();
        activateSending(sending, true);

        requestDisable(sending);

        assertNoScheduledJobs();

        sending = sendingService.getById(sending.getId());
        Assertions.assertNull(sending.getSendJobName());
        Assertions.assertNull(sending.getEndNotificationJobName());
    }

    /**
     * В случае если у рассылки установлена дата истечения и настроено оповещение
     * при активации создается quartz-джоба оповещения о приближении конца действия
     */
    @Test
    public void testEnableSendingWithExpirationDateAndEmailToNotify() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();

        LocalDateTime endTime = LocalDateTime.now().plusDays(7);

        Schedule schedule = new Schedule()
                .setTime(LocalTime.of(13, 0, 0))
                .setDaysOfWeek(Set.of(DayOfWeek.MONDAY))
                .setActiveInterval(
                        new DateTimeInterval()
                                .setBegin(LocalDateTime.now())
                                .setEnd(endTime)
                );

        sendingService.updateSchedule(sending.getId(), schedule);

        NotificationSettings nfSettigns = new NotificationSettings()
                .setDaysBeforeEnd(1)
                .setEmails(Set.of("operator@yandex-team.ru"));

        sendingService.updateNotificationSettings(sending.getId(), nfSettigns);

        requestEnable(sending);

        sending = sendingService.getById(sending.getId());

        String expectedJobName = nfJobName(sending);
        Assertions.assertEquals(expectedJobName, sending.getEndNotificationJobName());

        List<Trigger> triggers = scheduler.getTriggers();
        Assertions.assertEquals(2, triggers.size());

        boolean notifyJobScheduled = triggers.stream()
                .anyMatch(x -> expectedJobName.equals(x.getJobKey().getName()));

        Assertions.assertTrue(notifyJobScheduled);
    }

    /**
     * Ручка GET /api/periodic_sendings/email/{id} возвращает рыссылку с id
     * в случае если она существует
     */
    @Test
    public void testGetSending() throws Exception {
        Campaign campaign = prepareCampaign();

        Segment segment = segmentService.addSegment(segment(
                passportGender("m")
        ));

        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending(campaign, segment, s -> {
            UploadedImage image = prepareUploadedImage();
            s.setBanners(List.of(image));
        });

        MvcResult result = mockMvc.perform(get("/api/periodic_sendings/email/by_id/{id}", sending.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        EmailPeriodicSendingDto dto = jsonDeserializer.readObject(
                EmailPeriodicSendingDto.class,
                result.getResponse().getContentAsString()
        );

        Assertions.assertEquals(sending.getName(), dto.getName());
        Assertions.assertEquals(EmailPeriodicSendingDto.Status.DRAFT, dto.getStatus());

        EmailSendingConf config = sending.getConfig();
        Assertions.assertNotNull(config);

        Assertions.assertNotNull(sending.getSchedule());

        SegmentDto segmentDto = dto.getSegment();
        Assertions.assertNotNull(segmentDto);
        Assertions.assertEquals(segment.getId(), segmentDto.getId());
        Assertions.assertEquals(segment.getName(), segmentDto.getName());

        Campaign campaignDto = dto.getCampaign();
        Assertions.assertNotNull(campaignDto);
        Assertions.assertEquals(campaign.getId(), campaignDto.getId());
        Assertions.assertEquals(campaign.getName(), campaignDto.getName());

        Assertions.assertNotNull(dto.getReplyTo());
        Assertions.assertNotNull(dto.getFrom());

        SubscriptionType subscriptionType = dto.getSubscriptionType();
        Assertions.assertNotNull(subscriptionType);
        Assertions.assertEquals(2, subscriptionType.getId());

        List<UploadedImage> banners = dto.getBanners();
        Assertions.assertNotNull(banners);
        Assertions.assertEquals(1, banners.size());

        Assertions.assertTrue(dto.isEditable());
        Assertions.assertTrue(dto.isMayBeDeleted());
    }

    /**
     * Ручка GET /api/periodic_sendings/email/{id} отдает 404 в случае если
     * запрошенной рассылки не существует
     */
    @Test
    public void test404OnGetSendingWhichDoesntExist() throws Exception {
        mockMvc.perform(get("/api/periodic_sendings/email/by_id/{id}", "unknown_sending"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    /**
     * С помощью ручки PUT /api/periodic_sendings/email/{id} можно изменить конфиг,
     * название рассылки и баннеры.
     * <p>
     * В случае если рассылка уже отправлялась (есть созданная кампания в рассыляторе) создается её новая
     * версия. Старая версия при этом не изменяется.
     * <p>
     * - Расписание и настройки оповещений переходят в новую версию без изменений.
     * - Значение счетчика отправок в новой версии равен значению счетчика в старой
     */
    @Test
    public void testUpdateEmailSending() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();

        Schedule schedule = new Schedule()
                .setTime(LocalTime.of(13, 0, 0))
                .setDaysOfWeek(Set.of(DayOfWeek.MONDAY))
                .setActiveInterval(
                        new DateTimeInterval()
                                .setBegin(LocalDateTime.now().minusDays(1).withNano(0))
                                .setEnd(LocalDateTime.now().plusDays(10).withNano(0))
                );

        sendingService.updateSchedule(sending.getId(), schedule);

        NotificationSettings nfSettigns = new NotificationSettings()
                .setDaysBeforeEnd(1)
                .setEmails(Set.of("operator@yandex-team.ru"));

        sendingService.updateNotificationSettings(sending.getId(), nfSettigns);
        sendingService.setSentFlag(sending.getId());

        emailPeriodicSendingDAO.updateIteration(sending.getId(), 5);

        EmailSendingConf oldConfig = sending.getConfig();

        Segment newSegment = segmentService.addSegment(segment(plusFilter()));
        UploadedImage image = prepareUploadedImage();
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable();

        EmailSendingConf newConfig = new EmailSendingConf();
        newConfig.setTarget(new TargetAudience(LinkingMode.ALL, newSegment.getId()));
        newConfig.setVariants(oldConfig.getVariants());
        newConfig.setPluggedTables(List.of(
                new PluggedTable(pluggableTable.getId(), "table")
        ));
        newConfig.setSubscriptionType(1L);

        EmailPeriodicSendingDto dto = new EmailPeriodicSendingDto();
        dto.setName("New name");
        dto.setConfig(newConfig);
        dto.setBanners(List.of(image));

        requestUpdate(sending, dto);

        EmailPeriodicSending newVersion = sendingService.getById(
                sending.getKey() + ":" + (sending.getVersion() + 1)
        );

        Assertions.assertEquals(dto.getName(), newVersion.getName());
        Assertions.assertEquals(
                newConfig.getTarget().getSegment(),
                newVersion.getConfig().getTarget().getSegment()
        );

        List<UploadedImage> banners = newVersion.getBanners();
        Assertions.assertNotNull(banners);
        Assertions.assertEquals(1, banners.size());
        Assertions.assertEquals(image.getId(), banners.get(0).getId());
        Assertions.assertEquals(image.getName(), banners.get(0).getName());
        Assertions.assertEquals(image.getUrl(), banners.get(0).getUrl());

        Set<String> usingSendings = pluggableTablesInSendingDAO.getUsingSendings(pluggableTable.getId()).stream()
                .map(NamedEntity::getId)
                .collect(Collectors.toSet());

        Assertions.assertEquals(Set.of(newVersion.getId()), usingSendings);

        Assertions.assertTrue(newVersion.isExecuted());
        Assertions.assertEquals(5, newVersion.getIteration());
        Assertions.assertEquals(sending.getCreationTime(), newVersion.getCreationTime());

        Schedule newVersionSchedule = newVersion.getSchedule();
        Assertions.assertNotNull(newVersionSchedule);
        Assertions.assertEquals(schedule.getTime(), newVersionSchedule.getTime());
        Assertions.assertEquals(schedule.getDaysOfWeek(), newVersionSchedule.getDaysOfWeek());
        Assertions.assertEquals(schedule.getActiveInterval().getBegin(),
                newVersionSchedule.getActiveInterval().getBegin()
        );
        Assertions.assertEquals(schedule.getActiveInterval().getEnd(),
                newVersionSchedule.getActiveInterval().getEnd()
        );

        NotificationSettings newVersionNfSettings = newVersion.getNotificationSettings();
        Assertions.assertNotNull(newVersionNfSettings);
        Assertions.assertEquals(nfSettigns.getEmails(), newVersionNfSettings.getEmails());
        Assertions.assertEquals(nfSettigns.getDaysBeforeEnd(), newVersionNfSettings.getDaysBeforeEnd());

        EmailPeriodicSending oldVersion = sendingService.getById(sending.getId());

        Assertions.assertEquals(sending.getName(), oldVersion.getName());
        Assertions.assertEquals(sending.getVersion(), oldVersion.getVersion());
        Assertions.assertEquals(oldConfig.getTarget(), oldVersion.getConfig().getTarget());
    }

    /**
     * При редактировании рассылки которая еще ни разу не была отправлена
     * новая версия не создается
     */
    @Test
    public void testDoNotCreateNewVersionOfSendingWhichHasNotBeenSent() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();

        Segment newSegment = segmentService.addSegment(segment(plusFilter()));

        EmailSendingConf newConfig = new EmailSendingConf();
        newConfig.setTarget(new TargetAudience(LinkingMode.ALL, newSegment.getId()));
        newConfig.setVariants(sending.getConfig().getVariants());
        newConfig.setSubscriptionType(1L);

        EmailPeriodicSendingDto dto = new EmailPeriodicSendingDto();
        dto.setName(sending.getName());
        dto.setConfig(newConfig);

        var response = requestUpdate(sending, dto);

        EmailPeriodicSendingDto sendingFromResponse = response.getEntity();
        Assertions.assertNotNull(sendingFromResponse);
        Assertions.assertEquals(sending.getVersion(), sendingFromResponse.getVersion());
        Assertions.assertEquals(sending.getId(), sendingFromResponse.getId());

        EmailPeriodicSending actualSending = sendingService.getById(sending.getId());

        Assertions.assertEquals(sending.getVersion(), actualSending.getVersion());

        Assertions.assertEquals(
                newConfig.getTarget(),
                actualSending.getConfig().getTarget()
        );

        EmailPeriodicSending lastVersion = emailPeriodicSendingDAO.getByKey(sending.getKey())
                .orElseThrow();

        Assertions.assertEquals(sending.getId(), lastVersion.getId(), "New version has been created");
    }

    /**
     * Проверка отправки превью варианта
     */
    @Test
    public void testSendPreview() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();
        EmailSendingVariantConf variant = sending.getConfig().getVariants().get(0);

        BlockingQueue<YaSenderCampaign> campaignQueue = new ArrayBlockingQueue<>(1);
        yaSenderHelper.onSendOrCreatePromo(campaignQueue::add);

        TestEmailsGroup newGroup = new TestEmailsGroup()
                .setId(UUID.randomUUID().toString())
                .setName("Test group")
                .setItems(List.of(
                        new TestEmail("user_1@yandex.ru", true),
                        new TestEmail("user_2@yandex.ru", false)
                ));

        mockMvc.perform(
                post(
                        "/api/periodic_sendings/email/by_id/{sendingId}/variants/{variantId}/send_preview",
                        sending.getId(),
                        variant.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSerializer.writeObjectAsString(List.of(newGroup)))
        )
                .andDo(print())
                .andExpect(status().isOk());

        YaSenderCampaign yaSenderCampaign = campaignQueue.poll(10, TimeUnit.SECONDS);
        Assertions.assertNotNull(yaSenderCampaign, "Campaign has not been sent");
        Assertions.assertEquals("[preview] " + sending.getName(), yaSenderCampaign.getTitle());
        Assertions.assertEquals("[preview] " + variant.getSubject(), yaSenderCampaign.getSubject());

        String path = (String) yaSenderCampaign.getSegment().getParams().get("path");
        Assertions.assertNotNull(path, "Table path is not specified");

        List<YaSenderDataRow> rows = ytClient.read(YPath.simple(path), YaSenderDataRow.class);
        Assertions.assertEquals(1, rows.size());
        Assertions.assertEquals("user_1@yandex.ru", rows.get(0).getEmail());

        List<TestEmailsGroup> storedGroups = testEmailsDAO.getAll();
        Assertions.assertEquals(1, storedGroups.size());

        TestEmailsGroup storedGroup = storedGroups.get(0);
        Assertions.assertEquals(newGroup.getId(), storedGroup.getId());
        Assertions.assertEquals(newGroup.getName(), storedGroup.getName());
        Assertions.assertEquals(newGroup.getItems(), storedGroup.getItems());
    }

    /**
     * Ручка PUT /api/periodic_sendings/email/{id}/schedule обновляет расписание рассылки
     */
    @Test
    public void testEditSchedule() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();

        Schedule newSchedule = new Schedule()
                .setTime(LocalTime.of(18, 30))
                .setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));

        MvcResult result = mockMvc.perform(put(
                "/api/periodic_sendings/email/by_id/{sendingId}/schedule",
                sending.getId()
        )
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsString(newSchedule)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        Schedule returnedSchedule = jsonDeserializer.readObject(
                Schedule.class,
                result.getResponse().getContentAsString()
        );

        Assertions.assertEquals(newSchedule.getTime(), returnedSchedule.getTime());
        Assertions.assertEquals(newSchedule.getDaysOfWeek(), returnedSchedule.getDaysOfWeek());

        Schedule savedSchedule = sendingService.getById(sending.getId()).getSchedule();
        Assertions.assertEquals(newSchedule.getTime(), savedSchedule.getTime());
        Assertions.assertEquals(newSchedule.getDaysOfWeek(), savedSchedule.getDaysOfWeek());
    }

    /**
     * Ручка PUT /api/periodic_sendings/email/{id}/nf_settings обновляет настройки оповещений
     * <p>
     * При этом если рассылка не активирована джоба оповещения не регистрируется
     */
    @Test
    public void testEditNotificationSettings() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();

        Schedule schedule = new Schedule()
                .setTime(LocalTime.of(18, 30))
                .setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                .setActiveInterval(
                        new DateTimeInterval()
                                .setBegin(LocalDateTime.now())
                                .setEnd(LocalDateTime.now().plusDays(7))
                );

        sendingService.updateSchedule(sending.getId(), schedule);

        NotificationSettings settings = new NotificationSettings()
                .setDaysBeforeEnd(3)
                .setEmails(Set.of("manager@yandex-team.ru"));

        NotificationSettings resultSettings = requestNfSettingsUpdate(sending, settings);

        Assertions.assertEquals(settings.getDaysBeforeEnd(), resultSettings.getDaysBeforeEnd());
        Assertions.assertEquals(settings.getEmails(), resultSettings.getEmails());

        EmailPeriodicSending actualSending = sendingService.getById(sending.getId());
        Assertions.assertNull(actualSending.getEndNotificationJobName());

        assertNoScheduledJobs();

        NotificationSettings savedSettings = actualSending.getNotificationSettings();
        Assertions.assertNotNull(savedSettings);
        Assertions.assertEquals(settings.getDaysBeforeEnd(), savedSettings.getDaysBeforeEnd());
        Assertions.assertEquals(settings.getEmails(), savedSettings.getEmails());
    }

    /**
     * У активированной рассылки в выдаче ручки проставляется статус ENABLED
     * и запрещается редактирование
     */
    @Test
    public void testEnabledSendingHasActiveStatus() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();
        activateSending(sending, false);

        MvcResult result = mockMvc.perform(get("/api/periodic_sendings/email/by_id/{id}", sending.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        EmailPeriodicSendingDto dto = jsonDeserializer.readObject(
                EmailPeriodicSendingDto.class,
                result.getResponse().getContentAsString()
        );

        Assertions.assertEquals(EmailPeriodicSendingDto.Status.ACTIVE, dto.getStatus());
        Assertions.assertFalse(dto.isEditable());
    }

    /**
     * Ручка GET /api/periodic_sendings/email/{id}/events возвращает события относящиеся
     * к указанной рассылке
     */
    @Test
    public void testGetFirstPageOfSendingEvents() throws Exception {
        EmailPeriodicSending sending1 = emailPeriodicSendingTestHelper.prepareSending();
        EmailPeriodicSending sending2 = emailPeriodicSendingTestHelper.prepareSending();

        sendingService.addEvent(sending1.getKey(), new DefaultEvent(EventType.ENABLED));
        sendingService.addEvent(sending1.getKey(), new DefaultEvent(EventType.DISABLED));
        sendingService.addEvent(sending2.getKey(), new DefaultEvent(EventType.ENABLED));

        MvcResult result = mockMvc.perform(get(
                "/api/periodic_sendings/email/by_key/{key}/events",
                sending1.getKey()
        ))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        List<Event> events = jsonDeserializer.readObject(
                new TypeReference<>() {
                },
                result.getResponse().getContentAsString()
        );

        List<EventType> eventTypes = events.stream()
                .map(Event::getType)
                .collect(Collectors.toList());

        Assertions.assertEquals(List.of(EventType.DISABLED, EventType.ENABLED), eventTypes);
    }

    /**
     * При редактирования настроек оповещений для уже активированной рассылки в случае
     * если появилась необходимость оповещения оператора регистрируется соответствующая джоба
     */
    @Test
    public void testEditNfSettingOfActivatedSending() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();

        Schedule schedule = new Schedule()
                .setTime(LocalTime.of(18, 30))
                .setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                .setActiveInterval(
                        new DateTimeInterval()
                                .setBegin(LocalDateTime.now())
                                .setEnd(LocalDateTime.now().plusDays(7))
                );

        sendingService.updateSchedule(sending.getId(), schedule);

        activateSending(sending, false);

        NotificationSettings settings = new NotificationSettings()
                .setDaysBeforeEnd(1)
                .setEmails(Set.of("manager@yandex-team.ru"));

        requestNfSettingsUpdate(sending, settings);

        EmailPeriodicSending actualSending = sendingService.getById(sending.getId());

        String expectedJobName = nfJobName(sending);
        Assertions.assertEquals(expectedJobName, actualSending.getEndNotificationJobName());

        boolean notifyJobScheduled = scheduler.getTriggers().stream()
                .anyMatch(x -> expectedJobName.equals(x.getJobKey().getName()));

        Assertions.assertTrue(notifyJobScheduled);
    }

    /**
     * При редактировании настроек оповещения активированной рассылки с имеющейстя
     * джобой оповещения в случае если, согласно новым настройкам, джоба больше не
     * нужна она удаляется
     */
    @Test
    public void testDeleteNfJobIfItsNoLongerNeeded() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();

        Schedule schedule = new Schedule()
                .setTime(LocalTime.of(18, 30))
                .setDaysOfWeek(Set.of(DayOfWeek.MONDAY))
                .setActiveInterval(
                        new DateTimeInterval()
                                .setBegin(LocalDateTime.now())
                                .setEnd(LocalDateTime.now().plusDays(7))
                );

        sendingService.updateSchedule(sending.getId(), schedule);

        NotificationSettings settings = new NotificationSettings()
                .setDaysBeforeEnd(1)
                .setEmails(Set.of("manager@yandex-team.ru"));

        sendingService.updateNotificationSettings(sending.getId(), settings);

        activateSending(sending, true);

        NotificationSettings newSettings = new NotificationSettings()
                .setDaysBeforeEnd(1)
                .setEmails(Set.of());

        requestNfSettingsUpdate(sending, newSettings);

        EmailPeriodicSending actualSending = sendingService.getById(sending.getId());
        Assertions.assertNotNull(actualSending.getSendJobName());
        Assertions.assertNull(actualSending.getEndNotificationJobName());

        boolean notifyJobScheduled = scheduler.getTriggers().stream()
                .anyMatch(x -> nfJobName(sending).equals(x.getJobKey().getName()));

        Assertions.assertFalse(notifyJobScheduled);
    }

    /**
     * В случае если рассылка ниразу не была отправлена её можно удалить
     */
    @Test
    public void testDeleteNotSentSending() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();
        sendingService.addEvent(sending.getKey(), new DefaultEvent(EventType.ENABLED));

        UploadedImage image = prepareUploadedImage();
        sending.setBanners(List.of(image));
        sendingService.update(sending);

        requestDelete(sending)
                .andExpect(status().isOk());

        Assertions.assertFalse(sendingService.existsWithKey(sending.getKey()));
        Assertions.assertTrue(sendingService.getEvents(sending.getKey(), 0).isEmpty());

        Assertions.assertTrue(
                uploadedImageDAO.list(ImageOwnerType.EMAIL_PERIODIC_SENDING, sending.getId())
                        .isEmpty(),
                "Orphaned banners left");
    }

    /**
     * В случае если рассылка уже была отправлена её нельзя удалить
     */
    @Test
    public void test400OnDeleteSendingWithOneSentVersion() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();
        sendingService.setSentFlag(sending.getId());

        sending.setName("New Name");
        sendingService.update(sending);

        requestDelete(sending)
                .andExpect(status().isBadRequest());

        Assertions.assertTrue(sendingService.existsWithKey(sending.getKey()));
    }

    private void activateSending(EmailPeriodicSending sending, boolean withNfJob) {
        JobKey sendJobKey = scheduleJob("send_job_name");
        JobKey nfJobKey = withNfJob ? scheduleJob(nfJobName(sending)) : null;

        emailPeriodicSendingDAO.updateJobNames(
                sending.getId(),
                sendJobKey.getName(),
                nfJobKey == null ? null : nfJobKey.getName()
        );
    }

    @NotNull
    private JobKey scheduleJob(String jobName) {
        JobKey sendJobKey = new JobKey(jobName, "PERIODIC_SENDINGS");

        JobDetail job = mock(JobDetail.class);
        when(job.getKey()).thenReturn(sendJobKey);

        Trigger trigger = mock(Trigger.class);
        when(trigger.getJobKey()).thenReturn(sendJobKey);

        scheduler.scheduleJob(job, trigger);
        return sendJobKey;
    }

    @Nonnull
    private UploadedImage prepareUploadedImage() {
        UploadedImage image = new UploadedImage("Image.jpg", "https://yandex.ru/image");
        return uploadedImageDAO.create(ImageOwnerType.EMAIL_PERIODIC_SENDING, image);
    }

    @Nonnull
    private Campaign prepareCampaign() {
        Campaign campaign = new Campaign();
        campaign.setName("Test campaign");
        return campaignDAO.insert(campaign);
    }

    private PeriodicEntityUpdateResponse<EmailPeriodicSendingDto> requestUpdate(
            EmailPeriodicSending sending,
            EmailPeriodicSendingDto dto) throws Exception {
        MvcResult result = mockMvc.perform(put("/api/periodic_sendings/email/by_key/{key}", sending.getKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        PeriodicEntityUpdateResponse<EmailPeriodicSendingDto> response = jsonDeserializer.readObject(
                new TypeReference<>() {
                },
                result.getResponse().getContentAsString()
        );

        Assertions.assertEquals(UpdateResponse.UpdateResult.OK, response.getResult());

        return response;
    }

    private void requestEnable(EmailPeriodicSending sending) throws Exception {
        mockMvc.perform(post("/api/periodic_sendings/email/by_id/{id}/enable", sending.getId()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    private void requestDisable(EmailPeriodicSending sending) throws Exception {
        mockMvc.perform(post("/api/periodic_sendings/email/by_id/{id}/disable", sending.getId()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    private void assertNoScheduledJobs() {
        Assertions.assertTrue(scheduler.getJobs().isEmpty());
        Assertions.assertTrue(scheduler.getTriggers().isEmpty());
    }

    private NotificationSettings requestNfSettingsUpdate(EmailPeriodicSending sending, NotificationSettings settings) throws Exception {
        MvcResult result = mockMvc.perform(put(
                "/api/periodic_sendings/email/by_id/{sendingId}/nf_settings",
                sending.getId()
        )
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsString(settings)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        return jsonDeserializer.readObject(
                NotificationSettings.class,
                result.getResponse().getContentAsString()
        );
    }

    @NotNull
    private ResultActions requestDelete(EmailPeriodicSending sending) throws Exception {
        return mockMvc.perform(delete("/api/periodic_sendings/email/by_key/{key}", sending.getKey()))
                .andDo(print());
    }
}
