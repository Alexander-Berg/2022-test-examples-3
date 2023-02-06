package ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.adv.direct.banner.resources.BannerResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BannerNamesHandlerTest {

    private BannerNamesHandler handler;

    @BeforeEach
    void before() {
        handler = mock(BannerNamesHandler.class);
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
                .setName("")
                .build();

        assertThat(bannerResourcesBuilder.build()).isEqualTo(expectedBannerResources);
    }

    @Test
    void mapResourceToProtoTest() {
        var bannerResourcesBuilder = BannerResources.newBuilder();

        handler.mapResourceToProto().invoke("SomeName", bannerResourcesBuilder);
        bannerResourcesBuilder
                .setExportId(1L).setAdgroupId(2L).setBannerId(3L).setOrderId(4L).setIterId(5L).setUpdateTime(6L);

        var expectedBannerResources = BannerResources.newBuilder()
                .setExportId(1L).setAdgroupId(2L).setBannerId(3L).setOrderId(4L).setIterId(5L).setUpdateTime(6L)
                .setName("SomeName")
                .build();

        assertThat(bannerResourcesBuilder.build()).isEqualTo(expectedBannerResources);
    }

}
