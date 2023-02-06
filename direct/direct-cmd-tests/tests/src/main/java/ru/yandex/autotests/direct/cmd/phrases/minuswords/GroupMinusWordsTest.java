package ru.yandex.autotests.direct.cmd.phrases.minuswords;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Изменение минус слов в группе")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class GroupMinusWordsTest {
    public static final String CLIENT = "at-backend-minuswords-group";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    @Parameterized.Parameter(value = 0)
    public String minusKeywordsStr;
    @Parameterized.Parameter(value = 1)
    public String expMinusKeywordsStr;

    @Parameterized.Parameters(name = "Меняем минус слова в группе с {0} на {2}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"", null},
                {"стол-стул", "стол-стул"},
                {"[конь]", "[конь]"},
                {"[для]", "[для]"},
                {"new minus words", "minus new words"},
                {"!words +minus new", "!words +minus new"},
        });
    }

    @Test
    @Description("Добавляем новую группу с минус-словами")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10730")
    public void addNewGroupWithMinusKeywords() {
        Group group = bannersRule.getGroup()
                .withTags(emptyMap())
                .withMinusWords(Arrays.asList(minusKeywordsStr.split(" ")));

        GroupsParameters groupsParameters = GroupsParameters
                .forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupsParameters);
        Long newGroupId = cmdRule.cmdSteps().campaignSteps().getShowCamp(
                CLIENT, bannersRule.getCampaignId().toString())
                .getGroups().get(1).getAdGroupId();

        List actualMinusKeywords = cmdRule.cmdSteps().groupsSteps().getGroup(
                CLIENT, bannersRule.getCampaignId(), newGroupId).getMinusWords();
        List expMinusKeywords = expMinusKeywordsStr == null ?
                new ArrayList<>() : Arrays.asList(expMinusKeywordsStr.split(" "));
        assertThat("Сохраненные минус-слова совпадают с ожидаемыми", actualMinusKeywords, equalTo(expMinusKeywords));
    }

}
