package ru.yandex.direct.grid.processing.service.creative;

import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeBusinessType;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.creative.service.CreativeService;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.service.FeedService;
import ru.yandex.direct.core.testing.data.TestFeeds;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType;
import ru.yandex.direct.grid.processing.model.cliententity.GdSmartCreative;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupRegionsInfo;
import ru.yandex.direct.grid.processing.model.group.GdSmartAdGroup;
import ru.yandex.direct.grid.processing.service.client.converter.ClientEntityConverter;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;


public class CreativeDataServiceTest {

    @Mock
    private CreativeService creativeService;

    @Mock
    private FeedService feedService;

    @Mock
    private GeoTree geoTree;

    @Mock
    private ClientGeoService clientGeoService;

    @InjectMocks
    private CreativeDataService creativeDataService;

    private ClientId clientId;
    private Feed feed;
    private Creative creative;
    private GdSmartAdGroup gdSmartAdGroup;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);

        MockitoAnnotations.initMocks(this);

        feed = TestFeeds.defaultFeed(clientId)
                .withId(1L)
                .withClientId(clientId.asLong())
                .withBusinessType(BusinessType.RETAIL);

        creative = defaultPerformanceCreative(clientId, 13L)
                .withType(CreativeType.PERFORMANCE)
                .withSumGeo(singletonList(Region.RUSSIA_REGION_ID))
                .withBusinessType(CreativeBusinessType.RETAIL)
                .withStatusModerate(StatusModerate.YES);

        gdSmartAdGroup = new GdSmartAdGroup()
                .withId(123L)
                .withFeedId(feed.getId())
                .withRegionsInfo(
                        new GdAdGroupRegionsInfo().withRegionIds(singletonList((int) Region.RUSSIA_REGION_ID)))
                .withType(GdAdGroupType.PERFORMANCE);

        doReturn(singletonList(feed)).when(feedService).getFeeds(clientId, singletonList(feed.getId()));
        doReturn(singletonList(creative)).when(creativeService)
                .getCreativesWithBusinessType(clientId, feed.getBusinessType(), null);
        doReturn(ImmutableSet.of(Region.RUSSIA_REGION_ID)).when(geoTree).getModerationCountries(anyList());
        doReturn(geoTree).when(clientGeoService).getClientTranslocalGeoTree(any(ClientId.class));
    }

    @Test
    public void getAvailableCreatives_success() {

        List<List<GdSmartCreative>> availableCreatives =
                creativeDataService.getAvailableCreatives(clientId, singletonList(gdSmartAdGroup), null);

        assertThat(availableCreatives, hasSize(1));
        assertThat(availableCreatives.get(0).get(0),
                beanDiffer((GdSmartCreative) ClientEntityConverter.toGdCreativeImplementation(creative)));
    }

    @Test
    public void getAvailableCreatives_cannotFindBusinessType() {
        doReturn(emptyList()).when(creativeService).getCreativesWithBusinessType(clientId, feed.getBusinessType(),
                null);

        List<List<GdSmartCreative>> availableCreatives =
                creativeDataService.getAvailableCreatives(clientId, singletonList(gdSmartAdGroup), null);

        assertThat(availableCreatives, hasSize(1));
        assertThat(availableCreatives.get(0), is(emptyList()));
    }

    @Test
    public void getAvailableCreatives_unconsistentGeo() {
        doReturn(ImmutableSet.of(Region.TURKEY_REGION_ID)).when(geoTree).getModerationCountries(anyList());

        List<List<GdSmartCreative>> availableCreatives =
                creativeDataService.getAvailableCreatives(clientId, singletonList(gdSmartAdGroup), null);

        assertThat(availableCreatives, hasSize(1));
        assertThat(availableCreatives.get(0), is(emptyList()));
    }

    @Test
    public void getAvailableCreatives_unconsistentGeo_statusModerateNew() {
        doReturn(singletonList(creative.withStatusModerate(StatusModerate.NEW)))
                .when(creativeService).getCreativesWithBusinessType(clientId, feed.getBusinessType(), null);
        doReturn(ImmutableSet.of(Region.TURKEY_REGION_ID)).when(geoTree).getModerationCountries(anyList());

        List<List<GdSmartCreative>> availableCreatives =
                creativeDataService.getAvailableCreatives(clientId, singletonList(gdSmartAdGroup), null);

        assertThat(availableCreatives, hasSize(1));
        assertThat(availableCreatives.get(0).get(0),
                beanDiffer((GdSmartCreative) ClientEntityConverter.toGdCreativeImplementation(creative)));
    }

    @Test
    public void getAvailableCreatives_unconsistentGeo_statusModerateError() {
        doReturn(singletonList(creative.withStatusModerate(StatusModerate.ERROR)))
                .when(creativeService).getCreativesWithBusinessType(clientId, feed.getBusinessType(), null);
        doReturn(ImmutableSet.of(Region.TURKEY_REGION_ID)).when(geoTree).getModerationCountries(anyList());

        List<List<GdSmartCreative>> availableCreatives =
                creativeDataService.getAvailableCreatives(clientId, singletonList(gdSmartAdGroup), null);

        assertThat(availableCreatives, hasSize(1));
        assertThat(availableCreatives.get(0).get(0),
                beanDiffer((GdSmartCreative) ClientEntityConverter.toGdCreativeImplementation(creative)));
    }

}
