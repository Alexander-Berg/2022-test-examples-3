package ru.yandex.autotests.direct.httpclient.banners.phrases.ajaxeditadgroupdynamicconditions;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.yandex.direct.api.v5.dynamictextadtargets.StringConditionOperatorEnum;
import com.yandex.direct.api.v5.dynamictextadtargets.WebpageConditionOperandEnum;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.DynamicGroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.dynamicconditions.DynamicConditionsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxEditAdGroupDynamicConditionsParameters;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxEditAdGroupDynamicConditionsRequestBean;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxUpdateShowConditionsBean;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxUpdateShowConditionsGroupPhrasesBeanBuilder;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.BeanType;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.directapi.model.api5.dynamictextadtargets.WebpageAddItemMap;
import ru.yandex.autotests.directapi.model.api5.dynamictextadtargets.WebpageConditionMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;


/**
 * Created by f1nal
 * on 19.08.15.
 * TESTIRT-6778
 */

public abstract class AjaxEditAdGroupDynamicConditionsBase {

    public static final String CLIENT_LOGIN = "at-direct-backend-c";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public static CSRFToken csrfToken;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    public Long campaignId;
    public DynamicGroupsCmdBean expectedGroups;
    public Long groupId;
    protected List<String> expectedPhrasesId;
    protected AjaxEditAdGroupDynamicConditionsParameters requestParams;
    protected AjaxEditAdGroupDynamicConditionsRequestBean jsonPhrases;
    protected AjaxUpdateShowConditionsGroupPhrasesBeanBuilder groupPhrases;
    protected Map<String, AjaxUpdateShowConditionsBean> phrasesMap;
    protected AjaxUpdateShowConditionsBean phraseBean;

    @Before
    public void before() {
        setUp();
    }

    protected abstract Long createCamp();

    protected void setUp() {

        cmdRule.oldSteps().onPassport().authoriseAs(Logins.SUPER, User.get(Logins.SUPER).getPassword()); //фича пока
        // доступна только суперу
        csrfToken = getCsrfTokenFromCocaine(User.get(Logins.SUPER).getPassportUID());
        cmdRule.getApiStepsRule().as("at-direct-super", CLIENT_LOGIN);

        campaignId = createCamp();
        groupId = saveBanner();
        expectedGroups = getResponseDynamicGroupById(groupId);
        expectedPhrasesId = new ArrayList<>();
        Integer expectedCnt = expectedGroups.getGroups().get(0).getDynamicConditions().size() > 2 ? 3 : 1;
        for (Integer i = 0; i < expectedCnt; i++) {
            expectedPhrasesId.add(expectedGroups.getGroups().get(0).getDynamicConditions().get(i).getDynamicConditionId());
        }
        phrasesMap = new HashMap<>();
        groupPhrases = new AjaxUpdateShowConditionsGroupPhrasesBeanBuilder();
        jsonPhrases = new AjaxEditAdGroupDynamicConditionsRequestBean();
        requestParams = new AjaxEditAdGroupDynamicConditionsParameters();
        requestParams.setUlogin(CLIENT_LOGIN);
        requestParams.setCid(String.valueOf(campaignId));
        requestParams.setJsonPhrases(jsonPhrases);
    }

    protected Long saveBanner() {
        Long pid = cmdRule.apiSteps().adGroupsSteps().addDefaultGroupDynamic(campaignId);
        cmdRule.apiSteps().adsSteps().addDefaultDynamicTextAd(pid);

        WebpageAddItemMap[] map = new WebpageAddItemMap[]{
                new WebpageAddItemMap()
                        .withConditions(new WebpageConditionMap()
                                .withOperand(WebpageConditionOperandEnum.URL)
                                .withOperator(StringConditionOperatorEnum.CONTAINS_ANY)
                                .withArguments("Different Condition_0")
                        )
                        .withName("Different Name_0")
                        .withAdGroupId(pid),
                new WebpageAddItemMap()
                        .withConditions(new WebpageConditionMap()
                                .withOperand(WebpageConditionOperandEnum.URL)
                                .withOperator(StringConditionOperatorEnum.CONTAINS_ANY)
                                .withArguments("Different Condition_1")
                        )
                        .withName("Different Name_1")
                        .withAdGroupId(pid),
                new WebpageAddItemMap()
                        .withConditions(new WebpageConditionMap()
                                .withOperand(WebpageConditionOperandEnum.URL)
                                .defaultWebpage()
                                .withOperator(StringConditionOperatorEnum.CONTAINS_ANY)
                                .withArguments("Different Condition_2")
                        )
                        .withName("Different Name_2")
                        .withAdGroupId(pid)
        };
        List<Long> longs = cmdRule.apiSteps().dynamicTextAdTargetsSteps().addWebpages(map);
        assumeThat("условия добавились", longs, Matchers.hasSize(3));

        return pid;
    }

    protected DynamicGroupsCmdBean getResponseDynamicGroupById(Long gId) {
        GroupsParameters requestParams = new GroupsParameters();
        requestParams.setUlogin(CLIENT_LOGIN);
        requestParams.setAdgroupIds(gId.toString());
        requestParams.setCid(String.valueOf(campaignId));
        requestParams.setBannerStatus("all");
        DirectResponse response = cmdRule.oldSteps().groupsSteps().editDynamicAdGroups(csrfToken, requestParams);
        return JsonPathJSONPopulater.eval(response.getResponseContent().asString(), new DynamicGroupsCmdBean(),
                BeanType.RESPONSE, Arrays.asList("filter_schema_performance", "filter_schema_dynamic",
                        "static_file_hashsums"));
    }

    protected void checkPhrasePrice(Map<String, AjaxUpdateShowConditionsBean> checkedPhrases) {
        DynamicGroupsCmdBean actualGroup = getResponseDynamicGroupById(groupId);
        List<DynamicConditionsCmdBean> actualConditions = actualGroup.getGroups().get(0).getDynamicConditions();
        Integer foundPhrasesCound = 0;
        for (DynamicConditionsCmdBean condition : actualConditions) {
            if (checkedPhrases.containsKey(condition.getDynamicConditionId())) {
                assertPhrasePrice(condition, checkedPhrases.get(condition.getDynamicConditionId()));
                foundPhrasesCound++;
            }
        }
        assertThat("Измененные фразы найдены",
                checkedPhrases.size(), equalTo(foundPhrasesCound));
    }

    protected void sendRequestAjaxEditAdGroupDynamicConditions() {
        CommonResponse response = new Gson().fromJson(cmdRule.oldSteps().ajaxEditAdGroupDynamicConditions()
                .ajaxEditAdGroupDynamicConditions(csrfToken, requestParams).getResponseContent().toString(),
                CommonResponse.class);
        assumeThat("Ответ контроллера Ок", response.getResult(), equalTo("ok"));
    }

    abstract void assertPhrasePrice(DynamicConditionsCmdBean condition, AjaxUpdateShowConditionsBean checkPhraseBean);

    @After
    public void deleteCampaign() {
        if (campaignId != null) {
            cmdRule.apiAggregationSteps().makeCampaignReadyForDelete(campaignId);
            cmdRule.cmdSteps().campaignSteps().deleteCampaign(CLIENT_LOGIN, campaignId);
        }
    }
}
