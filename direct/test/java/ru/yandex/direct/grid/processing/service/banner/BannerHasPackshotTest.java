package ru.yandex.direct.grid.processing.service.banner;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.banner.GdAd;
import ru.yandex.direct.grid.processing.model.banner.GdAdWithTotals;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContainer;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContext;
import ru.yandex.direct.grid.processing.model.banner.GdCPMBannerAd;
import ru.yandex.direct.grid.processing.model.client.GdClient;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.cliententity.GdCreative;
import ru.yandex.direct.grid.processing.service.cache.CachedGridData;
import ru.yandex.direct.grid.processing.service.cache.GridCacheService;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdBaseGroup;

public class BannerHasPackshotTest {

    private GridGraphQLContext gridGraphQLContext;
    private GdAdsContainer inputContainer;

    @Mock
    private GridCacheService gridCacheService;

    @Mock
    private BannerDataService bannerDataService;

    @Mock
    private GridValidationService gridValidationService;

    @Mock
    private FeatureService featureService;

    @InjectMocks
    private AdGraphQlService adGraphQlService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        gridGraphQLContext = ContextHelper.buildDefaultContext();
        when(gridCacheService.getResultAndSaveToCacheIfRequested(any(), any(), any(), any(), anyBoolean()))
                .thenAnswer(invocation -> {
                    CachedGridData data = invocation.getArgument(1);
                    data.setRowset(invocation.getArgument(2));
                    return data;
                });

        inputContainer = getDefaultGdAdsContainer();
    }


    @Test
    public void checkHasPackshot() {
        generateBanner();
        GdAdsContext adsContext =
                adGraphQlService.getAds(gridGraphQLContext, mock(GdClient.class), inputContainer);

        assertTrue(adsContext.getRowset().get(0).getCreative().getHasPackshot());
    }

    private void generateBanner() {
        GdAd ad = new GdCPMBannerAd().withAdGroup(defaultGdBaseGroup())
                .withCreative(new GdCreative()
                        .withName("video_addition_creative")
                        .withPreviewUrl("http://ya.ru")
                        .withHasPackshot(true));
        List<GdAd> gdAds = Collections.singletonList(ad);

        GdClientInfo queriedClient = gridGraphQLContext.getQueriedClient();
        doReturn(new GdAdWithTotals().withGdAds(gdAds))
                .when(bannerDataService)
                .getBanners(eq(queriedClient), eq(inputContainer), eq(gridGraphQLContext));
    }

}
