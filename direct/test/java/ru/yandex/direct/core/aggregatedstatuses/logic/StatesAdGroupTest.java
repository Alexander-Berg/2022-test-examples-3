package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.aggrstatus.AggregatedStatusAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupStatesEnum;
import ru.yandex.direct.regions.Region;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroupType.BASE;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroupType.DYNAMIC;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroupType.PERFORMANCE;
import static ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupStatesEnum.STATUS_BL_GENERATED_NOTHING_GENERATED;
import static ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupStatesEnum.STATUS_BL_GENERATED_PROCESSING;

@RunWith(Parameterized.class)
public class StatesAdGroupTest {
    private static final List<Long> GLOBAL_GEO = Collections.singletonList(Region.GLOBAL_REGION_ID);
    private static final List<Long> RESTRICTED_EFFECTIVE_GEO = Collections.singletonList(Region.RUSSIA_REGION_ID);
    private static final List<Long> EMPTY_GEO = Collections.emptyList();

    private static AdGroupStates adGroupStatesCalculator;
    private Set<AdGroupType> dynamicAndSmartAdGroupTypes = Set.of(DYNAMIC, PERFORMANCE);

    @Parameterized.Parameter
    public AdGroupType type;

    @Parameterized.Parameter(1)
    public StatusModerate statusModerate;

    @Parameterized.Parameter(2)
    public List<Long> geo;

    @Parameterized.Parameter(3)
    public List<Long> effectiveGeo;

    @Parameterized.Parameter(4)
    public Boolean isBsRarelyServed;

    @Parameterized.Parameter(5)
    public Collection<AdGroupStatesEnum> expectedStates;

    @Parameterized.Parameters(name = "statusModerate: {0} => States: [{5}]")
    public static Object[][] params() {
        return new Object[][]{
                {BASE, StatusModerate.YES, GLOBAL_GEO, null, false, Collections.emptyList()},
                {BASE, StatusModerate.NEW, GLOBAL_GEO, GLOBAL_GEO, false, List.of(AdGroupStatesEnum.DRAFT)},
                {BASE, StatusModerate.READY, GLOBAL_GEO, GLOBAL_GEO, false, List.of(AdGroupStatesEnum.MODERATION)},
                {BASE, StatusModerate.SENT, GLOBAL_GEO, GLOBAL_GEO, false, List.of(AdGroupStatesEnum.MODERATION)},
                {BASE, StatusModerate.SENDING, GLOBAL_GEO, GLOBAL_GEO, false, List.of(AdGroupStatesEnum.MODERATION)},
                {BASE, StatusModerate.NO, GLOBAL_GEO, GLOBAL_GEO, false, List.of(AdGroupStatesEnum.REJECTED)},
                {BASE, StatusModerate.YES, GLOBAL_GEO, GLOBAL_GEO, false, Collections.emptyList()},
                {BASE, StatusModerate.YES, GLOBAL_GEO, RESTRICTED_EFFECTIVE_GEO, false,
                        List.of(AdGroupStatesEnum.HAS_RESTRICTED_GEO)},
                {BASE, StatusModerate.YES, GLOBAL_GEO, EMPTY_GEO, false,
                        List.of(AdGroupStatesEnum.HAS_NO_EFFECTIVE_GEO)},
                {BASE, StatusModerate.YES, GLOBAL_GEO, GLOBAL_GEO, true, List.of(AdGroupStatesEnum.BS_RARELY_SERVED)},
                {DYNAMIC, StatusModerate.YES, GLOBAL_GEO, GLOBAL_GEO, true, Collections.emptyList()},
                {PERFORMANCE, StatusModerate.YES, GLOBAL_GEO, GLOBAL_GEO, true, Collections.emptyList()}
        };
    }

    @BeforeClass
    public static void prepare() {
        adGroupStatesCalculator = new AdGroupStates();
    }

    @Test
    public void test() {
        AggregatedStatusAdGroup adGroup = new AggregatedStatusAdGroup()
                .withType(type)
                .withStatusModerate(statusModerate)
                .withGeo(geo)
                .withBsRarelyLoaded(isBsRarelyServed)
                .withEffectiveGeo(effectiveGeo);

        Collection<AdGroupStatesEnum> states = adGroupStatesCalculator.calc(adGroup);

        assertEquals("got right states", states, expectedStates);
    }

    @Test
    public void test_StatusBlGeneratedIsProcessing() {
        AggregatedStatusAdGroup adGroup = new AggregatedStatusAdGroup()
                .withType(type)
                .withStatusModerate(statusModerate)
                .withGeo(geo)
                .withBsRarelyLoaded(isBsRarelyServed)
                .withStatusBlGenerated(StatusBLGenerated.PROCESSING)
                .withEffectiveGeo(effectiveGeo);

        Collection<AdGroupStatesEnum> states = adGroupStatesCalculator.calc(adGroup);

        List<AdGroupStatesEnum> expected = new ArrayList<>(expectedStates);
        if (dynamicAndSmartAdGroupTypes.contains(adGroup.getType())) {
            expected.add(STATUS_BL_GENERATED_PROCESSING);
        }

        assertEquals("got right states", states, expected);
    }

    @Test
    public void test_StatusBlGeneratedNothingCreated() {
        AggregatedStatusAdGroup adGroup = new AggregatedStatusAdGroup()
                .withType(type)
                .withStatusModerate(statusModerate)
                .withGeo(geo)
                .withBsRarelyLoaded(isBsRarelyServed)
                .withStatusBlGenerated(StatusBLGenerated.NO)
                .withEffectiveGeo(effectiveGeo);

        Collection<AdGroupStatesEnum> states = adGroupStatesCalculator.calc(adGroup);

        List<AdGroupStatesEnum> expected = new ArrayList<>(expectedStates);
        if (dynamicAndSmartAdGroupTypes.contains(adGroup.getType())) {
            expected.add(STATUS_BL_GENERATED_NOTHING_GENERATED);
        }

        assertEquals("got right states", states, expected);
    }
}
