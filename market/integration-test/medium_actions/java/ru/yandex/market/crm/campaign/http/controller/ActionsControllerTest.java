package ru.yandex.market.crm.campaign.http.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.actions.ActionConfig;
import ru.yandex.market.crm.campaign.domain.actions.ActionStatus;
import ru.yandex.market.crm.campaign.domain.actions.ActionVariant;
import ru.yandex.market.crm.campaign.domain.actions.GetStepPreviewRequest;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.PushPreviewResponse;
import ru.yandex.market.crm.campaign.domain.actions.StepType;
import ru.yandex.market.crm.campaign.domain.actions.WfPermissions;
import ru.yandex.market.crm.campaign.domain.actions.WfPermissions.StepAction;
import ru.yandex.market.crm.campaign.domain.actions.conditions.ActionSegmentGroupPart;
import ru.yandex.market.crm.campaign.domain.actions.conditions.ActionSegmentPart;
import ru.yandex.market.crm.campaign.domain.actions.conditions.SegmentConditionPart;
import ru.yandex.market.crm.campaign.domain.actions.status.BuildSegmentStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.IssueBunchStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.MultifilterStepStatus;
import ru.yandex.market.crm.campaign.domain.actions.status.StepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.BuildSegmentStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.IssueCoinsStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.SendEmailsStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.SendPushesStep;
import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.dto.actions.PlainActionDto;
import ru.yandex.market.crm.campaign.services.actions.PlainActionsDAO;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.campaign.test.utils.BlockTemplateTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailTemplatesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.IdGenerationUtils;
import ru.yandex.market.crm.campaign.test.utils.LoyaltyTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushTemplatesTestHelper;
import ru.yandex.market.crm.core.domain.messages.AbstractPushConf;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.messages.EmailMessageConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.PluggedTable;
import ru.yandex.market.crm.core.domain.messages.PushMessageConf;
import ru.yandex.market.crm.core.domain.segment.Condition;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.domain.sending.conf.InfoBlockConf;
import ru.yandex.market.crm.core.test.utils.YaSenderHelper;
import ru.yandex.market.crm.external.loyalty.Coin;
import ru.yandex.market.crm.http.security.SecurityUtils;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.MobilePlatform;
import ru.yandex.market.crm.mapreduce.domain.push.PushMessageData;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderData;
import ru.yandex.market.crm.mapreduce.util.ActionVars;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.db.Constants;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.services.actions.ActionConstants.SEGMENT_STEP_ID;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.foldByCrypta;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.issueCoins;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.mobileUsersCondition;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.multifilter;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.sendEmails;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.sendPushes;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.variant;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.SubscriptionTypes.ALL;

/**
 * @author apershukov
 */
public class ActionsControllerTest extends AbstractControllerMediumTest {

    private static final long PROMO_ID = 111;
    private static final String PREVIEW_MOCK = "<div/>";

    private static final Coin PROMO_INFO = new Coin(
            0,
            0L,
            "Title",
            "subtitle",
            "FIXED",
            100500.,
            "",
            "",
            null,
            null,
            null,
            "https://yandex.ru/mega-image",
            Collections.emptyMap(),
            "#FFFFFF",
            null,
            false,
            null,
            "EMAIL_COMPANY",
            null,
            null,
            null
    );

    @Inject
    private ActionTestHelper actionTestHelper;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private StepsStatusDAO stepsStatusDAO;

    @Inject
    private PlainActionsDAO actionsDAO;

    @Inject
    private SegmentService segmentService;

    @Inject
    private EmailTemplatesTestHelper emailTemplatesTestHelper;

    @Inject
    private BlockTemplateTestHelper blockTemplateTestHelper;

    @Inject
    private PushTemplatesTestHelper pushTemplatesTestHelper;

    @Inject
    private LoyaltyTestHelper loyaltyTestHelper;

    @Inject
    private YaSenderHelper yaSenderHelper;

    @Inject
    @Named(Constants.DEFAULT_TRANSACTION_TEMPLATE)
    private TransactionTemplate txTemplate;

    @Inject
    private PluggableTablesTestHelper pluggableTablesTestHelper;

    private Segment segment;

    @BeforeEach
    public void setUp() {
        segment = prepareSegment();
    }

    /**
     * Получение акции которая была только что сконфигурирована
     */
    @Test
    public void testGetJustConfiguredAction() throws Exception {
        PlainAction action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE);

