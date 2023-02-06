package ru.yandex.autotests.direct.cmd.campaigns.save;

import java.time.LocalDate;
import java.time.Year;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.campaigns.CampaignErrors;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.errors.DomainErrors;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Валидация параметров при сохранении существующей текстовой кампании")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
public class SaveTextCampNegativeTest {

    private final static String CLIENT = "at-direct-backend-c";

    private static TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    private static final String MODEL_WRONG_VALUE = "wrong_value";                         // wrong value
    private static final String MODEL_LAST_YANDEX_DIRECT_CLICK = "last_yandex_direct_click";   // BK AttributionType = 4

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.
            defaultClassRule().
            useAuth(true).
            as(CLIENT).
            withRules(bannersRule);

    private SaveCampRequest request;
    private EditCampResponse response;

    @Before
    public void before() {
        request = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_TEXT_CAMP_FULL, SaveCampRequest.class);
        request.withCid(String.valueOf(bannersRule.getCampaignId()))
                .withUlogin(CLIENT);
    }

    @Test
    @Description("Сохранение невалидной аттрибуционной модели для текстовой кампании")
    @TestCaseId("11010")
    @Ignore("Старое редактирование выключено на 100% пользователей")
    public void testSaveCampWrongAttributionModel() {
        request.withAttributionModel(MODEL_WRONG_VALUE);
        cmdRule.cmdSteps().campaignSteps().postSaveCampInvalidData(request);
        response = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
        assertThat("аттрибуционная модель на кампании соответствует ожидаемой: ",
                response.getCampaign().getAttributionModel(), equalTo(MODEL_LAST_YANDEX_DIRECT_CLICK));
    }

    @Test
    @Description("Валидация пустого cid при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9487")
    public void testSaveCampEmptyCid() {
        request.setCid(null);
        sendAndCheckCommonError(TextResourceFormatter.resource(CampaignErrors.EMPTY_CID).toString());
    }

    @Test
    @Description("Валидация некорректного cid при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9488")
    public void testSaveCampInvalidCid() {
        String cid = "aaa";
        request.setCid(cid);
        sendAndCheckCommonError(String.format(TextResourceFormatter.resource(CampaignErrors.INCORRECT_CID).toString(), cid));
    }

    @Test
    @Description("Валидация пустого имени при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9486")
    public void testSaveCampEmptyName() {
        request.setName("");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.EMPTY_CAMPAIGN_NAME).toString());
    }

    @Test
    @Description("Валидация имени, состоящего только из пробелов, при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9491")
    public void testSaveCampWithOnlySpacesInName() {
        request.setName("   ");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.BACKSPACES_CAMPAIGN_NAME).toString());
    }

    @Test
    @Description("Валидация имени, содержащего некорректные символы, при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9492")
    public void testSaveCampWithIncorrectCharsInName() {
        request.setName("<имя кампании>");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.INCORRECT_SYMBOLS_CAMPAIGN_NAME).toString());
    }

    @Test
    @Description("Валидация пустого email при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9493")
    public void testSaveCampEmptyEmail() {
        request.setEmail("");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.EMPTY_EMAIL).toString());
    }

    @Test
    @Description("Валидация длинного email при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9494")
    public void testSaveCampLongEmail() {
        request.setEmail("abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111@yandex.ru");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.TOO_LONG_EMAIL).toString());
    }

    @Test
    @Description("Валидация некорректного email при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9495")
    public void testSaveCampInvalidEmail() {
        request.setEmail("aaa.yandex.ru");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.INCORRECT_EMAIL).toString());
    }

    @Test
    @Description("Валидация отсутствия даты запуска при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9498")
    public void testSaveCampEmptyStartDate() {
        request.setStart_date(null);
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.EMPTY_START_DATE).toString());
    }

    @Test
    @Description("Валидация даты остановки меньше текущей даты при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9499")
    public void testSaveCampFinishDateLowerThanCurrentDate() {
        request.setStart_date("2014-01-01");
        request.setFinish_date("2014-01-01");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.FINISH_DATE_LESS_THAN_CURRENT).toString());
    }

    @Test
    @Description("Валидация даты остановки меньше даты запуска при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9500")
    public void testSaveCampFinishDateLowerThanStartDate() {
        LocalDate firstOfJanNextYear = Year.now().plusYears(1).atDay(1);
        LocalDate firstOfFebNextYear = firstOfJanNextYear.plusMonths(1);

        request.setStart_date(firstOfFebNextYear.toString());
        request.setFinish_date(firstOfJanNextYear.toString());
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.FINISH_DATE_LESS_THAN_START_DATE).toString());
    }

    @Test
    @Description("Валидация некорректного broadMatchLimit при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9489")
    public void testSaveCampInvalidBroadMatchLimit() {
        request.setBroad_match_limit("0");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.INCORRECT_BROAD_MATCH_LIMIT).toString());
    }

    @Test
    @Description("Валидация домена yandex.ru в списке запрещенных при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9501")
    public void testSaveCampDontShowAtYandex() {
        String domain = "yandex.ru";
        request.setDontShow(domain);
        sendAndCheckCampaignErrors(String.format(TextResourceFormatter.resource(DomainErrors.YANDEX_DOMAIN_ERROR).toString(), domain));
    }

    @Test
    @Description("Валидация некорректного домена в списке запрещенных при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9502")
    public void testSaveCampDontShowInvalidDomain() {
        String domain = ".ru";
        request.setDontShow(domain);
        sendAndCheckCampaignErrors(String.format(TextResourceFormatter.resource(DomainErrors.INVALID_DOMAIN_FORMAT).toString(), domain));
    }

    @Test
    @Description("Валидация некорректного домена второго уровня в списке запрещенных при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9503")
    public void testSaveCampDontShowInvalidSecondLevelDomain() {
        String domain = "msk.ru";
        request.setDontShow(domain);
        sendAndCheckCampaignErrors(String.format(TextResourceFormatter.resource(DomainErrors.ONLY_THIRD_LEVEL_DOMAIN).toString(), domain));
    }

    @Test
    @Description("Валидация длинного домена второго уровня в списке запрещенных при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9504")
    public void testSaveCampDontShowTooLongDomain() {
        String domain = "bcvaaaaaaaaaaaaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaaaaaaa.aaaaaaaaaaaaa.aaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaa.aaaaaaaaaaa.aaaaaaaaaaaaa.aaaaaaaaaaaaaaa.aaaaaaaaa.aaaaaaaaa.aaaaaaaaaaaaaaaa.aaaaaaaaa.aaaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaa.ru";
        request.setDontShow(domain);
        sendAndCheckCampaignErrors(String.format(TextResourceFormatter.resource(DomainErrors.TOO_LONG_DOMAIN).toString(), domain));
    }

    @Test
    @Description("Валидация невалидных значений двух полей (имя, дата начала) при изменении кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9505")
    public void testSaveCampInvalidTwoFields() {
        request.setName("");
        request.setStart_date(null);
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.EMPTY_CAMPAIGN_NAME).toString(),
                TextResourceFormatter.resource(CampaignErrors.EMPTY_START_DATE).toString());
    }

    private void sendAndCheckCampaignErrors(String... errors) {
        String error = StringUtils.join(errors, "\n");
        CampaignErrorResponse response = cmdRule.cmdSteps().campaignSteps().postSaveCampInvalidData(request);
        assertThat("ошибка соответствует ожидаемой", response.getCampaignErrors().getError(), equalTo(error));
    }

    private void sendAndCheckCommonError(String error) {
        CampaignErrorResponse response = cmdRule.cmdSteps().campaignSteps().postSaveCampInvalidData(request);
        assertThat("ошибка соответствует ожидаемой", response.getError(), equalTo(error));
    }
}
