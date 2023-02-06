package ru.yandex.autotests.direct.cmd.bssynced.phrases;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;

import java.util.Collections;

public abstract class StatusBsSyncedAfterPhraseChangeBaseTest {
    protected static final String CLIENT = "at-direct-bids-1";
    protected static final String NEW_PHRASE = "новая фраза";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected BannersRule bannersRule;
    protected Long campaignId;
    protected CampaignTypeEnum campaignType;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        addPhrase("доп фраза");
        BsSyncedHelper.moderateCamp(cmdRule, campaignId);
        BsSyncedHelper.setGroupBsSynced(cmdRule, bannersRule.getGroupId(), StatusBsSynced.YES);
        bannersRule.getCurrentGroup().getPhrases().stream()
                .forEach(f -> BsSyncedHelper.setPhraseBsSynced(cmdRule, f.getId(), StatusBsSynced.YES));
    }

    protected void startPhrase(String phraseId) {
        startStopPhrase(phraseId, "0");
    }

    protected void stopPhrase(String phraseId) {
        startStopPhrase(phraseId, "1");
    }

    protected void startStopPhrase(String phraseId, String status) {
        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withCid(campaignId.toString())
                .withGroupPhrases(bannersRule.getGroupId().toString(),
                        new AjaxUpdateShowConditionsObjects()
                                .withEdited(phraseId, new AjaxUpdateShowConditions()
                                        .withIsSuspended(status)))
                .withUlogin(CLIENT);

        cmdRule.cmdSteps().phrasesSteps().postAjaxUpdateShowCondition(request);
    }

    protected void addPhrase(String text) {
        Group group = bannersRule.getCurrentGroup();
        Phrase newPhrase = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_PHRASE_DEFAULT2, Phrase.class);
        newPhrase.withPhrase(text);
        group.getPhrases().add(newPhrase);
        group.setRetargetings(Collections.emptyList());
        group.setTags(Collections.emptyMap());
        group.setAdGroupID(bannersRule.getGroupId().toString());

        GroupsParameters groupsParameters = GroupsParameters.
                forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        bannersRule.saveGroup(groupsParameters);
    }

}
