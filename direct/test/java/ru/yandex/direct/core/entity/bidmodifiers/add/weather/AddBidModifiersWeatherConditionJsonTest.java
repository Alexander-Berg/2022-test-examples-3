package ru.yandex.direct.core.entity.bidmodifiers.add.weather;

import java.util.List;

import com.google.common.collect.Lists;
import org.jooq.Record1;
import org.jooq.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherLiteral;
import ru.yandex.direct.core.entity.bidmodifier.OperationType;
import ru.yandex.direct.core.entity.bidmodifier.WeatherType;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.dbschema.ppc.Tables;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getRealId;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientWeatherAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientWeatherModifier;
import static ru.yandex.direct.dbschema.ppc.tables.WeatherMultiplierValues.WEATHER_MULTIPLIER_VALUES;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Позитивные сценарии добавления погодных корректировок ставок")
public class AddBidModifiersWeatherConditionJsonTest {
    private static final String WEATHER_MULTIPLIER_FILE = "classpath:///bidmodifiers/weather_condition_json.json";
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private CampaignSteps campaignSteps;
    @Autowired
    private AdGroupSteps adGroupSteps;

    private AdGroupInfo adGroup;

    @Before
    public void before() {
        CampaignInfo campaign = campaignSteps.createActiveCpmBannerCampaign();
        adGroup = adGroupSteps.createActiveCpmBannerAdGroup(campaign);
    }

    @Test
    @Description("Добавляем корректировку и проверяем JSON условий, который был сохранён в БД")
    public void weatherModifiersDbConditionJsonTest() {
        BidModifierWeatherAdjustment adjustment = createDefaultClientWeatherAdjustment()
                .withExpression(singletonList(singletonList(
                        new BidModifierWeatherLiteral().withParameter(WeatherType.CLOUDNESS).withOperation(
                                OperationType.EQ).withValue(50)))).withPercent(110);
        List<BidModifierWeatherAdjustment> adjustments = Lists.newArrayList(adjustment);

        MassResult<List<Long>> result = bidModifierService.add(Lists.newArrayList(
                createEmptyClientWeatherModifier()
                        .withAdGroupId(adGroup.getAdGroupId())
                        .withEnabled(true)
                        .withWeatherAdjustments(adjustments)), adGroup.getClientId(), adGroup.getUid());

        Result<Record1<String>> records =
                dslContextProvider.ppc(adGroup.getShard()).select(Tables.WEATHER_MULTIPLIER_VALUES.CONDITION_JSON)
                        .from(WEATHER_MULTIPLIER_VALUES)
                        .where(WEATHER_MULTIPLIER_VALUES.WEATHER_MULTIPLIER_VALUE_ID
                                .in(singleton(getRealId(result.get(0).getResult().get(0))))).fetch();

        String actualConditionJsonFromDb = (String) records.get(0).getValue(0);
        String expectedConditionJsonFromDb = LiveResourceFactory.get(WEATHER_MULTIPLIER_FILE).getContent();

        assertEquals("получен ожидаемый condition_json", expectedConditionJsonFromDb, actualConditionJsonFromDb);
    }
}
