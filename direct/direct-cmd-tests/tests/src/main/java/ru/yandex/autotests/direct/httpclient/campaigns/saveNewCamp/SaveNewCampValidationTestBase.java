package ru.yandex.autotests.direct.httpclient.campaigns.saveNewCamp;

import java.time.LocalDate;
import java.time.Year;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.SaveCampParameters;
import ru.yandex.autotests.direct.httpclient.data.textresources.DomainValidationErrors;
import ru.yandex.autotests.direct.httpclient.data.textresources.campaigns.CampaignValidationErrors;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;

public abstract class SaveNewCampValidationTestBase {
    private final static String CAMPAIGN_ERROR_PATH = ".campaign.error[0]";
    private final static String IP_ADDRESS = "213.0.0.1";
    private final static Integer IP_ADDRESS_MAX_COUNT = 25;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    protected SaveCampParameters saveCampParameters;
    private User client = User.get("at-direct-backend-c");

    protected abstract String getTemplateName();

    @Before
    public void before() {
        cmdRule.oldSteps().onPassport().authoriseAs(client.getLogin(), client.getPassword());
        PropertyLoader<SaveCampParameters> propertyLoader = new PropertyLoader<>(SaveCampParameters.class);
        saveCampParameters = propertyLoader.getHttpBean(getTemplateName());

    }

    protected void checkSaveNewCampValidation(String errorText) {
        CSRFToken csrfToken = getCsrfTokenFromCocaine(client.getPassportUID());
        DirectResponse saveNewCampResponse = cmdRule.oldSteps().clientSteps().saveNewCampaign(csrfToken, saveCampParameters);
        cmdRule.oldSteps().commonSteps().checkDirectResponseJsonProperty(saveNewCampResponse, CAMPAIGN_ERROR_PATH,
                equalTo(errorText));
    }

    @Description("Проверяем валидацию при сохранении кампании с пустым названием")
    public void emptyCampaignNameValidationTest() {
        saveCampParameters.setName(null);
        checkSaveNewCampValidation(TextResourceFormatter.resource(CampaignValidationErrors.EMPTY_CAMPAIGN_NAME).toString());
    }

    @Description("Проверяем валидацию при сохранении кампании с пустым email")
    public void emptyEmailValidationTest() {
        saveCampParameters.setEmail(null);
        checkSaveNewCampValidation(TextResourceFormatter.resource(CampaignValidationErrors.EMPTY_EMAIL).toString());
    }

    @Description("Проверяем валидацию при сохранении кампании с очень длинным email")
    public void tooLongEmailValidationTest() {
        saveCampParameters.setEmail("abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111abc11111111111111111111@yandex.ru");
        checkSaveNewCampValidation(TextResourceFormatter.resource(CampaignValidationErrors.TOO_LONG_EMAIL).toString());
    }

    @Description("Проверяем валидацию при сохранении кампании с некорректным email")
    public void incorrectEmailValidationTest() {
        saveCampParameters.setEmail("aaa.yandex.ru");
        checkSaveNewCampValidation(TextResourceFormatter.resource(CampaignValidationErrors.INCORRECT_EMAIL).toString());
    }

    @Description("Проверяем валидацию при сохранении кампании с пустой датой начала кампании")
    public void emptyStartDateValidationTest() {
        saveCampParameters.setStart_date(null);
        checkSaveNewCampValidation(TextResourceFormatter.resource(
                CampaignValidationErrors.EMPTY_START_DATE).toString());
    }

    @Description("Проверяем валидацию при сохранении кампании с датой окончания кампании меньше текущей даты")
    public void finishDateLessThanCurrentValidationTest() {
        saveCampParameters.setStart_date("2014-01-01");
        saveCampParameters.setFinish_date("2014-01-01");
        checkSaveNewCampValidation(TextResourceFormatter.resource(
                CampaignValidationErrors.FINISH_DATE_LESS_THAN_CURRENT).toString());
    }

    @Description("Проверяем валидацию при сохранении кампании с датой окончания кампании меньше даты начала")
    public void finishDateLessThanStartDateValidationTest() {
        LocalDate firstOfJanNextYear = Year.now().plusYears(1).atDay(1);
        LocalDate firstOfFebNextYear = firstOfJanNextYear.plusMonths(1);

        saveCampParameters.setStart_date(firstOfFebNextYear.toString());
        saveCampParameters.setFinish_date(firstOfJanNextYear.toString());
        checkSaveNewCampValidation(TextResourceFormatter.resource(
                CampaignValidationErrors.FINISH_DATE_LESS_THAN_START_DATE).toString());
    }

