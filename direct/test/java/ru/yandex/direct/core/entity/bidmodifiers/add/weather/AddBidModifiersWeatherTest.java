package ru.yandex.direct.core.entity.bidmodifiers.add.weather;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeather;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherLiteral;
import ru.yandex.direct.core.entity.bidmodifier.OperationType;
import ru.yandex.direct.core.entity.bidmodifier.WeatherType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.container.CampaignIdAndAdGroupIdPair;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getExternalId;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getRealId;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientWeatherAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientWeatherAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultWeatherAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientWeatherModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyWeatherModifier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Позитивные сценарии добавления погодных корректировок ставок")
public class AddBidModifiersWeatherTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private CampaignSteps campaignSteps;
    @Autowired
    private AdGroupSteps adGroupSteps;

    private AdGroupInfo adGroup;

    private AdGroupInfo audioAdGroup;

    @Before
    public void before() {
        CampaignInfo campaign = campaignSteps.createActiveCpmBannerCampaign();
        adGroup = adGroupSteps.createActiveCpmBannerAdGroup(campaign);
        audioAdGroup = adGroupSteps.createActiveCpmAudioAdGroup();
    }

    @Test
    @Description("Добавляем одну корректировку корректировку по погоде на Аудиогруппу")
    public void addWeatherModifierToCpmAudioAdGroup() {
        List<BidModifierWeatherAdjustment> weatherAdjustments = createDefaultWeatherAdjustments();
        MassResult<List<Long>> result = bidModifierService.add(
                singletonList(
                        createEmptyWeatherModifier()
                                .withAdGroupId(audioAdGroup.getAdGroupId())
                                .withWeatherAdjustments(weatherAdjustments)
                ), audioAdGroup.getClientId(), audioAdGroup.getUid());

        List<BidModifier> gotModifiers =
                bidModifierService.getByAdGroupIds(audioAdGroup.getClientId(), singleton(audioAdGroup.getAdGroupId()),
                        singleton(audioAdGroup.getCampaignId()),
                        singleton(BidModifierType.WEATHER_MULTIPLIER),
                        singleton(BidModifierLevel.ADGROUP), audioAdGroup.getUid());

        List<BidModifierWeatherAdjustment> adjustmentsSaved =
                ((BidModifierWeather) gotModifiers.get(0)).getWeatherAdjustments();

        assertThat(adjustmentsSaved).hasSize(1);
        assertThat(gotModifiers).hasSize(1);
        assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    @Description("Добавляем одну корректировку по погоде на Аудиогруппу c помощью метода replace")
    public void addWeatherModifierToCpmAudioAdGroupWithReplace() {
        List<BidModifierWeatherAdjustment> weatherAdjustments = createDefaultWeatherAdjustments();

        bidModifierService.replaceModifiers(
                audioAdGroup.getClientId(),
                audioAdGroup.getClientInfo().getUid(),
                singletonList(
                        createEmptyWeatherModifier()
                                .withAdGroupId(audioAdGroup.getAdGroupId())
                                .withWeatherAdjustments(weatherAdjustments)
                                .withCampaignId(audioAdGroup.getCampaignId())
                ),
                singleton(
                        new CampaignIdAndAdGroupIdPair()
                                .withCampaignId(audioAdGroup.getCampaignId())
                                .withAdGroupId(audioAdGroup.getAdGroupId())
                )
        );

        List<BidModifier> gotModifiers =
                bidModifierService.getByAdGroupIds(
                        audioAdGroup.getClientId(),
                        singleton(audioAdGroup.getAdGroupId()),
                        singleton(audioAdGroup.getCampaignId()),
                        singleton(BidModifierType.WEATHER_MULTIPLIER),
                        singleton(BidModifierLevel.ADGROUP),
                        audioAdGroup.getUid()
                );

        List<BidModifierWeatherAdjustment> adjustmentsSaved =
                ((BidModifierWeather) gotModifiers.get(0)).getWeatherAdjustments();

        assertThat(adjustmentsSaved).hasSize(1);
        assertThat(gotModifiers).hasSize(1);
    }

    @Test
    @Description("Добавляем одну корректировку и проверяем, что она после этого получается методом get")
    public void addOneWeatherModifierTest() {
        List<BidModifierWeatherAdjustment> weatherAdjustments = createDefaultClientWeatherAdjustments();
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientWeatherModifier()
                                .withAdGroupId(adGroup.getAdGroupId())
                                .withWeatherAdjustments(weatherAdjustments)));
        List<BidModifier> gotModifiers =
                bidModifierService.getByAdGroupIds(adGroup.getClientId(), singleton(adGroup.getAdGroupId()), emptySet(),
                        singleton(BidModifierType.WEATHER_MULTIPLIER),
                        singleton(BidModifierLevel.ADGROUP), adGroup.getUid());
        Long adjustmentId = ((BidModifierWeather) gotModifiers.get(0)).getWeatherAdjustments().get(0).getId();

        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(result.getResult().get(0).getResult()).hasSize(1);
            softly.assertThat(result.getResult().get(0).getResult()).is(matchedBy(contains(
                    equalTo(getExternalId(adjustmentId, BidModifierType.WEATHER_MULTIPLIER)))));
            softly.assertThat(gotModifiers.get(0)).is(matchedBy(
                    weatherModifierWithProperties(
                            adGroup,
                            weatherAdjustments.get(0).getPercent(),
                            weatherAdjustments.get(0).getExpression(),
                            true
                    )));
        });
    }

    private Matcher<BidModifier> weatherModifierWithProperties(
            AdGroupInfo adGroup, int percent, List<List<BidModifierWeatherLiteral>> expression, boolean enabled) {
        return allOf(
                hasProperty("campaignId", equalTo(adGroup.getCampaignId())),
                hasProperty("adGroupId", equalTo(adGroup.getAdGroupId())),
                hasProperty("enabled", equalTo(enabled)),
                hasProperty("weatherAdjustments", contains(
                        allOf(
                                hasProperty("percent", equalTo(percent)),
                                hasProperty("expression", equalTo(expression))
                        )
                ))
        );
    }

    @Test
    @Description("Добавляем две корректировки и проверяем, как они разложились в БД")
    public void addTwoWeatherModifiersDbStateTest() {
        BidModifierWeatherAdjustment adjustment1 = createDefaultClientWeatherAdjustment()
                .withExpression(singletonList(singletonList(
                        new BidModifierWeatherLiteral().withParameter(WeatherType.CLOUDNESS).withOperation(
                                OperationType.EQ).withValue(50)))).withPercent(110);
        BidModifierWeatherAdjustment adjustment2 = createDefaultClientWeatherAdjustment()
                .withExpression(ImmutableList.of(
                        asList(new BidModifierWeatherLiteral().withValue(25).withOperation(OperationType.GE)
                                        .withParameter(WeatherType.TEMP),
                                new BidModifierWeatherLiteral().withValue(45).withOperation(OperationType.LE)
                                        .withParameter(WeatherType.TEMP)))).withPercent(120);
        List<BidModifierWeatherAdjustment> adjustments = Lists.newArrayList(adjustment1, adjustment2);
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientWeatherModifier()
                                .withAdGroupId(adGroup.getAdGroupId())
                                .withWeatherAdjustments(adjustments)));

        List<BidModifier> gotModifiers =
                bidModifierService.getByAdGroupIds(adGroup.getClientId(), singleton(adGroup.getAdGroupId()),
                        singleton(adGroup.getCampaignId()),
                        singleton(BidModifierType.WEATHER_MULTIPLIER),
                        singleton(BidModifierLevel.ADGROUP), adGroup.getUid());

        List<BidModifierWeatherAdjustment> adjustmentsSaved =
                ((BidModifierWeather) gotModifiers.get(0)).getWeatherAdjustments();

        assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(adjustmentsSaved).is(matchedBy(allOf(hasSize(2),
                    containsInAnyOrder(
                            allOf(
                                    hasProperty("id", equalTo(getRealId(result.get(0).getResult().get(0)))),
                                    hasProperty("percent", equalTo(adjustment1.getPercent())),
                                    hasProperty("expression", equalTo(adjustment1.getExpression()))
                            ),
                            allOf(
                                    hasProperty("id", equalTo(getRealId(result.get(0).getResult().get(1)))),
                                    hasProperty("percent", equalTo(adjustment2.getPercent())),
                                    hasProperty("expression", equalTo(adjustment2.getExpression()))
                            )
                    ))));
        });
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, adGroup.getClientId(), adGroup.getUid());
    }
}
