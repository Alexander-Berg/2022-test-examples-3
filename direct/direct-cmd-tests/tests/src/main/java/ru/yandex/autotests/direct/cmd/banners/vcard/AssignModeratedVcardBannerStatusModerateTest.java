package ru.yandex.autotests.direct.cmd.banners.vcard;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.DirectCmdSteps;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.BeanMapper;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Статус модерации после привязки промодерированной визитки к баннеру")
@Stories(TestFeatures.Banners.VCARD)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.VCARD)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.DYNAMIC)
@Tag("TESTIRT-9435")
@RunWith(Parameterized.class)
public class AssignModeratedVcardBannerStatusModerateTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    public CampaignTypeEnum campaignType;

    @Parameterized.Parameters(name = "statusModerate баннера после добавления промодерированной визитки." +
            " Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.DTO}
        });
    }


    public AssignModeratedVcardBannerStatusModerateTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .withUlogin(CLIENT);
        Banner secondBanner = BeanMapper.map(bannersRule.getBanner(), Banner.class);
        bannersRule.getGroup().getBanners().add(secondBanner);
        bannersRule.overrideBannerTemplate(new Banner()
                .withHasVcard(1)
                .withContactInfo(BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class)
                        .withOGRN(null)));
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    private DirectCmdSteps directCmdSteps;
    private Long vcardId;
    private Long secondBannerId;

    @Before
    public void before() {
        directCmdSteps = cmdRule.cmdSteps();
        vcardId = bannersRule.getCurrentGroup().getBanners().get(0).getVcardId();
        secondBannerId = TestEnvironment.newDbSteps(CLIENT)
                .bannersSteps().getBannersByCid(bannersRule.getCampaignId())
                .stream().filter(t -> !t.getBid().equals(bannersRule.getBannerId()))
                .findFirst().get().getBid().longValue();
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignFullyModerated(bannersRule.getCampaignId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannersRule.getBannerId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(secondBannerId);
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(bannersRule.getGroupId());
    }

    @Test
    @Description("статус модерации при привязки промодерированной визитки")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10816")
    public void checkStatusModerateApartChange() {
        ErrorResponse response = directCmdSteps.vCardsSteps()
                .assignVCard(String.valueOf(vcardId), String.valueOf(secondBannerId), CLIENT);
        assumeThat("карточка привязалась", response.getError(), nullValue());
        check();
    }

    private void check() {
        assertThat("statusModerate не изменился", bannersRule.getCurrentGroup().getBanners()
                        .stream()
                        .filter(t -> t.getBid().equals(secondBannerId))
                        .findFirst().get().getStatusModerate(),
                equalTo(StatusModerate.YES.toString()));
    }

}
