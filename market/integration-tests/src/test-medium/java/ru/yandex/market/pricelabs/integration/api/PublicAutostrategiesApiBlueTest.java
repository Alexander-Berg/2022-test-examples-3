package ru.yandex.market.pricelabs.integration.api;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.CoreTestUtils;
import ru.yandex.market.pricelabs.MockMvcProxyHttpException;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyDrr;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterSimple;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterVendor;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyLoad;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySaveWithId;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsCPA;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsCPO;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsDRR;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsPOS;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsVPOS;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTest.checkResponse;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.assertThrowsWithMessage;

public class PublicAutostrategiesApiBlueTest extends AbstractAutostrategiesApiTest {

    private static final OffsetDateTime CREATED_TIME = OffsetDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final String CAMPAIGN_NAME_FORMATTER = "Название рекламной компании %d";

    @BeforeEach
    void init() {
        this.init(AutostrategyTarget.blue, CoreTestUtils.emptyRunnable());
        executors.blueBidsRecommender().insert(new ArrayList<>(getBlueBidsRecommendations().values()));
    }

    @Test
    void testEstimateCpaDrr() {
        assertEquals(new AutostrategyDrr(),
                checkResponse(getPublicApi().autostrategyEstimateCpaDrrPost(SHOP1, null, getTarget(), null, null)));
    }

    @Test
    void testLookupMaxBid() {
        assertThrowsWithMessage(MockMvcProxyHttpException.class, () ->
                        getPublicApi().autostrategyLookupMaxBidPost(SHOP1, null, getTarget()),
                "Unsupported autoStrategyTarget: blue. Expect: [vendorBlue]");
    }

    @Test
    void testLookupMaxBids() {
        assertThrowsWithMessage(MockMvcProxyHttpException.class, () ->
                        getPublicApi().autostrategyLookupMaxBidsPost(SHOP1, null, getTarget()),
                "Unsupported autoStrategyTarget: blue. Expect: [vendorBlue]");
    }

    @Test
    void testEstimateCpaDrrOffersFromSameCategory() {
        // берем офферы из одной категории
        List<Offer> offersInCategory = offersSampleShop1GroupedByCategories().get(CATEGORY1);
        List<String> offerIds = List.of(
                offersInCategory.get(0).getOffer_id(),
                offersInCategory.get(1).getOffer_id()
        );

        double category1Drr = getBlueBidsRecommendations().get(CATEGORY1).getCpa_rec_bid();
        double expectedDrr = (category1Drr * offerIds.size()) / offerIds.size();

        AutostrategyDrr actual = checkResponse(getPublicApi()
                .autostrategyEstimateCpaDrrPost(SHOP1, autostrategyFilter(offerIds), getTarget(), null, null));
        assertEquals(toDrr(expectedDrr), actual.getDrr());
    }

    @Test
    void autostrategyBatchPost_lastOrderTrue_success() {
        List<AutostrategyLoad> autostrategyLoadsFirst = checkResponse(
                publicApi.autostrategyBatchPost(
                        3543,
                        List.of(createAutostrategySave(500)),
                        "white",
                        492L,
                        true
                )
        );
        assertThat(autostrategyLoadsFirst, 500);

        List<AutostrategyLoad> autostrategyLoadSecond = checkResponse(
                publicApi.autostrategyBatchPost(
                        3543,
                        List.of(createAutostrategySave(510)),
                        "white",
                        492L,
                        true
                )
        );
        assertThat(autostrategyLoadSecond, 510);

        assertSame(autostrategyLoadsFirst.get(0).getPriority() + 1, autostrategyLoadSecond.get(0).getPriority());

        List<AutostrategyLoad> autostrategyLoadList = publicApi.autostrategiesGet(3543, "white")
                .getBody();

        assertNotNull(autostrategyLoadList);
        assertEquals(2, autostrategyLoadList.size());

        Map<String, AutostrategyLoad> autostrategyLoadMap = autostrategyLoadList.stream()
                .collect(Collectors.toMap(AutostrategyLoad::getName, Function.identity()));

        AutostrategyLoad autostrategyLoadOne = autostrategyLoadMap.get("Название рекламной компании 500");
        AutostrategyLoad autostrategyLoadTwo = autostrategyLoadMap.get("Название рекламной компании 510");
        assertTrue(autostrategyLoadTwo.getPriority() < autostrategyLoadOne.getPriority());
        assertSame(autostrategyLoadOne.getPriority(), autostrategyLoadsFirst.get(0).getPriority() + 2);
        assertSame(autostrategyLoadTwo.getPriority(), autostrategyLoadsFirst.get(0).getPriority() + 1);
    }

    private void assertThat(@Nonnull List<AutostrategyLoad> autostrategyLoadsFirst, int id) {
        Assertions.assertThat(autostrategyLoadsFirst.get(0))
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "priority", "id", "timestamp")
                .isEqualTo(createAutostrategyLoad(id));
        Assertions.assertThat(autostrategyLoadsFirst.get(0).getId())
                .isGreaterThan(0);
    }

    @Nonnull
    private AutostrategyLoad createAutostrategyLoad(int id) {
        return new AutostrategyLoad()
                .name(String.format(CAMPAIGN_NAME_FORMATTER, id))
                .enabled(true)
                .uid(492L)
                .state(AutostrategyLoad.StateEnum.ACTIVATING)
                .recommendedCount(0)
                .offerCount(0)
                .recommendationType(AutostrategyLoad.RecommendationTypeEnum.MAXIMUM)
                .filter(
                        new AutostrategyFilter()
                                .type(AutostrategyFilter.TypeEnum.SIMPLE)
                                .vendor(
                                        new AutostrategyFilterVendor()
                                                .models(List.of())
                                                .shops(List.of())
                                                .businesses(List.of())
                                )
                                .simple(
                                        new AutostrategyFilterSimple()
                                                .categories(List.of())
                                                .excludeCategories(List.of())
                                                .vendors(List.of())
                                                .offerIds(List.of("1", "2"))
                                )
                )
                .settings(
                        new AutostrategySettings()
                                .type(AutostrategySettings.TypeEnum.CPA)
                                .cpa(
                                        new AutostrategySettingsCPA()
                                                .drrBid(100L)
                                )
                                .cpo(new AutostrategySettingsCPO())
                                .drr(new AutostrategySettingsDRR())
                                .pos(new AutostrategySettingsPOS())
                                .vpos(new AutostrategySettingsVPOS())
                );
    }

    private AutostrategySaveWithId createAutostrategySave(int id) {
        return new AutostrategySaveWithId()
                .autostrategy(
                        new AutostrategySave()
                                .name(String.format(CAMPAIGN_NAME_FORMATTER, id))
                                .createdAt(CREATED_TIME)
                                .enabled(true)
                                .recommendationType(AutostrategySave.RecommendationTypeEnum.MAXIMUM)
                                .filter(
                                        new AutostrategyFilter()
                                                .type(AutostrategyFilter.TypeEnum.SIMPLE)
                                                .simple(
                                                        new AutostrategyFilterSimple()
                                                                .offerIds(List.of("1", "2"))
                                                )
                                )
                                .settings(
                                        new AutostrategySettings()
                                                .type(AutostrategySettings.TypeEnum.CPA)
                                                .cpa(
                                                        new AutostrategySettingsCPA()
                                                                .drrBid(100L)
                                                )
                                )
                );
    }
}
