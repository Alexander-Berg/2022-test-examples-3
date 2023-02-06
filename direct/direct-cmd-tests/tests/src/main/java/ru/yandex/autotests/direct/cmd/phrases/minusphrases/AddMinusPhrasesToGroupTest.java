package ru.yandex.autotests.direct.cmd.phrases.minusphrases;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение минус-фраз в группе")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class AddMinusPhrasesToGroupTest extends MinusPhrasesBaseTest {

    @Test
    @Description("Сохранение группы с минус-фразами")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10697")
    public void saveGroupWithMinusPhrases() {
        Group group = bannersRule.getGroup().
                withAdGroupID(bannersRule.getGroupId().toString())
                .withTags(emptyMap())
                .withMinusWords(minusPhraseList);
        group.getBanners().get(0).withBid(bannersRule.getBannerId());

        GroupsParameters groupsParameters = GroupsParameters
                .forExistingCamp(CLIENT, bannersRule.getCampaignId(), group);
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupsParameters);
        List actualMinusPhrasesList = cmdRule.cmdSteps().groupsSteps().getGroup(
                CLIENT, bannersRule.getCampaignId(), bannersRule.getGroupId()).getMinusWords();

        assertThat("Сохраненные минус-фразы совпадают с ожидаемыми", actualMinusPhrasesList, equalTo(minusPhraseList));
    }
}
