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
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
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
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("Сохранение существующей мобильной кампании")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.MOBILE)
@Tag(TrunkTag.YES)
@Tag("sb_test")
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SaveMobileCampTest {

    private final static String CLIENT = "at-direct-backend-c";

    private static MobileBannersRule bannersRule = new MobileBannersRule().withUlogin(CLIENT);

    private static final String MODEL_NULL_VALUE = null;          // NULL value
    private static final String MODEL_LAST_YANDEX_DIRECT_CLICK = "last_yandex_direct_click";   // дефолтное значение для любой кампании at BK AttributionType = 4

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
        request = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_MOBILE_CAMP_FULL, SaveCampRequest.class).
                withCid(String.valueOf(bannersRule.getCampaignId())).
                withUlogin(CLIENT);
        request.setJsonCampaignMinusWords(Collections.singletonList("химия"));
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

        response = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
    }

    @Test
    @Description("Сохранение аттрибуционной модели для мобильной кампании как NULL")
    @TestCaseId("11008")
    public void testSaveMobileCampAttributionModelAsNull() {
        request.withAttributionModel(MODEL_NULL_VALUE);
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);
        response = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
        assertThat("аттрибуционная модель на кампании не соответствует ожидаемой: ",
                response.getCampaign().getAttributionModel(), equalTo(MODEL_LAST_YANDEX_DIRECT_CLICK));
    }

    @Test
    @Description("Сохранение имени существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9434")
    public void testSaveCampName() {
        assertThat("имя соответствует сохраненному",
                response.getCampaign().getName(), equalTo(request.getName()));
    }

    @Test
    @Description("Сохранение имени клиента существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9435")
    public void testSaveCampFio() {
        assertThat("имя соответствует сохраненному",
                response.getCampaign().getFio(), equalTo(request.getFio()));
    }

    @Test
    @Description("Сохранение даты начала существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9436")
    public void testSaveCampStartDate() {
        assertThat("дата начала соответствует сохраненной",
                response.getCampaign().getStartDate(), equalTo(request.getStart_date()));
    }

    @Test
    @Description("Сохранение даты окончания существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9437")
    public void testSaveCampFinishDate() {
        assertThat("дата окончания соответствует сохраненной",
                response.getCampaign().getFinishDate(), equalTo(request.getFinish_date()));
    }

    @Test
    @Description("Сохранение емайла для уведомлений существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9438")
    public void testSaveCampEmail() {
        assertThat("емайл для уведомлений соответствует сохраненному",
                response.getCampaign().getEmail(), equalTo(request.getEmail()));
    }

    @Test
    @Description("Сохранение интервала уведомлений о смене позиции существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9439")
    public void testSaveCampWarnPlaceInterval() {
        assertThat("интервал уведомлений о смене позиции соответствует сохраненному",
                response.getCampaign().getWarnPlaceInterval(), equalTo(request.getWarnPlaceInterval()));
    }

    @Test
    @Description("Сохранение стратегии существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9440")
    public void testSaveCampStrategy() {
        assertThat("стратегия соответствует сохраненной",
                response.getCampaign().getStrategy(), beanDiffer(request.getJsonStrategy()));
    }

    @Test
    @Description("Сохранение корректировок ставок существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9441")
    public void testSaveCampHierarchicalMultipliers() {
        assertThat("корректировки ставок сохраненной",
                response.getCampaign().getHierarchicalMultipliers(),
                beanDiffer(request.getHierarchicalMultipliers())
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    @Description("Сохранение галочки \"расширенный географический таргетинг\" существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9442")
    public void testSaveCampExtGeo() {
        Integer actual = response.getCampaign().getNoExtendedGeotargeting();
        Integer expected = Integer.valueOf(1).equals(request.getExtendedGeotargeting()) ? 0 : 1;
        assertThat("галочка \"расширенный временной таргетинг\" соответствует сохраненной",
                actual, equalTo(expected));
    }

    @Test
    @Description("Сохранение единого региона существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9443")
    public void testSaveCampGeo() {
        assertThat("единый регион соответствует сохраненному",
                response.getCampaign().getGeo(), equalTo(request.getGeo()));
    }

    @Test
    @Description("Сохранение единых минус-слов существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9444")
    public void testSaveCampMinusWords() {
        assertThat("единые минус-слова соответствуют сохраненным",
                response.getCampaign().getMinusWords(), equalTo(request.getJsonCampaignMinusWords()));
    }

    @Test
    @Description("Сохранение ограничения расхода в РСЯ в существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9445")
    public void testSaveCampContextLimit() {
        assertThat("ограничение расхода в РСЯ соответствует сохраненному",
                response.getCampaign().getContextLimit(), equalTo(request.getContextLimit()));
    }

    @Test
    @Description("Сохранение ограничения цены клика в РСЯ в существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9446")
    public void testSaveCampContextPriceCoef() {
        assertThat("ограничение цены клика в РСЯ - сохранено значени по умолчанию",
                response.getCampaign().getContextPriceCoef(), equalTo("100"));
    }

    @Test
    @Description("Сохранение опции \"удерживать среднюю цену клика в РСЯ ниже чем на поиске\" в существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9447")
    public void testSaveCampEnableCpcHold() {
        assertThat("опция \"удерживать среднюю цену клика в РСЯ ниже чем на поиске\" соответствует сохраненной",
                response.getCampaign().getEnableCpcHold(), equalTo(request.getEnableCpcPriceHold()));
    }

    @Test
    @Description("Сохранение запрещенных площадок в существующей мобильной кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9449")
    public void testSaveCampDisabledDomains() {
        assertThat("запрещенные площадки соответствует сохраненным",
                StringUtils.join(response.getCampaign().getDontShow(), ","), equalTo(request.getDontShow()));
    }
}
