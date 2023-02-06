package ru.yandex.direct.core.entity.bidmodifiers.delete;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_LEVELS;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyGeoModifier;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.regions.Region.CRIMEA_REGION_ID;

@CoreTest
@RunWith(Parameterized.class)
@Description("Проверка удаления скрытой корректировки для Крыма")
public class DeleteHiddenBidModifiersForCrimeaTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private BidModifierService bidModifierService;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public Long regionId;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Клиент из России", Region.RUSSIA_REGION_ID},
                {"Клиент из Украины", Region.UKRAINE_REGION_ID},
        };
        return Arrays.asList(data);
    }

    private CampaignInfo campaignInfo;
    private Long campaignId;
    private Long id;

    @Before
    public void before() throws Exception {

        // Создаём кампанию
        campaignInfo = campaignSteps.createActiveTextCampaign(
                clientSteps.createClient(defaultClient().withCountryRegionId(regionId)));
        campaignId = campaignInfo.getCampaignId();

        //Создаем корректировку
        BidModifier regional = createEmptyGeoModifier()
                .withRegionalAdjustments(Collections.singletonList(
                        new BidModifierRegionalAdjustment().withRegionId(regionId).withPercent(110).withHidden(false)));

        List<BidModifier> bidModifiers = singletonList(regional.withCampaignId(campaignId));

        MassResult<List<Long>> addResult = bidModifierService.add(bidModifiers,
                campaignInfo.getClientId(), campaignInfo.getUid());

        id = addResult.getResult().get(0).getResult().get(0);

        //Проверяем наличие двух гео корректировок
        List<BidModifier> savedBidModifiers =
                bidModifierService.getByCampaignIds(campaignInfo.getClientId(), singleton(campaignId),
                        singleton(BidModifierType.GEO_MULTIPLIER), ALL_LEVELS, campaignInfo.getUid());
        List<BidModifierRegionalAdjustment> savedAdjustments =
                ((BidModifierGeo) savedBidModifiers.get(0)).getRegionalAdjustments();

        assertTrue("В бд есть 2 гео корректировки", savedAdjustments.size() == 2);
    }

    @Test
    @Description("При удалении корректировки на родительский регион скрытая корректировка на Крым тоже должна удалиться")
    public void removeHiddenModifierForCrimeaTest() {
        MassResult<Long> result = bidModifierService.delete(singletonList(id),
                campaignInfo.getClientId(), campaignInfo.getUid());

        assertFalse("Результат не должен содержать ошибок валидации", result.getValidationResult().hasAnyErrors());

        List<BidModifier> list = bidModifierService.getByCampaignIds(campaignInfo.getClientId(),
                singleton(campaignId), singleton(BidModifierType.GEO_MULTIPLIER),
                ALL_LEVELS, campaignInfo.getUid());
        assertTrue("Гео корректировки удалены", list.isEmpty());
    }

    @Test
    @Description("Если есть явная корректировка на Крым, то при удалении корректировки на родительский регион корректировка на Крым не должна удалиться")
    public void keepModifierForCrimeaTest() {

        //Добавляем явную корректировку на Крым
        BidModifier crimeaRegion = createEmptyGeoModifier()
                .withRegionalAdjustments(Collections.singletonList(
                        new BidModifierRegionalAdjustment().withRegionId(CRIMEA_REGION_ID).withPercent(110)
                                .withHidden(false)));
        List<BidModifier> crimeaBidModifiers = singletonList(crimeaRegion.withCampaignId(campaignId));
        bidModifierService.add(crimeaBidModifiers, campaignInfo.getClientId(), campaignInfo.getUid());

        MassResult<Long> result = bidModifierService.delete(singletonList(id),
                campaignInfo.getClientId(), campaignInfo.getUid());

        assertFalse("Результат не должен содержать ошибок валидации", result.getValidationResult().hasAnyErrors());

        List<BidModifier> list = bidModifierService.getByCampaignIds(campaignInfo.getClientId(),
                singleton(campaignId), singleton(BidModifierType.GEO_MULTIPLIER),
                ALL_LEVELS, campaignInfo.getUid());
        List<BidModifierRegionalAdjustment> gotAdjustments = ((BidModifierGeo) list.get(0)).getRegionalAdjustments();
        assertEquals("в бд есть 1 гео корректировки", 1, gotAdjustments.size());
        assertEquals("в бд есть корректировка для Крыма", gotAdjustments.get(0).getRegionId().longValue(), CRIMEA_REGION_ID);
    }
}