    @Description("Проверяем валидацию при сохранении кампании с названием из пробелов")
    public void backspacesCampaignNameValidationTest() {
        saveCampParameters.setName("  ");
        checkSaveNewCampValidation(TextResourceFormatter.
                resource(CampaignValidationErrors.BACKSPACES_CAMPAIGN_NAME).toString());
    }

    @Description("Проверяем валидацию при сохранении кампании с названием, содержащим некорректные символы")
    public void incorrectSymbolsCampaignNameValidationTest() {
        saveCampParameters.setName("<incorrect campaign name>");
        checkSaveNewCampValidation(TextResourceFormatter.
                resource(CampaignValidationErrors.INCORRECT_SYMBOLS_CAMPAIGN_NAME).toString());
    }

    @Description("Проверяем валидацию при сохранении кампании с некорректным broadMatchLimit")
    public void incorrectBroadMatchLimitValidationTest() {
        saveCampParameters.setBroad_match_limit("0");
        checkSaveNewCampValidation(TextResourceFormatter.
                resource(CampaignValidationErrors.INCORRECT_BROAD_MATCH_LIMIT).toString());
    }

    @Description("Проверяем валидацию при сохранении кампании с доменом Яндекса в запрещенных площадках")
    public void yandexDomainValidationTest() {
        saveCampParameters.setDontShow("yandex.ru");
        checkSaveNewCampValidation(String.format(TextResourceFormatter.
                resource(DomainValidationErrors.YANDEX_DOMAIN_ERROR).toString(), "yandex.ru"));
    }

    @Description("Проверяем валидацию при сохранении кампании с некорректным доменом в запрещенных площадках")
    public void invalidDomainFormatValidationTest() {
        saveCampParameters.setDontShow(".ru");
        checkSaveNewCampValidation(String.format(TextResourceFormatter.
                resource(DomainValidationErrors.INVALID_DOMAIN_FORMAT).toString(), ".ru"));
    }

    @Description("Проверяем валидацию при сохранении кампании с некорректным доменом второго уровня в запрещенных площадках")
    public void onlyThirdLevelDomainValidationTest() {
        saveCampParameters.setDontShow("msk.ru");
        checkSaveNewCampValidation(String.format(TextResourceFormatter.
                resource(DomainValidationErrors.ONLY_THIRD_LEVEL_DOMAIN).toString(), "msk.ru"));
    }

    @Description("Проверяем валидацию при сохранении кампании с очень динным доменом в запрещенных площадках")
    public void tooLongDomainDomainValidationTest() {
        String tooLongDomain =
                "bcvaaaaaaaaaaaaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaaaaaaaa.aaaaaaaaaaaaa.aaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaa.aaaaaaaaaaa.aaaaaaaaaaaaa.aaaaaaaaaaaaaaa.aaaaaaaaa.aaaaaaaaa.aaaaaaaaaaaaaaaa.aaaaaaaaa.aaaaaaaaaaaaaaaaa.aaaaaaaaaaaaaaaaa.ru";
        saveCampParameters.setDontShow(tooLongDomain);
        checkSaveNewCampValidation(String.format(TextResourceFormatter.
                resource(DomainValidationErrors.TOO_LONG_DOMAIN).toString(), tooLongDomain));
    }

    @Description("Проверяем валидацию при сохранении кампании с пустой датой начала кампании и пустым названием кампании")
    public void emptyStartDateAndDontShowAtYandexValidationTest() {
        saveCampParameters.setDontShow("yandex.ru");
        saveCampParameters.setStart_date(null);
        CSRFToken csrfToken = getCsrfTokenFromCocaine(client.getPassportUID());
        DirectResponse saveNewCampResponse = cmdRule.oldSteps().clientSteps().saveNewCampaign(csrfToken, saveCampParameters);

        String emptyStartDateError = TextResourceFormatter.resource(CampaignValidationErrors.EMPTY_START_DATE).toString();
        String domainError = String.format(TextResourceFormatter.
                resource(DomainValidationErrors.YANDEX_DOMAIN_ERROR).toString(), "yandex.ru");

        cmdRule.oldSteps().commonSteps().checkDirectResponseJsonProperty(saveNewCampResponse, CAMPAIGN_ERROR_PATH,
                both(containsString(emptyStartDateError)).and(containsString(domainError)));
    }

    @Description("Проверяем валидацию при сохранении кампании с больше чем 25 запрещенными для показа ip адресами")
    public void incorrectDisabledIpsValidationTest() {
        saveCampParameters.setDisabledIps(StringUtils.repeat(IP_ADDRESS + ",", IP_ADDRESS_MAX_COUNT) + IP_ADDRESS);
        checkSaveNewCampValidation(TextResourceFormatter.
                resource(CampaignValidationErrors.MAX_IP_ADDRESS_COUNT_EXCEEDED).toString() + "\n");
    }
}
