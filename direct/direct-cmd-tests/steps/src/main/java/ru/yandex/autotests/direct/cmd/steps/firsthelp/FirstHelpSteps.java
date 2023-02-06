package ru.yandex.autotests.direct.cmd.steps.firsthelp;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.firsthelp.AcceptOptimizeRequest;
import ru.yandex.autotests.direct.cmd.data.firsthelp.CompleteOptimizingRequest;
import ru.yandex.autotests.direct.cmd.data.firsthelp.SendCampaignOptimisingOrderRequest;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class FirstHelpSteps extends DirectBackEndSteps {
    private static final Long FIRST_AID_MIN_BUDGET = 300l;

    @Step("Отправляем заявку на первую помощь")
    public void postSendCampaignOptimizingOrder(SendCampaignOptimisingOrderRequest request) {
        post(CMD.SEND_CAMPAIGN_OPTIMIZING_ORDER, request, Void.class);
    }

    @Step("Отправляем заявку на первую помощь для client: {0}, кампании: {1}")
    public void postSendCampaignOptimizingOrder(String client, Long campaignId) {
        SendCampaignOptimisingOrderRequest request = new SendCampaignOptimisingOrderRequest()
                .withBudget(FIRST_AID_MIN_BUDGET)
                .withCid(campaignId)
                .withUlogin(client);
        postSendCampaignOptimizingOrder(request);
    }

    @Step("Отправляем баннеры на оптимизацию c невалидными данными")
    public ErrorResponse acceptOptimizeWithError(AcceptOptimizeRequest acceptOptimizeRequest) {
        return post(CMD.ACCEPT_OPTIMIZE, acceptOptimizeRequest, ErrorResponse.class);
    }

    @Step("Отправляем баннеры на оптимизацию")
    public Void acceptOptimize(AcceptOptimizeRequest acceptOptimizeRequest) {
        return post(CMD.ACCEPT_OPTIMIZE, acceptOptimizeRequest, Void.class);
    }

    @Step("Отправляем баннеры на оптимизацию c невалидными данными campaignId: {0}, phraseIds: {1}, requestId: {2}")
    public ErrorResponse acceptOptimizeWithError(Long cid, List<Long> phraseIds, Long optimizeRequestID, String client) {
        AcceptOptimizeRequest request = new AcceptOptimizeRequest()
                .withCid(cid)
                .withPhIds(phraseIds)
                .withOptimizeRequestId(optimizeRequestID)
                .withUlogin(client);
        return acceptOptimizeWithError(request);
    }

    @Step("Отправляем баннеры на оптимизацию campaignId: {0}, phraseIds: {1}, requestId: {2}")
    public Void acceptOptimize(Long cid, List<Long> phraseIds, Long optimizeRequestID, String client) {
        AcceptOptimizeRequest request = new AcceptOptimizeRequest()
                .withCid(cid)
                .withPhIds(phraseIds)
                .withOptimizeRequestId(optimizeRequestID)
                .withUlogin(client);
        return acceptOptimize(request);
    }

    @Step("Завершаем оптимизацию баннеров")
    public void completeOptimizing(CompleteOptimizingRequest request) {
        post(CMD.COMPLETE_OPTIMIZING, request, Void.class);
    }

    @Step("Завершаем оптимизацию баннеров campaignId: {0}, requestId: {1}")
    public void completeOptimizing(Long cid, Long optimizeRequestID) {
        CompleteOptimizingRequest completeOptimizingRequest = new CompleteOptimizingRequest()
                .withOptimizeRequestId(optimizeRequestID)
                .withCid(cid)
                .withOptimizationComment(RandomStringUtils.randomAlphabetic(8));
        post(CMD.COMPLETE_OPTIMIZING, completeOptimizingRequest, Void.class);
    }
}
