package ru.yandex.market.crm.campaign.http.controller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.domain.promo.entities.TestIdsGroup;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.EmailPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.EmailSendingFactInfo;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactStatus;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactType;
import ru.yandex.market.crm.campaign.domain.sending.SendingStage;
import ru.yandex.market.crm.campaign.domain.sending.TestEmail;
import ru.yandex.market.crm.campaign.domain.sending.TestEmailsGroup;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.dto.sending.EmailPlainSendingDto;
import ru.yandex.market.crm.campaign.dto.sending.ScheduleGenerationRequest;
import ru.yandex.market.crm.campaign.dto.sending.SendRequest;
import ru.yandex.market.crm.campaign.dto.sending.TestSendEmailPromoRequest;
import ru.yandex.market.crm.campaign.dto.sending.facts.EmailSendingFactInfoDto;
import ru.yandex.market.crm.campaign.services.security.Roles;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.EmailPlainSendingService;
import ru.yandex.market.crm.campaign.services.sending.EmailSendingDAO;
import ru.yandex.market.crm.campaign.services.sending.email.EmailSendingYtPaths;
import ru.yandex.market.crm.campaign.services.sending.facts.EmailSendingFactInfoDAO;
import ru.yandex.market.crm.campaign.services.sending.params.SendTaskParams;
import ru.yandex.market.crm.campaign.services.sql.TestEmailsDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.campaign.test.utils.BlockTemplateTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.core.domain.messages.PluggedTable;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.domain.yasender.YaSenderCampaign;
import ru.yandex.market.crm.core.domain.yasender.YaSenderSendingState;
import ru.yandex.market.crm.core.test.loggers.TestSentLogWriter;
import ru.yandex.market.crm.core.test.utils.SecurityUtils;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.core.test.utils.YaSenderHelper;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.domain.CompositeUserRole;
import ru.yandex.market.crm.http.security.BlackboxProfile;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.EmailState;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.yasender.SkippedYaSenderDataRow;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderData;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderDataRow;
import ru.yandex.market.crm.util.yt.CommonAttributes;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.mcrm.http.HttpRequest;
import ru.yandex.market.mcrm.http.ResponseBuilder;
import ru.yandex.market.tsum.event.EventId;
import ru.yandex.misc.thread.ThreadUtils;

import static java.util.Comparator.comparing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.utils.EmailSendingConfigUtils.variant;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.allUsers;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.subscriptionFilter;

/**
 * @author apershukov
 */
public class EmailSendingsControllerTest extends AbstractControllerMediumTest {

    private static class Variant {
        final String id;
        final int percent;
        final String subject;

        Variant(String id, int percent, String subject) {
            this.id = id;
            this.percent = percent;
            this.subject = subject;
        }

        public Variant(String id, int percent) {
            this(id, percent, "Subject");
        }
    }

    private class LogRecordsChecker {
        private final Map<String, Map<String, String>> recordsCache = new HashMap<>();

        LogRecordsChecker checkPresent(String email, String variant, boolean isControl, boolean isGlobalControl) {
            var record = getRecord(email, 60);
            assertNotNull(record, "Log record with email '" + email + "' is missing");
            assertField(record, "variantId", variant);
            assertField(record, "control", String.valueOf(isControl));
            assertField(record, "globalControl", String.valueOf(isGlobalControl));
            return this;
        }

        LogRecordsChecker checkAbsent(String email) {
            var record = getRecord(email, 1);
            assertNull(record, "Log record for email " + email + " found");
            return this;
        }

        @Nullable
        private Map<String, String> getRecord(String email, int timeout) {
            var record = recordsCache.get(email);
            if (record != null) {
                return record;
            }

            Map<String, String> nextRecord;
            while ((nextRecord = pollNext(timeout)) != null) {
                var nextEmail = nextRecord.get("email");
                assertNotNull(nextEmail, "Email field of log record is missing");
                recordsCache.put(nextEmail, nextRecord);

                if (nextEmail.equals(email)) {
                    return nextRecord;
                }
            }

            return null;
        }

