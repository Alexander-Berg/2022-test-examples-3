package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BaseBannerWithResourcesForBsExport;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusshow;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BannerResourceType;
import ru.yandex.direct.ess.logicobjects.bsexport.resources.BsExportBannerResourcesObject;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResource;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.BannerResourcesStat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannerStopLoaderTest {

    private static final int SHARD = 1;
    private BannerTypedRepository newBannerTypedRepository;
    private BannerStopLoader loader;
    private BsOrderIdCalculator bsOrderIdCalculator;

    @BeforeEach
    void setUp() {
        this.newBannerTypedRepository = mock(BannerTypedRepository.class);
        this.bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        var context = new BannerResourcesLoaderContext(newBannerTypedRepository, new BannerResourcesHelper(bsOrderIdCalculator));
        this.loader = new BannerStopLoader(context);
    }

    private static Stream<Arguments> params() {
        /*
        statusShow, statusPostModerate, expectedStop
         */
        return Stream.of(
                Arguments.of(BannersStatusshow.No, BannerStatusPostModerate.NEW, true),
                Arguments.of(BannersStatusshow.No, BannerStatusPostModerate.NO, true),
                Arguments.of(BannersStatusshow.No, BannerStatusPostModerate.READY, true),
                Arguments.of(BannersStatusshow.No, BannerStatusPostModerate.SENT, true),
                Arguments.of(BannersStatusshow.No, BannerStatusPostModerate.YES, true),
                Arguments.of(BannersStatusshow.No, BannerStatusPostModerate.REJECTED, true),
                Arguments.of(BannersStatusshow.Yes, BannerStatusPostModerate.NEW, false),
                Arguments.of(BannersStatusshow.Yes, BannerStatusPostModerate.NO, false),
                Arguments.of(BannersStatusshow.Yes, BannerStatusPostModerate.READY, false),
                Arguments.of(BannersStatusshow.Yes, BannerStatusPostModerate.SENT, false),
                Arguments.of(BannersStatusshow.Yes, BannerStatusPostModerate.YES, false),
                Arguments.of(BannersStatusshow.Yes, BannerStatusPostModerate.REJECTED, true)
        );
    }

    @ParameterizedTest
    @MethodSource("params")
    void bannerStopTest(BannersStatusshow statusShow,
                        BannerStatusPostModerate statusPostModerate,
                        Boolean expectedStop) {
        mockOrderIdCalculator();
        BsExportBannerResourcesObject object = new BsExportBannerResourcesObject.Builder()
                .setBid(1L)
                .setResourceType(BannerResourceType.BANNER_STOP)
                .build();

        var bannerFromDb = getBannerWithCommonFields()
                .withStatusModerate(BannerStatusModerate.YES)
                .withStatusShow(BannersStatusshow.Yes.equals(statusShow))
                .withStatusPostModerate(statusPostModerate);

        doReturn(List.of(bannerFromDb))
                .when(newBannerTypedRepository)
                .getSafely(anyInt(), anyCollection(), eq(BaseBannerWithResourcesForBsExport.class));

        var res = loader.loadResources(SHARD, List.of(object));
        var expectedStat = new BannerResourcesStat().setSent(1).setCandidates(1);

        var expectedBannerResource = getResourceWithCommonFields()
                .setResource(expectedStop)
                .build();
        assertThat(res.getResources()).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(expectedBannerResource);
        assertThat(res.getStat()).isEqualToComparingFieldByFieldRecursively(expectedStat);
    }

    private BannerResource.Builder<Boolean> getResourceWithCommonFields() {
        return new BannerResource.Builder<Boolean>()
                .setBid(1L)
                .setPid(3L)
                .setCid(5L)
                .setBsBannerId(40L)
                .setOrderId(30L);
    }

    private BaseBannerWithResourcesForBsExport getBannerWithCommonFields() {
        return new TextBanner()
                .withId(1L)
                .withAdGroupId(3L)
                .withCampaignId(5L)
                .withBsBannerId(40L);
    }

    private void mockOrderIdCalculator() {
        when(bsOrderIdCalculator.calculateOrderIdIfNotExist(anyInt(), anyCollection())).thenReturn(Map.of(5L, 30L));
    }
}
