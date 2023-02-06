package ru.yandex.autotests.direct.cmd.banners.imagecreative;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.data.banners.BannerErrors;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.CommonErrorsResource;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.common.CreativeBanner;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@RunWith(Parameterized.class)
public abstract class NegativeCreativeImageBannerTestBase {
    protected static final String CLIENT = "at-direct-creative-construct4";
    protected static final String ANOTHER_CLIENT = "at-direct-creative-construct5";
    private static final Long WRONG_CREATIVE_ID = 123L;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected CampaignRule campaignRule;
    protected Long campaignId;
    protected Banner newBanner;
    protected CampaignTypeEnum campaignType;

    @Parameterized.Parameters(name = "Сохранение невалидных графических объявлений. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Before
    public void before() {
        campaignId = campaignRule.getCampaignId();
        newBanner = BannersFactory.getDefaultImageBanner(campaignType);
    }

    @Description("Нельзя сохранить/изменить ГО без creative_id")
    public void imageBannerWithoutCreativeId() {
        newBanner.withCreativeBanner(new CreativeBanner());
        ErrorResponse response = saveGroup();
        assertThat("Получили ошибку", response.getError(),
                equalTo(CommonErrorsResource.WRONG_INPUT_DATA.toString()));
    }

    @Description("Нельзя сохранить/изменить ГО с несуществующим creative_id")
    public void imageBannerWithInvalidCreativeId() {
        newBanner.withCreativeBanner(new CreativeBanner().withCreativeId(WRONG_CREATIVE_ID));
        ErrorResponse response = saveGroup();
        assertThat("Получили ошибку", response.getError(),
                equalTo(BannerErrors.ERROR_CANVAS_CREATIVE_OR_PERMISSION_DENIED.toString()));
    }

    @Description("Нельзя сохранить/изменить ГО без creative")
    public void imageBannerWithoutCreative() {
        newBanner.withCreativeBanner(null);
        ErrorResponse response = saveGroup();
        assertThat("Получили ошибку", response.getError(),
                containsString(CommonErrorsResource.WRONG_INPUT_DATA.toString()));
    }

    @Description("Нельзя сохранить/изменить креатив ГО, загруженный другим пользователем")
    public void imageBannerWithAnotherLoginImageCreative() {
        Long creativeId = TestEnvironment.newDbSteps().useShardForLogin(ANOTHER_CLIENT).perfCreativesSteps()
                .saveDefaultCanvasCreativesForClient(Long.valueOf(User.get(ANOTHER_CLIENT).getClientID()));
        newBanner.withCreativeBanner(new CreativeBanner().withCreativeId(creativeId));
        ErrorResponse response = saveGroup();
        assertThat("Получили ошибку", response.getError(),
                equalTo(BannerErrors.ERROR_CANVAS_CREATIVE_OR_PERMISSION_DENIED.toString()));
    }

    protected ErrorResponse saveGroup() {
        switch (campaignType) {
            case TEXT:
                return cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroupsInvalidData(getGroupParameters());
            case MOBILE:
                return cmdRule.cmdSteps().groupsSteps().postSaveMobileAdGroupsInvalidData(getGroupParameters());
            default:
                throw new BackEndClientException("Не указан тип кампании");
        }

    }

    protected abstract GroupsParameters getGroupParameters();
}
