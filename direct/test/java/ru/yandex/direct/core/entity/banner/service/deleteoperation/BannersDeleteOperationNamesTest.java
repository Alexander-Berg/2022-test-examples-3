package ru.yandex.direct.core.entity.banner.service.deleteoperation;

import java.util.List;
import java.util.Set;

import jdk.jfr.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerNameStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerNamesRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannersDeleteOperationNamesTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private TestBannerNamesRepository bannerNamesRepository;

    private int shard;
    private ClientId clientId;
    private Long clientUid;
    private NewTextBannerInfo bannerToDelete;
    private NewTextBannerInfo bannerToKeep;

    @Before
    public void before() {
        bannerToDelete = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo().withBanner(
                        fullTextBanner()
                                .withStatusModerate(BannerStatusModerate.NEW)
                                .withBsBannerId(0L)
                                .withName("test name")
                                .withNameStatusModerate(BannerNameStatusModerate.NEW)));

        bannerToKeep = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo().withBanner(
                        fullTextBanner()
                                .withStatusModerate(BannerStatusModerate.NEW)
                                .withBsBannerId(0L)
                                .withName("test name")
                                .withNameStatusModerate(BannerNameStatusModerate.NEW)));

        var clientInfo = bannerToDelete.getClientInfo();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
        clientUid = clientInfo.getUid();
    }

    @Test
    @Description("Проверяем, что у баннера, который удаляли, banner_names удаляются. А у баннера, который не " +
            "удаляли, banner_names не удаляются.")
    public void bannerDeleted_namesDeleted() {
        MassResult<Long> result = bannerService.deleteBannersPartial(clientUid, clientId,
                List.of(bannerToDelete.getBannerId()));
        assertThat(result, isSuccessful());
        assertThat(result.get(0).getResult(), equalTo(bannerToDelete.getBannerId()));

        Set<Long> bannersWithName = bannerNamesRepository.getBannersWithName(shard,
                List.of(bannerToDelete.getBannerId(), bannerToKeep.getBannerId()));
        assertThat(bannersWithName, equalTo(Set.of(bannerToKeep.getBannerId())));
    }

}
