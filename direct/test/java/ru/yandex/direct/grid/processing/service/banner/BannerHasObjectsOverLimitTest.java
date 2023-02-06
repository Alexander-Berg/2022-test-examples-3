package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.grid.core.entity.banner.service.GridBannerConstants;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.banner.GdAd;
import ru.yandex.direct.grid.processing.model.banner.GdAdWithTotals;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContainer;
import ru.yandex.direct.grid.processing.model.banner.GdAdsContext;
import ru.yandex.direct.grid.processing.model.banner.GdTextAd;
import ru.yandex.direct.grid.processing.model.client.GdClient;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.service.cache.GridCacheService;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.grid.processing.data.TestGdAds.getDefaultGdAdsContainer;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdBaseGroup;

public class BannerHasObjectsOverLimitTest {

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
                .then(returnsSecondArg());

        inputContainer = getDefaultGdAdsContainer();
    }


    @Test
    public void checkHasNotObjectsOverLimit() {
        generateBanners(GridBannerConstants.getMaxBannerRows() - 1);
        GdAdsContext adsContext =
                adGraphQlService.getAds(gridGraphQLContext, mock(GdClient.class), inputContainer);

        assertThat(adsContext.getHasObjectsOverLimit())
                .isFalse();
    }

    @Test
    public void checkHasObjectsOverLimit() {
        generateBanners(GridBannerConstants.getMaxBannerRows());
        GdAdsContext adsContext =
                adGraphQlService.getAds(gridGraphQLContext, mock(GdClient.class), inputContainer);

        assertThat(adsContext.getHasObjectsOverLimit())
                .isTrue();
    }


    private void generateBanners(int bannersCount) {
        List<GdAd> gdAds = StreamEx.generate(() -> new GdTextAd().withAdGroup(defaultGdBaseGroup()))
                .limit(bannersCount)
                .collect(Collectors.toList());

        GdClientInfo queriedClient = gridGraphQLContext.getQueriedClient();
        doReturn(new GdAdWithTotals().withGdAds(gdAds))
                .when(bannerDataService)
                .getBanners(eq(queriedClient), eq(inputContainer), eq(gridGraphQLContext));
    }

}
