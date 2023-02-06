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
@Description("Привязка текстовых дополнений к ДТО баннерам")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
public class SaveDynamicBannersCalloutsTest extends SaveBannersCalloutsTestBase {

    @Override
    public void saveCallouts(String... callouts) {
        GroupsParameters request = helper.getRequestForDynamic(helper.newDynamicGroupAndSet(callouts));
        helper.saveCalloutsForDynamic(request);
    }

    @Override
    public void saveCalloutsForExistingGroup(String... callouts) {
        GroupsParameters request = helper.getRequestForDynamic(helper.existingDynamicGroupAndSet(callouts));
        helper.saveCalloutsForDynamic(request);
    }

    @Override
    public CampaignRule getCampaignRule() {
        return new CampaignRule().withMediaType(CampaignTypeEnum.DTO).withUlogin(getUlogin());
    }

    @Override
    public String getUlogin() {
        return "at-direct-banners-callouts-8";
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9088")
    public void saveCalloutsForBanner() {
        super.saveCalloutsForBanner();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9089")
    public void updateCalloutsForBannerSavedForClient() {
        super.updateCalloutsForBannerSavedForClient();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9090")
    public void updateCalloutsForBanner() {
        super.updateCalloutsForBanner();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9091")
    public void removeCalloutsFromBanner() {
        super.removeCalloutsFromBanner();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9092")
    public void saveMaxCalloutsForBanner() {
        super.saveMaxCalloutsForBanner();
    }


    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9093")
    public void calloutsOrder() {
        super.calloutsOrder();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9094")
    public void canChangeCalloutsOrder() {
        super.canChangeCalloutsOrder();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9095")
    public void canSaveDifferentCalloutsOrderForDifferentBanners() {
        super.canSaveDifferentCalloutsOrderForDifferentBanners();
    }

}
