package ru.yandex.direct.core.entity.banner.repository;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithCreative;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.NewCpmBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerTypedRepositoryCreativeFilterTest {

    @Autowired
    private Steps steps;
    @Autowired
    private BannerTypedRepository typedRepository;

    private Long creativeId;
    private int shard;
    private ClientId clientId;
    private Long expectedBannerId;

    @Before
    public void setUp() {
        ClientInfo defaultClient = steps.clientSteps().createDefaultClient();
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCanvasCreative(defaultClient);
        creativeId = creativeInfo.getCreativeId();
        shard = defaultClient.getShard();
        clientId = defaultClient.getClientId();

        NewCpmBannerInfo bannerInfo = steps.cpmBannerSteps().createCpmBanner(
                new NewCpmBannerInfo()
                        .withClientInfo(defaultClient)
                        .withCreative(creativeInfo.getCreative())
                        .withBanner(fullCpmBanner(creativeId)));

        expectedBannerId = bannerInfo.getBannerId();
    }

    @Test
    public void testGetBannersByCreativeIds() {
        List<BannerWithCreative> banners = typedRepository
                .getBannersByCreativeIds(shard, clientId, singleton(creativeId), BannerWithCreative.class);

        assumeThat(banners, hasSize(1));

        SoftAssertions.assertSoftly(softly -> {
            BannerWithCreative actualBanner = banners.get(0);
            softly.assertThat(actualBanner.getCreativeId()).isEqualTo(creativeId);
            softly.assertThat(actualBanner.getId()).isEqualTo(expectedBannerId);
        });
    }
}
