package ru.yandex.direct.core.entity.bidmodifiers.set;

import java.util.Arrays;
import java.util.Collection;
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
import ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.DEFAULT_PERCENT;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyGeoModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.getModelChangesForUpdate;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.regions.Region.CRIMEA_REGION_ID;

@CoreTest
@RunWith(Parameterized.class)
@Description("Проверка изменения скрытой корректировки для Крыма")
public class SetHiddenBidModifiersForCrimeaTest {
    private static final Integer NEW_PERCENT = DEFAULT_PERCENT / 2;

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

    @Autowired
    private BidModifierRepository bidModifierRepository;

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
        int shard = campaignInfo.getShard();

        //Создаем корректировку
        BidModifier bidModifier = createEmptyGeoModifier()
                .withCampaignId(campaignId)
                .withRegionalAdjustments(singletonList(new BidModifierRegionalAdjustment()
                        .withRegionId(regionId)
                        .withHidden(false)
                        .withPercent(110)));

        MassResult<List<Long>> result = bidModifierService.add(singletonList(bidModifier), campaignInfo.getClientId(),
                campaignInfo.getUid());

        id = BidModifierService.getRealId(result.getResult().get(0).getResult().get(0));
    }

    @Test
    @Description("При изменении корректировки на родительский регион скрытая корректировка на Крым тоже должна измениться")
    public void updateHiddenModifierForCrimeaTest() {
        ModelChanges<BidModifierAdjustment> modelChanges = getModelChangesForUpdate(BidModifierRegionalAdjustment.class,
                id, NEW_PERCENT);

        MassResult<Long> result = bidModifierService.set(singletonList(modelChanges), campaignInfo.getClientId(),
                campaignInfo.getUid());

        assertFalse("Результат не должен содержать ошибок валидации", result.getValidationResult().hasAnyErrors());

        List<BidModifier> modifiersInDb = bidModifierService.getByCampaignIds(campaignInfo.getClientId(),
                singleton(campaignInfo.getCampaignId()),
                singleton(BidModifierType.GEO_MULTIPLIER),
                singleton(BidModifierLevel.CAMPAIGN), campaignInfo.getUid());

        List<BidModifierRegionalAdjustment> adjustments
                = ((BidModifierGeo) modifiersInDb.get(0)).getRegionalAdjustments();

        assertEquals("В бд есть 2 гео корректировки", 2, adjustments.size());
        assertEquals("Процент корректировки на основной регион изменился", NEW_PERCENT,
                adjustments.get(0).getPercent());
        assertEquals("Процент корректировки на Крым изменился", NEW_PERCENT, adjustments.get(1).getPercent());
    }

    @Test
    @Description("Если есть явная корректировка на Крым, то при изменении корректировки на родительский регион корректировка на Крым не должна измениться")
    public void updateModifierForCrimeaTest() {

        // Добавляем явную корректировку на Крым
        BidModifier bidModifier = createEmptyGeoModifier()
                .withCampaignId(campaignId)
                .withRegionalAdjustments(singletonList(new BidModifierRegionalAdjustment()
                        .withRegionId(CRIMEA_REGION_ID)
                        .withHidden(false)
                        .withPercent(110)));

        bidModifierService.add(singletonList(bidModifier), campaignInfo.getClientId(), campaignInfo.getUid());

        // Изменяем корректировку
        ModelChanges<BidModifierAdjustment> modelChanges = getModelChangesForUpdate(BidModifierRegionalAdjustment.class,
                id, NEW_PERCENT);

        MassResult<Long> result = bidModifierService.set(singletonList(modelChanges), campaignInfo.getClientId(),
                campaignInfo.getUid());

        assertFalse("Результат не должен содержать ошибок валидации", result.getValidationResult().hasAnyErrors());

        List<BidModifier> modifiersInDb = bidModifierService.getByCampaignIds(campaignInfo.getClientId(),
                singleton(campaignInfo.getCampaignId()),
                singleton(BidModifierType.GEO_MULTIPLIER),
                singleton(BidModifierLevel.CAMPAIGN), campaignInfo.getUid());

        List<BidModifierRegionalAdjustment> adjustments
                = ((BidModifierGeo) modifiersInDb.get(0)).getRegionalAdjustments();

        assertEquals("В бд есть 2 гео корректировки", 2, adjustments.size());
        assertEquals("Процент корректировки на основной регион изменился", NEW_PERCENT,
                adjustments.get(0).getPercent());
        assertEquals("Процент корректировки на Крым не изменился", DEFAULT_PERCENT, adjustments.get(1).getPercent());
    }
}
