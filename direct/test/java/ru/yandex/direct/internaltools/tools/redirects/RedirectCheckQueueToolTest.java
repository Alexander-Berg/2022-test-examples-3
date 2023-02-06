package ru.yandex.direct.internaltools.tools.redirects;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.redirectcheckqueue.service.RedirectCheckQueueService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.redirects.container.RedirectCheckQueueItem;
import ru.yandex.direct.internaltools.tools.redirects.model.RedirectCheckQueueParameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultTextBanner;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RedirectCheckQueueToolTest {
    @Autowired
    private RedirectCheckQueueTool tool;
    @Autowired
    private RedirectCheckQueueService service;
    @Autowired
    private BannerSteps bannerSteps;
    @Autowired
    private AdGroupSteps adGroupSteps;

    private TextBannerInfo bannerOne;
    private TextBannerInfo bannerTwo;

    @Before
    public void before() {
        String randomDomain = UUID.randomUUID().toString();
        AdGroupInfo activeTextAdGroup = adGroupSteps.createActiveTextAdGroup();
        bannerOne = bannerSteps
                .createBanner(defaultTextBanner(activeTextAdGroup.getCampaignId(), activeTextAdGroup.getAdGroupId())
                                .withDomain(randomDomain),
                        activeTextAdGroup);
        bannerTwo = bannerSteps
                .createBanner(defaultTextBanner(activeTextAdGroup.getCampaignId(), activeTextAdGroup.getAdGroupId())
                                .withDomain(randomDomain),
                        activeTextAdGroup);
        service.pushBannersIntoQueue(Collections.singletonList(bannerOne.getBannerId()));
    }

    @Test
    public void testGetData() {
        List<RedirectCheckQueueItem> massData = tool.getMassData();

        assertThat(massData)
                .isNotNull();
        assertThat(massData)
                .size().isGreaterThanOrEqualTo(1);

        RedirectCheckQueueItem item = null;
        for (RedirectCheckQueueItem massDatum : massData) {
            if (massDatum.getDomain().equals(bannerOne.getBanner().getDomain())) {
                item = massDatum;
                break;
            }
        }
        assertThat(item)
                .isNotNull();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(item.getCampaignsNum())
                .isEqualTo(1);
        soft.assertThat(item.getBannersNum())
                .isEqualTo(1);
        soft.assertThat(item.getSecondsInQueue())
                .isGreaterThanOrEqualTo(0);
        soft.assertAll();
    }

    @Test
    public void testAddBannerAndGetData() {
        RedirectCheckQueueParameters parameters = new RedirectCheckQueueParameters()
                .withBannerIds(String.valueOf(bannerTwo.getBannerId()));
        parameters.setOperator(mock(User.class, RETURNS_DEEP_STUBS));
        List<RedirectCheckQueueItem> massData = tool.getMassData(parameters);

        assertThat(massData)
                .isNotNull();
        assertThat(massData)
                .size().isGreaterThanOrEqualTo(1);

        RedirectCheckQueueItem item = null;
        for (RedirectCheckQueueItem massDatum : massData) {
            if (massDatum.getDomain().equals(bannerOne.getBanner().getDomain())) {
                item = massDatum;
                break;
            }
        }
        assertThat(item)
                .isNotNull();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(item.getCampaignsNum())
                .isEqualTo(1);
        soft.assertThat(item.getBannersNum())
                .isEqualTo(2);
        soft.assertThat(item.getSecondsInQueue())
                .isGreaterThanOrEqualTo(0);
        soft.assertAll();
    }

    @Test
    public void testAddCampaignAndGetData() {
        RedirectCheckQueueParameters parameters = new RedirectCheckQueueParameters()
                .withCampaignIds(String.valueOf(bannerTwo.getCampaignId()));
        parameters.setOperator(mock(User.class, RETURNS_DEEP_STUBS));
        List<RedirectCheckQueueItem> massData = tool.getMassData(parameters);

        assertThat(massData)
                .isNotNull();
        assertThat(massData)
                .size().isGreaterThanOrEqualTo(1);

        RedirectCheckQueueItem item = null;
        for (RedirectCheckQueueItem massDatum : massData) {
            if (bannerOne.getBanner().getDomain().equals(massDatum.getDomain())) {
                item = massDatum;
                break;
            }
        }
        assertThat(item)
                .isNotNull();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(item.getCampaignsNum())
                .isEqualTo(1);
        soft.assertThat(item.getBannersNum())
                .isEqualTo(2);
        soft.assertThat(item.getSecondsInQueue())
                .isGreaterThanOrEqualTo(0);
        soft.assertAll();
    }
}
