package ru.yandex.autotests.direct.cmd.banners.vcard;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Collections;

public abstract class AfterSimpleVcardStatusModerateTestBase {

    protected static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    protected BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    protected ContactInfo vcard;

    protected abstract StatusModerate getSetStatusModerate();

    protected abstract StatusModerate getExpectedStatusModerate();

    protected abstract void sendAndCheck();

    public AfterSimpleVcardStatusModerateTestBase(CampaignTypeEnum campaignType) {
        this.bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .overrideBannerTemplate(new Banner()
                        .withHasVcard(1)
                        .withContactInfo(BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class)))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        vcard = bannersRule.getCurrentGroup().getBanners().get(0).getContactInfo();
        BsSyncedHelper.moderateCamp(cmdRule, bannersRule.getCampaignId());
        cmdRule.apiSteps().bannersFakeSteps()
                .setPhoneflag(bannersRule.getBannerId(), getSetStatusModerate().toString());
    }

    @Description("статус модерации при изменении email визитки")
    public void checkStatusModerateEmailChange() {
        vcard.setContactEmail("ya@ya.ru");

        sendAndCheck();
    }

    @Description("статус модерации при изменении imlogin визитки")
    public void checkStatusModerateIMLoginChange() {
        vcard.setIMClient("Skype");
        vcard.setIMLogin("44");

        sendAndCheck();
    }

    protected Group getGroup() {
        Group group = bannersRule.getGroup().
                withAdGroupID(bannersRule.getGroupId().toString());
        group.getBanners().get(0).setBid(bannersRule.getBannerId());
        return group;
    }

    protected Group getExpectedGroup() {
        return new Group()
                .withBanners(Collections.singletonList(new Banner()
                        .withStatusModerate(StatusModerate.YES.toString())
                        .withPhoneFlag(getExpectedStatusModerate().toString())));
    }

}
