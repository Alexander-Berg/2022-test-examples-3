package ru.yandex.market.crm.campaign.http.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.activity.Activity;
import ru.yandex.market.crm.campaign.domain.activity.SendingGenerationActivityData;
import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.domain.promo.entities.UsageType;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.EmailPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.EmailSendingGenerationResult;
import ru.yandex.market.crm.campaign.domain.sending.SendingType;
import ru.yandex.market.crm.campaign.domain.sending.SystemVar;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.campaign.services.segments.SegmentBuildsDAO;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.EmailSendingDAO;
import ru.yandex.market.crm.campaign.services.timings.StepsTimingsDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ActivitiesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.BlockTemplateTestHelper;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.core.domain.messages.PluggedTable;
import ru.yandex.market.crm.core.domain.segment.BuildStatus;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.segment.SegmentBuild;
import ru.yandex.market.crm.core.domain.segment.SegmentBuild.Initiator;
import ru.yandex.market.crm.core.domain.segment.export.IdType;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.domain.sending.conf.BlockConf;
import ru.yandex.market.crm.core.domain.templates.BlockTemplate;
import ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper.IdRelation;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.yt.paths.CrmYtTables;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.mapreduce.domain.ImageLink;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUserData;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUserRow;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.EmailState;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.BlockData;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.BlockState;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.block.ModelBlockData;
import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.user.User;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderData;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderDataRow;
import ru.yandex.market.crm.platform.models.Subscription;
import ru.yandex.market.crm.tasks.domain.TaskStatus;
import ru.yandex.market.crm.util.yt.CommonAttributes;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.mcrm.http.HttpResponse;
import ru.yandex.market.mcrm.http.ResponseMock;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportEmail;
import static ru.yandex.market.crm.campaign.test.utils.EmailSendingConfigUtils.variant;
import static ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper.pluggedTableRow;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.allUsers;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.subscriptionFilter;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.cryptaMatchingEntry;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.uniformSplitEntry;
import static ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper.subscription;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.EMAIL_MD5;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.passportProfile;
import static ru.yandex.market.mcrm.http.HttpRequest.get;

/**
 * @author apershukov
 */
public class EmailSendingGenerationTest1 extends AbstractControllerLargeTest {

    private static class EmailData {
        final String email;
        final String cryptaId;

        EmailData(String email, String cryptaId) {
            this.email = email;
            this.cryptaId = cryptaId;
        }
    }

    @NotNull
    private static List<EmailData> generate1000EmailsOf250Users() {
        return IntStream.rangeClosed(1, 250)
                .mapToObj(i -> "crypta-id-" + i)
                .flatMap(cryptaId -> IntStream.rangeClosed(1, 4)
                        .mapToObj(i -> new EmailData(cryptaId + "-" + i + "-@yandex.ru", cryptaId))
                )
                .sorted(Comparator.comparing(x -> x.email))
                .collect(Collectors.toList());
    }

    private static final String EMAIL_1 = "email_1@yandex.ru";
    private static final String EMAIL_2 = "email_2@yandex.ru";
    private static final String EMAIL_3 = "email_3@yandex.ru";

    private static final long PUID_1 = 111;
    private static final long PUID_2 = 222;
    private static final long PUID_3 = 333;

    private static final String CRYPTA_ID_1 = "crypta-id-1";
    private static final String CRYPTA_ID_2 = "crypta-id-2";
    private static final String CRYPTA_ID_3 = "crypta-id-3";

    private static final String TABLE_ALIAS = "table";

    private static final String INVALID_TEMPLATE_BODY_PATTERN =
            """
            <%1$s>
            <a href="%2$s">link</a>
            <%3$s src="">
            </%1$s>
            """;