        PlainActionDto actionDto = requestAction(action);

        WfPermissions permissions = actionDto.getPermissions();
        assertNotNull(permissions);

        Map<String, Set<StepAction>> stepPermissions = permissions.getSteps();
        assertNotNull(permissions.getSteps());
        assertEquals(1, stepPermissions.size());

        assertEquals(
                ImmutableSet.of(StepAction.START, StepAction.EDIT, StepAction.DELETE, StepAction.MOVE),
                stepPermissions.get(SEGMENT_STEP_ID)
        );
    }

    /**
     * При запросе выполняющейся акции ответ содержит её статус
     */
    @Test
    public void testGetActionWithStageStates() throws Exception {
        ActionStep issueCoinsStep = issueCoins(111L);
        PlainAction action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE, issueCoinsStep);

        // Если сохранять статусы без транзакций они не всегда сразу становятся видны для
        // read-only транзакции в которой акция читается в рамках запроса
        txTemplate.execute(v -> {
            BuildSegmentStepStatus buildSegmentStepStatus = new BuildSegmentStepStatus()
                    .setStageStatus(StageStatus.FINISHED)
                    .setOutputCount(1000);

            stepsStatusDAO.upsert(action.getId(), buildSegmentStepStatus);

            IssueBunchStepStatus issueBunchStepStatus = new IssueBunchStepStatus()
                    .setStepId(issueCoinsStep.getId())
                    .setStageStatus(StageStatus.IN_PROGRESS)
                    .setOutputCount(500);

            stepsStatusDAO.upsert(action.getId(), issueBunchStepStatus);

            return null;
        });

        PlainActionDto actionDto = requestAction(action);

        ActionStatus status = actionDto.getStatus();
        assertNotNull(status);

        Map<String, StepStatus<?>> steps = status.getSteps();
        assertNotNull(steps);
        assertEquals(2, steps.size());

        BuildSegmentStepStatus buildSegmentStepStatus = (BuildSegmentStepStatus) steps.get(SEGMENT_STEP_ID);
        assertNotNull(buildSegmentStepStatus);
        assertEquals(StageStatus.FINISHED, buildSegmentStepStatus.getStageStatus());

        IssueBunchStepStatus issueBunchStepStatus = (IssueBunchStepStatus) steps.get(issueCoinsStep.getId());
        assertNotNull(issueBunchStepStatus);
        assertEquals(StageStatus.IN_PROGRESS, issueBunchStepStatus.getStageStatus());
        assertEquals(500, (int) issueBunchStepStatus.getOutputCount());
    }

    /**
     * Редактировать выполняющуюся акцию запрещено
     */
    @Test
    public void test400OnEditExecutingAction() throws Exception {
        PlainAction action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE);

        BuildSegmentStepStatus buildSegmentStepStatus = new BuildSegmentStepStatus()
                .setStageStatus(StageStatus.IN_PROGRESS);

        stepsStatusDAO.upsert(action.getId(), buildSegmentStepStatus);

        requestEditAssertFail(action);
    }

    /**
     * При попытке сохранить акцию шаги которой имеют дублирующиеся идентификаторы
     * возвращается 400
     */
    @Test
    public void test400OnSaveActionWithDublicatedStepId() throws Exception {
        ActionStep step1 = foldByCrypta();
        ActionStep step2 = foldByCrypta();

        PlainAction action = actionTestHelper.prepareActionWithVariants(segment.getId(), LinkingMode.NONE,
                variant("variant_a", 50, step1),
                variant("variant_b", 50, step2)
        );

        step2.setId(step1.getId());

        requestEditAssertFail(action);
    }

    /**
     * При попытке сохранить акцию у шагов которой не заполнены идентфикаторы
     * возвращается 400
     */
    @Test
    public void test400OnSaveActionWithStepWithoutId() throws Exception {
        PlainAction action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE);

        ActionStep step = foldByCrypta();
        step.setId(null);

        action.getConfig().getVariants().get(0)
                .setSteps(Collections.singletonList(step));

        requestEditAssertFail(action);
    }

    @Test
    public void testDeleteExecutedAction() throws Exception {
        PlainAction action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE);

        BuildSegmentStepStatus buildSegmentStepStatus = new BuildSegmentStepStatus()
                .setStageStatus(StageStatus.FINISHED);

        stepsStatusDAO.upsert(action.getId(), buildSegmentStepStatus);

        mockMvc.perform(delete("/api/actions/{id}", action.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        Assertions.assertFalse(actionsDAO.getAction(action.getId()).isPresent(), "Action is still in DB");
        assertTrue(stepsStatusDAO.getOfAction(action.getId()).isEmpty(), "Action steps status entries left");
    }

    @Test
    public void test400OnDeleteExecutingAction() throws Exception {
        PlainAction action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE);

        BuildSegmentStepStatus buildSegmentStepStatus = new BuildSegmentStepStatus()
                .setStageStatus(StageStatus.IN_PROGRESS);

        stepsStatusDAO.upsert(action.getId(), buildSegmentStepStatus);

        mockMvc.perform(delete("/api/actions/{id}", action.getId()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /**
     * Проверка работы превью для шаблона с единственным информационным блоком
     */
    @Test
    public void testGetSimpleEmailPreview() throws Exception {
        InfoBlockConf infoBlock = blockTemplateTestHelper.prepareInfoBlock("{{data.vars.COINS[0].title}}");
        var template = emailTemplatesTestHelper.prepareEmailTemplate("Subject", infoBlock);

        SendEmailsStep sendEmailsStep = sendEmails(template.getId());

        PlainAction action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE,
                foldByCrypta(),
                issueCoins(PROMO_ID),
                sendEmailsStep
        );

        loyaltyTestHelper.preparePromoInfo(PROMO_ID, PROMO_INFO);

        yaSenderHelper.expectPreview(PREVIEW_MOCK, request -> {
            YaSenderData data = request.getData().getData();
            Map<String, YTreeNode> vars = data.getVars();
            assertNotNull(vars);

            YTreeNode coins = vars.get(ActionVars.COINS);
            assertNotNull(coins);
            assertTrue(coins.isListNode());
            assertEquals(PROMO_INFO.getTitle(), coins.asList().get(0).mapNode().getString("title"));
        });

        requestAndAssertPreview(action, sendEmailsStep, Collections.emptyMap());
    }

    /**
     * В случае если для шага выдачи монет указан параметр requireAuth=true
     * в превью попадает монета, выданная на неавторизованного пользователя
     */
    @Test
    public void testGetSimpleEmailPreviewWithParameters() throws Exception {
        InfoBlockConf infoBlock = blockTemplateTestHelper.prepareInfoBlock("{{data.vars.COINS[0].title}}");
        var template = emailTemplatesTestHelper.prepareEmailTemplate("Subject", infoBlock);

        IssueCoinsStep issueCoinsStep = issueCoins(PROMO_ID);
        SendEmailsStep sendEmailsStep = sendEmails(template.getId());

        PlainAction action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE,
                foldByCrypta(),
                issueCoinsStep,
                sendEmailsStep
        );

        loyaltyTestHelper.preparePromoInfo(PROMO_ID, PROMO_INFO);

        yaSenderHelper.expectPreview(PREVIEW_MOCK, request -> {
            YaSenderData data = request.getData().getData();
            Map<String, YTreeNode> vars = data.getVars();
            assertNotNull(vars);

            YTreeNode coins = vars.get(ActionVars.COINS);
            assertNotNull(coins);
            assertTrue(coins.isListNode());

            ru.yandex.market.crm.mapreduce.domain.loyalty.Coin coin =
                    ru.yandex.market.crm.mapreduce.domain.loyalty.Coin.deserialize(coins.listNode().get(0));

            assertEquals(PROMO_INFO.getTitle(), coin.getTitle());
            assertTrue(coin.isRequireAuth());
            assertNotNull(coin.getActivationUrl());
        });

        Map<String, Map<String, Object>> parameters = Collections.singletonMap(
                issueCoinsStep.getId(), Collections.singletonMap("requireAuth", true)
        );

        requestAndAssertPreview(action, sendEmailsStep, parameters);
    }

    /**
     * Пользовательские переменные из подключаемых таблиц при отображении превью принимают
     * замоканные значения
     */
    @Test
    public void testUserVarsAreAvailableInStepPreview() throws Exception {
        var template = prepareEmailTemplateWithPluggedTable();

        SendEmailsStep sendEmailsStep = sendEmails(template.getId());
        PlainAction action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE, sendEmailsStep);

        yaSenderHelper.expectPreview(PREVIEW_MOCK, request -> {
            YaSenderData data = request.getData().getData();

            Map<String, Map<String, YTreeNode>> expected = Map.of(
                    "table", Map.of("saved_money", YTree.stringNode("saved_money"))
            );

            assertEquals(expected, data.getUVars());
        });

        requestAndAssertPreview(action, sendEmailsStep, Map.of());
    }

    /**
     * Проверка простейшего кейса получения первью push-оповещения
     */
    @Test
    public void testGetSimplePushTemplatePreview() throws Exception {
        var template = pushTemplatesTestHelper.prepare();

        SendPushesStep sendPushesStep = sendPushes(template.getId());
        PlainAction action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE, sendPushesStep);

        PushMessageData data = requestPushStepPreview(action, sendPushesStep);
        AbstractPushConf pushConf = template.getConfig().getPushConfigs().get(MobilePlatform.ANDROID);

        assertEquals(pushConf.getTitle(), data.getTitle());
        assertEquals(pushConf.getText(), data.getText());
    }

    /**
     * Пользовательские переменные из подключаемых таблиц при отображении превью принимают
     * замоканные значения
     */
    @Test
    public void testGetPushTemplateWithUserVarsPreview() throws Exception {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable();

        PushMessageConf config = new PushMessageConf();
        AndroidPushConf pushConf = new AndroidPushConf();
        pushConf.setTitle("Title");
        pushConf.setText("${u_vars.table.saved_money}");
        config.setPushConfigs(Map.of(pushConf.getPlatform(), pushConf));
        config.setPluggedTables(List.of(new PluggedTable(pluggableTable.getId(), "table")));

        var template = pushTemplatesTestHelper.prepare(config);

        SendPushesStep sendPushesStep = sendPushes(template.getId());
        PlainAction action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE, sendPushesStep);

        PushMessageData data = requestPushStepPreview(action, sendPushesStep);
        assertEquals(pushConf.getTitle(), data.getTitle());
        assertEquals("saved_money", data.getText());
    }

    /**
     * Полностью выполненная акция, содержащая шаг-мультифильтр и шаг отправки пушей
     * допускает добавление нового шага в конец варианта.
     * <p>
     * Тест проверяет то что шаг мультифильтрации, который небыл изменен при
     * редактировании не считается измененным при сохранении нового конфига акции.
     */
    @Test
    public void testItsPossibleToAddNewStepAfterExecutedMultifilterStep() throws Exception {
        ActionSegmentPart segmentConfig = new ActionSegmentGroupPart()
                .setCondition(Condition.ALL)
                .addPart(

                        new SegmentConditionPart()
                                .setSegmentCondition(mobileUsersCondition())
                );

        ActionStep multifilterStep = multifilter(segmentConfig);
        ActionStep issueCoinsStep = issueCoins(111);

        PlainAction action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE,
                multifilterStep,
                issueCoinsStep
        );

        BuildSegmentStepStatus buildSegmentStepStatus = new BuildSegmentStepStatus()
                .setStageStatus(StageStatus.FINISHED);

        stepsStatusDAO.upsert(action.getId(), buildSegmentStepStatus);

        MultifilterStepStatus multifilterStepStatus = new MultifilterStepStatus()
                .setStepId(multifilterStep.getId())
                .setStageStatus(StageStatus.FINISHED);

        stepsStatusDAO.upsert(action.getId(), multifilterStepStatus);

        IssueBunchStepStatus issueBunchStepStatus = new IssueBunchStepStatus()
                .setStepId(issueCoinsStep.getId())
                .setStageStatus(StageStatus.FINISHED);

        stepsStatusDAO.upsert(action.getId(), issueBunchStepStatus);

        PlainActionDto dto = requestAction(action.getId());

        dto.getConfig().getVariants().get(0).getSteps().add(foldByCrypta());

        requestEdit(dto)
                .andExpect(status().isOk());
    }

    @Test
    public void test404OnCopyAbsentAction() throws Exception {
        String absentId = IdGenerationUtils.dateTimeId();

        requestCopyActionAndPrint(absentId, new PlainActionDto())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", equalTo("Action with id '" + absentId + "' not found")));
    }

    @Test
    public void test400OnCopyActionWithExistingId() throws Exception {
        PlainAction original = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE);

        PlainActionDto changeDto = new PlainActionDto();
        changeDto.setId(original.getId());

        requestCopyActionAndPrint(original.getId(), changeDto)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", equalTo("Plain action with same id exists")));
    }

    @Test
    public void test422OnCopyActionWithNullColor() throws Exception {
        PlainAction original = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE);

        PlainActionDto changeDto = new PlainActionDto();
        changeDto.setId(IdGenerationUtils.dateTimeId());

        requestCopyActionAndPrint(original.getId(), changeDto)
                .andDo(print())
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void test422OnCopyActionWithEmptyName() throws Exception {
        PlainAction original = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE);

        PlainActionDto changeDto = new PlainActionDto();
        changeDto.setId(IdGenerationUtils.dateTimeId());

        requestCopyActionAndPrint(original.getId(), changeDto)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", equalTo("Plain action field 'name' shouldn't be empty")));
    }

    @Test
    public void testCopiedActionIsReallyCreated() throws Exception {
        PlainAction original = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE);

        String copyId = IdGenerationUtils.dateTimeId();
        PlainActionDto changeDto = createChangeDto(original, copyId);

        PlainActionDto result = requestCopyAction(original.getId(), changeDto);
        PlainActionDto copy = requestAction(copyId);

        result.setCreationTime(null);
        copy.setCreationTime(null);

        MatcherAssert.assertThat(jsonSerializer.writeObjectAsString(copy),
                equalTo(jsonSerializer.writeObjectAsString(result)));
    }

    @Test
    public void testCopiedActionHasCorrectMainProperties() throws Exception {
        PlainAction original = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE);

        String copyId = IdGenerationUtils.dateTimeId();
        PlainActionDto changeDto = createChangeDto(original, copyId);

        PlainActionDto copyDto = requestCopyAction(original.getId(), changeDto);

        MatcherAssert.assertThat(copyDto.getId(), equalTo(copyId));
        MatcherAssert.assertThat(copyDto.getName(), equalTo(changeDto.getName()));
        MatcherAssert.assertThat(copyDto.getCreationTime(), notNullValue());
        MatcherAssert.assertThat(copyDto.getAuthor(), equalTo(SecurityUtils.getLogin()));
        MatcherAssert.assertThat(copyDto.getCampaign().getId(), is(original.getCampaignId()));
    }

    @Test
    public void testCopiedActionHasEmptyStatus() throws Exception {
        PlainAction original = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE);

        BuildSegmentStepStatus originalStepStatus = new BuildSegmentStepStatus()
                .setStageStatus(StageStatus.FINISHED)
                .setOutputCount(1000);
        stepsStatusDAO.upsert(original.getId(), originalStepStatus);

        PlainActionDto changeDto = createChangeDto(original, IdGenerationUtils.dateTimeId());

        PlainActionDto copyDto = requestCopyAction(original.getId(), changeDto);

        MatcherAssert.assertThat(copyDto.getStatus().getSteps().size(), is(0));
    }

    @Test
    public void testCopiedActionHasCorrectConfig() throws Exception {
        SendEmailsStep originalSendEmailsStep = new SendEmailsStep();
        originalSendEmailsStep.setId(StepType.SEND_EMAILS.name());
        originalSendEmailsStep.setSubscriptionType(ALL);

        PlainAction original = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE,
                new BuildSegmentStep(), originalSendEmailsStep);

        PlainActionDto changeDto = createChangeDto(original, IdGenerationUtils.dateTimeId());

        PlainActionDto copyDto = requestCopyAction(original.getId(), changeDto);

        ActionConfig copyConfig = copyDto.getConfig();

        MatcherAssert.assertThat(copyConfig.getVariants().size(), is(1));

        ActionVariant copyVariant = copyConfig.getVariants().get(0);

        MatcherAssert.assertThat(copyVariant.getId(), equalTo(copyDto.getId() + "_a"));

        MatcherAssert.assertThat(copyVariant.getSteps().size(), is(2));
        MatcherAssert.assertThat(copyVariant.getSteps().get(0), instanceOf(BuildSegmentStep.class));
        MatcherAssert.assertThat(copyVariant.getSteps().get(1), instanceOf(SendEmailsStep.class));

        SendEmailsStep copySendEmailsStep = (SendEmailsStep) copyVariant.getSteps().get(1);

        MatcherAssert.assertThat(copySendEmailsStep.getUtmCampaign(), equalTo(copyVariant.getId()));
    }

    /**
     * Нельзя сохранить акцию в конфигурации которой присутствуют два шага с одинаковым id
     */
    @Test
    void test400OnSavingActionWithDuplicatedStepIds() throws Exception {
        var action = actionTestHelper.prepareAction(segment.getId(), LinkingMode.NONE);

        var stepId = "step_id";

        var step1 = issueCoins(111L);
        step1.setId(stepId);

        var step2 = issueCoins(222L);
        step2.setId(stepId);

        var newConfig = new ActionConfig();
        newConfig.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        newConfig.setVariants(List.of(
                variant("a", 40, step1),
                variant("b", 40, step2)
        ));

        action.setConfig(newConfig);
        requestEditAssertFail(action);
    }

    private PlainActionDto createChangeDto(PlainAction original, String changeId) {
        PlainActionDto changeDto = new PlainActionDto();
        changeDto.setId(changeId);
        changeDto.setName(original.getName() + " (Copy)");

        return changeDto;
    }

    private PushMessageData requestPushStepPreview(PlainAction action, SendPushesStep sendPushesStep) throws Exception {
        MvcResult result = mockMvc.perform(
                post(
                        "/api/actions/{actionId}/steps/{stepId}/get_push_preview",
                        action.getId(),
                        sendPushesStep.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                jsonSerializer.writeObjectAsString(
                                        new GetStepPreviewRequest(Map.of(), action.getConfig())
                                )
                        )
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        PushPreviewResponse response = jsonDeserializer.readObject(
                PushPreviewResponse.class,
                result.getResponse().getContentAsString()
        );

        return response.getMessages().get(MobilePlatform.ANDROID);
    }

    private MessageTemplate<EmailMessageConf> prepareEmailTemplateWithPluggedTable() {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable();

        BannerBlockConf bannerBlock = emailTemplatesTestHelper.prepareBannerBlock(
                block -> block.setText("{{data.u_vars.table.saved_money}}")
        );

        EmailMessageConf config = new EmailMessageConf();
        config.setTemplate(emailTemplatesTestHelper.prepareMessageTemplate().getId());
        config.setBlocks(List.of(bannerBlock));
        config.setSubject("Test Email");
        config.setPluggedTables(List.of(new PluggedTable(pluggableTable.getId(), "table")));

        return emailTemplatesTestHelper.prepareEmailTemplate(config);
    }

    private void requestAndAssertPreview(PlainAction action,
                                         SendEmailsStep sendEmailsStep,
                                         Map<String, Map<String, Object>> parameters) throws Exception {
        String html = mockMvc.perform(post(
                "/api/actions/{actionId}/steps/{stepId}/get_email_preview",
                action.getId(),
                sendEmailsStep.getId()
        )
                .contentType("application/json")
                .content(
                        jsonSerializer.writeObjectAsString(
                                new GetStepPreviewRequest(parameters, action.getConfig())
                        )
                ))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString();

        assertEquals(PREVIEW_MOCK, html);
    }

    private ResultActions requestCopyActionAndPrint(String id, PlainActionDto changeDto) throws Exception {
        return mockMvc.perform(
                post("/api/actions/{id}/copy", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSerializer.writeObjectAsString(changeDto))
        )
                .andDo(print());
    }

    private String requestCopyActionContent(String id, PlainActionDto changeDto) throws Exception {
        return requestCopyActionAndPrint(id, changeDto)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private PlainActionDto requestCopyAction(String id, PlainActionDto changeDto) throws Exception {
        String content = requestCopyActionContent(id, changeDto);
        return jsonDeserializer.readObject(PlainActionDto.class, content);
    }

    private String requestActionContent(String id) throws Exception {
        return mockMvc.perform(get("/api/actions/{id}", id))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private PlainActionDto requestAction(PlainAction action) throws Exception {
        return requestAction(action.getId());
    }

    private PlainActionDto requestAction(String id) throws Exception {
        String content = requestActionContent(id);
        return jsonDeserializer.readObject(PlainActionDto.class, content);
    }

    private void requestEditAssertFail(PlainAction action) throws Exception {
        PlainActionDto body = new PlainActionDto();
        body.setId(action.getId());
        body.setName(action.getName());
        body.setConfig(action.getConfig());

        requestEdit(body)
                .andExpect(status().isBadRequest());
    }

    private ResultActions requestEdit(PlainActionDto dto) throws Exception {
        return mockMvc.perform(put("/api/actions/{id}", dto.getId())
                .contentType("application/json")
                .content(jsonSerializer.writeObjectAsString(dto)))
                .andDo(print());
    }

    private Segment prepareSegment() {
        Segment segment = segment(plusFilter());
        segment = segmentService.addSegment(segment);
        return segment;
    }
}
