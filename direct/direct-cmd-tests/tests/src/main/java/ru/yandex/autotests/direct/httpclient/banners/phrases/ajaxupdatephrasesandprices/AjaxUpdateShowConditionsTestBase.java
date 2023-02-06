package ru.yandex.autotests.direct.httpclient.banners.phrases.ajaxupdatephrasesandprices;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.httpclient.CocaineSteps;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxUpdateShowConditionsBean;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxUpdateShowConditionsGroupPhrasesBeanBuilder;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxUpdateShowConditionsParameters;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxUpdateShowConditionsRequestBean;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.autotests.directapi.model.User;

/**
 * Created by shmykov on 27.05.15.
 * TESTIRT-4965
 */
public class AjaxUpdateShowConditionsTestBase {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static CSRFToken csrfToken;
    protected final String CLIENT_LOGIN = "at-direct-b-phrasesandprices";

    public TextBannersRule bannersRule;
    @Rule
    public DirectCmdRule cmdRule;
    protected Integer campaignId;
    protected Long bannerId;
    protected Long adgroupId;
    protected Long firstPhraseId;
    protected Long secondPhraseId;
    protected AjaxUpdateShowConditionsGroupPhrasesBeanBuilder groupPhrases;
    protected Map<String, AjaxUpdateShowConditionsBean> phrasesMap;

    protected AjaxUpdateShowConditionsBean phraseBean;
    protected AjaxUpdateShowConditionsRequestBean jsonPhrases;
    protected AjaxUpdateShowConditionsParameters requestParams;
    protected DirectResponse response;

    public AjaxUpdateShowConditionsTestBase(Group overrideGroup) {
        bannersRule = new TextBannersRule()
                .overrideGroupTemplate(overrideGroup)
                .withUlogin(CLIENT_LOGIN);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        phrasesMap = new HashMap<>();
        csrfToken = CocaineSteps.getCsrfTokenFromCocaine(User.get(CLIENT_LOGIN).getPassportUID());
        campaignId = bannersRule.getCampaignId().intValue();

        bannerId = bannersRule.getBannerId();

        adgroupId = bannersRule.getGroupId();

        List<Phrase> phraseList = bannersRule.getCurrentGroup().getPhrases();
        firstPhraseId = phraseList.get(0).getId();
        if (phraseList.size() > 1) {
            secondPhraseId = phraseList.get(1).getId();
        }

        phraseBean = new PropertyLoader<>(AjaxUpdateShowConditionsBean.class)
                .getHttpBean("defaultPhraseForAjaxUpdatePhrasesAndPrices");
        groupPhrases = new AjaxUpdateShowConditionsGroupPhrasesBeanBuilder();
        jsonPhrases = new AjaxUpdateShowConditionsRequestBean();

        requestParams = new AjaxUpdateShowConditionsParameters();
        requestParams.setUlogin(CLIENT_LOGIN);
        requestParams.setCid(String.valueOf(campaignId));
        requestParams.setJsonPhrases(jsonPhrases);
        requestParams.setJsonRetargetings("{}");

        cmdRule.oldSteps().onPassport().authoriseAs(CLIENT_LOGIN, User.get(CLIENT_LOGIN).getPassword());
    }

    protected AjaxUpdateShowConditionsBean getPhraseFromResponse(DirectResponse response, String phraseId) {
        String jsonPhrase =
                cmdRule.oldSteps().commonSteps().readResponseJsonProperty(response, "." + phraseId + "[0]").toString();
        return JsonPathJSONPopulater.eval(jsonPhrase, new AjaxUpdateShowConditionsBean(), BeanType.RESPONSE);
    }
}
