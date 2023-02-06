package ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.adv.direct.banner.resources.BannerResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannerMobileContentDataHandlerTest {

    private BannerMobileContentDataHandler handler;

    @BeforeEach
    void before() {
        handler = mock(BannerMobileContentDataHandler.class);
        when(handler.mapResourceToProto()).thenCallRealMethod();
    }

    @Test
    void mapResourceToProtoTest() {
        var bannerResourcesBuilder = BannerResources.newBuilder();

        handler.mapResourceToProto().invoke("https://view.adjust.com/impression/q1w2e3", bannerResourcesBuilder);
        bannerResourcesBuilder
                .setExportId(1L).setAdgroupId(2L).setBannerId(3L).setOrderId(4L).setIterId(5L).setUpdateTime(6L);

        var expectedBannerResources = BannerResources.newBuilder()
                .setExportId(1L).setAdgroupId(2L).setBannerId(3L).setOrderId(4L).setIterId(5L).setUpdateTime(6L)
                .setMobileContentImpressionUrl("https://view.adjust.com/impression/q1w2e3")
                .build();

        assertThat(bannerResourcesBuilder.build()).isEqualTo(expectedBannerResources);
    }
}
