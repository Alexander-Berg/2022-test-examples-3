package ru.yandex.autotests.direct.cmd.banners.imagecreative;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.BannerType;
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.PerformanceCampaignHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersPerformanceRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsNull.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Удаление баннера с креативом в ТГО/РМП кампаниях")
@Stories(TestFeatures.Banners.DELETE_BANNER)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.DEL_BANNER)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class DeleteCreativeImageBannerTest {
    private static final String CLIENT = "at-direct-creative-construct";

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    private CreativeBannerRule bannersRule;
    private Long defaultBannerId;
    private Long creativeImageBannerId;

    public DeleteCreativeImageBannerTest(CampaignTypeEnum campaignType) {
        bannersRule = new CreativeBannerRule(campaignType).withUlogin(CLIENT);
        bannersRule.getGroup().getBanners().add(BannersFactory.getDefaultBanner(campaignType));
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Parameterized.Parameters(name = "Удаление баннера с креативом. Тип кампании {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Before
    public void before() {
        List<Banner> banners = cmdRule.cmdSteps().groupsSteps()
                .getBanners(CLIENT, bannersRule.getCampaignId(), bannersRule.getGroupId());
        creativeImageBannerId = banners.stream()
                .filter(t -> t.getAdType().equals(BannerType.IMAGE_AD.toString()))
                .findFirst().orElseThrow(() -> new DirectCmdStepsException("ГО не найден"))
                .getBid();
        defaultBannerId = banners.stream()
                .filter(t -> !t.getBid().equals(creativeImageBannerId))
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("текстовый баннер не найден"))
                .getBid();
    }

    @After
    public void after() {
        if (bannersRule.getCreativeId() != null) {
            PerformanceCampaignHelper.deleteCreativeQuietly(CLIENT, bannersRule.getCreativeId());
        }
    }

    @Test
    @Description("Удаление связки с креативом, после удаления ГО")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9260")
    public void deleteCreativeImageBannerDBTest() {
        deleteBanner(String.valueOf(creativeImageBannerId));
        assumeThat("остался один баннер", bannersRule.getCurrentGroup().getBanners(), hasSize(1));

        check();
    }

    @Test
    @Description("Удаление связки с креативом, после удаления группы с ГО")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9259")
    public void deleteCreativeImageBannerGroupDBTest() {
        deleteBanner(defaultBannerId.toString());
        deleteBanner(creativeImageBannerId.toString());
        assumeThat("группа удалилась", cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, bannersRule.getCampaignId().toString()).getGroups(), hasSize(0));

        check();
    }

    private void deleteBanner(String bannerId) {
        CommonResponse response = cmdRule.cmdSteps().bannerSteps()
                .deleteBanner(String.valueOf(bannersRule.getCampaignId()), String.valueOf(bannersRule.getGroupId()),
                        bannerId, CLIENT);
        assumeThat("удаление прошло успешно", response.getStatus(), equalTo("success"));
    }

    private void check() {
        BannersPerformanceRecord record = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersPerformanceSteps()
                .getBannersPerformance(creativeImageBannerId, bannersRule.getCreativeId());
        assertThat("связь креатива с баннером удалена", record, nullValue());
    }
}