        private Map<String, String> pollNext(int timeout) {
            try {
                return sentLogWriter.getEmailLog().poll(timeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final String CAMPAIGN_SLUG = "campaign_slug";

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private TestEmailsDAO testEmailsDao;

    @Inject
    private SegmentService segmentService;

    @Inject
    private EmailSendingTestHelper emailSendingTestHelper;

    @Inject
    private YaSenderHelper yaSenderHelper;

    @Inject
    private PluggableTablesTestHelper pluggableTablesTestHelper;

    @Inject
    private BlockTemplateTestHelper blockTemplateTestHelper;

    @Inject
    private YtClient ytClient;

    @Inject
    private EmailSendingDAO sendingDAO;

    @Inject
    private EmailSendingFactInfoDAO sendingFactInfoDAO;

    @Inject
    private YtFolders ytFolders;

    @Inject
    private EmailSendingFactInfoDAO sendingFactsDAO;

    @Inject
    private TestSentLogWriter sentLogWriter;

    @Inject
    private UsersRolesDao usersRolesDao;

    @Inject
    private EmailPlainSendingService sendingService;

    private EmailPlainSending sending;

    private static void assertEmail(TestEmail expected, TestEmail actual) {
        assertEquals(expected.getEmail(), actual.getEmail());
        assertFalse(actual.isSelected());
    }

    private static void assertGroup(TestEmailsGroup expected, TestEmailsGroup actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getItems().size(), actual.getItems().size());

        expected.getItems().sort(comparing(TestEmail::getEmail));
        actual.getItems().sort(comparing(TestEmail::getEmail));

        for (int i = 0; i < expected.getItems().size(); ++i) {
            assertEmail(expected.getItems().get(i), actual.getItems().get(i));
        }
    }

    @BeforeEach
    public void setUp() {
        var segment = segmentService.addSegment(segment(allUsers()));

        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock();
        sending = emailSendingTestHelper.prepareSending(segment, LinkingMode.NONE, creative);


        httpEnvironment.when(HttpRequest.post("https://tsum-api.market.yandex.net:4203/events/addEvent"))
                .then(
                        ResponseBuilder.newBuilder()
                                .body(jsonSerializer.writeObjectAsString(
                                        EventId.newBuilder()
                                                .setId(UUID.randomUUID().toString())
                                                .build()
                                ))
                                .build()
                );
    }

    /**
     * Сохранение единственной новой группы с новыми адресами
     */
    @Test
    public void testSaveSingleNewGroupOnSendTest() throws Exception {
        List<TestEmail> emails = Arrays.asList(
                new TestEmail("email1@yandex.ru"),
                new TestEmail("email2@yandex.ru", true)
        );

        TestEmailsGroup group = new TestEmailsGroup()
                .setId("id-1")
                .setName("New group")
                .setItems(emails);

        sendTest(group);

        List<TestEmailsGroup> savedGroups = testEmailsDao.getAll();
        assertEquals(1, savedGroups.size());

        TestEmailsGroup savedGroup = savedGroups.get(0);

        assertEquals(group.getId(), savedGroup.getId());
        assertEquals(group.getName(), savedGroup.getName());

        List<TestEmail> savedEmails = savedGroup.getItems();
        assertEquals(2, savedEmails.size());

        savedEmails.sort(comparing(TestEmail::getEmail));

        assertEmail(emails.get(0), savedEmails.get(0));
        assertEmail(emails.get(1), savedEmails.get(1));
    }

    @Test
    public void testDeleteAllGroups() throws Exception {
        List<TestEmail> emails = Collections.singletonList(
                new TestEmail("email@yandex.ru")
        );

        TestEmailsGroup group = new TestEmailsGroup()
                .setId("id-1")
                .setName("New group")
                .setItems(emails);

        testEmailsDao.saveGroups(Collections.singletonList(group));

        sendTest();

        List<TestEmailsGroup> savedGroups = testEmailsDao.getAll();
        assertTrue(savedGroups.isEmpty());
    }

    @Test
    public void testEditGroup() throws Exception {
        TestEmailsGroup group1 = new TestEmailsGroup()
                .setId("id-1")
                .setName("Group 1")
                .setItems(Collections.singletonList(
                        new TestEmail("email.1@yandex.ru")
                ));

        TestEmailsGroup group2 = new TestEmailsGroup()
                .setId("id-2")
                .setName("Group 2")
                .setItems(Arrays.asList(
                        new TestEmail("email.2@yandex.ru"),
                        new TestEmail("email.3@yandex.ru")
                ));

        testEmailsDao.saveGroups(Arrays.asList(group1, group2));

        group2.setName("Altered title");
        group2.setItems(Arrays.asList(
                group2.getItems().get(0),
                new TestEmail("email.4@yandex.ru")
        ));

        sendTest(group1, group2);

        List<TestEmailsGroup> savedGroups = testEmailsDao.getAll();
        assertEquals(2, savedGroups.size());

        savedGroups.sort(comparing(TestIdsGroup::getId));

        // Проверяем что первая группа не изменилась
        assertGroup(group1, savedGroups.get(0));
        assertGroup(group2, savedGroups.get(1));
    }

    @Test
    public void testSaveTwoSimilarEmailsInDifferentGroups() throws Exception {
        TestEmailsGroup group1 = new TestEmailsGroup()
                .setId("id-1")
                .setName("Group 1")
                .setItems(Collections.singletonList(
                        new TestEmail("email@yandex.ru")
                ));

        TestEmailsGroup group2 = new TestEmailsGroup()
                .setId("id-2")
                .setName("Group 2")
                .setItems(Collections.singletonList(
                        new TestEmail("email@yandex.ru")
                ));

        sendTest(group1, group2);

        List<TestEmailsGroup> savedGroups = testEmailsDao.getAll();
        assertEquals(2, savedGroups.size());

        savedGroups.sort(comparing(TestIdsGroup::getId));

        assertEquals(1, savedGroups.get(0).getItems().size());
        assertEmail(group1.getItems().get(0), savedGroups.get(0).getItems().get(0));

        assertEquals(1, savedGroups.get(1).getItems().size());
        assertEmail(group2.getItems().get(0), savedGroups.get(1).getItems().get(0));
    }

    @Test
    public void testFilterEmptyEmailsOnSave() throws Exception {
        TestEmailsGroup group = new TestEmailsGroup()
                .setId("id-1")
                .setName("Group 1")
                .setItems(Arrays.asList(
                        new TestEmail("email@yandex.ru"),
                        new TestEmail("")
                ));

        sendTest(group);

        List<TestEmailsGroup> savedGroups = testEmailsDao.getAll();
        assertEquals(1, savedGroups.size());

        List<TestEmail> emails = savedGroups.get(0).getItems();
        assertEquals(1, emails.size());
        assertEmail(group.getItems().get(0), emails.get(0));
    }

    @Test
    public void testGetGroups() throws Exception {
        TestEmailsGroup group1 = new TestEmailsGroup()
                .setId("id-1")
                .setName("Group 1")
                .setItems(Collections.singletonList(
                        new TestEmail("email.1@yandex.ru")
                ));

        TestEmailsGroup group2 = new TestEmailsGroup()
                .setId("id-2")
                .setName("Group 2")
                .setItems(Arrays.asList(
                        new TestEmail("email.2@yandex.ru"),
                        new TestEmail("email.3@yandex.ru")
                ));

        testEmailsDao.saveGroups(Arrays.asList(group1, group2));

        MvcResult result = mockMvc.perform(get("/api/sendings/email/test-emails"))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        List<TestEmailsGroup> groups = jsonDeserializer.readObject(
                new TypeReference<>() {
                },
                result.getResponse().getContentAsString()
        );

        assertEquals(2, groups.size());
        assertGroup(group1, groups.get(0));
        assertGroup(group2, groups.get(1));
    }

    @Test
    public void testDeleteEmailUsingInAnotherGroup() throws Exception {
        TestEmailsGroup group1 = new TestEmailsGroup()
                .setId("id-1")
                .setName("Group 1")
                .setItems(Collections.singletonList(
                        new TestEmail("email.1@yandex.ru")
                ));

        TestEmailsGroup group2 = new TestEmailsGroup()
                .setId("id-2")
                .setName("Group 2")
                .setItems(Arrays.asList(
                        new TestEmail("email.1@yandex.ru"),
                        new TestEmail("email.2@yandex.ru")
                ));

        testEmailsDao.saveGroups(Arrays.asList(group1, group2));

        group2.setItems(Collections.singletonList(
                new TestEmail("email.2@yandex.ru")
        ));

        sendTest(group1, group2);

        List<TestEmailsGroup> savedGroups = testEmailsDao.getAll();
        assertEquals(2, savedGroups.size());

        assertGroup(group1, savedGroups.get(0));
        assertGroup(group2, savedGroups.get(1));
    }

    /**
     * При выгрузке email-рассылки:
     * 1. Факт отправки изменяет свой статус вместе с кампанией в Рассыляторе
     * 2. Происходит снятие атрибута expiration_time с директории рассылки. Это нужно для того чтобы таблица
     *    не была удалена до окончания отправки на стороне Рассылятора.
     * 3. После завершения выгрузки на стороне Рассылятора на директорию рассылки устанавливается аттрибут expiration_time
     * 4. Информация о выгруженных письмах а также о локальном и глобальном контроле пишется в лог для отправки в Платформу
     */
    @Test
    void testSendSending() throws Exception {
        var variantA = "variant_a";
        var variantB = "variant_b";

        var sending = prepareSending(
                new Variant(variantA, 40),
                new Variant(variantB, 40)
        );

        sending.getConfig().setGlobalControlEnabled(true);
        emailSendingTestHelper.updateSending(sending);

        var sendingId = sending.getId();
        var email1 = "user1@yandex.ru";
        var email2 = "user2@yandex.ru";
        var email3 = "user3@yandex.ru";

        prepareSenderData(sendingId,
                dataRow(email1, variantA),
                dataRow(email2, variantB),
                dataRow(email3, variantA)
        );

        var email4 = "user4@yandex.ru";
        var email5 = "user5@yandex.ru";
        var email6 = "user6@yandex.ru";

        prepareSkippedSenderData(sendingId,
                skippedRow(email4, EmailState.CONTROL),
                skippedRow(email5, EmailState.CONTROL),
                skippedRow(email6, EmailState.ERROR)
        );

        var email7 = "user7@yandex.ru";
        prepareGlobalControlData(sendingId, email7);

        setSendingStatus(sending, SendingStage.GENERATE, LocalDateTime.now());

        prepareCampaign(YaSenderSendingState.SENDING);

        setExpirationTime(sendingId);

        var senderCampaign = sendEmailSending(sending);

        var path = (String) senderCampaign.getSegment().getParams().get("path");
        assertNotNull(path);
        assertEquals(getSenderDataPath(sendingId), YPath.simple(path));

        var facts = sendingFactsDAO.getSendingFacts(sending.getId());
        assertThat(facts, hasSize(1));

        var factId = facts.get(0).getId();

        assertSendingFact(
                factId,
                fact -> assertNotNull(fact.getId()),
                fact -> assertEquals(SendingFactType.FINAL, fact.getType()),
                fact -> assertEquals(sending.getId(), fact.getSendingId()),
                fact -> assertNull(fact.getErrorMessage()),
                fact -> assertNotNull(fact.getStartUploadTime())
        );

        waitForStatus(factId, SendingFactStatus.SENDING_IN_PROGRESS);

        assertSendingFact(
                factId,
                fact -> assertNotNull(fact.getUploadTime()),
                fact -> assertNotNull(fact.getStartSendingTime())
        );

        assertFalse(expirationTimeIsSet(sendingId), "Expiration time is set on sending directory");

        prepareCampaign(YaSenderSendingState.SENT);
        waitForStatus(factId, SendingFactStatus.FINISHED);

        assertSendingFact(
                factId,
                fact -> assertNotNull(fact.getSendingTime())
        );

        new LogRecordsChecker()
                .checkPresent(email1, variantA, false, false)
                .checkPresent(email2, variantB, false, false)
                .checkPresent(email3, variantA, false, false)
                .checkPresent(email4, null, true, false)
                .checkPresent(email5, null, true, false)
                .checkAbsent(email6)
                .checkPresent(email7, null, true, true);

        assertTrue(expirationTimeIsSet(sendingId), "Expiration time is not set on sending directory");
    }

    /**
     * В случае если у рассылки несколько вариантов в качестве темы письма в рассылатор будет
     * отдан шаблон, который резолвит значение темы в зависимости от варианта
     */
    @Test
    public void testSubjectForSendingWithMultipleVariants() throws Exception {
        var variantA = "variant_a";
        var variantB = "variant_b";

        var sending = prepareSending(
                new Variant(variantA, 50, "Subject A"),
                new Variant(variantB, 50, "Subject B")
        );

        prepareSenderData(sending.getId(),
                dataRow("user1@yandex.ru", variantA),
                dataRow("user2@yandex.ru", variantB),
                dataRow("user3@yadnex.ru", variantA)
        );

        YaSenderCampaign campaign = sendEmailSending(sending);

        String expectedSubject = "{% if data.variant == \"variant_a\" %}Subject A" +
                "{% elif data.variant == \"variant_b\" %}Subject B{% endif %}";

        assertEquals(expectedSubject, campaign.getSubject());
    }

    /**
     * В случае если у рассылки всего один вариант тема письма из него
     * напрямую подставляется в тему письма в кампании рассылятора
     */
    @Test
    public void testSubjectForSendingWithSingleVariant() throws Exception {
        var sending = prepareSending();

        prepareSenderData(sending.getId(),
                dataRow("user1@yandex.ru", "variant_a"),
                dataRow("user2@yandex.ru", "variant_a"),
                dataRow("user3@yadnex.ru", "variant_a")
        );

        YaSenderCampaign campaign = sendEmailSending(sending);

        assertEquals("Subject", campaign.getSubject());
    }

    /**
     * В случае если к рассылке подключена таблица, при тестовой отправке будет отправлено
     * письмо с замоканными значениями переменных
     */
    @Test
    public void testSendPreviewWithUserVars() throws Exception {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable(UidType.PUID);

        Segment segment = segmentService.addSegment(segment(
                passportGender("m")
        ));

        String messageTemplateId = blockTemplateTestHelper.prepareMessageTemplate();
        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock(
                "{{data.vars.lastname}}. {{data.u_vars.table.saved_money}}"
        );
        EmailSendingVariantConf variant = variant("variant_a", 100, messageTemplateId, creative);

        EmailSendingConf config = new EmailSendingConf();
        config.setSubscriptionType(2L);
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setVariants(Collections.singletonList(variant));
        config.setPluggedTables(Collections.singletonList(new PluggedTable(pluggableTable.getId(), "table")));

        EmailPlainSending sending = emailSendingTestHelper.prepareSending(config);

        BlockingQueue<YaSenderCampaign> campaignQueue = new ArrayBlockingQueue<>(1);
        yaSenderHelper.onSendOrCreatePromo(campaignQueue::put);

        TestEmailsGroup group = new TestEmailsGroup()
                .setId("id-1")
                .setName("Group 1")
                .setItems(List.of(new TestEmail("email.1@yandex.ru", true)));

        mockMvc.perform(post(
                "/api/sendings/email/{id}/variants/{variantId}/send-preview",
                sending.getId(),
                variant.getId()
        )
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(List.of(group))))
                .andDo(print())
                .andExpect(status().isOk());

        YaSenderCampaign campaign = campaignQueue.poll(1, TimeUnit.MINUTES);
        assertNotNull(campaign, "No campaign was received");

        String path = (String) campaign.getSegment().getParams().get("path");
        assertNotNull(path, "Table path was not specified");

        List<YaSenderDataRow> rows = ytClient.read(YPath.simple(path), YaSenderDataRow.class);
        assertEquals(1, rows.size());

        YaSenderData data = jsonDeserializer.readObject(YaSenderData.class, rows.get(0).getJsonData());

        Map<String, YTreeNode> vars = data.getVars();
        assertNotNull(vars);
        assertTrue(vars.containsKey("firstname"));

        Map<String, Map<String, YTreeNode>> expected = Map.of(
                "table", Map.of("saved_money", YTree.stringNode("saved_money"))
        );

        assertEquals(expected, data.getUVars());
    }

    /**
     * Если рассылка была собрана более суток назад, в выдаче ручки GET /api/sendings/email/{}
     * устанавливается флаг sendingDataIsStale
     */
    @Test
    void testSetStaleFlagForSendingGeneratedMoreThanADayAgo() throws Exception {
        var generationTime = LocalDateTime.now().minusDays(2);
        makeGenerated(generationTime);

        var dto = requestSending(sending.getId());

        assertTrue(dto.sendingDataIsStale());
    }

    /**
     * Если рассылка была собрана менее суток назад флаг sendingDataIsStale в выдаче ручки
     * GET /api/sendings/email/{} не устанавливается
     */
    @Test
    void testDoNotSetStaleFlagForSendingGeneratedLessThanADayAgo() throws Exception {
        var generationTime = LocalDateTime.now().minusHours(8);
        makeGenerated(generationTime);

        var dto = requestSending(sending.getId());

        assertFalse(dto.sendingDataIsStale());
    }

    /**
     * В случае если рассылка была собрана более суток назад в факте её боевой отправки не возвращается
     * ссылка на таблицу с данными в YT. Это делается для того чтобы не давать пользователю ссылку, которая
     * почти наверняка будет указывать на таблицу которая больше не существует.
     */
    @Test
    void testDoNotReturnDataTableUrlForSendingBuiltMoreThanADayAgo() throws Exception {
        var generationTime = LocalDateTime.now().minusDays(2);

        sending.setGenerationTime(generationTime);
        sending.setStageAndStatus(SendingStage.UPLOAD, StageStatus.FINISHED);

        var sendingId = sending.getId();
        sendingDAO.updateSendingStates(sendingId, sending);

        prepareSendingFact(sendingId, SendingFactType.FINAL, LocalDateTime.now().minusDays(1));

        var dto = requestSending(sendingId);

        var facts = dto.getSendingFacts();
        assertThat(facts, hasSize(1));

        var fact = (EmailSendingFactInfoDto) facts.get(0);
        assertNull(fact.getSenderdataUrl());
    }

    /**
     * Если preview-рассылка ушла более суток назад в факте отправки не возвращается сслыка
     * на таблицу на yt.
     */
    @Test
    void testDoNotReturnPreviewSendingDataTableUrlIfUploadingWasMoreThanADayAgo() throws Exception {
        var sendingId = sending.getId();

        prepareSendingFact(sendingId, SendingFactType.PREVIEW, LocalDateTime.now().minusDays(2));

        var dto = requestSending(sendingId);

        var facts = dto.getSendingFacts();
        assertThat(facts, hasSize(1));

        var fact = (EmailSendingFactInfoDto) facts.get(0);
        assertNull(fact.getSenderdataUrl());
    }

    /**
     * Если сборка рассылки была завершена более суток назад, в выдаче ручки GET /api/sendings/email/{}
     * устанавливается флаг sendingDataIsStale
     */
    @Test
    void testStaleDataFlagIsSetForSendingWhichGenerationCompletedMoreThanADayAgo() throws Exception {
        setSendingStatus(SendingStage.GENERATE, LocalDateTime.now().minusDays(2));
        var dto = requestSending(sending.getId());
        assertTrue(dto.sendingDataIsStale());
    }

    /**
     * Если сборка рассылки была завершена менее суток назад, флаг sendingDataIsStale в выдаче ручки
     * GET /api/sendings/email/{} не устанавливается
     */
    @Test
    void testStaleDataFlagIsSetForSendingWhichGenerationCompletedLessThanADayAgo() throws Exception {
        setSendingStatus(SendingStage.GENERATE, LocalDateTime.now().minusHours(6));
        var dto = requestSending(sending.getId());
        assertFalse(dto.sendingDataIsStale());
    }

    /**
     * Если рассылка была выгружена, флаг sendingDataIsStale в выдаче ручки GET /api/sendings/email/{}
     * не устанавливается вне зависимости от того когда была завешена сборка
     */
    @Test
    void testStaleDataFlagIsNotSetForSendingWhichHasBeenUploaded() throws Exception {
        setSendingStatus(SendingStage.UPLOAD, LocalDateTime.now().minusDays(15));
        var dto = requestSending(sending.getId());
        assertFalse(dto.sendingDataIsStale());
    }

    /**
     * При вызове ручки POST /api/sending/email/{}/schedule_generation
     * 1. Рассылка переводится в состояние GENERATE - SCHEDULED
     * 2. Время переданное в качестве startTime устанавливается в качестве значения поля scheduled_time
     */
    @Test
    void testScheduleGenerationWithoutSending() throws Exception {
        var sending = prepareNewSending();

        var scheduleTime = LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS);

        var dto = requestGenerationScheduling(sending, scheduleTime, false);

        assertAll(
                () -> assertEquals(scheduleTime, dto.getScheduleTime()),
                () -> assertEquals(SendingStage.GENERATE, dto.getStage()),
                () -> assertEquals(StageStatus.SCHEDULED, dto.getStageStatus()),
                () -> assertFalse(dto.isSendAfterGeneration())
        );
    }