    @Inject
    private YtClient ytClient;
    @Inject
    private YtFolders ytFolders;
    @Inject
    private UserTestHelper userTestHelper;
    @Inject
    private EmailSendingDAO sendingDAO;
    @Inject
    private StepsTimingsDAO stepsTimingsDAO;
    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;
    @Inject
    private SubscriptionsTestHelper subscriptionsTestHelper;
    @Inject
    private JsonDeserializer jsonDeserializer;
    @Inject
    private EmailSendingTestHelper emailSendingTestHelper;
    @Inject
    private PluggableTablesTestHelper pluggableTablesTestHelper;
    @Inject
    private SegmentService segmentService;
    @Inject
    private BlockTemplateTestHelper blockTemplateTestHelper;
    @Inject
    private SegmentBuildsDAO segmentBuildsDAO;
    @Inject
    private GlobalSplitsTestHelper globalSplitsTestHelper;
    @Inject
    private CrmYtTables ytTables;
    @Inject
    private ActivitiesTestHelper activitiesTestHelper;
    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    private static User user(Uid uid1, Uid uid2) {
        return new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(uid1)
                                .addNode(uid2)
                                .addEdge(0, 1)
                );
    }

    private static void assertVar(String expectedValue, SystemVar var, YaSenderData sendrData) {
        Map<String, YTreeNode> vars = sendrData.getVars();
        assertNotNull(vars);

        YTreeNode value = vars.get(var.name().toLowerCase());
        assertNotNull(value);

        assertEquals(expectedValue, value.stringValue());
    }

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareSubscriptionFactsTable();
        ytSchemaTestHelper.preparePassportProfilesTable();
        ytSchemaTestHelper.prepareModelInfoTable();
        ytSchemaTestHelper.prepareModelStatTable();
        ytSchemaTestHelper.prepareEmailOwnershipFactsTable();
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.EMAIL_MD5, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareEmailFactsTable();
        ytSchemaTestHelper.prepareGlobalControlSplitsTable();
        ytSchemaTestHelper.prepareChytPassportEmailsTable();
        ytSchemaTestHelper.prepareEmailsGeoInfo();
        ytSchemaTestHelper.prepareUserTables();
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
        EmailPlainSending sending = prepareSendingWithBanner(
                segment(
                        allUsers()
                )
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1)
        );

        prepareUser();

        EmailSendingVariantConf variantConf = sending.getConfig().getVariants().get(0);
        String templateId = variantConf.getTemplate();
        blockTemplateTestHelper.updateBlockTemplate(templateId, templateBody);

        String message = String.format(
                "Email sending=%s, в Header/footer (%s) присутствуют ошибки:%n%s",
                sending.getId(), variantConf.getId(), errorMessage
        );
        generateAndError(sending, message);
    }

    /**
     * Smoke-test формирования примитивной рассылки
     */
    @Test
    public void testGenerateSimpleSending() throws Exception {
        EmailPlainSending sending = prepareSendingWithBanner(
                segment(
                        allUsers()
                )
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1)
        );

        prepareUser();

        List<CampaignUserRow> resultRows = generate(sending);

        assertEquals(1, resultRows.size());

        CampaignUserRow row = resultRows.get(0);
        assertEquals(EMAIL_1, row.getEmail());
        assertEquals(EmailState.COMPLETED, row.getState());

        String segmentId = sending.getConfig().getTarget().getSegment();
        List<SegmentBuild> facts = segmentBuildsDAO.getForSegment(segmentId);
        assertEquals(1, facts.size());

        SegmentBuild buildingFact = facts.get(0);

        assertEquals(BuildStatus.COUNTED, buildingFact.getStatus());
        assertEquals(Initiator.SYSTEM, buildingFact.getInitiator());
        assertEquals(Map.of(IdType.EMAIL, 1L), buildingFact.getCounts());
        assertNotNull(buildingFact.getStartTime());
        assertNotNull(buildingFact.getFinishTime());
        assertNotNull(buildingFact.getResultDirectory());

        assertFalse(stepsTimingsDAO.getAllTimings().isEmpty());

        List<Activity> activities = activitiesTestHelper.getAllActivities();
        MatcherAssert.assertThat(activities, hasSize(1));

        Activity activity = activities.get(0);
        assertEquals(TaskStatus.COMPLETED, activity.getFinalStatus());
        assertNotNull(activity.getStartTime());
        assertNotNull(activity.getEndTime());

        SendingGenerationActivityData activityData = (SendingGenerationActivityData) activity.getData();
        assertNotNull(activityData);
        assertEquals(sending.getId(), activityData.getEntityId());
        assertEquals(sending.getName(), activityData.getEntityName());
        assertEquals(SendingType.EMAIL, activityData.getSendingType());
        assertEquals(UsageType.DISPOSABLE, activityData.getUsageType());

        var sendingDir = ytFolders.getCampaignPath(sending.getId());

        var expirationTime = ytClient.getAttribute(sendingDir, CommonAttributes.EXPIRATION_TIME)
                .filter(YTreeNode::isStringNode);

        assertTrue(expirationTime.isPresent(), "Expiration time is not set");
    }

    /**
     * В случае если в сегмент попали паспортные идентификаторы рассылка будет сформирована
     * из их паспортных адресов. При этом не обязательно чтобы эти адреса присутствовали в
     * таблице users
     */
    @Test
    public void testGenerateSendingUsingPassportUids() throws Exception {
        EmailPlainSending sending = prepareSendingWithBanner(
                segment(
                        passportGender("m")
                )
        );

        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m"),
                passportProfile(PUID_2, "m"),
                passportProfile(PUID_3, "m")
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1),
                subscription(EMAIL_2, PUID_2)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1),
                chytPassportEmail(PUID_2, EMAIL_2)
        );

        List<CampaignUserRow> resultRows = generate(sending);

        Set<String> emails = resultRows.stream()
                .map(CampaignUserRow::getEmail)
                .collect(Collectors.toSet());

        assertEquals(ImmutableSet.of(EMAIL_1, EMAIL_2), emails);
    }

    /**
     * В случае если в сегмент попал паспортный идентификатор и его паспортный email
     * дублирования адреса в рассылке не происходит
     */
    @Test
    public void testIfCrmEmailAndPassportEmailAreSameEmailIsIncludedOnlyOnce() throws Exception {
        EmailPlainSending sending = prepareSendingWithBanner(
                segment(
                        passportGender("m")
                ),
                LinkingMode.ALL
        );

        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m")
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1)
        );

        prepareUser();

        List<CampaignUserRow> resultRows = generate(sending);

        Set<String> emails = resultRows.stream()
                .map(CampaignUserRow::getEmail)
                .collect(Collectors.toSet());

        assertEquals(ImmutableSet.of(EMAIL_1), emails);
    }

    /**
     * Содержимое модельного блока формирутся по склееным идентификаторам пользователя,
     * если удалось их найти
     */
    @Test
    public void testGenerateModelBlockForLinkedIds() throws Exception {
        List<Long> modelsIds = List.of(111L, 222L, 333L);
        mockReportGetProductsByHistoryResponse(PUID_1, modelsIds);

        BlockConf modelBlock = blockTemplateTestHelper.prepareModelBlock(3);

        EmailPlainSending sending = emailSendingTestHelper.prepareSending(
                segment(
                        subscriptionFilter(SubscriptionTypes.ADVERTISING)
                ),
                LinkingMode.ALL,
                modelBlock
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1)
        );

        prepareUser();

        List<CampaignUserRow> resultRows = generate(sending);
        assertEquals(1, resultRows.size());

        CampaignUserRow row = resultRows.get(0);
        assertEquals(EMAIL_1, row.getEmail());
        assertEquals(EmailState.COMPLETED, row.getState());

        CampaignUserData data = jsonDeserializer.readObject(CampaignUserData.class, row.getData());
        assertModelsInBlock(modelsIds, data.getBlock(modelBlock.getId()));
    }

    /**
     * Паспортный идентификатор, попавший в рассылку используется не только для
     * получения его адреса но и для генерации содержимого письма. При этом
     * наличие соответствующего пользователя в таблице users не обязательно
     */
    @Test
    public void testUsePuidToGenerateEmailContent() throws Exception {
        List<Long> modelsIds = List.of(111L, 222L, 333L);
        mockReportGetProductsByHistoryResponse(PUID_1, modelsIds);

        BlockConf modelBlock = blockTemplateTestHelper.prepareModelBlock(3);

        EmailPlainSending sending = emailSendingTestHelper.prepareSending(
                segment(
                        passportGender("m")
                ),
                LinkingMode.NONE,
                modelBlock
        );

        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m")
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1)
        );

        List<CampaignUserRow> resultRows = generate(sending);
        assertEquals(1, resultRows.size());

        CampaignUserRow row = resultRows.get(0);
        assertEquals(EMAIL_1, row.getEmail());
        assertEquals(EmailState.COMPLETED, row.getState());

        CampaignUserData data = jsonDeserializer.readObject(CampaignUserData.class, row.getData());
        assertModelsInBlock(modelsIds, data.getBlock(modelBlock.getId()));
    }

    /**
     * В случае если в сегмент попал puid при генерации рассылки используются
     * идентификаторы, связанные с этим puid'ом, а не с его email-адресом.
     */
    @Test
    public void testGenerationIdsIsLinkedByOriginalIdNotResolvedEmail() throws Exception {
        List<Long> modelsIds1 = List.of(111L, 222L, 333L);
        mockReportGetProductsByHistoryResponse(PUID_1, modelsIds1);

        List<Long> modelsIds2 = List.of(444L, 555L, 666L);
        mockReportGetProductsByHistoryResponse(PUID_2, modelsIds2);

        BlockConf modelBlock = blockTemplateTestHelper.prepareModelBlock(3);

        EmailPlainSending sending = emailSendingTestHelper.prepareSending(
                segment(
                        passportGender("m")
                ),
                LinkingMode.NONE,
                modelBlock
        );

        userTestHelper.addPassportProfiles(
                passportProfile(PUID_3, "m")
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_3)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_3, EMAIL_1)
        );

        prepareUsers(
                user(Uid.asPuid(PUID_1), Uid.asEmail(EMAIL_1)),
                //В классе HttpReportClient при заполнении http запроса идентификаторами пользователя (метод
                // fillUserParams)
                //для получения данных из репорта, берётся один идентификатор из списка доступных.
                //В данном случае таким идентификатором будет PUID_2
                user(Uid.asPuid(PUID_3), Uid.asPuid(PUID_2))
        );

        List<CampaignUserRow> resultRows = generate(sending);
        assertEquals(1, resultRows.size());

        CampaignUserRow row = resultRows.get(0);
        assertEquals(EMAIL_1, row.getEmail());
        assertEquals(EmailState.COMPLETED, row.getState());

        CampaignUserData data = jsonDeserializer.readObject(CampaignUserData.class, row.getData());
        assertModelsInBlock(modelsIds2, data.getBlock(modelBlock.getId()));
    }

    @Test
    public void testSquashEmails() throws Exception {
        String email1 = "user@yandex.kz";
        String email2 = "user@ya.ru";

        EmailPlainSending sending = prepareSendingWithBanner(
                segment(
                        passportGender("m")
                ),
                LinkingMode.ALL
        );

        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m")
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(email1, PUID_1),
                subscription(email2)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, email1)
        );

        prepareUsers(
                user(Uid.asPuid(PUID_1), Uid.asEmail(email2))
        );

        List<CampaignUserRow> resultRows = generate(sending);

        Set<String> emails = resultRows.stream()
                .map(CampaignUserRow::getEmail)
                .collect(Collectors.toSet());

        assertEquals(ImmutableSet.of("user@yandex.ru"), emails);
    }

    /**
     * В случае если в настройках рассылки используется переменные имени пользователя
     * при сборке эти переменные резолвятся и их значения попадают в таблицу с информацией для рассылятора.
     * <p>
     * Если зарезолвить переменную не удалось, письмо все равно попадает в рассылку. При этом в шаблоне
     * рассылятора на месте data.vars будет пустая мапа.
     */
    @Test
    public void testFillUsernameVariables() throws Exception {
        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m", "Иван", "Иванов"),
                passportProfile(PUID_2, "m", "Петр", "Петров"),
                passportProfile(PUID_3, "m", null, null)
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

        Segment segment = segment(
                passportGender("m")
        );

        BlockTemplate template = blockTemplateTestHelper.prepareBannerBlockTemplate();

        BannerBlockConf bannerBlock = new BannerBlockConf();
        bannerBlock.setId("creative");
        bannerBlock.setTemplate(template.getId());
        bannerBlock.setText("Hello, {{data.vars.firstname}} {{data.vars.lastname}}");
        bannerBlock.setBanners(Collections.singletonList(
                new ImageLink(null, "https://market.yandex.ru/hello", "")
        ));

        EmailPlainSending sending = emailSendingTestHelper.prepareSending(segment, LinkingMode.NONE, bannerBlock);

        generate(sending);

        Map<String, YaSenderData> sendrData = readSenderData(sending).stream()
                .collect(Collectors.toMap(YaSenderDataRow::getEmail, YaSenderDataRow::getData));

        assertEquals(3, sendrData.size());

        YaSenderData data1 = sendrData.get(EMAIL_1);
        assertVar("Иван", SystemVar.FIRSTNAME, data1);
        assertVar("Иванов", SystemVar.LASTNAME, data1);

        YaSenderData data2 = sendrData.get(EMAIL_2);
        assertVar("Петр", SystemVar.FIRSTNAME, data2);
        assertVar("Петров", SystemVar.LASTNAME, data2);

        YaSenderData data3 = sendrData.get(EMAIL_3);
        Map<String, YTreeNode> vars = data3.getVars();
        assertNotNull(vars);
        assertTrue(vars.isEmpty());
    }

    /**
     * В случае если фамилия используется в теме письма она резолвится
     * и передается в рассылятор
     */
    @Test
    public void testResolveLastnameIfItIsUsedInSubject() throws Exception {
        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m", "Иван", "Иванов")
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1)
        );

        Segment segment = segment(
                passportGender("m")
        );

        String messageTemplateId = blockTemplateTestHelper.prepareMessageTemplate();
        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock();

        EmailPlainSending sending = emailSendingTestHelper.prepareSending(
                segment,
                LinkingMode.NONE,
                variant(
                        "variant_a",
                        100,
                        messageTemplateId,
                        "Hello, {{data.vars.lastname}}",
                        creative
                )
        );

        generate(sending);

        List<YaSenderDataRow> sendrData = readSenderData(sending);
        assertEquals(1, sendrData.size());
        assertVar("Иванов", SystemVar.LASTNAME, sendrData.get(0).getData());
    }

    /**
     * В случае если к рассылке подключена внешняя таблица с идентификаторами пользователя типа PUID,
     * значения её колонок можно использовать в теле письма. При этом:
     * <p>
     * 1. Если значение нашлось, оно будет доступно в переменной вида
     * data.u_vars.${алиас таблицы, указанный в рассылке}.${название колонки}
     * 2. Если значение не нашлось, письмо все равно попадает в рассылку. При этом переменная
     * data.u_vars.${алиас таблицы, указанный в рассылке} заполнена пустым объектом
     */
    @Test
    public void testBuildSendingWithPluggedPuidVariables() throws Exception {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable(UidType.PUID,
                pluggedTableRow(String.valueOf(PUID_1), "100500"),
                pluggedTableRow(String.valueOf(PUID_2), "9.99")
        );

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

        Segment segment = segmentService.addSegment(segment(
                passportGender("m")
        ));

        String messageTemplateId = blockTemplateTestHelper.prepareMessageTemplate();
        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock(
                "{{data.u_vars.table.saved_money}}"
        );
        EmailSendingVariantConf variant = variant("variant_a", 100, messageTemplateId, creative);

        EmailSendingConf config = new EmailSendingConf();
        config.setSubscriptionType(2L);
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setVariants(Collections.singletonList(variant));
        config.setPluggedTables(Collections.singletonList(new PluggedTable(pluggableTable.getId(), TABLE_ALIAS)));

        EmailPlainSending sending = emailSendingTestHelper.prepareSending(config);

        generate(sending);

        Map<String, YaSenderData> sendrData = readSenderData(sending).stream()
                .collect(Collectors.toMap(YaSenderDataRow::getEmail, YaSenderDataRow::getData));

        assertEquals(3, sendrData.size());

        assertEquals(
                Map.of(TABLE_ALIAS, Map.of("saved_money", YTree.stringNode("100500"))),
                sendrData.get(EMAIL_1).getUVars()
        );

        assertEquals(
                Map.of(TABLE_ALIAS, Map.of("saved_money", YTree.stringNode("9.99"))),
                sendrData.get(EMAIL_2).getUVars()
        );

        assertEquals(Map.of(TABLE_ALIAS, Map.<String, YTreeNode>of()), sendrData.get(EMAIL_3).getUVars());
    }

    /**
     * В случае если к рассылке подключена внешняя таблица с идентификаторами пользователя типа EMAIL,
     * значения её колонок можно использовать в теле письма. При этом поведение будет аналогичным
     * подключению таблицы с идентификаторами любого типа за исключением того что сопоставление с
     * письмами в рассылке будет идти сразу по его email вне зависимости от происхождения адреса
     * (был вычислен сегментатором напрямую или через puid)
     */
    @Test
    public void testBuildSendingWithPluggedEmailVariables() throws Exception {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable(UidType.EMAIL,
                pluggedTableRow(EMAIL_1, "100500"),
                pluggedTableRow(EMAIL_2, "9.99")
        );

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

        Segment segment = segmentService.addSegment(segment(
                passportGender("m")
        ));

        String messageTemplateId = blockTemplateTestHelper.prepareMessageTemplate();
        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock(
                "{{data.u_vars.table.saved_money}}"
        );
        EmailSendingVariantConf variant = variant("variant_a", 100, messageTemplateId, creative);

        EmailSendingConf config = new EmailSendingConf();
        config.setSubscriptionType(2L);
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setVariants(Collections.singletonList(variant));
        config.setPluggedTables(Collections.singletonList(new PluggedTable(pluggableTable.getId(), TABLE_ALIAS)));

        EmailPlainSending sending = emailSendingTestHelper.prepareSending(config);

        generate(sending);

        Map<String, YaSenderData> sendrData = readSenderData(sending).stream()
                .collect(Collectors.toMap(YaSenderDataRow::getEmail, YaSenderDataRow::getData));

        assertEquals(3, sendrData.size());

        assertEquals(
                Map.of(TABLE_ALIAS, Map.of("saved_money", YTree.stringNode("100500"))),
                sendrData.get(EMAIL_1).getUVars()
        );

        assertEquals(
                Map.of(TABLE_ALIAS, Map.of("saved_money", YTree.stringNode("9.99"))),
                sendrData.get(EMAIL_2).getUVars()
        );

        assertEquals(Map.of(TABLE_ALIAS, Map.<String, YTreeNode>of()), sendrData.get(EMAIL_3).getUVars());
    }

    /**
     * В случае если у рассылки включено вычитание глобального контроля
     * при генерации в нее попадают только адреса глобальной целевой группы
     */
    @Test
    public void testDoNotIncludeGlobalControlGroup() throws Exception {
        EmailPlainSending sending = prepareSendingWithGlobalControl();

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1),
                subscription(EMAIL_2),
                subscription(EMAIL_3)
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

        Set<String> resultEmails = generate(sending).stream()
                .map(CampaignUserRow::getEmail)
                .collect(Collectors.toSet());

        assertEquals(Set.of(EMAIL_1, EMAIL_2), resultEmails);

        EmailSendingGenerationResult generationResult = sendingDAO.getSending(sending.getId()).getGenerationResult();
        assertNotNull(generationResult);
        assertEquals((Integer) 0, generationResult.getInControlGroup());
        assertEquals((Integer) 1, generationResult.getInGlobalControlGroup());
    }

    /**
     * В случае если в сегмент попадает адрес, которого нет ни в одном из глобальных сплитов, но
     * при этом в таблице соответствий для него нашелся crypta-id, включенный в глобальную целевую группу,
     * неизвестный адрес так же попадает в целевую группу.
     */
    @Test
    public void testGlobalSplittingUnknownEmailWithKnownSplittedCryptaId() throws Exception {
        EmailPlainSending sending = prepareSendingWithGlobalControl();

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1),
                subscription(EMAIL_2),
                subscription(EMAIL_3)
        );

        globalSplitsTestHelper.prepareGlobalControlSplits(
                uniformSplitEntry(CRYPTA_ID_1, true),
                uniformSplitEntry(CRYPTA_ID_2, false)
        );

        globalSplitsTestHelper.prepareCryptaMatchingEntries(
                EMAIL_MD5,
                cryptaMatchingEntry(EMAIL_1, EMAIL_MD5, CRYPTA_ID_1),
                cryptaMatchingEntry(EMAIL_2, EMAIL_MD5, CRYPTA_ID_2),
                cryptaMatchingEntry(EMAIL_3, EMAIL_MD5, CRYPTA_ID_1)
        );

        Set<String> resultEmails = generate(sending).stream()
                .map(CampaignUserRow::getEmail)
                .collect(Collectors.toSet());

        assertEquals(Set.of(EMAIL_1, EMAIL_3), resultEmails);

        List<YTreeMapNode> splitTableRows = globalSplitsTestHelper.getGlobalSplitsRows();
        assertEquals(2, splitTableRows.size());
    }

    /**
     * В случае если в сегмент попадают адреса, crypta-id которых еще не проходили разбиение на
     * глобальные сплиты, адреса проходят разбивку на ходу с добавлением их в таблицу с глобальными
     * сплитами.
     * <p>
     * При этом все адреса, связанные с одним crypta-id, помещаются строго в один сплит.
     */
    @Test
    @Disabled("Переделать на проверку логируемых данных в SaveSplitsToGlobalSplitsTableTask")
    public void testGlobalSplittingUnknownEmailWithKnownNotSplittedCryptaId() throws Exception {
        List<EmailData> emailData = generate1000EmailsOf250Users();

        subscriptionsTestHelper.saveSubscriptions(
                emailData.stream()
                        .map(x -> x.email)
                        .map(SubscriptionsTestHelper::subscription)
                        .toArray(Subscription[]::new)
        );

        globalSplitsTestHelper.prepareCryptaMatchingEntries(
                EMAIL_MD5,
                emailData.stream()
                        .map(item -> cryptaMatchingEntry(item.email, EMAIL_MD5, item.cryptaId))
                        .toArray(YTreeMapNode[]::new)
        );

        EmailPlainSending sending = prepareSendingWithGlobalControl();
        int mailCount = generate(sending).size();

        MatcherAssert.assertThat(
                mailCount,
                allOf(
                        greaterThanOrEqualTo(800),
                        lessThanOrEqualTo(970)
                )
        );

        assertNewSplits(emailData);
    }

    @NotNull
    private EmailPlainSending prepareSendingWithGlobalControl() {
        EmailPlainSending sending = prepareSendingWithBanner(
                segment(
                        subscriptionFilter(SubscriptionTypes.ADVERTISING)
                )
        );

        enableGlobalControl(sending);
        return sending;
    }

    private void generateAndError(EmailPlainSending sending, String message) throws Exception {
        mockMvc.perform(post("/api/sendings/email/{id}/generate", sending.getId()))
                .andExpect(status().is5xxServerError())
                .andExpect(checkException(message))
                .andDo(print());
    }

    private static ResultMatcher checkException(String message) {
        return result -> {
            assertNotNull(result.getResolvedException());
            assertEquals(message, result.getResolvedException().getMessage());
        };
    }

    private List<CampaignUserRow> generate(EmailPlainSending sending) throws Exception {
        mockMvc.perform(post("/api/sendings/email/{id}/generate", sending.getId()))
                .andExpect(status().isOk())
                .andDo(print());

        emailSendingTestHelper.waitGenerated(sending.getId());

        YPath resultPath = ytFolders.getCampaignPath(sending.getId()).child("campaign");
        return ytClient.read(resultPath, CampaignUserRow.class);
    }

    @Nonnull
    private EmailPlainSending prepareSendingWithBanner(Segment segment) {
        return prepareSendingWithBanner(segment, LinkingMode.NONE);
    }

    @Nonnull
    private EmailPlainSending prepareSendingWithBanner(Segment segment, LinkingMode linkingMode) {
        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock();
        return emailSendingTestHelper.prepareSending(segment, linkingMode, creative);
    }

    private List<YaSenderDataRow> readSenderData(EmailPlainSending sending) {
        YPath path = ytFolders.getCampaignPath(sending.getId()).child("senderdata");

        return ytClient.read(path, YaSenderDataRow.class).stream()
                .peek(row -> {
                    YaSenderData data = jsonDeserializer.readObject(YaSenderData.class, row.getJsonData());
                    row.setData(data);
                })
                .collect(Collectors.toList());
    }

    private void prepareUser() {
        User user = user(Uid.asEmail(EMAIL_1), Uid.asPuid(PUID_1));
        prepareUsers(user);
    }

    private void prepareUsers(User... users) {
        userTestHelper.addUsers(users);
        userTestHelper.finishUsersPreparation();
    }

    private void assertModelsInBlock(Collection<Long> expectedModels, BlockData blockData) {
        assertNotNull(blockData);
        assertEquals(BlockState.COMPLETED, blockData.getState());

        ModelBlockData modelBlockData = (ModelBlockData) blockData;

        List<Long> actualModelIds = modelBlockData.getModels().stream()
                .map(ModelInfo::getId)
                .map(Long::parseLong)
                .collect(Collectors.toList());

        assertEquals(expectedModels, actualModelIds);
    }

    private void mockReportGetProductsByHistoryResponse(Long puid,
                                                        Collection<Long> modelIds) throws IOException, JSONException {
        String strModel = IOUtils.toString(
                getClass().getResourceAsStream("SimpleReportModelResponse.json"),
                StandardCharsets.UTF_8
        );

        JSONArray results = new JSONArray();

        for (Long modelId : modelIds) {
            JSONObject jsonModel = new JSONObject(strModel).put("id", modelId);
            results.put(jsonModel);
        }

        byte[] response = new JSONObject()
                .put("search", new JSONObject().put("results", results))
                .toString()
                .getBytes();

        httpEnvironment.when(
                get("http://int-report.vs.market.yandex.net:17151/yandsearch?rgb=GREEN&place=products_by_history&" +
                        "pp=18&pg=18&bsformat=2&numdoc=12&rids=213&puid=" + puid)
        ).then(new HttpResponse(new ResponseMock(response)));
    }

    private void enableGlobalControl(EmailPlainSending sending) {
        sending.getConfig().setGlobalControlEnabled(true);
        emailSendingTestHelper.updateSending(sending);
    }

    private void prepareEmailCryptaIdSnapshot(IdRelation... relations) {
        userTestHelper.saveLinks(
                UserTestHelper.EMAIL_MD5,
                UserTestHelper.CRYPTA_ID,
                relations
        );
    }

    private void assertNewSplits(List<EmailData> emailData) {
        List<YTreeMapNode> splitTableRows = ytClient.read(getGlobalSplitsTable(), YTableEntryTypes.YSON);
        assertEquals(emailData.size(), splitTableRows.size());

        // Проверяем то что каждый crypta_id попал в единственный глобальный сплит
        Map<String, Boolean> cryptaIdSplits = new HashMap<>();

        for (int i = 0; i < emailData.size(); ++i) {
            EmailData item = emailData.get(i);
            YTreeMapNode row = splitTableRows.get(i);

            String cryptaId = row.getString("crypta_id");
            boolean isTarget = row.getBool("in_target");

            Boolean previousSplit = cryptaIdSplits.put(cryptaId, isTarget);
            MatcherAssert.assertThat(
                    "Crypta id '" + cryptaId + "' is in multiple splits",
                    previousSplit, anyOf(nullValue(), equalTo(isTarget))
            );

            assertEquals(item.cryptaId, cryptaId);
            assertFalse(row.getBool("initial"));
        }
    }

    private YPath getGlobalSplitsTable() {
        return ytTables.getCurrentGlobalSplitsTable();
    }
}
