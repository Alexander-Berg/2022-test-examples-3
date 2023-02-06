package ru.yandex.autotests.direct.cmd.phrases.minusphrases.intersection.positive;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

public abstract class AjaxCheckMinusPhrasesPositiveBase {

    public static final String CLIENT = "at-backend-minuswords";
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
    public String minusPhraseSrt;

    @Parameterized.Parameters(name = "Проверяем пересечение КС {0} и МФ {1}:")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"купить +коня", "коня серого"},
                {"купить коня серого в яблоках весной", "купить коня [серого в яблоках] весной"},
                {"купить коня", "!купить коня"},
                {"!купить коня", "купить !коня"},
                {"купить коня", "!купить !коня"},
                {"!купить коня", "!купить !коня"},
                {"купить !коня", "!купить !коня"},
                {"купить коня", "купить коня серого"},
                {"купить коня", "!купить коня серого"},
                {"!купить коня", "!купить коня серого"},
                {"купить !коня", "!купить коня серого"},
                {"!купить !коня", "!купить коня серого"},
                {"купить коня серого", "!купить коня"},
                {"купить !коня серого", "!купить коня"},

                {"\"купить коня\"", "\"купить коня серого\""},
                {"купить коня серого быстро", "\"купить коня серого\""},
                {"купить коня серого", "\"купить коня серого\""},

                {"[купить коня]", "[коня купить]"},
                {"купить коня", "[купить коня]"},
                {"купить коня", "[купить коня серого]"},
                {"купить коня серого", "[купить коня]"},

                {"\"купить коня серого\"", "[купить коня серого]"},
                {"\"купить коня серого\"", "[купить серого коня]"},
                {"\"купить коня серого\"", "[купить коня]"},
        });
    }

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

}
