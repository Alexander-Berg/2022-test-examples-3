package ru.yandex.direct.core.entity.moderation.service.receiving.operations.banners;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.type.flags.BannerFlagsRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.entity.moderation.service.receiving.MinusRegionsToBannerFlagsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.banner.model.Age.AGE_16;
import static ru.yandex.direct.core.entity.banner.model.BannerFlags.AGE;
import static ru.yandex.direct.core.entity.banner.model.BannerFlags.MINUS_REGION_KZ;
import static ru.yandex.direct.core.entity.banner.model.BannerFlags.MINUS_REGION_RB;
import static ru.yandex.direct.core.entity.banner.model.BannerFlags.MINUS_REGION_RU;
import static ru.yandex.direct.core.entity.banner.model.BannerFlags.MINUS_REGION_TR;
import static ru.yandex.direct.core.entity.banner.model.BannerFlags.MINUS_REGION_UA;
import static ru.yandex.direct.core.entity.banner.model.BannerFlags.MINUS_REGION_UZ;
import static ru.yandex.direct.core.entity.banner.model.BannerFlags.NOT_ANIMATED;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;

@CoreTest
@RunWith(SpringRunner.class)
public class UpdateBannerFlagsOpTest {
    @Autowired
    private BannerFlagsRepository bannerFlagsRepository;

    @Autowired
    private FeatureService featureService;

    private PpcPropertiesSupport ppcPropertiesSupport;
    private PpcProperty<Boolean> mockedSaveEnabledProperty;
    private PpcProperty<Set<Long>> mockedSaveEnabledForCampaignIdsProperty;

    private MinusRegionsToBannerFlagsService minusRegionsToBannerFlagsService;
    private UpdateBannerFlagsOp updateBannerFlagsOp;

    @Before
    public void setUp() throws Exception {
        ppcPropertiesSupport = mock(PpcPropertiesSupport.class);

        mockedSaveEnabledProperty = mock(PpcProperty.class);
        when(ppcPropertiesSupport.get(
                eq(PpcPropertyNames.SAVE_MINUS_REGIONS_AS_BANNER_FLAGS),
                any(Duration.class)
        )).thenReturn(mockedSaveEnabledProperty);
        mockedSaveEnabledForCampaignIdsProperty = mock(PpcProperty.class);
        when(ppcPropertiesSupport.get(
                eq(PpcPropertyNames.SAVE_MINUS_REGIONS_AS_BANNER_FLAGS_ENABLED_CAMPAIGN_IDS),
                any(Duration.class)
        )).thenReturn(mockedSaveEnabledForCampaignIdsProperty);

        minusRegionsToBannerFlagsService = new MinusRegionsToBannerFlagsService(ppcPropertiesSupport);
        updateBannerFlagsOp = new UpdateBannerFlagsOp(bannerFlagsRepository,
                featureService,
                minusRegionsToBannerFlagsService);
    }

    @Test
    public void testDefaultValues_setDefaultAge() {
        BannerFlags dbFlags = BannerFlags.fromSource("finance:1");
        BannerFlags incomingFlags = new BannerFlags();
        incomingFlags.getFlags().put("age", "default");

        updateBannerFlagsOp.replaceDefaultValuesByRealValuesFromDb(dbFlags, incomingFlags.getFlags());

        assertThat(incomingFlags.getFlags()).hasSize(1);
        assertThat(incomingFlags.getFlags().get("age")).isEqualTo("18");
        assertThat(BannerFlags.toSource(incomingFlags)).isEqualTo("age:18");
    }

    @Test
    public void testDefaultValues_setDefaultYaPages() {
        BannerFlags dbFlags = BannerFlags.fromSource("finance:1");
        BannerFlags incomingFlags = new BannerFlags();
        incomingFlags.getFlags().put("ya_pages", "default");

        updateBannerFlagsOp.replaceDefaultValuesByRealValuesFromDb(dbFlags, incomingFlags.getFlags());

        assertThat(incomingFlags.getFlags()).hasSize(1);
        assertThat(incomingFlags.getFlags().get("ya_pages")).isEqualTo("ya_remove");
        assertThat(BannerFlags.toSource(incomingFlags)).isEqualTo("ya_pages:ya_remove");
    }

