package ru.yandex.autotests.direct.cmd.banners.additions.callouts;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Привязка текстовых дополнений к текстовым баннерам")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
public class SaveTextBannersCalloutsTest extends SaveBannersCalloutsTestBase {

    @Override
    public void saveCallouts(String... callouts) {
        GroupsParameters request = helper.getRequestFor(helper.newGroupAndSet(callouts));
        helper.saveCallouts(request);
    }

    @Override
    public void saveCalloutsForExistingGroup(String... callouts) {
        GroupsParameters request = helper.getRequestFor(helper.existingGroupAndSet(callouts));
        helper.saveCallouts(request);
    }

    @Override
    public CampaignRule getCampaignRule() {
        return new CampaignRule().withMediaType(CampaignTypeEnum.TEXT).withUlogin(getUlogin());
    }

    @Override
    public String getUlogin() {
        return "at-direct-banners-callouts-6";
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9097")
    public void saveCalloutsForBanner() {
        super.saveCalloutsForBanner();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9098")
    public void updateCalloutsForBannerSavedForClient() {
        super.updateCalloutsForBannerSavedForClient();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9099")
    public void updateCalloutsForBanner() {
        super.updateCalloutsForBanner();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9100")
    public void removeCalloutsFromBanner() {
        super.removeCalloutsFromBanner();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9101")
    public void saveMaxCalloutsForBanner() {
        super.saveMaxCalloutsForBanner();
    }


    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9102")
    public void calloutsOrder() {
        super.calloutsOrder();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9103")
    public void canChangeCalloutsOrder() {
        super.canChangeCalloutsOrder();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9104")
    public void canSaveDifferentCalloutsOrderForDifferentBanners() {
        super.canSaveDifferentCalloutsOrderForDifferentBanners();
    }

}
