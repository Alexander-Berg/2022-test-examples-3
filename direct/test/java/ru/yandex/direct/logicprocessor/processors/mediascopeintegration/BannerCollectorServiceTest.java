package ru.yandex.direct.logicprocessor.processors.mediascopeintegration;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.banner.type.measurers.BannerMeasurersRepository;
import ru.yandex.direct.ess.logicobjects.mediascopeintegration.MediascopePositionChangeObject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
class BannerCollectorServiceTest {

    private static final int SHARD = 1;

    private BannerRelationsRepository bannerRelationsRepository;
    private BannerMeasurersRepository bannerMeasurersRepository;

    private BannerCollectorService bannerCollectorService;

    private List<Long> campaignIds = List.of(44L);
    private List<Long> bannerIds = List.of(55L, 66L, 77L);

    @BeforeEach
    void before() {
        bannerRelationsRepository = mock(BannerRelationsRepository.class);
        bannerMeasurersRepository = mock(BannerMeasurersRepository.class);

        bannerCollectorService = new BannerCollectorService(bannerRelationsRepository, bannerMeasurersRepository);
    }

    @Test
    void getBannerIdsWithIntegration_FewBanners_ReturnIdsWithIntegration() {
        var logicObjects = List.of(
                new MediascopePositionChangeObject(null, bannerIds.get(0)),
                new MediascopePositionChangeObject(campaignIds.get(0), null));

        when(bannerRelationsRepository.getBannerIdsByCampaignIds(SHARD, campaignIds)).thenReturn(
                List.of(bannerIds.get(1)));

        Map<Long, List<BannerMeasurer>> measurersMap = Map.of(
                bannerIds.get(0), List.of(
                        new BannerMeasurer()
                                .withBannerMeasurerSystem(BannerMeasurerSystem.ADMETRICA)
                                .withHasIntegration(true),
                        new BannerMeasurer()
                                .withBannerMeasurerSystem(BannerMeasurerSystem.MEDIASCOPE)
                                .withHasIntegration(false)),
                bannerIds.get(1), List.of(
                        new BannerMeasurer()
                                .withBannerMeasurerSystem(BannerMeasurerSystem.ADMETRICA)
                                .withHasIntegration(true),
                        new BannerMeasurer()
                                .withBannerMeasurerSystem(BannerMeasurerSystem.MEDIASCOPE)
                                .withHasIntegration(true)));
        when(bannerMeasurersRepository.getMeasurersByBannerIds(anyInt(), anyCollection())).thenReturn(measurersMap);

        var result = bannerCollectorService.getBannerIdsWithIntegration(SHARD, logicObjects);
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(bannerIds.get(1)));
    }
}