    @Test
    public void testDefaultValues_setDefaultAge_replaceBabyFood() {
        BannerFlags dbFlags = BannerFlags.fromSource("baby_food:1");
        BannerFlags incomingFlags = new BannerFlags();
        incomingFlags.getFlags().put("age", "default");

        updateBannerFlagsOp.replaceDefaultValuesByRealValuesFromDb(dbFlags, incomingFlags.getFlags());

        assertThat(incomingFlags.getFlags()).hasSize(1);
        assertThat(incomingFlags.getFlags().get("age")).isEqualTo("18");
        assertThat(BannerFlags.toSource(incomingFlags)).isEqualTo("age:18");
    }

    @Test
    public void testDefaultValues2_setDefaultAge() {
        BannerFlags dbFlags = BannerFlags.fromSource("");
        BannerFlags incomingFlags = new BannerFlags();

        incomingFlags.getFlags().put("age", "default");

        updateBannerFlagsOp.replaceDefaultValuesByRealValuesFromDb(dbFlags, incomingFlags.getFlags());

        assertThat(incomingFlags.getFlags()).hasSize(1);
        assertThat(incomingFlags.getFlags().get("age")).isEqualTo("18");
        assertThat(BannerFlags.toSource(incomingFlags)).isEqualTo("age:18");
    }

    @Test
    public void testDefaultValues_ignoreDefaultAge() {
        BannerFlags dbFlags = BannerFlags.fromSource("age:6");
        BannerFlags incomingFlags = new BannerFlags();

        incomingFlags.getFlags().put("age", "default");

        updateBannerFlagsOp.replaceDefaultValuesByRealValuesFromDb(dbFlags, incomingFlags.getFlags());

        assertThat(incomingFlags.getFlags()).hasSize(1);
        assertThat(incomingFlags.getFlags().get("age")).isEqualTo("6");
        assertThat(BannerFlags.toSource(incomingFlags)).isEqualTo("age:6");
    }

    @Test
    public void testDefaultValues_replaceDbAge() {
        BannerFlags dbFlags = BannerFlags.fromSource("age:6");
        BannerFlags incomingFlags = new BannerFlags();

        incomingFlags.getFlags().put("age", "16");

        updateBannerFlagsOp.replaceDefaultValuesByRealValuesFromDb(dbFlags, incomingFlags.getFlags());

        assertThat(incomingFlags.getFlags()).hasSize(1);
        assertThat(incomingFlags.getFlags().get("age")).isEqualTo("16");
        assertThat(BannerFlags.toSource(incomingFlags)).isEqualTo("age:16");
    }

    @Test
    public void testMinusRegionFlags_withCommonFlags_SaveEnabled() {
        when(mockedSaveEnabledProperty.getOrDefault(anyBoolean())).thenReturn(true);

        var response = createResponse(
                Map.of("age", "16", "not_animated", "1"),
                List.of(225L, 977L, 159L, 187L, 149L, 983L, 171L));
        var bannerFlags = updateBannerFlagsOp.getFlagsFromResponse(response);

        assertThat(bannerFlags.getFlags()).hasSize(8);
        assertThat(bannerFlags.get(AGE)).isEqualTo(AGE_16);
        assertThat(bannerFlags.get(NOT_ANIMATED)).isTrue();
        assertThat(bannerFlags.get(MINUS_REGION_RU)).isTrue();
        assertThat(bannerFlags.get(MINUS_REGION_KZ)).isTrue();
        assertThat(bannerFlags.get(MINUS_REGION_UA)).isTrue();
        assertThat(bannerFlags.get(MINUS_REGION_RB)).isTrue();
        assertThat(bannerFlags.get(MINUS_REGION_TR)).isTrue();
        assertThat(bannerFlags.get(MINUS_REGION_UZ)).isTrue();
    }

