package ru.yandex.direct.core.entity.bidmodifiers.add.regional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
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
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientGeoModifier;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(Parameterized.class)
public class AddHiddenBidModifiersRegionalForCrimeaTest {

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    private CampaignInfo campaign;

    private TestContextManager testContextManager;

    private static final Long KIEV_REGION_ID = 143L;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public Long clientCountryRegionId;

    @Parameterized.Parameter(2)
    public Long homeCapitalCityRegionId;

    @Parameterized.Parameter(3)
    public Long abroadRegionId;

    @Parameterized.Parameters(name = "{0}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {"Клиент из России", Region.RUSSIA_REGION_ID, Region.MOSCOW_REGION_ID, Region.UKRAINE_REGION_ID},
                {"Клиент из Украины", Region.UKRAINE_REGION_ID, KIEV_REGION_ID, Region.RUSSIA_REGION_ID},
        };
        return Arrays.asList(data);
    }

    @Before
    public void before() throws Exception {
        // Manual Spring integration (because we're using Parametrized runner)
        this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);

        campaign = campaignSteps.createActiveTextCampaign(
                clientSteps.createClient(defaultClient().withCountryRegionId(clientCountryRegionId)));
    }

    @Test
    @Description("Клиент ставит корректировку на свою страну - должна появиться скрытая корректировка на Крым с таким же мультипликатором")
    public void clientSetModifierForHomeParentRegion() {
        int percent = 110;
        List<BidModifierRegionalAdjustment> geoAdjustment = Collections.singletonList(
                new BidModifierRegionalAdjustment()
                        .withRegionId(clientCountryRegionId).withHidden(false).withPercent(percent));
        Pair<MassResult<List<Long>>, List<BidModifierGeo>> result = addGeoModifiers(geoAdjustment);

        assertSoftly(softly -> {
            softly.assertThat(result.getLeft().getResult().get(0).getResult())
                    .as("Метод вернул одну корректировку").hasSize(1);
            softly.assertThat(result.getRight())
                    .as("В БД сохранено две: одна из них скрытая с таким же значением percent")
                    .is(matchedBy(
                            contains(hasProperty("regionalAdjustments", containsInAnyOrder(
                                    allOf(
                                            hasProperty("regionId", equalTo(clientCountryRegionId)),
                                            hasProperty("percent", equalTo(percent)),
                                            hasProperty("hidden", equalTo(false))
                                    ),
                                    allOf(
                                            hasProperty("regionId", equalTo(Region.CRIMEA_REGION_ID)),
                                            hasProperty("percent", equalTo(percent)),
                                            hasProperty("hidden", equalTo(true))
                                    )
                            )))));
        });
    }

    @Test
    @Description("Клиент ставит корректировку на дочерний регион своей страны (столицу) - скрытая корректировка на Крым не должна появиться")
    public void clientSetModifierForChildRegionOfHomeRegion() {
        int percent = 130;
        List<BidModifierRegionalAdjustment> geoAdjustment = Collections.singletonList(
                new BidModifierRegionalAdjustment()
                        .withRegionId(homeCapitalCityRegionId).withHidden(false).withPercent(percent));
        Pair<MassResult<List<Long>>, List<BidModifierGeo>> result = addGeoModifiers(geoAdjustment);
        List<BidModifierRegionalAdjustment> adjustments = result.getRight().get(0).getRegionalAdjustments();

        assertSoftly(softly -> {
            softly.assertThat(result.getLeft().getResult().get(0).getResult())
                    .as("Метод вернул одну корректировку").hasSize(1);
            softly.assertThat(adjustments).as("В БД сохранена одна запись").hasSize(1);
            softly.assertThat(adjustments).as("Только та, которая не-hidden").is(matchedBy(contains(
                    allOf(
                            hasProperty("regionId", equalTo(homeCapitalCityRegionId)),
                            hasProperty("percent", equalTo(percent)),
                            hasProperty("hidden", equalTo(false))
                    )
            )));
        });
    }

    @Test
    @Description("Клиент ставит корректировку на соседнюю страну - скрытая корректировка на Крым не должна появиться")
    public void clientSetModifierForAbroadCountry() {
        int percent = 120;
        List<BidModifierRegionalAdjustment> geoAdjustment = Collections.singletonList(
                new BidModifierRegionalAdjustment()
                        .withRegionId(abroadRegionId).withHidden(false).withPercent(percent));
        Pair<MassResult<List<Long>>, List<BidModifierGeo>> result = addGeoModifiers(geoAdjustment);
        List<BidModifierRegionalAdjustment> adjustments = result.getRight().get(0).getRegionalAdjustments();

        assertSoftly(softly -> {
            softly.assertThat(result.getLeft().getResult().get(0).getResult())
                    .as("Метод вернул одну корректировку").hasSize(1);
            softly.assertThat(adjustments).as("В БД сохранена одна запись").hasSize(1);
            softly.assertThat(adjustments).as("Только та, которая не-hidden").is(matchedBy(contains(
                    allOf(
                            hasProperty("regionId", equalTo(abroadRegionId)),
                            hasProperty("percent", equalTo(percent)),
                            hasProperty("hidden", equalTo(false))
                    )
            )));
        });
    }

    private Pair<MassResult<List<Long>>, List<BidModifierGeo>> addGeoModifiers(
            List<BidModifierRegionalAdjustment> geoAdjustment) {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientGeoModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withRegionalAdjustments(geoAdjustment)));
        List<BidModifier> bidModifiers =
                bidModifierService.getByCampaignIds(campaign.getClientId(), singleton(campaign.getCampaignId()),
                        singleton(BidModifierType.GEO_MULTIPLIER),
                        singleton(BidModifierLevel.CAMPAIGN), campaign.getUid());
        return Pair.of(result, (List<BidModifierGeo>) (List) bidModifiers);
    }

    @Test
    @Description("Клиент ставит корректировку на свою страну, затем добавляет корректировку на Крым - скрытая корректировка на Крым должна быть заменена на выставленную")
    public void clientSetModifierForCrimea() {
        int firstPercent = 110;
        int crimeaPercent = 140;
        BidModifierRegionalAdjustment firstAdjustment = new BidModifierRegionalAdjustment()
                .withRegionId(clientCountryRegionId).withHidden(false).withPercent(firstPercent);
        BidModifierRegionalAdjustment crimeaAdjustment = new BidModifierRegionalAdjustment()
                .withRegionId(Region.CRIMEA_REGION_ID).withHidden(false).withPercent(crimeaPercent);
        // Добавляем корректировку на свою страну
        addBidModifiers(
                singletonList(
                        createEmptyClientGeoModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withRegionalAdjustments(singletonList(firstAdjustment))));
        // Добавляем явную корректрировку на Крым
        addBidModifiers(
                singletonList(
                        createEmptyClientGeoModifier()
                                .withCampaignId(campaign.getCampaignId())
                                .withRegionalAdjustments(singletonList(crimeaAdjustment))));

        List<BidModifierGeo> modifiersInDb = (List)
                bidModifierService.getByCampaignIds(campaign.getClientId(), singleton(campaign.getCampaignId()),
                        singleton(BidModifierType.GEO_MULTIPLIER),
                        singleton(BidModifierLevel.CAMPAIGN), campaign.getUid());
        List<BidModifierRegionalAdjustment> adjustments = modifiersInDb.get(0).getRegionalAdjustments();

        assertSoftly(softly -> {
            softly.assertThat(adjustments).as("В БД сохранено две записи").hasSize(2);
            softly.assertThat(adjustments).as("Обе записи не скрытые, со своими значениями percent")
                    .is(matchedBy(
                            containsInAnyOrder(
                                    allOf(
                                            hasProperty("regionId", equalTo(clientCountryRegionId)),
                                            hasProperty("percent", equalTo(firstPercent)),
                                            hasProperty("hidden", equalTo(false))
                                    ),
                                    allOf(
                                            hasProperty("regionId", equalTo(Region.CRIMEA_REGION_ID)),
                                            hasProperty("percent", equalTo(crimeaPercent)),
                                            hasProperty("hidden", equalTo(false))
                                    )
                            )));
        });
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, campaign.getClientId(), campaign.getUid());
    }
}