    /**
     * При вызове ручки POST /api/sending/email/{}/schedule_generation с параметром sendAfterGeneration
     * при планировании генерации в рассылке проставляется одноименный параметр.
     */
    @Test
    void testScheduleGenerationWithImmediateSendings() throws Exception {
        var sending = prepareNewSending();

        var scheduleTime = LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS);

        var dto = requestGenerationScheduling(sending, scheduleTime, true);

        assertAll(
                () -> assertEquals(scheduleTime, dto.getScheduleTime()),
                () -> assertEquals(SendingStage.GENERATE, dto.getStage()),
                () -> assertEquals(StageStatus.SCHEDULED, dto.getStageStatus()),
                () -> assertTrue(dto.isSendAfterGeneration())
        );
    }

    /**
     * При вызове ручки POST /api/sending/email/{}/schedule_generation без параметра startTime
     * возвращается код 400.
     */
    @Test
    void test400IfGenerationSchedulingIsRequestedWithoutStartTime() throws Exception {
        var sending = prepareNewSending();

        makeScheduleRequest(sending, null, true)
                .andExpect(status().isBadRequest());
    }

    /**
     * При вызове ручки POST /api/sending/email/{}/schedule_generation пользователем с ролью AGENT
     * и sendAfterGeneration=true
     * 1. Рассылка переводится в состояние CONFIRM
     * 2. Время переданное в качестве startTime устанавливается в качестве значения поля scheduled_time
     */
    @Test
    void testScheduleGenerationWithSendingByAgentAuthor() throws Exception {
        var agent = prepareProfile("agent", Roles.AGENT);
        SecurityUtils.setAuthentication(agent);
        var sending = prepareNewSending();

        var scheduleTime = LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS);

