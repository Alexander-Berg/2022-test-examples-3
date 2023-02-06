package ru.yandex.autotests.direct.cmd.vcards;

import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

//TESTIRT-9607
@Aqua.Test
@Description("Проверка сохоранения турецких номеров")
@Stories(TestFeatures.VCards.SAVE_VCARD)
@Features(TestFeatures.VCARDS)
@Tag(CmdTag.SAVE_VCARD)
@Tag(ObjectTag.VCARD)
@Tag(CampTypeTag.TEXT)
public class PhonePrefixNumberTest {
    protected static final String CLIENT = Logins.DEFAULT_CLIENT;
    private static final String PHONE_WITH_444_START = "4441212";
    private static final String PHONE_WITHOUT_CITY_CODE = "4451212";
    private static final String SIMPLE_PHONE = "7005119";
    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();
    protected BannersRule bannersRule = new TextBannersRule().overrideVCardTemplate(getVCard()).withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(bannersRule);

    @Test
    @Description("Тестирование номера начинающегося на 444 без кода города")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10051")
    public void turkishNumberWithoutCityCode() {
        ContactInfo contactInfo = bannersRule.getBanner().getContactInfo()
                .withCountry("Turkey")
                .withCity("Stambuli")
                .withCityCode(null)
                .withPhone(PHONE_WITH_444_START)
                .withCountryCode("+90");
        saveAndCheck(contactInfo, equalTo(PHONE_WITH_444_START));
    }

    @Test
    @Description("Тестирование номера начинающегося на 444 без кода города на наличие сдвовенного #")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10052")
    public void turkishNumberWithoutCityCodeSplitter() {
        ContactInfo contactInfo = bannersRule.getBanner().getContactInfo()
                .withCountry("Turkey")
                .withCity("Stambuli")
                .withCityCode(null)
                .withPhone(PHONE_WITH_444_START)
                .withCountryCode("+90");
        save(contactInfo);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT);
        String actualPhone = TestEnvironment.newDbSteps().vCardSteps()
                .getVCards(bannersRule.getCurrentGroup().getBanners().get(0).getVcardId()).getPhone();

        assertThat("Номер соответствует ожиданиям", actualPhone, containsString("##"));
    }


    @Test
    @Description("Тестирование номера начинающегося не на 444")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10053")
    public void turkishNumberWithCityCode() {
        ContactInfo contactInfo = bannersRule.getBanner().getContactInfo()
                .withCountry("Turkey")
                .withCity("Stambuli")
                .withCityCode("212")
                .withPhone(PHONE_WITHOUT_CITY_CODE)
                .withCountryCode("+90");
        saveAndCheck(contactInfo, equalTo(PHONE_WITHOUT_CITY_CODE));
    }

    @Test
    @Description("Тестирование номера начинающегося на 444 без кода города")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10054")
    public void turkishNumberWithCityCodeAndPrefix() {
        ContactInfo contactInfo = bannersRule.getBanner().getContactInfo()
                .withCountry("Turkey")
                .withCity("Stambuli")
                .withCityCode("212")
                .withPhone(PHONE_WITH_444_START)
                .withCountryCode("+90");
        saveAndCheck(contactInfo, equalTo(PHONE_WITH_444_START));
    }

    @Test
    @Description("Тестирование номера начинающегося не на 444 и без кода города")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10055")
    public void turkishNumberWithoutCityNumberNegative() {
        String oldPhone = bannersRule.getBanner().getContactInfo().getPhone();
        ContactInfo contactInfo = bannersRule.getBanner().getContactInfo()
                .withCountry("Turkey")
                .withCity("Stambuli")
                .withCityCode(null)
                .withPhone(PHONE_WITHOUT_CITY_CODE)
                .withCountryCode("+90");
        saveAndCheck(contactInfo, equalTo(oldPhone));
    }

    @Test
    @Description("Тестирование номера начинающегося не на 444 и без кода города")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10049")
    public void plusPrefix8NumberNegative() {
        String oldPhone = bannersRule.getBanner().getContactInfo().getPhone();
        ContactInfo contactInfo = bannersRule.getBanner().getContactInfo()
                .withCityCode("804")
                .withPhone(SIMPLE_PHONE)
                .withCountryCode("+8");
        saveAndCheck(contactInfo, equalTo(oldPhone));
    }

    @Test
    @Description("Тестирование номера начинающегося не на 444 и без кода города")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10050")
    public void plusPrefix0NumberNegative() {
        String oldPhone = bannersRule.getBanner().getContactInfo().getPhone();
        ContactInfo contactInfo = bannersRule.getBanner().getContactInfo()
                .withCityCode("800")
                .withPhone(SIMPLE_PHONE)
                .withCountryCode("+0");
        saveAndCheck(contactInfo, equalTo(oldPhone));
    }

    private ContactInfo getVCard() {
        ContactInfo clientVCard = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class);
        return clientVCard;
    }

    private void saveAndCheck(ContactInfo contactInfo, Matcher<String> phoneMatcher) {
        save(contactInfo);
        cmdRule.getApiStepsRule().as(CLIENT);
        String actualPhone = cmdRule.apiSteps().vCardsSteps()
                .vCardsGet(bannersRule.getCurrentGroup().getBanners().get(0).getVcardId()).get(0).getPhone()
                .getPhoneNumber();
        assertThat("Номер соответствует ожиданиям", actualPhone, phoneMatcher);
    }

    private void save(ContactInfo contactInfo) {
        cmdRule.cmdSteps().vCardsSteps().saveVCard(
                CLIENT,
                bannersRule.getCampaignId(),
                bannersRule.getBannerId(),
                bannersRule.getCurrentGroup().getBanners().get(0).getVcardId(),
                contactInfo
        );
    }


}
