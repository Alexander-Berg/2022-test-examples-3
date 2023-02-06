package ru.yandex.autotests.direct.cmd.phrases.minuswords.intersection.negative;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.phrases.AjaxCheckMinusWordsRequest;
import ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;

import java.util.Collections;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

public abstract class AjaxCheckMinusWordsNegativeBase {

    public static final String CLIENT = "at-backend-minus-phrase-err";
    public static Long campaignId;
    public static Group group;
    public static TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.stepsClassRule().withRules(bannersRule);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    @Parameterized.Parameter(0)
    public String keyPhraseSrt;

    @Parameterized.Parameter(1)
    public String minusWordsSrt;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        group = bannersRule.getGroup()
                .withCampaignID(campaignId.toString())
                .withAdGroupID(bannersRule.getGroupId().toString())
                .withTags(emptyMap())
                .withPhrases(singletonList(PhrasesFactory.getDefaultPhrase().withPhrase(keyPhraseSrt)));
        group.getBanners().get(0).withBid(bannersRule.getBannerId());
        GroupsParameters groupsParameters = GroupsParameters.forExistingCamp(CLIENT, campaignId, group);
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupsParameters);
    }

    public void ajaxCheckCampMinusWords() {
        AjaxCheckMinusWordsRequest ajaxCheckMinusWordsRequest = new AjaxCheckMinusWordsRequest()
                .withCampaignId(campaignId.toString())
                .withJsonMinusWords(Collections.singletonList(minusWordsSrt))
                .withUlogin(CLIENT);
        CommonResponse response = cmdRule.cmdSteps().phrasesSteps().ajaxCheckCampMinusWords(ajaxCheckMinusWordsRequest);

        assertThat("Получили предупреждение о пересечении КС и МС", response.getOk(), equalTo("0"));
    }

    public void ajaxCheckBannersMinusWords() {
        AjaxCheckMinusWordsRequest ajaxCheckMinusWordsRequest = new AjaxCheckMinusWordsRequest()
                .withCampaignId(campaignId.toString())
                .withJsonKeyWords(Collections.singletonList(keyPhraseSrt))
                .withJsonMinusWords(Collections.singletonList(minusWordsSrt))
                .withUlogin(CLIENT);
        CommonResponse response = cmdRule.cmdSteps().phrasesSteps().ajaxCheckBannersMinusWords(ajaxCheckMinusWordsRequest);

        assertThat("Получили предупреждение о пересечении КС и МС", response.getOk(), equalTo("0"));
    }

}
