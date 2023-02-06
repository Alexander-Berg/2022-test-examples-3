package ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.adv.direct.banner.resources.BannerResources;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.TurboLandingInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannerTurboLandingHandlerTest {

    private BannerTurboLandingHandler handler;

    @BeforeEach
    void before() {
        handler = mock(BannerTurboLandingHandler.class);
        when(handler.mapResourceToProto()).thenCallRealMethod();
    }

    @Test
    void mapResourceToProto_NullResourceTest() {
        var bannerResourcesBuilder = BannerResources.newBuilder();
        handler.mapResourceToProto().invoke(null, bannerResourcesBuilder);
        bannerResourcesBuilder
                .setExportId(1L).setAdgroupId(2L).setBannerId(3L).setOrderId(4L).setIterId(5L).setUpdateTime(6L);

        var expectedBannerResources = BannerResources.newBuilder()
                .setExportId(1L).setAdgroupId(2L).setBannerId(3L).setOrderId(4L).setIterId(5L).setUpdateTime(6L)
                .setTurbolandingId(0L)
                .setTurbolandingHref("")
                .setTurbolandingSite("")
                .setTurbolandingDomainFilter("")
                .build();

        assertThat(bannerResourcesBuilder.build()).isEqualTo(expectedBannerResources);
    }

    @Test
    void mapResourceToProtoTest() {
        var bannerResourcesBuilder = BannerResources.newBuilder();

        var turboLanding = TurboLandingInfo.builder()
                .withTurbolandingId(123L)
                .withHref("yandex.ru/turbo")
                .withSite("yandex.ru")
                .withDomainFilter("123.y-turbo")
                .build();
        handler.mapResourceToProto().invoke(turboLanding, bannerResourcesBuilder);
        bannerResourcesBuilder
                .setExportId(1L).setAdgroupId(2L).setBannerId(3L).setOrderId(4L).setIterId(5L).setUpdateTime(6L);

        var expectedBannerResources = BannerResources.newBuilder()
                .setExportId(1L).setAdgroupId(2L).setBannerId(3L).setOrderId(4L).setIterId(5L).setUpdateTime(6L)
                .setTurbolandingId(123L)
                .setTurbolandingHref("yandex.ru/turbo")
                .setTurbolandingSite("yandex.ru")
                .setTurbolandingDomainFilter("123.y-turbo")
                .build();

        assertThat(bannerResourcesBuilder.build()).isEqualTo(expectedBannerResources);
    }
}
