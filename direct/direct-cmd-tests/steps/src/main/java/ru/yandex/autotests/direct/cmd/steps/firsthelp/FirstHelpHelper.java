package ru.yandex.autotests.direct.cmd.steps.firsthelp;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.directapi.darkside.model.Status;

import static java.util.stream.Collectors.toList;

public class FirstHelpHelper {
    private static final Float BALANCE = 1000f;
    public static Long prepareCampForOptimizinhAccept(DirectCmdRule cmdRule, Long cid, Long groupId, String client) {
        List<Long> campaigns = cmdRule.apiSteps().campaignStepsV5().getAllCampaignIds(client);
        campaigns.remove(cid);
        cmdRule.apiAggregationSteps().deleteActiveCampaignsQuietly(client,
                campaigns.toArray(new Long[campaigns.size()])
        );
        cmdRule.apiAggregationSteps().activateCampaignWithMoney(client, cid, BALANCE);
        cmdRule.darkSideSteps().getClientFakeSteps().setShowFaTeaserToUser(client, Status.YES);
        cmdRule.cmdSteps().firstHelpSteps().postSendCampaignOptimizingOrder(client, cid);
        Long optimizeRequestId = cmdRule.cmdSteps().campaignSteps().getShowCamp(
                client,
                String.valueOf(cid)
        ).getOptimizeCamp().getRequestId();
        cmdRule.cmdSteps().groupsSteps().postSendOptimize(client,
                cid,
                groupId,
                optimizeRequestId);
        cmdRule.cmdSteps().firstHelpSteps().completeOptimizing(cid, optimizeRequestId);
        return optimizeRequestId;
    }
}
