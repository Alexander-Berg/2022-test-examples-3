package ru.yandex.autotests.direct.cmd.campaigns.save;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Сохранение существующей текстовой кампании")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@Tag("sb_test")
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SaveTextCampTest {

    private final static String CLIENT = "at-direct-backend-c";

    private static TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.
            defaultClassRule().
            useAuth(true).
            as(CLIENT).
            withRules(bannersRule);

    private static SaveCampRequest request;
    private static EditCampResponse response;

    @BeforeClass
    public static void beforeClass() {
        request = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_TEXT_CAMP_FULL, SaveCampRequest.class);
        request.setJsonCampaignMinusWords(Collections.singletonList("химия"));
        request.withCid(String.valueOf(bannersRule.getCampaignId())).
                withUlogin(CLIENT);

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

        response = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
    }

    @Test
    @Description("Сохранение имени существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9520")
    public void testSaveCampName() {
        assertThat("имя соответствует сохраненному",
                response.getCampaign().getName(), equalTo(request.getName()));
    }

    @Test
    @Description("Сохранение имени клиента существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9521")
    public void testSaveCampFio() {
        assertThat("имя соответствует сохраненному",
                response.getCampaign().getFio(), equalTo(request.getFio()));
    }

    @Test
    @Description("Сохранение даты начала существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9522")
    public void testSaveCampStartDate() {
        assertThat("дата начала соответствует сохраненной",
                response.getCampaign().getStartDate(), equalTo(request.getStart_date()));
    }

    @Test
    @Description("Сохранение даты окончания существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9523")
    public void testSaveCampFinishDate() {
        assertThat("дата окончания соответствует сохраненной",
                response.getCampaign().getFinishDate(), equalTo(request.getFinish_date()));
    }

    @Test
    @Description("Сохранение процента от последней оплаты (для уведомлений) существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9508")
    public void testSaveCampMoneyWarnValue() {
        assertThat("процент от последней оплаты (для уведомлений) соответствует сохраненному",
                response.getCampaign().getMoneyWarningValue(), equalTo(request.getMoney_warning_value()));
    }

    @Test
    @Description("Сохранение емайла для уведомлений существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9524")
    public void testSaveCampEmail() {
        assertThat("емайл для уведомлений соответствует сохраненному",
                response.getCampaign().getEmail(), equalTo(request.getEmail()));
    }

    @Test
    @Description("Сохранение интервала уведомлений о смене позиции существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9525")
    public void testSaveCampWarnPlaceInterval() {
        assertThat("интервал уведомлений о смене позиции соответствует сохраненному",
                response.getCampaign().getWarnPlaceInterval(), equalTo(request.getWarnPlaceInterval()));
    }

    @Test
    @Description("Сохранение стратегии существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9526")
    public void testSaveCampStrategy() {
        assertThat("стратегия соответствует сохраненной",
                response.getCampaign().getStrategy(), beanDiffer(request.getJsonStrategy()));
    }

    @Test
    @Description("Сохранение корректировок ставок существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9527")
    public void testSaveCampHierarchicalMultipliers() {
        assertThat("корректировки ставок сохраненной",
                response.getCampaign().getHierarchicalMultipliers(),
                beanDiffer(request.getHierarchicalMultipliers())
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    @Description("Сохранение временного таргетинга существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9509")
    public void testSaveCampTimeTargeting() {
        assertThat("временной таргетинг соответствует сохраненному",
                response.getCampaign().getTimeTarget(), equalTo(request.getTimeTarget()));
    }

    @Test
    @Description("Сохранение галочки \"расширенный временной таргетинг\" существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9528")
    public void testSaveCampExtGeo() {
        Integer actual = response.getCampaign().getNoExtendedGeotargeting();
        Integer expected = Integer.valueOf(1).equals(request.getExtendedGeotargeting()) ? 0 : 1;
        assertThat("галочка \"расширенный временной таргетинг\" соответствует сохраненной",
                actual, equalTo(expected));
    }

    @Test
    @Description("Сохранение единого региона существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9529")
    public void testSaveCampGeo() {
        assertThat("единый регион соответствует сохраненному",
                response.getCampaign().getGeo(), equalTo(request.getGeo()));
    }

    @Test
    @Description("Сохранение страны существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9510")
    public void testSaveCampCountry() {
        assertThat("страна соответствует сохраненной",
                response.getCampaign().getVcard().getCountry(), equalTo(request.getCountry()));
    }

    @Test
    @Description("Сохранение города существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9519")
    public void testSaveCampCity() {
        assertThat("город соответствует сохраненному",
                response.getCampaign().getVcard().getCity(), equalTo(request.getCity()));
    }

    @Test
    @Description("Сохранение кода страны существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9511")
    public void testSaveCampCountryCode() {
        assertThat("код страны соответствует сохраненному",
                response.getCampaign().getVcard().getCountryCode(), equalTo(request.getCountry_code()));
    }

    @Test
    @Description("Сохранение кода города существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9512")
    public void testSaveCampCityCode() {
        assertThat("код города соответствует сохраненному",
                response.getCampaign().getVcard().getCityCode(), equalTo(request.getCity_code()));
    }

    @Test
    @Description("Сохранение телефона существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9513")
    public void testSaveCampPhone() {
        assertThat("телефон соответствует сохраненному",
                response.getCampaign().getVcard().getPhone(), equalTo(request.getPhone()));
    }

    @Test
    @Description("Сохранение добавочного номера существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9514")
    public void testSaveCampPhoneExt() {
        assertThat("добавочный номер соответствует сохраненному",
                response.getCampaign().getVcard().getPhoneExt(), equalTo(request.getExt()));
    }

    @Test
    @Description("Сохранение название компании существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9515")
    public void testSaveCampCompanyName() {
        assertThat("название компании соответствует сохраненному",
                response.getCampaign().getVcard().getCompanyName(), equalTo(request.getCi_name()));
    }

    @Test
    @Description("Сохранение контактного лица существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9516")
    public void testSaveCampContactPerson() {
        assertThat("контактное лицо соответствует сохраненному",
                response.getCampaign().getVcard().getContactPerson(), equalTo(request.getContactperson()));
    }

    @Test
    @Description("Сохранение времени работы существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9517")
    public void testSaveCampWorktime() {
        assertThat("время работы соответствует сохраненному",
                response.getCampaign().getVcard().getWorkTime(), equalTo(request.getWorktime()));
    }

    @Test
    @Description("Сохранение улицы существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9518")
    public void testSaveCampStreet() {
        assertThat("улица соответствует сохраненной",
                response.getCampaign().getVcard().getStreet(), equalTo(request.getStreet()));
    }

    @Test
    @Description("Сохранение номера дома существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9537")
    public void testSaveCampHouse() {
        assertThat("номер дома соответствует сохраненному",
                response.getCampaign().getVcard().getHouse(), equalTo(request.getHouse()));
    }

    @Test
    @Description("Сохранение номера корпуса существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9538")
    public void testSaveCampBuild() {
        assertThat("номер корпуса соответствует сохраненному",
                response.getCampaign().getVcard().getBuild(), equalTo(request.getBuild()));
    }

    @Test
    @Description("Сохранение номера офиса существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9539")
    public void testSaveCampApart() {
        assertThat("номер офиса соответствует сохраненному",
                response.getCampaign().getVcard().getApart(), equalTo(request.getApart()));
    }

    @Test
    @Description("Сохранение станции метро существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9540")
    public void testSaveCampMetro() {
        assertThat("станция метро соответствует сохраненной",
                response.getCampaign().getVcard().getMetro(), equalTo(request.getMetro()));
    }

    @Test
    @Description("Сохранение контактного емайла существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9541")
    public void testSaveCampContactEmail() {
        assertThat("контактный емайл соответствует сохраненному",
                response.getCampaign().getVcard().getContactEmail(), equalTo(request.getContact_email()));
    }

    @Test
    @Description("Сохранение пункта \"подробнее\" существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9542")
    public void testSaveCampExtraMessage() {
        assertThat("пункт \"подробнее\" соответствует сохраненному",
                response.getCampaign().getVcard().getExtraMessage(), equalTo(request.getExtra_message()));
    }

    @Test
    @Description("Сохранение ОГРН существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9536")
    public void testSaveCampOgrn() {
        assertThat("ОГРН соответствует сохраненному",
                response.getCampaign().getVcard().getOrgDetails().getOGRN(), equalTo(request.getOgrn()));
    }

    @Test
    @Description("Сохранение единых минус-слов существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9530")
    public void testSaveCampMinusWords() {
        assertThat("единые минус-слова соответствуют сохраненным",
                response.getCampaign().getMinusWords(), equalTo(request.getJsonCampaignMinusWords()));
    }

    @Test
    @Description("Сохранение ограничения расхода в РСЯ в существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9531")
    public void testSaveCampContextLimit() {
        // DIRECT-102108 - так как это автобюджет
        assertThat("ограничение расхода в РСЯ - сохранилось значение по умолчанию",
                response.getCampaign().getContextLimit(), equalTo("0"));
    }

    @Test
    @Description("Сохранение ограничения цены клика в РСЯ в существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9532")
    public void testSaveCampContextPriceCoef() {
        assertThat("ограничение цены клика в РСЯ - сохранилось значение по умолчанию",
                response.getCampaign().getContextPriceCoef(), equalTo("100"));
    }

    @Test
    @Description("Сохранение опции \"удерживать среднюю цену клика в РСЯ ниже чем на поиске\" в существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9533")
    public void testSaveCampEnableCpcHold() {
        assertThat("опция \"удерживать среднюю цену клика в РСЯ ниже чем на поиске\" - отключено для автобюджета",
                response.getCampaign().getEnableCpcHold(), equalTo("0"));
    }

    @Test
    @Description("Сохранение расхода по доп. релевантным фразам в существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9543")
    public void testSaveCampBroadMatchLimit() {
        assertThat("расход по доп. релевантным фразам соответствует сохраненному",
                response.getCampaign().getBroadMatchLimit(), equalTo(request.getBroad_match_limit()));
    }

    @Test
    @Description("Сохранение запрещенных площадок в существующей текстовой кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9535")
    public void testSaveCampDisabledDomains() {
        assertThat("запрещенные площадки соответствует сохраненным",
                StringUtils.join(response.getCampaign().getDontShow(), ","), equalTo(request.getDontShow()));
    }
}
