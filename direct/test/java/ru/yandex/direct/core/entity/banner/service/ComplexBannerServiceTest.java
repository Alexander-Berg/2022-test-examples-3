package ru.yandex.direct.core.entity.banner.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.banner.container.ComplexBanner;
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithCreative;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestVcards;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexBannerServiceTest {

    @Autowired
    private ComplexBannerService complexBannerService;

    @Autowired
    private Steps steps;

    private ClientId clientId;

    private NewTextBannerInfo bannerInfo;
    private VcardInfo vcardInfo;
    private CreativeInfo creativeInfo;
    private SitelinkSetInfo sitelinkSetInfo;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();

        vcardInfo = steps.vcardSteps().createVcard(TestVcards.fullVcard(), clientInfo);
        sitelinkSetInfo = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);
        creativeInfo = steps.creativeSteps().createCreative(clientInfo);

        bannerInfo = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withClientInfo(clientInfo)
                        .withBanner(
                                fullTextBanner()
                                        .withCalloutIds(emptyList())
                                        .withSitelinksSetId(sitelinkSetInfo.getSitelinkSetId())
                                        .withVcardId(vcardInfo.getVcardId())
                                        .withCreativeId(creativeInfo.getCreativeId())
                                        .withCreativeStatusModerate(BannerCreativeStatusModerate.YES))
        );
    }

    @Test
    public void getComplexBannersByAdGroupIds_ReturnAllProperties() {
        List<ComplexBanner> complexBanners =
                complexBannerService.getComplexBannersByAdGroupIds(clientId, bannerInfo.getUid(),
                        singletonList(bannerInfo.getAdGroupId()));

        assertThat(complexBanners, hasSize(1));

        ComplexBanner expectedComplexBanner = new ComplexBanner()
                .withBanner(bannerInfo.getBanner())
                .withCreative(creativeInfo.getCreative())
                .withSitelinkSet(sitelinkSetInfo.getSitelinkSet())
                .withVcard(vcardInfo.getVcard());

        assertThat(complexBanners.get(0), beanDiffer(expectedComplexBanner)
                .useCompareStrategy(DefaultCompareStrategies.allFieldsExcept(
                        newPath(ComplexBanner.BANNER.name(), BannerWithSystemFields.LAST_CHANGE.name()),
                        newPath(ComplexBanner.VCARD.name(), Vcard.LAST_CHANGE.name()),
                        newPath(ComplexBanner.VCARD.name(), Vcard.LAST_DISSOCIATION.name()),
                        newPath(ComplexBanner.SITELINK_SET.name(), SitelinkSet.SITELINKS.name(), "\\d+",
                                Sitelink.ORDER_NUM.name()),
                        newPath(ComplexBanner.BANNER.name(), BannerWithCreative.CREATIVE_RELATION_ID.name()),
                        newPath(ComplexBanner.BANNER.name(), BannerWithCreative.CREATIVE_STATUS_MODERATE.name()),
                        newPath(ComplexBanner.BANNER.name(), BannerWithCreative.SHOW_TITLE_AND_BODY.name())
                )));
    }
}
