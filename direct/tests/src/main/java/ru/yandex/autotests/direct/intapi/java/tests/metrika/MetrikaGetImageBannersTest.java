package ru.yandex.autotests.direct.intapi.java.tests.metrika;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.images.AbstractImageUploadHelper;
import ru.yandex.autotests.direct.cmd.steps.images.ImageParams;
import ru.yandex.autotests.direct.cmd.steps.images.ImageUploadHelper;
import ru.yandex.autotests.direct.cmd.util.ImageUtils;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.models.MetrikaBannersParam;
import ru.yandex.autotests.direct.intapi.models.MetrikaBannersResult;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.beans.images.ImageFormat;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("metrika/banners")
@Stories(TestFeatures.Metrika.BANNERS)
@Features(TestFeatures.METRIKA)
@Tag(Tags.METRIKA)
@Tag(TagDictionary.TRUNK)
@Tag("DIRECT-64729")
@Issue("DIRECT-64729")
public class MetrikaGetImageBannersTest {
    public static final String ULOGIN = "at-direct-intapi-metrika2";
    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    private ImageParams imageParams = new ImageParams().
            withFormat(ImageUtils.ImageFormat.JPG).
            withWidth(1080).
            withHeight(607).
            withResizeX1(0).
            withResizeX2(1080).
            withResizeY1(0).
            withResizeY2(607);

    private MobileBannersRule bannersRule = new MobileBannersRule()
            .withImageUploader((ImageUploadHelper) new ImageUploadHelper()
                    .withImageParams(imageParams)
                    .withUploadType(AbstractImageUploadHelper.UploadType.FILE)
                    .withClient(ULOGIN))
            .withUlogin(ULOGIN);

    private DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(bannersRule).as(ULOGIN);
    @Rule
    public DirectRule directRule = DirectRule.defaultRule()
            .withRules(cmdRule);

    @Test
    public void getImageBanners() {
        Long orderId = RandomUtils.nextLong(0L, Integer.MAX_VALUE);
        Long clientId = directRule.dbSteps().shardingSteps().getClientIdByLogin(ULOGIN);
        cmdRule.apiSteps().bannersFakeSteps().setBannerFakeImageBannerIDRandom(bannersRule.getBannerId());
        Long bsBannerId =
                (long) cmdRule.apiSteps().bannersFakeSteps().getBannerParams(bannersRule.getBannerId())
                        .getImageBannerID();

        directRule.dbSteps().shardingSteps().createOrderIdMapping(orderId, clientId);
        directRule.dbSteps().useShardForLogin(ULOGIN).campaignsSteps()
                .setOrderId(bannersRule.getCampaignId(), orderId);

        MetrikaBannersParam requestParam = new MetrikaBannersParam()
                .withBannerId(bsBannerId)
                .withOrderId(orderId);

        List<MetrikaBannersResult> results =
                directRule.intapiSteps().metrikaControllerSteps()
                        .getBanners(Collections.singletonList(requestParam));
        assumeThat("В результате получили 1 н баннер", results, Matchers.hasSize(1));

        ShowCampResponse createdCamp =
                cmdRule.cmdSteps().campaignSteps().getShowCamp(ULOGIN, bannersRule.getCampaignId().toString());

        assumeThat("В кампании есть 1н баннер", createdCamp.getGroups(), Matchers.hasSize(1));

        Banner createdBanner = createdCamp.getGroups().get(0);

        MetrikaBannersResult expected = new MetrikaBannersResult()
                .withOrderId(orderId)
                .withBody(createdBanner.getBody())
                .withDomain(createdBanner.getDomain())
                .withIsImageBanner(true)
                .withTitle(createdBanner.getTitle())
                .withBid(bannersRule.getBannerId())
                .withBannerId(bsBannerId);

        assertThat("Получили верные параметры баннера", results.get(0), beanDiffer(expected));
    }
}
