package ru.yandex.direct.core.entity.banner.type.creative.moderation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmGeoPinBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.rbac.RbacService;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.CPM_GEO_PIN_AUTO_MODERATION;
import static ru.yandex.direct.core.testing.data.TestNewCpmGeoPinBanners.clientCpmGeoPinBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;

@CoreTest
@RunWith(SpringRunner.class)
public class CpmGeoPinBannerWithCreativeModerationNoAutoModerationAddTest extends BannerAdGroupInfoAddOperationTestBase {
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Autowired
    private RbacService rbacService;

    @Before
    public void before() {
        ppcPropertiesSupport.remove(CPM_GEO_PIN_AUTO_MODERATION.getName());
    }

    @Test
    public void statusModerate() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmGeoPinAdGroup();
        Long creativeId = steps.creativeSteps()
                .addDefaultCanvasCreative(adGroupInfo.getClientInfo()).getCreativeId();

        Long permalinkId = createPermalinkId();

        CpmGeoPinBanner banner = clientCpmGeoPinBanner(creativeId, permalinkId)
                .withAdGroupId(adGroupInfo.getAdGroupId());


        Long id = prepareAndApplyValid(banner);

        CpmGeoPinBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeStatusModerate(), equalTo(BannerCreativeStatusModerate.READY));
        assertThat(actualBanner.getStatusModerate(), equalTo(BannerStatusModerate.READY));
    }

    private Long createPermalinkId() {
        long chiefUid = rbacService.getChiefByClientId(adGroupInfo.getClientId());

        Long permalinkId = defaultActiveOrganization(adGroupInfo.getClientId()).getPermalinkId();
        organizationsClient.addUidsByPermalinkId(permalinkId, List.of(chiefUid));
        return permalinkId;
    }
}
