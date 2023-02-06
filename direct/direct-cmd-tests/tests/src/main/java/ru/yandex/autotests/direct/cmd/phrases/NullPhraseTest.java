package ru.yandex.autotests.direct.cmd.phrases;

import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.group.Retargeting;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение группы без фраз с условием ретаргетинга")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class NullPhraseTest {
    protected static final String CLIENT = "at-direct-b-bannersmultisave";
    private Long campaignId;
    private Group group;

    TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule).as(CLIENT);


    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
        group = GroupsFactory.getDefaultTextGroup();
        group.setAdGroupID(String.valueOf(bannersRule.getGroupId()));
        group.getBanners().get(0).withBid(bannersRule.getBannerId());
        group.setPhrases(Collections.emptyList());
        group.setCampaignID(String.valueOf(campaignId));
        group.getBanners().stream().forEach(b -> b.withCid(campaignId));

        long retCondId = createRetargetingCondition();
        Retargeting retargeting = new Retargeting().withRetCondId(retCondId).withPriceContext("200");
        group.setRetargetings(Collections.singletonList(retargeting));
    }

    @Test
    @Description("Сохранение группы без фраз с условием ретаргетинга")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9920")
    public void saveGroup() {
        GroupsParameters groupRequest = GroupsParameters.forNewCamp(CLIENT, campaignId, group);

        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupRequest);
        List<Phrase> actualPhrases = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, String.valueOf(campaignId)).
                getGroups().get(0).getPhrases();
        assertThat("Сохраненные фразы совпадают с ожидаемыми", actualPhrases, hasSize(0));
    }

    private long createRetargetingCondition() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).retargetingConditionSteps()
                .deleteUnusedRetargetingsConditions(Long.valueOf(User.get(CLIENT).getClientID()));

        List<ru.yandex.autotests.directapi.common.api45.RetargetingCondition> retargetingConditions =
                cmdRule.apiSteps().retargetingSteps().addConditionsForUser(CLIENT, 1);
        return (long) retargetingConditions.get(0).getRetargetingConditionID();
    }
}
