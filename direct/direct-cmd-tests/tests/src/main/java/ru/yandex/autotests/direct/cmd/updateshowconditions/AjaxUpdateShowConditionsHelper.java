package ru.yandex.autotests.direct.cmd.updateshowconditions;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class AjaxUpdateShowConditionsHelper {

    public final static int MAX_PHRASES_IN_GROUP = 200;

    public static AjaxUpdateShowConditionsRequest buildRequestWithMaxAvailablePhrases(String phraseBase, Long groupId) {
        AjaxUpdateShowConditionsObjects phrases = new AjaxUpdateShowConditionsObjects();
        List<AjaxUpdateShowConditions> showConditionsList = new ArrayList<>();
        for (int i = 1; i <= MAX_PHRASES_IN_GROUP; ++i) {
            showConditionsList.add(
                    new AjaxUpdateShowConditions()
                            .withPhrase(phraseBase + i)
                            .withPrice(PhrasesFactory.getDefaultPhrase().getPrice().toString())
                            .withPriceContext(PhrasesFactory.getDefaultPhrase().getPriceContext().toString())
            );
        }
        phrases.withAdded(showConditionsList);
        return new AjaxUpdateShowConditionsRequest()
                .withGroupPhrases(groupId.toString(), phrases);
    }

    public static void turnOnRelevanceMatch(DirectCmdRule cmdRule, BannersRule bannersRule, Long bidId, String ulogin) {
        AjaxUpdateShowConditionsObjects showConditions = new AjaxUpdateShowConditionsObjects().withEdited(
                String.valueOf(bidId),
                new AjaxUpdateShowConditions().withIsSuspended("0")
        );

        updateShowConditions(showConditions, ulogin, cmdRule, bannersRule);
    }

    public static void turnOffRelevanceMatch(DirectCmdRule cmdRule, BannersRule bannersRule, Long bidId, String ulogin) {
        AjaxUpdateShowConditionsObjects showConditions = new AjaxUpdateShowConditionsObjects().withEdited(
                String.valueOf(bannersRule.getCurrentGroup().getRelevanceMatch().get(0).getBidId()),
                new AjaxUpdateShowConditions().withIsSuspended("1")
        );

        updateShowConditions(showConditions, ulogin, cmdRule, bannersRule);
    }

    public static void updateShowConditions(AjaxUpdateShowConditionsObjects showConditions, String ulogin,
            DirectCmdRule cmdRule, BannersRule bannersRule)
    {
        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withCid(String.valueOf(bannersRule.getCampaignId()))
                .withRelevanceMatch(bannersRule.getGroupId(), showConditions)
                .withUlogin(ulogin);
        ErrorResponse response =
                cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditions(request);
        assumeThat("нет ошибок при сохранении условия", response.getError(), nullValue());
    }
}
