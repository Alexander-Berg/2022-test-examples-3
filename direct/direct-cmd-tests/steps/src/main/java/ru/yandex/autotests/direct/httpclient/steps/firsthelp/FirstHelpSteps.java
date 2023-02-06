package ru.yandex.autotests.direct.httpclient.steps.firsthelp;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.firsthelp.AcceptOptimizeRequestBean;
import ru.yandex.autotests.direct.httpclient.data.firsthelp.CompleteOptimizingRequestBean;
import ru.yandex.autotests.direct.httpclient.data.firsthelp.CompleteOptimizingRequestBeanBuilder;
import ru.yandex.autotests.direct.httpclient.data.firsthelp.SendCampaignOptimizingOrderParameters;
import ru.yandex.autotests.direct.httpclient.data.mediaplan.SendOptimizeParameters;
import ru.yandex.autotests.direct.httpclient.data.mediaplan.SendOptimizeParametersBuilder;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.CampaignsSteps;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class FirstHelpSteps extends DirectBackEndSteps {

    private static final Integer FIRST_AID_MIN_BUDGET = 300;
    private static final String DEFAULT_COMMENT = "default comment";

    @Step("Отправляем заявку на первую помощь")
    public DirectResponse sendCampaignOptimizingOrder(SendCampaignOptimizingOrderParameters params,
                                                      CSRFToken csrfToken) {
        return execute(getRequestBuilder().post(CMD.SEND_CAMPAIGN_OPTIMIZING_ORDER, csrfToken, params));
    }

    @Step("Отправляем заявку на первую помощь и проверяем, что она успешно отправилась")
    public void requestFirstHelp(SendCampaignOptimizingOrderParameters params, CSRFToken csrfToken) {
        DirectResponse resp = sendCampaignOptimizingOrder(params, csrfToken);
        assertThat("заявка на первую помощь корректно отправляется",
                resp.getResponseContent().asDocument().select(".message-ok").size(), equalTo(1));
    }

    public String requestFirstHelp(String client, Integer campaignId, CSRFToken csrfToken) {
        SendCampaignOptimizingOrderParameters parameters = new SendCampaignOptimizingOrderParameters();
        parameters.setBudget(FIRST_AID_MIN_BUDGET.toString());
        parameters.setUlogin(client);
        parameters.setCid(String.valueOf(campaignId));
        requestFirstHelp(parameters, csrfToken);
        CampaignsSteps campaignsSteps = getInstance(CampaignsSteps.class, config);
        DirectResponse showCamp = campaignsSteps.openShowCamp(client, String.valueOf(campaignId));
        return campaignsSteps.getOptimizeRequestID(showCamp);
    }

    public DirectResponse sendOptimize(String client, Integer campaignId, String adGroupIds, String optimizeRequestID
            , CSRFToken csrfToken) {
        SendOptimizeParameters sendOptimizeParameters = new SendOptimizeParametersBuilder()
                .setAdgroupIds(adGroupIds).setCid(String.valueOf(campaignId)).setOptimizeRequestId(optimizeRequestID).
                        createSendOptimizeParameters();
        sendOptimizeParameters.setUlogin(client);
        CampaignsSteps campaignsSteps = getInstance(CampaignsSteps.class, config);
        return campaignsSteps.sendOptimize(sendOptimizeParameters, csrfToken);
    }

    @Step("Завершаем оптимизацию баннеров")
    public DirectResponse completeOptimizing(CompleteOptimizingRequestBean params, CSRFToken csrfToken) {
        return execute(getRequestBuilder().post(CMD.COMPLETE_OPTIMIZING, csrfToken, params));
    }

    public DirectResponse completeOptimizing(Integer campaignId, String optimizeRequestID, CSRFToken csrfToken) {
        CompleteOptimizingRequestBean completeOptimizingRequestBean =
                new CompleteOptimizingRequestBeanBuilder().setOptimizeRequestId(optimizeRequestID)
                .setCid(String.valueOf(campaignId)).setOptimizationComment(DEFAULT_COMMENT).
                        createCompleteOptimizingRequestBean();
        return completeOptimizing(completeOptimizingRequestBean, csrfToken);
    }

    @Step("Принимаем рекомендации первой помощи (первый шаг)")
    public DirectResponse acceptOptimize(AcceptOptimizeRequestBean params, CSRFToken csrfToken) {
        return execute(getRequestBuilder().post(CMD.ACCEPT_OPTIMIZE, csrfToken, params));
    }

    public DirectResponse acceptOptimize(Integer cid, String phraseIds, String optimizeRequestID, CSRFToken csrfToken) {
        PropertyLoader<AcceptOptimizeRequestBean> loader = new PropertyLoader<>(AcceptOptimizeRequestBean.class);
        AcceptOptimizeRequestBean acceptOptimizeRequestBean = loader.getHttpBean("acceptOptimizeRequestBean");
        acceptOptimizeRequestBean.setCid(String.valueOf(cid));
        acceptOptimizeRequestBean.setOptimizeRequestId(optimizeRequestID);
        acceptOptimizeRequestBean.setPhIds(phraseIds);
        return acceptOptimize(acceptOptimizeRequestBean, csrfToken);

    }
}
