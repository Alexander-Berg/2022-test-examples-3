package ru.yandex.autotests.direct.httpclient.steps.mobileappbanners;

import org.apache.commons.lang.StringUtils;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.MobileGroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.PhraseCmdBean;
import ru.yandex.autotests.direct.httpclient.data.banners.EditMobileGroupRequestBean;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.autotests.irt.testutils.beandiffer.beanconstraint.BlackListConstraint;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;


/**
 * Created by aleran on 29.09.2015.
 */
public class EditAdGroupsMobileContentSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера editAdGroupsMobileContent")
    public DirectResponse getEditAdGroupsMobileContentDirectResponse(EditMobileGroupRequestBean params) {
        return execute(getRequestBuilder().get(CMD.EDIT_ADGROUPS_MOBILE_CONTENT, params));
    }

    public DirectResponse getEditAdGroupsMobileContentDirectResponse(String login, String campaignId, String... adGroupIds) {
        EditMobileGroupRequestBean requestBean = new EditMobileGroupRequestBean();
        requestBean.setAdgroupIds(StringUtils.join(adGroupIds, ','));
        requestBean.setCid(campaignId);
        requestBean.setUlogin(login);
        return getEditAdGroupsMobileContentDirectResponse(requestBean);
    }

    public MobileGroupsCmdBean getEditAdGroupsMobileContent(String login, String campaignId, String... adGroupIds) {
        return JsonPathJSONPopulater.evaluateResponse(getEditAdGroupsMobileContentDirectResponse(login, campaignId, adGroupIds),
                new MobileGroupsCmdBean());
    }

    @Step("Проверем, что ожидаемая фраза совпадает с фразой (login = {1} campaignId = {2} adGroupId = {3}) по полям {5}")
    public void shouldSeePhrasesEqualsWithFields(PhraseCmdBean expectedPhraseCmdBean,
                                                 String login,
                                                 String campaignId,
                                                 String adGroupId,
                                                 String... fields) {
        PhraseCmdBean actualPhraseCmdBean = getEditAdGroupsMobileContent(login, campaignId, adGroupId)
                .getGroups().get(0).getPhrases().get(0);
        assertThat("Поля фраз не равны", actualPhraseCmdBean,
                beanEquals(expectedPhraseCmdBean).byFields(fields));
    }

    @Step("Проверяем, что параметры группы {2} соответствуют параметрам группы {5}")
    public void shouldSeeBannerGroupParametersEqualWithIgnoreFields(String actualCampaignLogin,
                                                                    String actualCampaignId,
                                                                    String actualGroupId,
                                                                    String expectedCampaignLogin,
                                                                    String expectedCampaignId,
                                                                    String expectedGroupId,
                                                                    String... ignoreFields) {
        MobileGroupsCmdBean actualGroup = getEditAdGroupsMobileContent(actualCampaignLogin, actualCampaignId, actualGroupId);
        MobileGroupsCmdBean expectedGroup = getEditAdGroupsMobileContent(expectedCampaignLogin, expectedCampaignId, expectedGroupId);

        AllureUtils.addJsonAttachment("actualGroup", actualGroup.toJson());
        AllureUtils.addJsonAttachment("expectedGroup", expectedGroup.toJson());

        assertThat(actualGroup, beanEquivalent(expectedGroup)
                .fields(new BlackListConstraint().putFields(ignoreFields)));
    }
}
