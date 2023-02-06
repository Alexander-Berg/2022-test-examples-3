package ru.yandex.direct.core.entity.banner.service.deleteoperation;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.repository.old.OldBannerMeasurersRepository;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannersDeleteOperationMeasurersTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestBannerRepository bannerRepository;

    @Autowired
    private TestCampaignRepository campaignRepository;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private OldBannerMeasurersRepository bannerMeasurersRepository;

    private int shard;

    private ClientId clientId;
    private Long clientUid;
    private Long bannerId;

    @Before
    public void before() {
        var bannerInfo = steps.bannerSteps().createActiveCpmBanner();
        var clientInfo = bannerInfo.getClientInfo();
        var campaignInfo = bannerInfo.getCampaignInfo();

        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
        clientUid = clientInfo.getUid();
        Long campaignId = campaignInfo.getCampaignId();
        bannerId = bannerInfo.getBannerId();

        bannerMeasurersRepository.addBannerMeasurers(shard, bannerInfo.getBanner().getMeasurers());
        bannerRepository.updateBannerId(shard, bannerInfo, 0L);
        campaignRepository.setCampaignMoney(campaignId, shard, BigDecimal.ZERO, BigDecimal.ZERO);
        assumeThat(bannerService.getCanBeDeletedBannersByIds(shard, clientId, List.of(bannerId)).get(bannerId),
                is(true));
    }

    @Test
    public void bannerDeleted_measurersDeleted() {
        var measurers = bannerMeasurersRepository.getMeasurersByBannerIds(shard, List.of(bannerId));
        assertThat(measurers.values(), not(empty()));

        MassResult<Long> result = bannerService.deleteBannersPartial(clientUid, clientId, List.of(bannerId));
        assertThat(result, isSuccessful());
        assertThat(result.get(0).getResult(), equalTo(bannerId));
        measurers = bannerMeasurersRepository.getMeasurersByBannerIds(shard, List.of(bannerId));
        assertThat(measurers.values(), empty());
    }
}
