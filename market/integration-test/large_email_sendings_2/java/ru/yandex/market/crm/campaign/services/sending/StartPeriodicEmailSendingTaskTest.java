package ru.yandex.market.crm.campaign.services.sending;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.periodic.Event;
import ru.yandex.market.crm.campaign.domain.periodic.EventType;
import ru.yandex.market.crm.campaign.domain.sending.EmailPeriodicSending;
import ru.yandex.market.crm.campaign.domain.sending.EmailSendingGenerationResult;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.sending.periodic.GeneratedEvent;
import ru.yandex.market.crm.campaign.domain.sending.periodic.UploadedEvent;
import ru.yandex.market.crm.campaign.domain.sending.periodic.YaSenderExternalInfo;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.periodic.EmailPeriodicSendingService;
import ru.yandex.market.crm.campaign.services.sending.periodic.StartPeriodicEmailSendingTask;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.BannedPromocodesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.BlockTemplateTestHelper;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.ClusterTasksTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailPeriodicSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PeriodicEntitiesTestUtils;
import ru.yandex.market.crm.campaign.test.utils.TaskFailedException;
import ru.yandex.market.crm.core.domain.yasender.YaSenderCampaign;
import ru.yandex.market.crm.core.domain.yasender.YaSenderSendingState;
import ru.yandex.market.crm.core.services.control.LocalControlSaltModifier;
import ru.yandex.market.crm.core.test.loggers.TestSentLogWriter;
import ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper;
import ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YaSenderHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderData;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderDataRow;
import ru.yandex.market.crm.platform.commons.SendingType;
import ru.yandex.market.crm.util.yt.CommonAttributes;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.yandex.market.crm.campaign.test.utils.BannedPromocodesTestHelper.promocodeRecord;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportEmail;
import static ru.yandex.market.crm.campaign.test.utils.EmailPeriodicSendingTestHelper.DEFAULT_SUBJECT;
import static ru.yandex.market.crm.campaign.test.utils.EmailPeriodicSendingTestHelper.DEFAULT_VARIANT;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.subscriptionFilter;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.cryptaMatchingEntry;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.uniformSplitEntry;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.ADVERTISING;
import static ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper.subscription;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.EMAIL_MD5;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.passportProfile;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.puidToEmailFact;
import static ru.yandex.market.crm.util.Dates.MOSCOW_ZONE;

/**
 * @author apershukov
 */
public class StartPeriodicEmailSendingTaskTest extends AbstractServiceLargeTest {

    private static final String EMAIL_1 = "email.1@yandex.ru";
    private static final String EMAIL_2 = "email.2@yandex.ru";
    private static final String EMAIL_3 = "email.3@yandex.ru";
    private static final String EMAIL_4 = "email.4@yandex.ru";

    private static final long PUID_1 = 111;
    private static final long PUID_2 = 222;
    private static final long PUID_3 = 333;

    private static final String CRYPTA_ID_1 = "crypta-id-1";
    private static final String CRYPTA_ID_2 = "crypta-id-2";
    private static final String CRYPTA_ID_3 = "crypta-id-3";

    private static final String CAMPAIGN_SLUG = "campaign_slug";

    private static final String INVALID_TEMPLATE_BODY_PATTERN =
            """
                    <%1$s>
                    <a href="%2$s">link</a>
                    <%3$s src="">
                    </%1$s>
                    """;

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private SubscriptionsTestHelper subscriptionsTestHelper;

    @Inject
    private EmailPeriodicSendingTestHelper sendingTestHelper;

    @Inject
    private EmailPeriodicSendingService sendingService;

    @Inject
    private YaSenderHelper yaSenderHelper;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private YtFolders ytFolders;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private ClusterTasksTestHelper clusterTasksTestHelper;

    @Inject
    private StartPeriodicEmailSendingTask task;

    @Inject
    private GlobalSplitsTestHelper globalSplitsTestHelper;

    @Inject
    private TestSentLogWriter testSentLogWriter;

    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    @Inject
    private SegmentService segmentService;

    @Inject
    private BannedPromocodesTestHelper bannedPromocodesTestHelper;

    @Inject
    private BlockTemplateTestHelper blockTemplateTestHelper;

