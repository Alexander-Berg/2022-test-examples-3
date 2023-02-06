package ru.yandex.autotests.direct.web.api.tests.vcard;

import java.util.Arrays;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.web.api.core.DirectRule;
import ru.yandex.autotests.direct.web.api.features.TestFeatures;
import ru.yandex.autotests.direct.web.api.features.tags.Tags;
import ru.yandex.autotests.direct.web.api.models.WebInstantMessenger;
import ru.yandex.autotests.direct.web.api.models.WebPhone;
import ru.yandex.autotests.direct.web.api.models.WebSuccessResponse;
import ru.yandex.autotests.direct.web.api.models.WebVcard;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Редактирование визиток в баннерах через мастер визиток")
@Stories(TestFeatures.Vcard.MANAGE_VCARDS)
@Features(TestFeatures.VCARD)
@Tag(Tags.VCARD)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.DYNAMIC)
@RunWith(Parameterized.class)
public class ManageVcardsTest {

    private static final String CLIENT_LOGIN = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    private BannersRule bannersRule;
    private BannersRule bannersRule2;

    @Rule
    public DirectRule directRule;

    @Parameterized.Parameters(name = " Тип кампании: {0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.DTO}
        });
    }

    public ManageVcardsTest(CampaignTypeEnum campaignType) {
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .overrideVCardTemplate(BeanLoadHelper
                        .loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class)
                        .withHouse("9"))
                .withUlogin(CLIENT_LOGIN);

        bannersRule2 = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .withUlogin(CLIENT_LOGIN);

        DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
                .withRules(bannersRule, bannersRule2)
                .as(CLIENT_LOGIN);

        directRule = DirectRule.defaultRule()
                .withRules(cmdRule)
                .as(CLIENT_LOGIN);
    }


    @Test
    public void saveVcardByBannerIdsTest() {
        WebSuccessResponse response = directRule.webApiSteps().vcardSteps().saveVcardByBannerIds(
                bannersRule.getCampaignId(), singletonList(bannersRule.getBannerId()),
                getDefaultWebVcard().withHouse("10"), CLIENT_LOGIN);
        assertThat("в ответе флаг success должен быть равен true",
                response.getSuccess(), is(true));
    }

    @Test
    public void saveVcardByVcardIdTest() {
        Long vcardId = bannersRule.getCurrentGroup().getBanners().get(0).getVcardId();
        assumeThat("у баннера должна быть визитка", vcardId, notNullValue());

        WebSuccessResponse response = directRule.webApiSteps().vcardSteps().saveVcardByVcardId(
                bannersRule.getCampaignId(), vcardId,
                getDefaultWebVcard().withHouse("11"), CLIENT_LOGIN);
        assertThat("в ответе флаг success должен быть равен true",
                response.getSuccess(), is(true));
    }

    @Test
    public void saveVcardForInvalidVcardTest() {
        WebSuccessResponse response = directRule.webApiSteps().vcardSteps().saveVcardByBannerIds(
                bannersRule.getCampaignId(), singletonList(bannersRule.getBannerId()),
                getDefaultWebVcard().withOgrn("123"), CLIENT_LOGIN);
        assertThat("в ответе флаг success должен быть равен false",
                response.getSuccess(), is(false));
    }

    @Test
    public void assignVcardTest() {
        Long vcardId = bannersRule.getCurrentGroup().getBanners().get(0).getVcardId();
        assumeThat("у баннера должна быть визитка", vcardId, notNullValue());

        WebSuccessResponse response = directRule.webApiSteps().vcardSteps().assignVcard(
                bannersRule2.getCampaignId(), singletonList(bannersRule2.getBannerId()), vcardId,
                CLIENT_LOGIN);
        assertThat("в ответе флаг success должен быть равен true",
                response.getSuccess(), is(true));
    }

    @Test
    public void unassignVcardTest() {
        WebSuccessResponse response = directRule.webApiSteps().vcardSteps().unassignVcard(
                bannersRule.getCampaignId(), singletonList(bannersRule.getBannerId()), CLIENT_LOGIN);
        assertThat("в ответе флаг success должен быть равен true",
                response.getSuccess(), is(true));
    }

    private WebVcard getDefaultWebVcard() {
        ContactInfo contactInfo = BeanLoadHelper.loadCmdBean(
                CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class);
        return new WebVcard()
                .withCompanyName(contactInfo.getCompanyName())
                .withCountry(contactInfo.getCountry())
                .withCity(contactInfo.getCity())
                .withWorkTime(contactInfo.getWorkTime())
                .withPhone(new WebPhone()
                        .withCountryCode(contactInfo.getCountryCode())
                        .withCityCode(contactInfo.getCityCode())
                        .withPhoneNumber(contactInfo.getPhone())
                        .withExtension(contactInfo.getPhoneExt()))
                .withContactPerson(contactInfo.getContactPerson())
                .withStreet(contactInfo.getStreet())
                .withHouse(contactInfo.getHouse())
                .withBuild(contactInfo.getBuild())
                .withApart(contactInfo.getApart())
                .withExtraMessage(contactInfo.getExtraMessage())
                .withInstantMessenger(new WebInstantMessenger()
                        .withType(contactInfo.getIMClient())
                        .withLogin(contactInfo.getIMLogin()))
                .withEmail(contactInfo.getContactEmail())
                .withOgrn(contactInfo.getOGRN());
    }
}
