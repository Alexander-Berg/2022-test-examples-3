package ru.yandex.autotests.direct.cmd.bssynced.href;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.common.AppMetricaHrefs;
import ru.yandex.autotests.direct.cmd.data.banners.BannersFactory;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;

import static java.util.Collections.emptyMap;


@RunWith(Parameterized.class)
public abstract class CoefGoalContextIdBaseTest {
    protected static final String CLIENT = "at-direct-bids-1";
    protected static final String PARAM_WITH_COEF_GOAL_CONTEXT = "?test={coef_goal_context_id}";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected BannersRule bannerRule;
    protected Long campaignId;

    protected CampaignTypeEnum campaignType;
    protected String href;

    @Parameterized.Parameters(name = "Тип кампании: {0}, ссылка: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT, "https://yandex.ru"},
                {CampaignTypeEnum.MOBILE, AppMetricaHrefs.HREF_THREE}
        });
    }

    public abstract String getNewHref();

    public abstract void check();

    @Before
    public void before() {
        campaignId = bannerRule.getCampaignId();
        BsSyncedHelper.setGroupBsSynced(cmdRule, bannerRule.getGroupId(), StatusBsSynced.YES);
        BsSyncedHelper.setBannerBsSynced(cmdRule, bannerRule.getBannerId(), StatusBsSynced.YES);
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(bannerRule.getGroupId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannerRule.getBannerId());
        cmdRule.apiSteps().campaignFakeSteps().setStatusModerate(bannerRule.getCampaignId(), "Yes");
    }

    public void test() {
        Group savingGroup = bannerRule.getCurrentGroup().withTags(emptyMap());
        prepareSave(savingGroup);
        savingGroup.getBanners().get(0).setHref(getNewHref());
        bannerRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, campaignId, savingGroup));

        check();
    }

    protected void prepareSave(Group group) {
        switch (campaignType) {
            case TEXT:
                break;
            case MOBILE:
                BannersFactory.addNeededAttribute(group.getBanners().get(0));
                break;
            default:
                throw new BackEndClientException("Не указан тип кампании");
        }
    }

}
