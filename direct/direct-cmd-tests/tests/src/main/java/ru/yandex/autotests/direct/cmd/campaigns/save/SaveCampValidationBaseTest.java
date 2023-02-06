package ru.yandex.autotests.direct.cmd.campaigns.save;

import java.time.LocalDate;
import java.time.Year;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.CmdBeansMaps;
import ru.yandex.autotests.direct.cmd.data.campaigns.CampaignErrors;
import ru.yandex.autotests.direct.cmd.data.commons.errors.DomainErrors;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.qatools.allure.annotations.Description;

public abstract class SaveCampValidationBaseTest {
    protected static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    protected SaveCampRequest request;
    protected CampaignTypeEnum campaignType;

    public SaveCampValidationBaseTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
    }

    @Before
    public void before() {
        request = BeanLoadHelper.loadCmdBean(CmdBeansMaps.MEDIA_TYPE_TO_TEMPLATE.get(campaignType), SaveCampRequest.class);
        request.withUlogin(CLIENT);
    }

    @Description("Валидация пустого имени при изменении кампании")
    public void testSaveCampEmptyName() {
        request.setName("");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.EMPTY_CAMPAIGN_NAME).toString());
    }

    @Description("Валидация имени, состоящего только из пробелов, при изменении кампании")
    public void testSaveCampWithOnlySpacesInName() {
        request.setName("   ");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.BACKSPACES_CAMPAIGN_NAME).toString());
    }

    @Description("Валидация имени, содержащего некорректные символы, при изменении кампании")
    public void testSaveCampWithIncorrectCharsInName() {
        request.setName("<имя кампании>");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.INCORRECT_SYMBOLS_CAMPAIGN_NAME).toString());
    }

    @Description("Валидация пустого email при изменении кампании")
    public void testSaveCampEmptyEmail() {
        request.setEmail("");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.EMPTY_EMAIL).toString());
    }

    @Description("Валидация длинного email при изменении кампании")
    public void testSaveCampLongEmail() {
        request.setEmail("abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111@yandex.ru");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.TOO_LONG_EMAIL).toString());
    }

    @Description("Валидация некорректного email при изменении кампании")
    public void testSaveCampInvalidEmail() {
        request.setEmail("aaa.yandex.ru");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.INCORRECT_EMAIL).toString());
    }

    @Description("Валидация отсутствия даты запуска при изменении кампании")
    public void testSaveCampEmptyStartDate() {
        request.setStart_date(null);
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.EMPTY_START_DATE).toString());
    }

    @Description("Валидация даты остановки меньше текущей даты при изменении кампании")
    public void testSaveCampFinishDateLowerThanCurrentDate() {
        request.setStart_date("2014-01-01");
        request.setFinish_date("2014-01-01");
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.FINISH_DATE_LESS_THAN_CURRENT).toString());
    }

    @Description("Валидация даты остановки меньше даты запуска при изменении кампании")
    public void testSaveCampFinishDateLowerThanStartDate() {
        LocalDate firstOfJanNextYear = Year.now().plusYears(1).atDay(1);
        LocalDate firstOfFebNextYear = firstOfJanNextYear.plusMonths(1);

        request.setStart_date(firstOfFebNextYear.toString());
        request.setFinish_date(firstOfJanNextYear.toString());
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.FINISH_DATE_LESS_THAN_START_DATE).toString());
    }

    @Description("Валидация домена yandex.ru в списке запрещенных при изменении кампании")
    public void testSaveCampDontShowAtYandex() {
        String domain = "yandex.ru";
        request.setDontShow(domain);
        sendAndCheckCampaignErrors(String.format(TextResourceFormatter.resource(DomainErrors.YANDEX_DOMAIN_ERROR).toString(), domain));
    }

    @Description("Валидация некорректного домена в списке запрещенных при изменении кампании")
    public void testSaveCampDontShowInvalidDomain() {
        String domain = ".ru";
        request.setDontShow(domain);
        sendAndCheckCampaignErrors(String.format(TextResourceFormatter.resource(DomainErrors.INVALID_DOMAIN_FORMAT).toString(), domain));
    }

    @Description("Валидация некорректного домена второго уровня в списке запрещенных при изменении кампании")
    public void testSaveCampDontShowInvalidSecondLevelDomain() {
        String domain = "msk.ru";
        request.setDontShow(domain);
        sendAndCheckCampaignErrors(String.format(TextResourceFormatter.resource(DomainErrors.ONLY_THIRD_LEVEL_DOMAIN).toString(), domain));
    }

    @Description("Валидация длинного домена второго уровня в списке запрещенных при изменении кампании")
    public void testSaveCampDontShowTooLongDomain() {
        String domain = "bcvaaaaaaaaaaaaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaaaaaaa.aaaaaaaaaaaaa.aaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaa.aaaaaaaaaaa.aaaaaaaaaaaaa.aaaaaaaaaaaaaaa.aaaaaaaaa.aaaaaaaaa.aaaaaaaaaaaaaaaa.aaaaaaaaa.aaaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaa.ru";
        request.setDontShow(domain);
        sendAndCheckCampaignErrors(String.format(TextResourceFormatter.resource(DomainErrors.TOO_LONG_DOMAIN).toString(), domain));
    }

    @Description("Валидация невалидных значений двух полей (имя, дата начала) при изменении кампании")
    public void testSaveCampInvalidTwoFields() {
        request.setName("");
        request.setStart_date(null);
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.EMPTY_CAMPAIGN_NAME).toString(),
                TextResourceFormatter.resource(CampaignErrors.EMPTY_START_DATE).toString());
    }

    protected abstract void sendAndCheckCampaignErrors(String... errors);

    protected abstract void sendAndCheckCommonError(String error);

}