    @Test
    public void testMinusRegionFlags_withNullFlags_SaveEnabled() {
        when(mockedSaveEnabledProperty.getOrDefault(anyBoolean())).thenReturn(true);

        var response = createResponse(null, List.of(225L, 171L));
        var bannerFlags = updateBannerFlagsOp.getFlagsFromResponse(response);

        assertThat(bannerFlags.getFlags()).hasSize(2);
        assertThat(bannerFlags.get(MINUS_REGION_RU)).isTrue();
        assertThat(bannerFlags.get(MINUS_REGION_UZ)).isTrue();
    }

    @Test
    public void testMinusRegionFlags_withCommonFlags_SaveDisabled() {
        when(mockedSaveEnabledProperty.getOrDefault(anyBoolean())).thenReturn(false);
        when(mockedSaveEnabledForCampaignIdsProperty.getOrDefault(anySet())).thenReturn(Collections.emptySet());

        var response = createResponse(
                Map.of("age", "16", "not_animated", "1"),
                List.of(225L, 977L, 159L, 187L, 149L, 983L, 171L));
        var bannerFlags = updateBannerFlagsOp.getFlagsFromResponse(response);

        assertThat(bannerFlags.getFlags()).hasSize(2);
        assertThat(bannerFlags.get(AGE)).isEqualTo(AGE_16);
        assertThat(bannerFlags.get(NOT_ANIMATED)).isTrue();
    }

    @Test
    public void testMinusRegionFlags_withCommonFlags_SaveEnabledForAnotherCampaign() {
        when(mockedSaveEnabledProperty.getOrDefault(anyBoolean())).thenReturn(false);
        when(mockedSaveEnabledForCampaignIdsProperty.getOrDefault(anySet())).thenReturn(Set.of(1001L));

        var response = createResponse(
                Map.of("age", "16", "not_animated", "1"),
                List.of(225L, 977L, 159L, 187L, 149L, 983L, 171L));
        var bannerFlags = updateBannerFlagsOp.getFlagsFromResponse(response);

        assertThat(bannerFlags.getFlags()).hasSize(2);
        assertThat(bannerFlags.get(AGE)).isEqualTo(AGE_16);
        assertThat(bannerFlags.get(NOT_ANIMATED)).isTrue();
    }

    @Test
    public void testMinusRegionFlags_withCommonFlags_SaveEnabledForCampaign() {
        when(mockedSaveEnabledProperty.getOrDefault(anyBoolean())).thenReturn(false);
        when(mockedSaveEnabledForCampaignIdsProperty.getOrDefault(anySet())).thenReturn(Set.of(1000L));

        var response = createResponse(
                Map.of("age", "16", "not_animated", "1"),
                List.of(225L, 977L, 159L, 187L, 149L, 983L, 171L));
        var bannerFlags = updateBannerFlagsOp.getFlagsFromResponse(response);

        assertThat(bannerFlags.getFlags()).hasSize(8);
        assertThat(bannerFlags.get(AGE)).isEqualTo(AGE_16);
        assertThat(bannerFlags.get(NOT_ANIMATED)).isTrue();
        assertThat(bannerFlags.get(MINUS_REGION_RU)).isTrue();
        assertThat(bannerFlags.get(MINUS_REGION_KZ)).isTrue();
        assertThat(bannerFlags.get(MINUS_REGION_UA)).isTrue();
        assertThat(bannerFlags.get(MINUS_REGION_RB)).isTrue();
        assertThat(bannerFlags.get(MINUS_REGION_TR)).isTrue();
        assertThat(bannerFlags.get(MINUS_REGION_UZ)).isTrue();
    }

    private BannerModerationResponse createResponse(Map<String, String> flags, List<Long> minusRegions) {
        BannerModerationResponse r = new BannerModerationResponse();

        r.setService(ModerationServiceNames.DIRECT_SERVICE);
        r.setType(ModerationObjectType.TEXT_AD);

        BannerModerationMeta meta = new BannerModerationMeta();
        meta.setClientId(1);
        meta.setBannerId(123);
        meta.setCampaignId(1000);
        meta.setUid(1);
        meta.setVersionId(1);

        r.setMeta(meta);

        Verdict v = new Verdict();
        v.setVerdict(Yes);
        if (minusRegions != null) {
            v.setMinusRegions(minusRegions);
        }
        if (flags != null) {
            v.setFlags(flags);
        }

        r.setResult(v);

        return r;
    }
}
