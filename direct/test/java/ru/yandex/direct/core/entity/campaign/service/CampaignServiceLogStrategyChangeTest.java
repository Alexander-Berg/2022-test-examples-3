package ru.yandex.direct.core.entity.campaign.service;

import java.time.LocalDateTime;
import java.util.List;

import jdk.jfr.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.StrategyChangeLogRecord;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.utils.JsonUtils;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.StatusBsSynced.YES;
import static ru.yandex.direct.core.entity.campaign.StrategyChangeLogRecord.LOG_STRATEGY_CHANGE_CMD_NAME;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка логирования факта смены стратегии на кампании")
public class CampaignServiceLogStrategyChangeTest {
//TODO: упростить тест по аналогии с CampaignServiceLogDayBudgetChangeTest

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private StrategyTranslationService strategyTranslationService;

    private CampaignInfo campaignInfo;
    private Long campaignId;
    TextCampaign campaign;

    @Before
    public void before() {
        LocalDateTime lastChange = LocalDateTime.now().minus(1, HOURS).truncatedTo(SECONDS);
        campaignInfo = campaignSteps.createCampaign(
                activeTextCampaign(null, null)
                        .withStatusBsSynced(YES)
                        .withLastChange(lastChange));
        campaignId = campaignInfo.getCampaignId();
        List<? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(),
                List.of(campaignId));
        List<TextCampaign> textCampaigns = mapList(typedCampaigns, TextCampaign.class::cast);
        campaign = textCampaigns.get(0);
    }

    private void checkStrategiesNames(StrategyChangeLogRecord logRecord, StrategyName correctOldName,
                                      StrategyName correctNewName) {

        DbStrategyHuman newStrategy = JsonUtils.fromJson(logRecord.getParam().get("new").toString(),
                DbStrategyHuman.class);
        DbStrategyHuman oldStrategy = JsonUtils.fromJson(logRecord.getParam().get("old").toString(),
                DbStrategyHuman.class);

        assertThat(logRecord.getParam().get("cid"), is(campaignId));
        assertThat(oldStrategy.getStrategyName(), is(correctOldName));
        assertThat(newStrategy.getStrategyName(), is(correctNewName));

        assertThat(oldStrategy.getHumanName(), is(strategyTranslationService.getRussianTranslation(correctOldName)));
        assertThat(newStrategy.getHumanName(), is(strategyTranslationService.getRussianTranslation(correctNewName)));
    }

    @Test
    public void loggingNoChangeTest() {
        ModelChanges<TextCampaignWithCustomStrategy> campaignModelChanges = new ModelChanges<>(campaignId,
                TextCampaignWithCustomStrategy.class);
        AppliedChanges<TextCampaignWithCustomStrategy> campaignAppliedChanges = campaignModelChanges.applyTo(campaign);

        StrategyChangeLogRecord logRecord = campaignService.getStrategyLogRecord(campaignAppliedChanges,
                campaignInfo.getUid());

        assertThat(
                logRecord,
                allOf(
                        hasProperty("path", equalTo(LOG_STRATEGY_CHANGE_CMD_NAME)),
                        hasProperty("cids", equalTo(List.of(campaignId))),
                        hasProperty("operatorUid", equalTo(campaignInfo.getUid()))));
        checkStrategiesNames(logRecord, StrategyName.DEFAULT_, StrategyName.DEFAULT_);
    }

    @Test
    public void loggingChangeStrategyFromDefaultTest() {
        ModelChanges<TextCampaignWithCustomStrategy> campaignModelChanges = new ModelChanges<>(campaignId,
                TextCampaignWithCustomStrategy.class);
        DbStrategy newStrategy = new DbStrategy();
        newStrategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP);
        campaignModelChanges.process(newStrategy, TextCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<TextCampaignWithCustomStrategy> campaignAppliedChanges = campaignModelChanges.applyTo(campaign);

        StrategyChangeLogRecord logRecord = campaignService.getStrategyLogRecord(campaignAppliedChanges,
                campaignInfo.getUid());

        assertThat(
                logRecord,
                allOf(
                        hasProperty("path", equalTo(LOG_STRATEGY_CHANGE_CMD_NAME)),
                        hasProperty("cids", equalTo(List.of(campaignId))),
                        hasProperty("operatorUid", equalTo(campaignInfo.getUid()))));
        checkStrategiesNames(logRecord, StrategyName.DEFAULT_, StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP);
    }

    @Test
    public void loggingChangesStrategyFromNotDefaultTest() {
        ModelChanges<TextCampaignWithCustomStrategy> campaignModelChanges = new ModelChanges<>(campaignId,
                TextCampaignWithCustomStrategy.class);
        DbStrategy firstStrategy = new DbStrategy();
        firstStrategy.setStrategyName(StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP);
        campaignModelChanges.process(firstStrategy, TextCampaignWithCustomStrategy.STRATEGY);
        campaignModelChanges.applyTo(campaign);

        DbStrategy secondStrategy = new DbStrategy();
        secondStrategy.setStrategyName(StrategyName.MIN_PRICE);
        campaignModelChanges.process(secondStrategy, TextCampaignWithCustomStrategy.STRATEGY);
        AppliedChanges<TextCampaignWithCustomStrategy> campaignAppliedChanges = campaignModelChanges.applyTo(campaign);

        StrategyChangeLogRecord logRecord = campaignService.getStrategyLogRecord(campaignAppliedChanges,
                campaignInfo.getUid());

        assertThat(
                logRecord,
                allOf(
                        hasProperty("path", equalTo(LOG_STRATEGY_CHANGE_CMD_NAME)),
                        hasProperty("cids", equalTo(List.of(campaignId))),
                        hasProperty("operatorUid", equalTo(campaignInfo.getUid()))));
        checkStrategiesNames(logRecord, StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP, StrategyName.MIN_PRICE);

    }
}
