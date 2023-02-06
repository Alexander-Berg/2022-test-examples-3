package ru.yandex.market.crm.campaign.http.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.status.IssueBunchStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.SendEmailsStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.StepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.IssueCoinsStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.SendEmailsStep;
import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.services.actions.ActionYtPaths;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailTemplatesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.core.domain.HasUtmLinks;
import ru.yandex.market.crm.core.domain.messages.EmailMessageConf;
import ru.yandex.market.crm.core.domain.messages.PluggedTable;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.domain.yasender.YaSenderSendingState;
import ru.yandex.market.crm.core.services.sending.UtmLinks;
import ru.yandex.market.crm.core.services.subscription.SubscriptionUrlGenerator;
import ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.YaSenderHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.util.CrmUrls;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.mapreduce.domain.action.StepOutputRow;
import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.user.User;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderData;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderDataRow;
import ru.yandex.market.crm.platform.models.Subscription.Status;
import ru.yandex.market.crm.util.yt.CommonAttributes;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.mcrm.db.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.issueCoins;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.outputRow;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.sendEmails;
import static ru.yandex.market.crm.campaign.test.utils.ChytDataTablesHelper.chytPassportEmail;
import static ru.yandex.market.crm.campaign.test.utils.CoinStepTestUtils.outputRowWithCoin;
import static ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper.pluggedTableRow;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.cryptaMatchingEntry;
import static ru.yandex.market.crm.core.test.utils.GlobalSplitsTestHelper.uniformSplitEntry;
import static ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper.subscription;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.EMAIL_MD5;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.passportProfile;

/**
 * @author apershukov
 */
public class SendEmailsStepTest extends AbstractControllerLargeTest {

    private static void assertCoinFilled(YaSenderData data) {
        Map<String, YTreeNode> vars = data.getVars();
        assertNotNull(vars);

        YTreeNode coinList = vars.get("COINS");
        assertNotNull(coinList);
        assertTrue(coinList.isListNode());
        assertFalse(coinList.asList().isEmpty());

        // Проверяем наличие и тип единственного поля просто чтобы убедиться что
        // объект чем-то заполнен
        YTreeMapNode coin = coinList.asList().get(0).mapNode();
        assertTrue(coin.get("id").map(YTreeNode::isIntegerNode).orElse(false));
    }

    private static void assertCoinFilled(YaSenderDataRow row) {
        assertCoinFilled(row.getData());
    }

    private static final long PROMO_ID = 12345678;

    private static final long PUID_1 = 111;
    private static final long PUID_2 = 222;

    private static final String EMAIL_1 = "email_1@yandex-team.ru";
    private static final String EMAIL_2 = "email_2@yandex-team.ru";
    private static final String EMAIL_3 = "email_3@yandex-team.ru";

    private static final String YUID_1 = "111";

    private static final String CRYPTA_ID_1 = "crypta-id-1";
    private static final String CRYPTA_ID_2 = "crypta-id-2";
    private static final String CRYPTA_ID_3 = "crypta-id-3";

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private EmailTemplatesTestHelper emailTemplatesTestHelper;

    @Inject
    private ActionTestHelper actionTestHelper;

    @Inject
    private YaSenderHelper yaSenderHelper;

    @Inject
    private StepsStatusDAO stepsStatusDAO;

    @Inject
    private YtClient ytClient;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private PluggableTablesTestHelper pluggableTablesTestHelper;

    @Inject
    private GlobalSplitsTestHelper globalSplitsTestHelper;

    @Inject
    private YtSchemaTestHelper schemaTestHelper;

    @Inject
    private SubscriptionsTestHelper subscriptionsTestHelper;

    @Inject
    @Named(Constants.DEFAULT_JDBC_TEMPLATE)
    private JdbcTemplate jdbcTemplate;

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private SubscriptionUrlGenerator subscriptionUrlGenerator;

    @Inject
    private ChytDataTablesHelper chytDataTablesHelper;

    @Inject
    private YtFolders ytFolders;

    private final Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    private Clock originalClock;

