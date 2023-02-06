package ru.yandex.autotests.direct.httpclient.banners.phrases.ajaxupdatephrasesandprices;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.direct.httpclient.data.textresources.phrases.AjaxUpdateTestPhrases;
import ru.yandex.autotests.direct.utils.config.DirectTestRunProperties;
import ru.yandex.autotests.direct.utils.money.MoneyCurrency;
import ru.yandex.autotests.direct.utils.money.MoneyFormat;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.autotests.direct.httpclient.data.textresources.phrases.AjaxUpdateErrorsResourse.ONLY_STOP_WORDS_NEW;
import static ru.yandex.autotests.direct.httpclient.data.textresources.phrases.AjaxUpdateErrorsResourse.PRICE_ABOVE_MAX_NEW;
import static ru.yandex.autotests.direct.httpclient.data.textresources.phrases.AjaxUpdateErrorsResourse.PRICE_BELOW_MIN_NEW;
import static ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter.resource;

@Aqua.Test
@Description("Проверка валидации параметров запроса в контроллере ajaxUpdateShowConditions")
@Stories(TestFeatures.Phrases.AJAX_UPDATE_PHRASES_AND_PRICES)
@Features(TestFeatures.PHRASES)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.PHRASE)
@Tag(CmdTag.AJAX_UPDATE_SHOW_CONDITIONS)
@Tag(CampTypeTag.TEXT)
public class AjaxUpdateShowConditionsRequestValidationTest extends AjaxUpdateShowConditionsTestBase {

    private final String ERROR_PATH = ".errors";

    public AjaxUpdateShowConditionsRequestValidationTest() {
        super(new Group());
    }

    @Test
    @Description("Неверный ulogin")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10260")
    public void wrongUloginTest() {
        final String OTHER_LOGIN = "at-backend-banners";
        requestParams.setUlogin(OTHER_LOGIN);
        doRequest();
        cmdRule.oldSteps().commonSteps()
                .checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }

    @Test
    @Description("Цена больше максимальной")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10265")
    public void aboveMaxPriceTest() {
        MoneyCurrency moneyCurrency = MoneyCurrency.get(User.get(CLIENT_LOGIN).getCurrency());
        phraseBean.setPrice(moneyCurrency.getMaxPrice().getNext().floatValue().toString());
        doRequest();
        String errorText = resource(PRICE_ABOVE_MAX_NEW)
                .args(moneyCurrency.getMaxPrice().stringValue(MoneyFormat.TWO_DIGITS_POINT_SEPARATED),
                        moneyCurrency.getAbbreviation(DirectTestRunProperties.getInstance().getDirectCmdLocale()))
                .toString();
        cmdRule.oldSteps().commonSteps()
                .checkDirectResponseJsonProperty(response, adgroupId + ERROR_PATH, hasItem(errorText));
    }

    @Test
    @Description("Цена меньше минимальной")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10266")
    public void belowMinPriceTest() {
        MoneyCurrency moneyCurrency = MoneyCurrency.get(User.get(CLIENT_LOGIN).getCurrency());
        phraseBean.setPrice(moneyCurrency.getMinPrice().getPrevious().floatValue().toString());
        doRequest();
        String errorText = resource(PRICE_BELOW_MIN_NEW)
                .args(moneyCurrency.getMinPrice().stringValue(MoneyFormat.TWO_DIGITS_POINT_SEPARATED),
                        moneyCurrency.getAbbreviation(DirectTestRunProperties.getInstance().getDirectCmdLocale()))
                .toString();
        cmdRule.oldSteps().commonSteps()
                .checkDirectResponseJsonProperty(response, adgroupId + ERROR_PATH, hasItem(errorText));
    }

    @Test
    @Description("Фраза только из стоп слов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10267")
    public void onlyStopWordsPhraseTest() {
        phraseBean.setPhrase(resource(AjaxUpdateTestPhrases.ONLY_STOP_WORDS).toString());
        doRequest();
        cmdRule.oldSteps().commonSteps().checkDirectResponseJsonProperty(response, adgroupId + ERROR_PATH,
                hasItem(resource(ONLY_STOP_WORDS_NEW).toString()));
    }

    private void doRequest() {
        phrasesMap.put(String.valueOf(firstPhraseId), phraseBean);
        groupPhrases.setEdited(phrasesMap);
        jsonPhrases.setGroupPhrases(String.valueOf(adgroupId), groupPhrases);
        response =
                cmdRule.oldSteps().ajaxUpdatePhrasesAndPricesSteps().ajaxUpdateShowConditions(csrfToken, requestParams);
    }
}
