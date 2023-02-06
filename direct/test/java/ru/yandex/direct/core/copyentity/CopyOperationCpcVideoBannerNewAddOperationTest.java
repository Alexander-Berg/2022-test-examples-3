package ru.yandex.direct.core.copyentity;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBannersService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupsAddOperationTestBase;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestNewCpcVideoBanners;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.NewCpcVideoBannerInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;

import static ru.yandex.direct.core.copyentity.CopyOperationAssert.Mode.COPIED;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CopyOperationCpcVideoBannerNewAddOperationTest extends AdGroupsAddOperationTestBase {

    @Autowired
    private CopyOperationFactory factory;

    @Autowired
    private CopyOperationAssert asserts;

    @Autowired
    private AdGroupWithBannersService adGroupWithBannersService;

    @Autowired
    private TestNewCpcVideoBanners testNewCpcVideoBanners;

    private Long uid;
    private ClientId clientId;
    private ClientInfo clientInfo;

    private Long campaignIdFrom;
    private CampaignInfo campaignInfoFrom;

    @Before
    public void setUp() {
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);

        uid = superClientInfo.getUid();

        clientInfo = steps.clientSteps().createDefaultClient();

        clientId = clientInfo.getClientId();

        campaignInfoFrom = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid())
                        .withEmail("test@yandex-team.ru"),
                clientInfo);
        campaignIdFrom = campaignInfoFrom.getCampaignId();

        asserts.init(clientId, clientId, uid);
    }

    @Test
    public void copyCpcVideoBanner() {
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfoFrom);
        NewCpcVideoBannerInfo bannerInfo = createBannerForCopy(adGroupInfo);

        var xerox = factory.build(copyConfig(adGroupInfo.getAdGroupId()));

        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        Set<Long> copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientId, uid, copiedAdGroupIds);
        asserts.assertEntitiesAreCopied(BannerWithAdGroupId.class, copiedBannerIds,
                List.of(bannerInfo.getBanner()), COPIED);
    }

    private NewCpcVideoBannerInfo createBannerForCopy(AdGroupInfo adGroupInfo) {
        Long creativeId = createCreative();

        return steps.cpcVideoBannerSteps().createBanner(
                new NewCpcVideoBannerInfo()
                        .withBanner(testNewCpcVideoBanners.fullCpcVideoBanner(creativeId)
                                .withStatusModerate(BannerStatusModerate.READY))
                        .withAdGroupInfo(adGroupInfo));
    }

    private Long createCreative() {
        Creative creative = defaultCpcVideoForCpcVideoBanner(clientId, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);
        return creativeInfo.getCreativeId();
    }

    private CopyConfig copyConfig(Long copyId) {
        return CopyEntityTestUtils.adGroupCopyConfig(clientInfo, copyId, campaignIdFrom, uid);
    }
}
