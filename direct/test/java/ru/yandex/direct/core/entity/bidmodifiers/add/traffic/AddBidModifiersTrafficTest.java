package ru.yandex.direct.core.entity.bidmodifiers.add.traffic;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.jooq.Record1;
import org.jooq.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierExpression;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierExpressionAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafficAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionLiteral;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.bidmodifiers.repository.mapper.Common.EXPRESSION_CONDITION_JSON_TYPE;
import static ru.yandex.direct.core.entity.bidmodifiers.repository.mapper.Common.conditionFromDb;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getExternalId;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getRealId;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultTrafficAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyTrafficModifier;
import static ru.yandex.direct.dbschema.ppc.Tables.EXPRESSION_MULTIPLIER_VALUES;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

/**
 * Тест на добавление универсальных корректировок (пока только применительно к типу TRAFFIC).
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Сценарии добавления корректировок по пробкам")
public class AddBidModifiersTrafficTest {
    private static final String EXPRESSION_MULTIPLIER_FILE = "classpath:///bidmodifiers/expression_condition.json";

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private CampaignSteps campaignSteps;
    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private Steps steps;

    private AdGroupInfo adGroup;

    private void enableFeatureForClient(ClientId clientId, FeatureName featureName) {
        steps.featureSteps().addClientFeature(clientId, featureName, true);
    }

    private void disableFeatureForClient(ClientId clientId, FeatureName featureName) {
        steps.featureSteps().addClientFeature(clientId, featureName, false);
    }

    @Before
    public void before() {
        CampaignInfo campaign = campaignSteps.createActiveCpmBannerCampaign();
        adGroup = adGroupSteps.createActiveCpmOutdoorAdGroup(campaign);
    }

    @Test
    @Description("Добавляем одну корректировку и проверяем, что она после этого получается методом get")
    public void addOneTrafficModifierTest() {
        List<BidModifierExpressionAdjustment> adjustments = createDefaultTrafficAdjustments();
        MassResult<List<Long>> result = bidModifierService.add(
                singletonList(
                        createEmptyTrafficModifier()
                                .withAdGroupId(adGroup.getAdGroupId())
                                .withExpressionAdjustments(adjustments)
                ), adGroup.getClientId(), adGroup.getUid());
        List<BidModifier> gotModifiers =
                bidModifierService.getByAdGroupIds(adGroup.getClientId(), singleton(adGroup.getAdGroupId()), emptySet(),
                        singleton(BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER),
                        singleton(BidModifierLevel.ADGROUP), adGroup.getUid());
        Long adjustmentId = ((BidModifierExpression) gotModifiers.get(0)).getExpressionAdjustments().get(0).getId();

        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(result.getResult().get(0).getResult()).hasSize(1);
            softly.assertThat(result.getResult().get(0).getResult()).is(matchedBy(contains(
                    equalTo(getExternalId(adjustmentId, BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER)))));
            softly.assertThat(gotModifiers.get(0)).is(matchedBy(
                    expressionModifierWithProperties(
                            adGroup,
                            adjustments.get(0).getPercent(),
                            adjustments.get(0).getCondition(),
                            true
                    )));
        });
    }

    private Matcher<BidModifier> expressionModifierWithProperties(
            AdGroupInfo adGroup, int percent, List<List<BidModifierExpressionLiteral>> condition, boolean enabled) {
        return allOf(
                hasProperty("campaignId", equalTo(adGroup.getCampaignId())),
                hasProperty("adGroupId", equalTo(adGroup.getAdGroupId())),
                hasProperty("enabled", equalTo(enabled)),
                hasProperty("expressionAdjustments", contains(
                        allOf(
                                hasProperty("percent", equalTo(percent)),
                                hasProperty("condition", equalTo(condition))
                        )
                ))
        );
    }

    @Test
    @Description("Добавляем две корректировки и проверяем, как они разложились в БД")
    public void addTwoTrafficModifiersDbStateTest() {
        BidModifierExpressionAdjustment adjustment1 = new BidModifierTrafficAdjustment()
                .withCondition(singletonList(singletonList(
                        new BidModifierExpressionLiteral()
                                .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                                .withOperation(BidModifierExpressionOperator.EQ)
                                .withValueString("0")))).withPercent(110);
        BidModifierExpressionAdjustment adjustment2 = new BidModifierTrafficAdjustment()
                .withCondition(ImmutableList.of(
                        ImmutableList.of(
                                new BidModifierExpressionLiteral()
                                        .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                                        .withOperation(BidModifierExpressionOperator.EQ)
                                        .withValueString("1"),
                                new BidModifierExpressionLiteral()
                                        .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                                        .withOperation(BidModifierExpressionOperator.EQ)
                                        .withValueString("2")
                        ))).withPercent(120);
        List<BidModifierExpressionAdjustment> adjustments = Lists.newArrayList(adjustment1, adjustment2);
        MassResult<List<Long>> result = bidModifierService.add(
                Lists.newArrayList(
                        createEmptyTrafficModifier()
                                .withAdGroupId(adGroup.getAdGroupId())
                                .withExpressionAdjustments(adjustments)
                ), adGroup.getClientId(), adGroup.getUid());

        List<BidModifier> gotModifiers =
                bidModifierService.getByAdGroupIds(adGroup.getClientId(), singleton(adGroup.getAdGroupId()),
                        singleton(adGroup.getCampaignId()),
                        singleton(BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER),
                        singleton(BidModifierLevel.ADGROUP), adGroup.getUid());

        List<BidModifierExpressionAdjustment> adjustmentsSaved =
                ((BidModifierExpression) gotModifiers.get(0)).getExpressionAdjustments();

        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(adjustmentsSaved).is(matchedBy(allOf(hasSize(2),
                    containsInAnyOrder(
                            allOf(
                                    hasProperty("id", equalTo(getRealId(result.get(0).getResult().get(0)))),
                                    hasProperty("percent", equalTo(adjustment1.getPercent())),
                                    hasProperty("condition", equalTo(adjustment1.getCondition()))
                            ),
                            allOf(
                                    hasProperty("id", equalTo(getRealId(result.get(0).getResult().get(1)))),
                                    hasProperty("percent", equalTo(adjustment2.getPercent())),
                                    hasProperty("condition", equalTo(adjustment2.getCondition()))
                            )
                    ))));
        });
    }

    @Test
    @Description("Добавляем корректировку и проверяем JSON условий, который был сохранён в БД")
    public void trafficModifiersDbConditionJsonTest() {
        BidModifierExpressionAdjustment adjustment = new BidModifierTrafficAdjustment()
                .withCondition(singletonList(singletonList(
                        new BidModifierExpressionLiteral()
                                .withParameter(BidModifierExpressionParameter.TRAFFIC_JAM)
                                .withOperation(BidModifierExpressionOperator.EQ)
                                .withValueString("1")))).withPercent(110);
        List<BidModifierExpressionAdjustment> adjustments = Lists.newArrayList(adjustment);

        MassResult<List<Long>> result = bidModifierService.add(Lists.newArrayList(
                createEmptyTrafficModifier()
                        .withAdGroupId(adGroup.getAdGroupId())
                        .withExpressionAdjustments(adjustments)), adGroup.getClientId(), adGroup.getUid());

        Result<Record1<String>> records =
                dslContextProvider.ppc(adGroup.getShard()).select(EXPRESSION_MULTIPLIER_VALUES.CONDITION_JSON)
                        .from(EXPRESSION_MULTIPLIER_VALUES)
                        .where(EXPRESSION_MULTIPLIER_VALUES.EXPRESSION_MULTIPLIER_VALUE_ID
                                .in(singleton(getRealId(result.get(0).getResult().get(0))))).fetch();

        String actualConditionJsonFromDb = (String) records.get(0).getValue(0);
        String expectedConditionJsonFromDb = LiveResourceFactory.get(EXPRESSION_MULTIPLIER_FILE).getContent();

        // Десериализуем оба json и проверяем, что они одинаковы
        List<List<BidModifierExpressionLiteral>> actual =
                conditionFromDb(JsonUtils.fromJson(actualConditionJsonFromDb, EXPRESSION_CONDITION_JSON_TYPE));
        List<List<BidModifierExpressionLiteral>> expected =
                conditionFromDb(JsonUtils.fromJson(expectedConditionJsonFromDb, EXPRESSION_CONDITION_JSON_TYPE));

        assertEquals("получен ожидаемый condition_json", expected, actual);
    }
}
