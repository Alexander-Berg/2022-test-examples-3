package ru.yandex.direct.core.entity.adgroup.service;


import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.banner.repository.BannerModerationRepository;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class GetAdGroupsEnrichAdGroupsWithEffectiveAndRestrictedGeoTest {

    private static final int DEFAULT_SHARD = 1;

    private static final Long FIRST_ADGROUP_ID = 1L;
    private static final Long SECOND_ADGROUP_ID = 2L;
    private static final Long THIRD_ADGROUP_ID = 3L;

    private GeoTree geoTree;
    private List<AdGroup> adgroups;

    @InjectMocks
    private AdGroupService service;

    @Mock
    private GeoTreeFactory geoTreeFactory;

    @Mock
    private BannerModerationRepository bannerModerationRepository;

    @Before
    public void setUp() {
        initMocks(this);

        geoTree = mock(GeoTree.class);

        adgroups = asList(
                new TextAdGroup().withId(FIRST_ADGROUP_ID).withGeo(asList(1L, 2L, 3L, 4L, 5L)),
                new MobileContentAdGroup().withId(SECOND_ADGROUP_ID).withGeo(asList(6L, 7L, 8L, 9L, 10L)),
                new DynamicTextAdGroup().withId(THIRD_ADGROUP_ID));

        when(bannerModerationRepository.getBannersMinusGeoByAdGroupIds(eq(DEFAULT_SHARD), anyList()))
                .thenReturn(ImmutableMap.<Long, List<Long>>builder()
                        .put(FIRST_ADGROUP_ID, asList(2L, 4L))
                        .put(SECOND_ADGROUP_ID, asList(7L, 9L))
                        .build());

        when(geoTree.excludeRegions(anyList(), anyList())).thenReturn(asList(1L, 3L, 5L))
                .thenReturn(asList(6L, 8L, 10L));

        when(geoTree.getDiffScaledToCountryLevel(anyList(), anyList())).thenReturn(asList(2L, 4L))
                .thenReturn(asList(7L, 9L));

        when(geoTreeFactory.getApiGeoTree()).thenReturn(geoTree);
    }

    @Test
    public void noAdGroupsGiven() {
        service.enrichAdGroupsWithEffectiveAndRestrictedGeo(DEFAULT_SHARD, new ArrayList<>());

        List<List<Long>> adgroupsEffectiveGeo = adgroups.stream().map(AdGroup::getEffectiveGeo).collect(toList());
        assertThat("effectiveGeo not set for each adgroup", adgroupsEffectiveGeo,
                contains(nullValue(), nullValue(), nullValue()));

        List<List<Long>> adgroupsRestrictedGeo = adgroups.stream().map(AdGroup::getRestrictedGeo).collect(toList());
        assertThat("restrictedGeo not set for each adgroup", adgroupsRestrictedGeo,
                contains(nullValue(), nullValue(), nullValue()));
    }

    @Test
    public void effectiveAndRestrictedGeoCalculatedForPartOfAdGroups() {
        service.enrichAdGroupsWithEffectiveAndRestrictedGeo(DEFAULT_SHARD, adgroups);

        List<List<Long>> adgroupsEffectiveGeo = adgroups.stream().map(AdGroup::getEffectiveGeo).collect(toList());
        assertThat("effectiveGeo set for adgroups", adgroupsEffectiveGeo,
                contains(contains(1L, 3L, 5L), contains(6L, 8L, 10L), nullValue()));

        List<List<Long>> adgroupsRestrictedGeo = adgroups.stream().map(AdGroup::getRestrictedGeo).collect(toList());
        assertThat("restrictedGeo set for adgroups", adgroupsRestrictedGeo,
                contains(contains(2L, 4L), contains(7L, 9L), nullValue()));
    }
}
