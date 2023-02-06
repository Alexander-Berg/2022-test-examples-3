package ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.adv.direct.UInt64List;
import ru.yandex.adv.direct.banner.resources.BannerResources;
import ru.yandex.direct.core.bsexport.resources.model.PermalinkAssignType;
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.PermalinksInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannerPermalinksHandlerTest {

    private BannerPermalinksHandler handler;

    @BeforeEach
    void before() {
        handler = mock(BannerPermalinksHandler.class);
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
                .setPermalinkId(0)
                .setPermalinkHref("")
                .setPermalinkSite("")
                .setPermalinkDomainFilter("")
                .setPermalinkAssignType("")
                .setPermalinkChainIds(UInt64List.newBuilder().build())
                .build();

        assertThat(bannerResourcesBuilder.build()).isEqualTo(expectedBannerResources);
    }

    @Test
    void mapResourceToProtoTest() {
        var bannerResourcesBuilder = BannerResources.newBuilder();

        var permalinksInfo = PermalinksInfo.builder()
                .withPermalinkId(12345L)
                .withPermalinkHref("https://yandex.ru/profile/12345")
                .withPermalinkSite("yandex.ru")
                .withPermalinkDomainFilter("12345")
                .withPermalinkAssignType(PermalinkAssignType.MANUAL)
                .withPermalinkChainIds(List.of(1L, 2L, 3L))
                .build();
        handler.mapResourceToProto().invoke(permalinksInfo, bannerResourcesBuilder);
        bannerResourcesBuilder
                .setExportId(1L).setAdgroupId(2L).setBannerId(3L).setOrderId(4L).setIterId(5L).setUpdateTime(6L);

        var expectedBannerResources = BannerResources.newBuilder()
                .setExportId(1L).setAdgroupId(2L).setBannerId(3L).setOrderId(4L).setIterId(5L).setUpdateTime(6L)
                .setPermalinkId(12345L)
                .setPermalinkHref("https://yandex.ru/profile/12345")
                .setPermalinkSite("yandex.ru")
                .setPermalinkDomainFilter("12345")
                .setPermalinkAssignType("manual")
                .setPermalinkChainIds(
                        UInt64List.newBuilder()
                            .addValues(1L).addValues(2L).addValues(3L)
                            .build())
                .build();

        assertThat(bannerResourcesBuilder.build()).isEqualTo(expectedBannerResources);
    }
}