        var dto = requestGenerationScheduling(sending, scheduleTime, true);

        assertAll(
                () -> assertEquals(scheduleTime, dto.getScheduleTime()),
                () -> assertEquals(SendingStage.CONFIRM, dto.getStage()),
                () -> assertNull(dto.getStageStatus()),
                () -> assertTrue(dto.isSendAfterGeneration())
        );
    }

    /**
     * При вызове ручки POST /api/sending/email/{}/schedule_generation пользователем с ролью AGENT
     * и sendAfterGeneration=false
     * 1. Рассылка переводится в состояние GENERATE (FINISHED)
     * 2. Время переданное в качестве startTime устанавливается в качестве значения поля scheduled_time
     */
    @Test
    void testScheduleGenerationWithoutSendingByAgentAuthor() throws Exception {
        var agent = prepareProfile("agent", Roles.AGENT);
        SecurityUtils.setAuthentication(agent);
        var sending = prepareNewSending();

        var scheduleTime = LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS);

        var dto = requestGenerationScheduling(sending, scheduleTime, false);

        assertAll(
                () -> assertEquals(scheduleTime, dto.getScheduleTime()),
                () -> assertEquals(SendingStage.GENERATE, dto.getStage()),
                () -> assertEquals(StageStatus.SCHEDULED, dto.getStageStatus()),
                () -> assertFalse(dto.isSendAfterGeneration())
        );
    }

    /**
     * При вызове ручки POST /api/sending/email/{}/schedule_generation/confirm пользователем с ролью AGENT,
     * являющимся автором рассылки, в ответ возвращается 403
     */
    @Test
    void test403OnConfirmSchedulingByAgentAuthor() throws Exception {
        var agent = prepareProfile("agent", Roles.AGENT);
        SecurityUtils.setAuthentication(agent);
        var sending = prepareNewSending();

        var sendingId = sending.getId();
        sendingDAO.updateSchedulingFields(sendingId, LocalDateTime.now().plusDays(7), SendingStage.CONFIRM,
                null,false);

        makeConfirmSchedulingRequest(sending)
                .andExpect(status().isForbidden());
    }

    /**
     * Пользователь с ролью "Агент" может формировать рассылки
     */
    @Test
    public void testUserWithAgentRoleIsAllowedToGenerateSendings() throws Exception {
        var agent = prepareProfile("agent", Roles.AGENT);
        SecurityUtils.setAuthentication(agent);
        var sending = prepareNewSending();

        SecurityUtils.setAuthentication(agent);

        mockMvc.perform(post("/api/sendings/email/{id}/generate", sending.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void testConfirmScheduling() throws Exception {
        var sending = prepareNewSending();

        var sendingId = sending.getId();
        var scheduleTime = LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS);
        sendingDAO.updateSchedulingFields(sendingId, scheduleTime, SendingStage.CONFIRM, null,false);

        var dto = requestConfirmScheduling(sending);

        assertAll(
                () -> assertEquals(SendingStage.GENERATE, dto.getStage()),
                () -> assertEquals(StageStatus.SCHEDULED, dto.getStageStatus()),
                () -> assertEquals(scheduleTime, dto.getScheduleTime())
        );
    }

    /**
     * При вызове ручки POST /api/sending/email/{}/cancel-scheduled для рассылки с
     * запланированной отправкой происходит её перевод в статус GENERATE-FINISHED.
     * Поле scheduleTime при этом сбрасывается.
     */
    @Test
    void testCancelScheduledSending() throws Exception {
        var sending = prepareSending();
        var sendingId = sending.getId();
        sendingService.scheduleSending(sendingId, LocalDateTime.now().plusDays(7), SendTaskParams.EMPTY);

        var dto = requestUnschedule(sendingId);

        assertAll(
                () -> assertEquals(SendingStage.GENERATE, dto.getStage()),
                () -> assertEquals(StageStatus.FINISHED, dto.getStageStatus()),
                () -> assertNull(dto.getScheduleTime())
        );
    }

    /**
     * При вызове ручки POST /api/sending/email/{}/cancel-scheduled для рассылки с
     * запланированной сборкой происходит её перевод в статус NEW-NULL.
     * Поле scheduleTime при этом сбрасывается.
     */
    @Test
    void testCancelScheduledGeneration() throws Exception {
        var sending = prepareNewSending();
        var sendingId = sending.getId();
        sendingDAO.updateSchedulingFields(sendingId, LocalDateTime.now().plusDays(7), SendingStage.GENERATE,
                StageStatus.SCHEDULED,true);

        var dto = requestUnschedule(sendingId);

        assertAll(
                () -> assertEquals(SendingStage.NEW, dto.getStage()),
                () -> assertNull(dto.getStageStatus()),
                () -> assertNull(dto.getScheduleTime())
        );
    }

    private BlackboxProfile prepareProfile(String login, String role) {
        var profile = SecurityUtils.profile(login);
        usersRolesDao.addRole(profile.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, role));
        return profile;
    }

    private void setSendingStatus(SendingStage stage, LocalDateTime generationTime) {
        setSendingStatus(sending, stage, generationTime);
    }

    private void setSendingStatus(EmailPlainSending sending, SendingStage stage, LocalDateTime generationTime) {
        sending.setStageAndStatus(stage, StageStatus.FINISHED);
        sending.setGenerationTime(generationTime);
        sendingDAO.updateSendingStates(sending.getId(), sending);
    }

    @Nonnull
    private YaSenderCampaign sendEmailSending(EmailPlainSending sending) throws Exception {
        BlockingQueue<YaSenderCampaign> campaignQueue = new ArrayBlockingQueue<>(1);
        yaSenderHelper.onSendOrCreatePromo(CAMPAIGN_SLUG, campaignQueue::put);

        mockMvc.perform(post("/api/sendings/email/{id}/send", sending.getId())
                .contentType("application/json")
                .content(jsonSerializer.writeObjectAsString(new SendRequest())))
                .andDo(print())
                .andExpect(status().isOk());

        YaSenderCampaign campaign = campaignQueue.poll(1, TimeUnit.MINUTES);
        assertNotNull(campaign, "No campaign was received");
        return campaign;
    }

    private void sendTest(TestEmailsGroup... groups) throws Exception {
        mockMvc.perform(post("/api/sendings/email/{id}/send-test", sending.getId())
                .param("version", "3")
                .characterEncoding("utf-8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(
                        new TestSendEmailPromoRequest()
                                .setEmailGroups(Arrays.asList(groups))
                )))
                .andExpect(status().isOk())
                .andDo(print());
    }

    private EmailPlainSendingDto requestSending(String sendingId) throws Exception {
        var response = mockMvc.perform(get("/api/sendings/email/{id}", sendingId))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse();

        return jsonDeserializer.readObject(EmailPlainSendingDto.class, response.getContentAsString());
    }

    private void makeGenerated(LocalDateTime generationTime) {
        sending.setGenerationTime(generationTime);
        sending.setStageAndStatus(SendingStage.GENERATE, StageStatus.FINISHED);
        sendingDAO.updateSendingStates(sending.getId(), sending);
    }

    private void prepareSendingFact(String sendingId, SendingFactType type, LocalDateTime uploadTime) {
        var sendingFact = new EmailSendingFactInfo();
        sendingFact.setId(sendingId);
        sendingFact.setSendingId(sendingId);
        sendingFact.setUploadTime(uploadTime);
        sendingFact.setType(type);
        sendingFact.setStatus(SendingFactStatus.FINISHED);

        sendingFactInfoDAO.save(sendingFact);
    }

    private void prepareSenderData(String sendingId, YaSenderDataRow... rows) {
        for (var row : rows) {
            row.setJsonData(jsonSerializer.writeObjectAsString(row.getData()));
        }

        var senderDataPath = getSenderDataPath(sendingId);
        ytClient.write(senderDataPath, YaSenderDataRow.class, List.of(rows));
    }

    private YaSenderDataRow dataRow(String email, String variantId) {
        var data = new YaSenderData()
                .setVariantId(variantId);

        return new YaSenderDataRow(email, data, null);
    }

    private EmailSendingYtPaths ytPaths(String sendingId) {
        return new EmailSendingYtPaths(ytFolders.getCampaignPath(sendingId));
    }

    private YPath getSenderDataPath(String sendingId) {
        return ytPaths(sendingId).getYaSenderDataTable();
    }

    private void prepareCampaign(YaSenderSendingState state) {
        var campaign = new YaSenderCampaign();
        campaign.setSlug(CAMPAIGN_SLUG);
        campaign.setState(state);

        yaSenderHelper.prepareCampaign(campaign);
    }

    private EmailPlainSending prepareSending(Variant... variants) {
        var segment = segment(
                subscriptionFilter(SubscriptionTypes.ADVERTISING)
        );

        String templateId = blockTemplateTestHelper.prepareMessageTemplate();
        BannerBlockConf bannerBlock = blockTemplateTestHelper.prepareCreativeBlock();

        var variantConfigs = Stream.of(variants)
                .map(variant -> variant(variant.id, variant.percent, templateId, variant.subject, bannerBlock))
                .toArray(EmailSendingVariantConf[]::new);

        return emailSendingTestHelper.prepareSending(segment, LinkingMode.ALL, variantConfigs);
    }

    private void prepareSkippedSenderData(String sendingId, SkippedYaSenderDataRow... rows) {
        var path = ytPaths(sendingId).getSkippedYaSenderDataTable();
        ytClient.write(path, SkippedYaSenderDataRow.class, List.of(rows));
    }

    private void prepareGlobalControlData(String sendingId, String email) {
        var path = ytPaths(sendingId).getGlobalControlTable();

        var row = YTree.mapBuilder()
                .key("id_value").value(email)
                .buildMap();

        ytClient.write(path, YTableEntryTypes.YSON, List.of(row));
    }

    private static SkippedYaSenderDataRow skippedRow(String email, EmailState state) {
        var row = new SkippedYaSenderDataRow();
        row.setEmail(email);
        row.setState(state);
        return row;
    }

    private static void assertField(Map<String, String> record, @Nonnull String field, String expectedValue) {
        assertEquals(expectedValue, record.get(field), "Wrong field value: " + field);
    }

    private void waitForStatus(String factId, SendingFactStatus status) {
        var startTime = System.currentTimeMillis();
        SendingFactStatus currentStatus = null;
        while (System.currentTimeMillis() - startTime < 60_000) {
            var fact = sendingFactInfoDAO.getById(factId);
            currentStatus = fact.getStatus();
            if (currentStatus == status) {
                return;
            }
            ThreadUtils.sleep(1, TimeUnit.SECONDS);
        }

        assertEquals(status, currentStatus, "Wrong sending fact status");
    }

    private void assertSendingFact(String factId, Consumer<EmailSendingFactInfo>... checkers) {
        var fact = sendingFactInfoDAO.getById(factId);

        var executables = Stream.of(checkers)
                .map(x -> (Executable) () -> x.accept(fact))
                .toArray(Executable[]::new);

        assertAll(executables);
    }

    private void setExpirationTime(String sendingId) {
        var dirPath = ytFolders.getCampaignPath(sendingId);
        ytClient.setExpirationTime(dirPath, LocalDateTime.now().plusDays(1));
    }

    private boolean expirationTimeIsSet(String sendingId) {
        var dirPath = ytFolders.getCampaignPath(sendingId);
        return ytClient.getAttribute(dirPath, CommonAttributes.EXPIRATION_TIME)
                .filter(YTreeNode::isStringNode)
                .isPresent();
    }

    @Nonnull
    private EmailPlainSending prepareSending() {
        return prepareSending(
                new Variant("variant_a", 85, "Subject")
        );
    }

    @Nonnull
    private EmailPlainSending prepareNewSending() {
        var sending = prepareSending();

        sending.setStageAndStatus(SendingStage.NEW, null);
        sendingDAO.updateSendingStates(sending.getId(), sending);
        return sending;
    }

    private ResultActions makeScheduleRequest(EmailPlainSending sending,
                                              LocalDateTime scheduleTime,
                                              boolean sendAfterGeneration) throws Exception {
        var requestBody = new ScheduleGenerationRequest();
        requestBody.setStartTime(scheduleTime);
        requestBody.setSendAfterGeneration(sendAfterGeneration);

        var request = post("/api/sendings/email/{id}/schedule_generation", sending.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsString(requestBody));

        return mockMvc.perform(request)
                .andDo(print());
    }

    private ResultActions makeConfirmSchedulingRequest(EmailPlainSending sending) throws Exception {
        var request = post(
                "/api/sendings/email/{id}/schedule_generation/confirm", sending.getId()
        );

        return mockMvc.perform(request)
                .andDo(print());
    }

    @Nonnull
    private EmailPlainSendingDto requestConfirmScheduling(EmailPlainSending sending) throws Exception {
        var response = makeConfirmSchedulingRequest(sending)
                .andExpect(status().isOk())
                .andReturn().getResponse();

        return jsonDeserializer.readObject(EmailPlainSendingDto.class, response.getContentAsString());
    }

    @Nonnull
    private EmailPlainSendingDto requestGenerationScheduling(EmailPlainSending sending,
                                                             LocalDateTime scheduleTime,
                                                             boolean sendAfterGeneration) throws Exception {
        var response = makeScheduleRequest(sending, scheduleTime, sendAfterGeneration)
                .andExpect(status().isOk())
                .andReturn().getResponse();

        return jsonDeserializer.readObject(EmailPlainSendingDto.class, response.getContentAsString());
    }

    private EmailPlainSendingDto requestUnschedule(String sendingId) throws Exception {
        var response = mockMvc.perform(post("/api/sendings/email/{id}/cancel-scheduled", sendingId))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse();

        return jsonDeserializer.readObject(EmailPlainSendingDto.class, response.getContentAsString());
    }
}
