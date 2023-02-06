package ru.yandex.direct.logicprocessor.processors.metrika.bannerupdate;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.banner.type.image.BannerImageRepository;
import ru.yandex.direct.core.entity.metrika.container.BannerWithTitleAndBody;
import ru.yandex.direct.core.entity.metrika.repository.MetrikaBannerRepository;
import ru.yandex.direct.ess.common.utils.TablesEnum;
import ru.yandex.direct.ess.logicobjects.metrika.bannerupdate.MetrikaBannerUpdateObject;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetrikaBannerUpdateServiceTest {
    private MetrikaBannerUpdateService metrikaBannerUpdateService;
    private MetrikaBannerRepository metrikaBannerRepository;
    private BannerImageRepository bannerImageRepository;

    @BeforeEach
    void before() {
        metrikaBannerRepository = mock(MetrikaBannerRepository.class);
        bannerImageRepository = mock(BannerImageRepository.class);
        metrikaBannerUpdateService = new MetrikaBannerUpdateService(metrikaBannerRepository, bannerImageRepository);
    }

    /**
     * Изменился тестовый баннер, у которого есть несколько изображений
     * В этом случае должны учесться изменения и родительского и всех его изображений
     */
    @Test
    void testGetMetrikaBannerUpdateResultList_ChangedBannerHasImages() {
        long bannerId = 1L;
        long bsBannerId = 2L;
        long imageBsBannerId1 = 3L;
        long imageBsBannerId2 = 4L;
        String title = "title";
        String body = "body";
        int shard = 0;
        var metrikaBannerUpdateObject = new MetrikaBannerUpdateObject(TablesEnum.BANNERS,
                bannerId);

        when(bannerImageRepository.getBannerIdsFromBids(eq(shard), eq(singleton(bannerId)))).thenReturn(
                Map.of(imageBsBannerId1, bannerId, imageBsBannerId2, bannerId));

        when(metrikaBannerRepository.getBannerWithTitleAndBodyFromBids(eq(shard), eq(singleton(bannerId)))).thenReturn(
                Map.of(bannerId, new BannerWithTitleAndBody(bannerId, bsBannerId, title, body)));

        List<MetrikaBannerUpdateResult> got = metrikaBannerUpdateService.getMetrikaBannerUpdateResultList(shard,
                singletonList(metrikaBannerUpdateObject));

        var expected = new MetrikaBannerUpdateResult[]{
                new MetrikaBannerUpdateResult(bannerId, bsBannerId, title, body),
                new MetrikaBannerUpdateResult(bannerId, imageBsBannerId1, title, body),
                new MetrikaBannerUpdateResult(bannerId, imageBsBannerId2, title, body)
        };

        assertThat(got).hasSize(3);
        assertThat(got).containsExactlyInAnyOrder(expected);
    }

    /**
     * При изменении изображения одновременно пришло изменение родительского баннера
     * В этом случае изменение изображения должно учесться один раз
     */
    @Test
    void testGetMetrikaBannerUpdateResultList_ImageAndBannerWithSameBid() {
        long bannerId = 1L;
        long bsBannerId = 2L;
        long imageId = 3L;
        long imageBsBannerId = 3L;
        String title = "title";
        String body = "body";
        int shard = 0;
        var metrikaBannerUpdateObject = new MetrikaBannerUpdateObject(TablesEnum.BANNERS,
                bannerId);

        var metrikaBannerUpdateObjectBannerImage = new MetrikaBannerUpdateObject(TablesEnum.BANNER_IMAGES,
                imageId);

        when(metrikaBannerRepository.getBannerWithTitleAndBodyFromImageIds(shard, singleton(imageId))).thenReturn(
                singletonList(new BannerWithTitleAndBody(bannerId, imageBsBannerId, title, body))
        );

        when(bannerImageRepository.getBannerIdsFromBids(eq(shard), eq(singleton(bannerId)))).thenReturn(
                Map.of(imageBsBannerId, bannerId));

        when(metrikaBannerRepository.getBannerWithTitleAndBodyFromBids(eq(shard), eq(singleton(bannerId)))).thenReturn(
                Map.of(bannerId, new BannerWithTitleAndBody(bannerId, bsBannerId, title, body)));

        List<MetrikaBannerUpdateResult> got = metrikaBannerUpdateService.getMetrikaBannerUpdateResultList(shard,
                List.of(metrikaBannerUpdateObject, metrikaBannerUpdateObjectBannerImage));

        var expected = new MetrikaBannerUpdateResult[]{
                new MetrikaBannerUpdateResult(bannerId, bsBannerId, title, body),
                new MetrikaBannerUpdateResult(bannerId, imageBsBannerId, title, body)
        };

        assertThat(got).hasSize(2);
        assertThat(got).containsExactlyInAnyOrder(expected);
    }
}