    @Value("${external.yasender.account.slug}")
    private String senderAccount;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareEmailOwnershipFactsTable();
        ytSchemaTestHelper.prepareSubscriptionFactsTable();
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.EMAIL_MD5, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareGlobalControlSplitsTable();
        ytSchemaTestHelper.prepareChytPassportEmailsTable();
        ytSchemaTestHelper.preparePassportProfilesTable();
        ytSchemaTestHelper.prepareEmailsGeoInfo();
        ytSchemaTestHelper.prepareUserTables();
    }

    @AfterEach
    public void tearDown() {
        testSentLogWriter.tearDown();
        LocalControlSaltModifier.setClock(Clock.system(MOSCOW_ZONE));
    }

    private static Stream<Arguments> dataForInvalidTemplate() {
        return Stream.of(
                arguments(
                        "testGenerateWithTemplateWithoutBody",
                        String.format(INVALID_TEMPLATE_BODY_PATTERN, "div", "${ctx.unsubscribe()}", "img"),
                        "отсутствует непустой тег body"
                ),
                arguments(
                        "testGenerateWithTemplateWithoutUnsubscribeLink",
                        String.format(INVALID_TEMPLATE_BODY_PATTERN, "body", "yandex.ru", "img"),
                        "отсутствует ссылка на отписку: ${ctx.unsubscribe()}"
                ),
                arguments(
                        "testGenerateWithTemplateWithoutImg",
                        String.format(INVALID_TEMPLATE_BODY_PATTERN, "body", "  ${ctx.unsubscribe()}  ", "p"),
                        "отсутствует хотя бы одно изображение"
                )
        );
    }

    @MethodSource("dataForInvalidTemplate")
    @ParameterizedTest(name = "{0}")
    public void testGenerateWithInvalidTemplate(String name, String templateBody, String errorMessage) throws Exception {
        EmailPeriodicSending sending = sendingTestHelper.prepareSending();
        EmailSendingVariantConf variantConf = sending.getConfig().getVariants().get(0);
        String templateId = variantConf.getTemplate();
        blockTemplateTestHelper.updateBlockTemplate(templateId, templateBody);

        var ex = Assertions.assertThrows(TaskFailedException.class, () -> generateAndSend(sending));
        String statusMessage = ex.getTaskInstanceInfo().getStatusMessage();
        String message = String.format(
                "Periodic email sending=%s, в Header/footer (%s) присутствуют ошибки:%n%s",
                sending.getId(), variantConf.getId(), errorMessage
        );
        assertTrue(
                statusMessage.contains(message),
                "Unexpected status message: " + statusMessage
        );
    }

    /**
     * Тестирование базового кейса отправки периодической рассылки
     * <p>
     * При вызове таски создается кластерная таска, которая собирает рассылку с учетом глобального контроля
     * и выгружает её как промо
     */
    @Test
    public void testGenerateAndSendSending() {
        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m"),
                passportProfile(PUID_2, "m"),
                passportProfile(PUID_3, "m")
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1),
                subscription(EMAIL_2, PUID_2),
                subscription(EMAIL_3, PUID_3)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1),
                chytPassportEmail(PUID_2, EMAIL_2),
                chytPassportEmail(PUID_3, EMAIL_3)
        );

        globalSplitsTestHelper.prepareGlobalControlSplits(
                uniformSplitEntry(CRYPTA_ID_1, true),
                uniformSplitEntry(CRYPTA_ID_2, true),
                uniformSplitEntry(CRYPTA_ID_3, false)
        );

        globalSplitsTestHelper.prepareCryptaMatchingEntries(
                EMAIL_MD5,
                cryptaMatchingEntry(EMAIL_1, EMAIL_MD5, CRYPTA_ID_1),
                cryptaMatchingEntry(EMAIL_2, EMAIL_MD5, CRYPTA_ID_2),
                cryptaMatchingEntry(EMAIL_3, EMAIL_MD5, CRYPTA_ID_3)
        );

        EmailPeriodicSending sending = sendingTestHelper.prepareSending(
                sending1 -> sending1.getConfig().setGlobalControlEnabled(true)
        );

        YPath dataPath = getDataPath(sending);
        YaSenderCampaign.Segment segment = new YaSenderCampaign.Segment("ytlist")
                .addParam("path", dataPath.toString());

        yaSenderHelper.onSendOrCreatePromo(CAMPAIGN_SLUG, campaign -> {
            assertEquals(sending.getName() + " (итерация 1)", campaign.getTitle());
            assertEquals(DEFAULT_SUBJECT, campaign.getSubject());
            assertEquals(List.of(sending.getKey()), campaign.getTags());

            assertNotNull(campaign.getUnsubscribeListSlug());

            assertNotNull(campaign.getFrom());

            var config = sending.getConfig();
            assertEquals(config.getFrom().getName(), campaign.getFrom().getName());
            assertEquals(config.getFrom().getEmail(), campaign.getFrom().getEmail());

            assertNotNull(campaign.getReplyTo());
            assertEquals(config.getReplyTo().getEmail(), campaign.getReplyTo());

            assertNotNull(campaign.getSegment());
            assertEquals(segment.getTemplate(), campaign.getSegment().getTemplate());
            assertEquals(segment.getParams(), campaign.getSegment().getParams());

            assertNotNull(campaign.getLetterBody());
            assertNotNull(campaign.getScheduleTime());
        });

        yaSenderHelper.prepareCampaignState(CAMPAIGN_SLUG, YaSenderSendingState.SENT);

        generateAndSend(sending);

        List<Pair<String, YaSenderData>> data = ytClient.read(dataPath, YaSenderDataRow.class).stream()
                .map(row -> Pair.of(row.getEmail(), jsonDeserializer.readObject(YaSenderData.class, row.getJsonData())))
                .collect(Collectors.toUnmodifiableList());

        assertSendRequest(EMAIL_1, data.get(0));
        assertSendRequest(EMAIL_2, data.get(1));

        var events = sendingService.getEvents(sending.getKey(), 0);
        assertEquals(2, events.size());

        var generatedEvent = (GeneratedEvent) events.get(1);
        assertEquals(StageStatus.FINISHED, generatedEvent.getStatus());
        assertEquals(generatedEvent.getTotalSteps(), generatedEvent.getCompletedSteps());
        assertNotNull(generatedEvent.getCurrentStepDescription());

        EmailSendingGenerationResult generationResult =
                (EmailSendingGenerationResult) generatedEvent.getGenerationResult();

        assertNotNull(generationResult);
        assertEquals(3, (long) generationResult.getSegmentSize());
        assertEquals(2, (long) generationResult.getInSentGroup());
        assertEquals(0, (long) generationResult.getInControlGroup());
        assertEquals(1, (long) generationResult.getInGlobalControlGroup());

        UploadedEvent uploadedEvent = (UploadedEvent) events.get(0);
        assertEquals(2, uploadedEvent.getUploadedRows());
        assertEquals(2, uploadedEvent.getTotalRows());
        assertEquals(StageStatus.FINISHED, uploadedEvent.getStatus());
        assertTrue(
                uploadedEvent.getExternalInfo() instanceof YaSenderExternalInfo,
                "Upload event should has Yasender external info"
        );

        YaSenderExternalInfo externalInfo = (YaSenderExternalInfo) uploadedEvent.getExternalInfo();
        assertNotNull(externalInfo.getCampaignId());
        assertNotNull(externalInfo.getCampaignSlug());

        EmailPeriodicSending sendingById = sendingService.getById(sending.getId());
        assertEquals(1, sendingById.getIteration(), "Sending iteration has not been incremented");

        Queue<Map<String, String>> emailLog = testSentLogWriter.getEmailLog();
        assertEquals(3, emailLog.size());

        assertEmailLog(EMAIL_1, sending, emailLog.poll(), false);
        assertEmailLog(EMAIL_2, sending, emailLog.poll(), false);
        assertEmailLog(EMAIL_3, sending, emailLog.poll(), true);

        var sendingPath = ytFolders.getCampaignPath(sending.getKey());
        var iterationPath = sendingPath.child("1");

        assertAll(
                () -> assertExpirationTimeIsSet(sendingPath),
                () -> assertExpirationTimeIsSet(iterationPath)
        );
    }

    /**
     * Если в настройках рассылки указан промокод в неё не попадают адреса, учетные записи которых
     * обладают этим промокодом а также адреса учетные записи которых нам неизвестны
     */
    @Test
    void testEmailsOfPuidsWithPromocodeDoNotGetInSending() {
        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1), // Пройдет. Другое промо
                subscription(EMAIL_2, PUID_2), // Не пройдет. Промо совпадает с указанным
                subscription(EMAIL_3, PUID_3), // Не пройдет. Промо совпадает с указанным
                subscription(EMAIL_4)          // Не пройдет. Если не знаем puid, не пропускаем
        );

        userTestHelper.preparePlatformPuidToEmails(
                puidToEmailFact(PUID_1, EMAIL_1),
                puidToEmailFact(PUID_2, EMAIL_2),
                puidToEmailFact(PUID_3, EMAIL_3)
        );

        var bannedPromocode = 1L;

        bannedPromocodesTestHelper.prepareBannedTable(
                promocodeRecord(2, PUID_1, null),
                promocodeRecord(bannedPromocode, PUID_2, null),
                promocodeRecord(bannedPromocode, PUID_3, null)
        );

        // Для того чтобы вызов не был неожиданным для httpEnvironment
        yaSenderHelper.onSendOrCreatePromo(CAMPAIGN_SLUG, campaign -> {
        });
        yaSenderHelper.prepareCampaignState(CAMPAIGN_SLUG, YaSenderSendingState.SENT);

        var campaign = sendingTestHelper.prepareCampaign();

        var segment = segmentService.addSegment(segment(
                subscriptionFilter(ADVERTISING)
        ));

        var sending = sendingTestHelper.prepareSending(
                campaign,
                segment,
                x -> x.getConfig().setBannedPromocode(bannedPromocode)
        );

        generateAndSend(sending);

        var resultEmails = ytClient.read(getDataPath(sending), YaSenderDataRow.class).stream()
                .map(YaSenderDataRow::getEmail)
                .collect(Collectors.toSet());

        assertEquals(Set.of(EMAIL_1), resultEmails);
    }

    private YPath getDataPath(EmailPeriodicSending sending) {
        return ytFolders.getCampaignPath(sending.getKey()).child("1").child("senderdata");
    }

    private void assertSendRequest(String expectedEmail, Pair<String, YaSenderData> emailToData) {
        assertEquals(expectedEmail, emailToData.getLeft());

        YaSenderData data = emailToData.getRight();
        assertEquals(DEFAULT_VARIANT, data.getVariantId());
        assertNotNull(data.getUser());
        assertNotNull(data.getUser().getEmailHash());
        assertNotNull(data.getUser().getUnsubscribe());
    }

    private void assertEmailLog(String email,
                                EmailPeriodicSending sending,
                                Map<String, String> emailRecord,
                                boolean inGlobalControl) {
        assertNotNull(emailRecord);
        assertEquals(inGlobalControl ? 10 : 14, emailRecord.size());

        if (!inGlobalControl) {
            assertEquals(senderAccount, emailRecord.get("senderAccount"));
            assertEquals(DEFAULT_SUBJECT, emailRecord.get("subject"));
            assertNotNull(emailRecord.get("campaignId"));
            assertEquals(DEFAULT_VARIANT, emailRecord.get("variantId"));
        }

        assertEquals(String.valueOf(inGlobalControl), emailRecord.get("control"));
        assertEquals(String.valueOf(SendingType.PERIODIC_SENDING.getNumber()), emailRecord.get("type"));
        assertEquals(sending.getId(), emailRecord.get("versionId"));
        assertEquals(sending.getKey(), emailRecord.get("sendingId"));
        assertEquals(email, emailRecord.get("originalEmail"));
        assertEquals(sending.getConfig().getTarget().getSegment(), emailRecord.get("segmentId"));
        assertEquals(String.valueOf(inGlobalControl), emailRecord.get("globalControl"));
        assertEquals(String.valueOf(sending.getIteration() + 1), emailRecord.get("iteration"));
        assertEquals(email, emailRecord.get("email"));
        assertNotNull(emailRecord.get("timestamp"));
    }

    private void generateAndSend(EmailPeriodicSending sending) {
        PeriodicEntitiesTestUtils.startTask(task, sending.getId());

        List<Event> events = sendingService.getEvents(sending.getKey(), 0);
        assertEquals(1, events.size());

        Event event = events.get(0);
        assertEquals(EventType.GENERATED, event.getType());
        assertNotNull(event.getTime());

        GeneratedEvent generatedEvent = (GeneratedEvent) event;
        assertNotNull(generatedEvent.getStatus());

        long taskId = generatedEvent.getTaskId();
        assertTrue(taskId > 0);

        clusterTasksTestHelper.waitCompleted(taskId, Duration.ofMinutes(30));
    }

    private void assertExpirationTimeIsSet(YPath path) {
        var value = ytClient.getAttribute(path, CommonAttributes.EXPIRATION_TIME)
                .filter(YTreeNode::isStringNode);

        assertTrue(value.isPresent(), "Expiration time is not set for path " + path);
    }
}
