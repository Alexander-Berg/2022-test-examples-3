package ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.adv.direct.banner.resources.BannerResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BannerStopHandlerTest {

    private BannerStopHandler handler;

    @BeforeEach
    void before() {
        handler = mock(BannerStopHandler.class);
        when(handler.mapResourceToProto()).thenCallRealMethod();
    }

    @Test
    void mapResourceToProtoTest() {
        var bannerResourcesBuilder = BannerResources.newBuilder();

        handler.mapResourceToProto().invoke(true, bannerResourcesBuilder);
        bannerResourcesBuilder
                .setExportId(1L).setAdgroupId(2L).setBannerId(3L).setOrderId(4L).setIterId(5L).setUpdateTime(6L);

        var expectedBannerResources = BannerResources.newBuilder()
                .setExportId(1L).setAdgroupId(2L).setBannerId(3L).setOrderId(4L).setIterId(5L).setUpdateTime(6L)
                .setStop(true)
                .build();

        assertThat(bannerResourcesBuilder.build()).isEqualTo(expectedBannerResources);
    }
}