    @BeforeEach
    public void setUp() {
        schemaTestHelper.prepareEmailOwnershipFactsTable();
        schemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.EMAIL_MD5, UserTestHelper.CRYPTA_ID);
        schemaTestHelper.prepareGlobalControlSplitsTable();
        schemaTestHelper.prepareSubscriptionFactsTable();
        schemaTestHelper.prepareChytPassportEmailsTable();
        schemaTestHelper.preparePassportProfilesTable();
        schemaTestHelper.prepareEmailsGeoInfo();
        schemaTestHelper.prepareUserTables();
        originalClock = subscriptionUrlGenerator.setClock(fixedClock);
    }

    @AfterEach
    public void tearDown() {
        subscriptionUrlGenerator.setClock(originalClock);
    }

    /**
     * Тестирование шага отправки писем
     *
     * После отправки на директории с данными шагов, предшествующих шагу отправки email,
     * устанавливается атрибут expiration time. На директории с шагом отправки при этом атрибут не устанавливается.
     */
    @Test
    public void testGenerateEmailNotification() throws Exception {
        subscriptionsTestHelper.saveSubscriptions(
                subscription("email1@yandex.ru", 1),
                subscription("email2@yandex.ru", 2),
                subscription("email3@yandex.ru", 3)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(1, "email1@yandex.ru"),
                chytPassportEmail(2, "email2@yandex.ru"),
                chytPassportEmail(3, "email3@yandex.ru")
        );

        var messageTemplate = emailTemplatesTestHelper.prepareEmailTemplate();

        ActionStep issueCoins = issueCoins(PROMO_ID);
        ActionStep sendStep = sendEmails(messageTemplate.getId());
        PlainAction action = prepareAction(issueCoins, sendStep);

        finishSegmentation(action.getId());
        finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        prepareStepOutput(action, issueCoins,
                outputRowWithCoin("1"),
                outputRowWithCoin("2"),
                outputRowWithCoin("3")
        );

        var actionId = action.getId();
        YPath dataPath = sendrDataPath(actionId, sendStep.getId());
        expectPromo(dataPath);

        List<StepOutputRow> result = execute(action, sendStep);
        assertEquals(3, result.size());

        List<YaSenderDataRow> senderData = readSenderData(actionId, sendStep.getId());

        assertEquals(3, senderData.size());

        assertEquals("email1@yandex.ru", senderData.get(0).getEmail());
        assertCoinFilled(senderData.get(0));

        assertEquals("email2@yandex.ru", senderData.get(1).getEmail());
        assertCoinFilled(senderData.get(1));

        assertEquals("email3@yandex.ru", senderData.get(2).getEmail());
        assertCoinFilled(senderData.get(2));

        List<YaSenderHelper.PromoInfo> promoInfos = yaSenderHelper.getCreatedPromos();
        assertEquals(1, promoInfos.size());

        String senderTemplate = promoInfos.get(0).getTemplate();
        assertTrue(senderTemplate.contains("utm_campaign=campaign"));
        assertTrue(senderTemplate.contains("utm_medium=medium"));
        assertTrue(senderTemplate.contains("utm_source=source"));

        SendEmailsStepStatus status = (SendEmailsStepStatus) stepsStatusDAO.get(actionId, sendStep.getId());
        assertNotNull(status.getYaSenderCampaignId());
        assertEquals(3, (long) status.getPassedEmailsCount());

        var sendingDir = actionTestHelper.getStepDirectory(actionId, sendStep.getId())
                .child("sending");

        assertExpirationTimeIsSet(sendingDir);

        var actionPath = ytFolders.getActionPath(actionId);
        var ytPaths = new ActionYtPaths(actionPath);

        assertExpirationTimeIsSet(ytPaths.getStepDirectory(issueCoins.getId()));
        assertExpirationTimeIsNotSet(ytPaths.getStepDirectory(sendStep.getId()));
        assertExpirationTimeIsNotSet(actionPath);
    }

    /**
     * В письме из акции можно использовать переменную username так же как и в
     * промо-рассылках
     */
    @Test
    public void testGenerateEmailNotificationWithUsernameVariable() throws Exception {
        subscriptionsTestHelper.saveSubscriptions(
                subscription("email@yandex.ru", PUID_1)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, "email@yandex.ru")
        );

        userTestHelper.addPassportProfiles(
                passportProfile(PUID_1, "m", "Иван", "Иванов")
        );

        var messageTemplate = emailTemplatesTestHelper.prepareEmailTemplate(
                "Здравствуйте, {{data.vars.firstname}} {{data.vars.lastname}}",
                emailTemplatesTestHelper.prepareBannerBlock(
                        block -> block.setText("Hi, {{data.vars.firstname}} {{data.vars.lastname}}")
                )
        );

        ActionStep issueCoins = issueCoins(PROMO_ID);
        ActionStep sendStep = sendEmails(messageTemplate.getId());
        PlainAction action = prepareAction(issueCoins, sendStep);

        finishSegmentation(action.getId());
        finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        prepareStepOutput(action, issueCoins,
                outputRowWithCoin(String.valueOf(PUID_1))
        );

        YPath dataPath = sendrDataPath(action.getId(), sendStep.getId());
        expectPromo(dataPath);

        execute(action, sendStep);

        List<YaSenderDataRow> senderData = readSenderData(action.getId(), sendStep.getId());
        assertEquals(1, senderData.size());

        YaSenderDataRow row = senderData.get(0);
        assertEquals("email@yandex.ru", row.getEmail());
        assertCoinFilled(row);

        YaSenderData data = row.getData();
        assertEquals("Иван", data.getVars().get("firstname").stringValue());
        assertEquals("Иванов", data.getVars().get("lastname").stringValue());
    }

    /**
     * В случае если в шаблоне email-сообщения подключена внешняя таблица
     * значение её колонок будут доступны в шаблоне в переменной вида:
     * data.u_vars.${alias подключенной таблицы}.${название колонки}
     * <p>
     * При этом если для какого-то письма не нашлась строка в подключенной таблице
     * оно все равно будет отправлено в рассылятор. Три этом в качестве значения
     * переменной data.u_vars.${alias подключенной таблицы} в рассылятор будет передан
     * пустой объект.
     */
    @Test
    public void testSendEmailNotificationWithPluggedTableVariable() throws Exception {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable(UidType.PUID,
                pluggedTableRow(String.valueOf(PUID_1), "100500")
        );

        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, PUID_1),
                subscription(EMAIL_2, PUID_2)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_1, EMAIL_1),
                chytPassportEmail(PUID_2, EMAIL_2)
        );

        BannerBlockConf bannerBlock = emailTemplatesTestHelper.prepareBannerBlock(
                block -> block.setText("{{data.u_vars.table.saved_money}}")
        );

        EmailMessageConf config = new EmailMessageConf();
        config.setTemplate(emailTemplatesTestHelper.prepareMessageTemplate().getId());
        config.setBlocks(List.of(bannerBlock));
        config.setSubject("Test Email");
        config.setPluggedTables(List.of(new PluggedTable(pluggableTable.getId(), "table")));

        var messageTemplate = emailTemplatesTestHelper.prepareEmailTemplate(config);

        ActionStep issueCoins = issueCoins(PROMO_ID);
        ActionStep sendStep = sendEmails(messageTemplate.getId());
        PlainAction action = prepareAction(issueCoins, sendStep);

        finishSegmentation(action.getId());
        finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        prepareStepOutput(action, issueCoins,
                outputRowWithCoin(String.valueOf(PUID_1)),
                outputRowWithCoin(String.valueOf(PUID_2))
        );

        YPath dataPath = sendrDataPath(action.getId(), sendStep.getId());
        expectPromo(dataPath);

        execute(action, sendStep);

        Map<String, YaSenderData> senderData = readSenderData(action.getId(), sendStep.getId()).stream()
                .collect(Collectors.toMap(YaSenderDataRow::getEmail, YaSenderDataRow::getData));

        assertEquals(2, senderData.size());

        YaSenderData data1 = senderData.get(EMAIL_1);
        assertCoinFilled(data1);
        assertEquals(
                Map.of("table", Map.of("saved_money", YTree.stringNode("100500"))),
                data1.getUVars()
        );

        YaSenderData data2 = senderData.get(EMAIL_2);
        assertCoinFilled(data2);
        assertEquals(Map.of("table", Map.<String, YTreeNode>of()), data2.getUVars());
    }

    /**
     * В случае если для одного YUID нашлось несколько адресов подходящих для отправки
     * будет отправлено не более одного письма на один из этих адресов
     * <p>
     * https://st.yandex-team.ru/LILUCRM-2053
     */
    @Test
    public void testSendSingleEmailForOneYuid() throws Exception {
        User user = new User(UUID.randomUUID().toString())
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(Uid.asYuid(YUID_1))
                                .addNode(Uid.asEmail(EMAIL_1))
                                .addNode(Uid.asEmail(EMAIL_2))
                                .addEdge(0, 1)
                                .addEdge(0, 2)
                );

        userTestHelper.addUsers(user);
        userTestHelper.finishUsersPreparation();

        var sendStep = prepareSendStep();
        PlainAction action = prepareAction(sendStep);

        actionTestHelper.prepareSegmentationResult(action.getId(), List.of(
                outputRow(UidType.YUID, YUID_1))
        );

        YPath dataPath = sendrDataPath(action.getId(), sendStep.getId());
        expectPromo(dataPath);

        execute(action, sendStep);

        List<?> yaSenderRows = readSenderData(action.getId(), sendStep.getId());
        assertEquals(1, yaSenderRows.size());
    }

    /**
     * В случае если в акции включено вычитание глобального контроля шаг
     * не оставляет письма на адреса, принадлежащие ему.
     */
    @Test
    public void testDoNotSendToGlobalControlGroup() throws Exception {
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

        var sendStep = prepareSendStep();
        PlainAction action = prepareAction(sendStep);
        actionTestHelper.enableGlobalControl(action);

        finishSegmentation(action.getId());

        actionTestHelper.prepareSegmentationResult(action.getId(), List.of(
                outputRow(UidType.EMAIL, EMAIL_1),
                outputRow(UidType.EMAIL, EMAIL_2),
                outputRow(UidType.EMAIL, EMAIL_3)
        ));

        YPath dataPath = sendrDataPath(action.getId(), sendStep.getId());
        expectPromo(dataPath);

        execute(action, sendStep);

        Set<String> emails = readSenderData(action.getId(), sendStep.getId()).stream()
                .map(YaSenderDataRow::getEmail)
                .collect(Collectors.toSet());

        assertEquals(Set.of(EMAIL_1, EMAIL_2), emails);
    }

    /**
     * Тестирование шага отправки писем, если не заполнен "Тип подписки"
     */
    @Test
    public void testGenerateEmailNotificationIfNullSubscriptionType() throws Exception {
        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_2, PUID_2)
        );

        chytDataTablesHelper.preparePassportEmails(
                chytPassportEmail(PUID_2, EMAIL_2)
        );

        var messageTemplate = emailTemplatesTestHelper.prepareEmailTemplate();

        IssueCoinsStep issueCoins = issueCoins(PROMO_ID);
        SendEmailsStep sendStep = sendEmails(messageTemplate.getId());
        PlainAction action = prepareAction(issueCoins, sendStep);
        resetActionSubscriptionType(action, sendStep);

        finishSegmentation(action.getId());
        finishStep(action.getId(), issueCoins.getId(), IssueBunchStepStatus::new);

        prepareStepOutput(action, issueCoins,
                outputRowWithCoin(UidType.EMAIL, EMAIL_1),
                outputRowWithCoin(String.valueOf(PUID_2))
        );

        YPath dataPath = sendrDataPath(action.getId(), sendStep.getId());
        expectPromo(dataPath);

        List<StepOutputRow> result = execute(action, sendStep);
        assertEquals(2, result.size());
        List<YaSenderDataRow> senderData = readSenderData(action.getId(), sendStep.getId());
        assertEquals(2, senderData.size());

        YaSenderDataRow senderDataRow = senderData.get(0);
        assertEquals(EMAIL_1, senderDataRow.getEmail());
        assertCoinFilled(senderDataRow);
        assertAdvertisingUnsubscribeUrl(senderDataRow, action, sendStep, EMAIL_1, null);

        senderDataRow = senderData.get(1);
        assertEquals(EMAIL_2, senderDataRow.getEmail());
        assertCoinFilled(senderDataRow);
        assertAdvertisingUnsubscribeUrl(senderDataRow, action, sendStep, EMAIL_2, PUID_2);
    }

    @Test
    public void testCorrectEndSendEmailsStepWithEmptyInput() throws Exception {
        var sendStep = prepareSendStep();
        PlainAction action = prepareAction(sendStep);

        actionTestHelper.prepareSegmentationResult(action.getId(), Collections.emptyList());
        List<StepOutputRow> result = execute(action, sendStep);
        assertTrue(ytClient.exists(actionTestHelper.getStepOutputPath(action.getId(), sendStep.getId())));
        assertEquals(0, result.size());
    }

    /**
     * Адреса, отписавшие от рассылки, не отправляются в Рассылятор
     */
    @Test
    void testUnsubscribedEmailIsFilteredOut() throws Exception {
        subscriptionsTestHelper.saveSubscriptions(
                subscription(EMAIL_1, Status.SUBSCRIBED),
                subscription(EMAIL_2, Status.SUBSCRIBED),
                subscription(EMAIL_3, Status.UNSUBSCRIBED)
        );

        var sendStep = prepareSendStep();
        sendStep.setSubscriptionType(SubscriptionTypes.ADVERTISING);

        var action = prepareAction(sendStep);
        finishSegmentation(action.getId());

        actionTestHelper.prepareSegmentationResult(action.getId(), List.of(
                outputRow(UidType.EMAIL, EMAIL_1),
                outputRow(UidType.EMAIL, EMAIL_2),
                outputRow(UidType.EMAIL, EMAIL_3)
        ));

        var dataPath = sendrDataPath(action.getId(), sendStep.getId());
        expectPromo(dataPath);

        execute(action, sendStep);

        var emails = readSenderData(action.getId(), sendStep.getId()).stream()
                .map(YaSenderDataRow::getEmail)
                .collect(Collectors.toSet());

        assertEquals(Set.of(EMAIL_1, EMAIL_2), emails);
    }

    private List<StepOutputRow> execute(PlainAction action, ActionStep step) throws Exception {
        return actionTestHelper.execute(action, step);
    }

    private PlainAction prepareAction(ActionStep... steps) {
        return actionTestHelper.prepareAction("segment_id", LinkingMode.NONE, steps);
    }

    private YPath sendrDataPath(String actionId, String stepId) {
        return actionTestHelper.getStepDirectory(actionId, stepId).child("sending").child("senderdata");
    }

    private List<YaSenderDataRow> readSenderData(String actionId, String stepId) {
        YPath senderDataPath = sendrDataPath(actionId, stepId);
        return ytClient.read(senderDataPath, YaSenderDataRow.class).stream()
                .peek(row -> row.setData(jsonDeserializer.readObject(YaSenderData.class, row.getJsonData())))
                .sorted(Comparator.comparing(YaSenderDataRow::getEmail))
                .collect(Collectors.toList());
    }

    private void finishStep(String actionId, String stepId, Supplier<? extends StepStatus<?>> statusSupplier) {
        actionTestHelper.finishStep(actionId, stepId, statusSupplier);
    }

    private void finishSegmentation(String actionId) {
        actionTestHelper.finishSegmentation(actionId);
    }

    private void prepareStepOutput(PlainAction action, ActionStep step, StepOutputRow... rows) {
        actionTestHelper.prepareStepOutput(action, step, rows);
    }

    /**
     * Обнуление "Типа подписки" приходится делать напрямую запросом в БД,
     * так как с точки зрения бизнес-логики это запрещено
     */
    private void resetActionSubscriptionType(PlainAction action, SendEmailsStep sendStep) {
        action.getConfig().getStepAndVariant(sendStep.getId())
                .map(Pair::getRight)
                .map(SendEmailsStep.class::cast)
                .ifPresent(step -> step.setSubscriptionType(null));

        jdbcTemplate.update(
                """
                        UPDATE actions
                        SET
                            name = ?,
                            config = ?::jsonb
                        WHERE
                            id = ?""",
                action.getName(),
                jsonSerializer.writeObjectAsString(action.getConfig()),
                action.getId()
        );
    }

    private void assertAdvertisingUnsubscribeUrl(YaSenderDataRow senderDataRow,
                                                 PlainAction action,
                                                 SendEmailsStep sendStep,
                                                 String email,
                                                 Long puid) {
        assertNotNull(senderDataRow.getData());
        assertNotNull(senderDataRow.getData().getUser());

        UtmLinks utmLinks = UtmLinks.forEmailSending(action.getId(),
                HasUtmLinks.from(
                        sendStep.getUtmCampaign(),
                        sendStep.getUtmSource(),
                        sendStep.getUtmMedium(),
                        sendStep.getUtmReferrer(),
                        sendStep.getClid()
                )
        ).withEmail(email);

        assertEquals(
                subscriptionUrlGenerator.generateUnsubscribeUrl(email, puid, 2L, CrmUrls.unsubscribe(utmLinks), null),
                senderDataRow.getData().getUser().getUnsubscribe()
        );
    }

    private void expectPromo(YPath dataPath) {
        var slug = "campaign_slug";
        yaSenderHelper.expectPromo(slug, dataPath);
        yaSenderHelper.prepareCampaignState(slug, YaSenderSendingState.SENT);
    }

    private void assertExpirationTimeIsSet(YPath path) {
        assertExpirationTime(path, true);
    }

    private void assertExpirationTimeIsNotSet(YPath path) {
        assertExpirationTime(path, false);
    }

    private void assertExpirationTime(YPath path, boolean isPresent) {
        var expirationTime = ytClient.getAttribute(path, CommonAttributes.EXPIRATION_TIME)
                .filter(YTreeNode::isStringNode);

        var attrIsPresent = expirationTime.isPresent();

        if (isPresent) {
            assertTrue(attrIsPresent, "Expiration time is set");
        } else {
            assertFalse(attrIsPresent, "Expiration time is not set");
        }
    }

    @Nonnull
    private SendEmailsStep prepareSendStep() {
        var messageTemplate = emailTemplatesTestHelper.prepareEmailTemplate();
        return sendEmails(messageTemplate.getId());
    }
}
