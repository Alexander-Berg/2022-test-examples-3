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
import ru.yandex.autotests.direct.cmd.data.commons.banner.OrgDetails;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.Collections;

public abstract class AfterVcardStatusModerateTestBase {

    protected static final String CLIENT = Logins.DEFAULT_CLIENT;
    private static final String OGRN = "1027700132195";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    protected BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    protected ContactInfo vcard;

    protected abstract StatusModerate getSetStatusModerate();

    protected abstract StatusModerate getExpectedStatusModerate();

    protected abstract void sendAndCheck();

    public AfterVcardStatusModerateTestBase(CampaignTypeEnum campaignType) {
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

    @Description("статус модерации при изменении apart визитки")
    public void checkStatusModerateApartChange() {
        vcard.setApart("44");

        sendAndCheck();
    }

    @Description("статус модерации при изменении house визитки")
    public void checkStatusModerateHouseChange() {
        vcard.setHouse("44");

        sendAndCheck();
    }

    @Description("статус модерации при изменении build визитки")
    public void checkStatusModerateBuildChange() {
        vcard.setBuild("44");

        sendAndCheck();
    }

    @Description("статус модерации при изменении ogrn визитки")
    public void checkStatusModerateOGRNChange() {
        vcard.setOrgDetails(new OrgDetails().withOGRN(OGRN));
        vcard.setOGRN(OGRN);

        sendAndCheck();
    }

    @Description("статус модерации при незначительном изменении страны визитки")
    public void checkStatusModerateCountryChange() {
        vcard.setCountry("США");

        sendAndCheck();
    }

    @Description("статус модерации при незначительном изменении города визитки")
    public void checkStatusModerateCityChange() {
        vcard.setCity("Москва");

        sendAndCheck();
    }

    @Description("статус модерации при незначительном изменении названия кампании визитки")
    public void checkStatusModerateCompanyNameChange() {
        vcard.setCompanyName("new company name");

        sendAndCheck();
    }

    @Description("статус модерации при незначительном изменении доп. информации визитки")
    public void checkStatusModerateExtraMessageChange() {
        vcard.setExtraMessage("new extra message");

        sendAndCheck();
    }

    @Description("статус модерации при незначительном изменении улицы визитки")
    public void checkStatusModerateStreetChange() {
        vcard.setStreet("Street");

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

    protected void prepareActualGroup(Group actualGroup) {
        if (StatusModerate.SENDING.toString().equals(actualGroup.getBanners().get(0).getPhoneFlag())) {
            actualGroup.getBanners().get(0).withPhoneFlag(StatusModerate.READY.toString());
        }
    }
}
