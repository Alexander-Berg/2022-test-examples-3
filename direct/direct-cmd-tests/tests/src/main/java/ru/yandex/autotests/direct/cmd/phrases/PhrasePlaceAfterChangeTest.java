package ru.yandex.autotests.direct.cmd.phrases;


import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.BannerPhraseFakeInfo;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collections;

import static org.apache.commons.lang3.RandomUtils.nextDouble;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка изменения place во фразах")
@Stories(TestFeatures.Phrases.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.AJAX_UPDATE_SHOW_CONDITIONS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class PhrasePlaceAfterChangeTest {
    private static final String CLIENT = "at-backend-phrase-1";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public TextBannersRule bannerRule = (TextBannersRule) new TextBannersRule()
            .withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannerRule);

    private long phraseId;

    @Before
    public void before() {
        Phrase phrase = bannerRule.getCurrentGroup().getPhrases().get(0);
        phraseId = phrase.getId();
        BannerPhraseFakeInfo p = cmdRule.apiSteps().phrasesFakeSteps().getBannerPhraseParams(phraseId);
        p.setPlace(10);
        cmdRule.apiSteps().phrasesFakeSteps().updateBannerPhrasesParams(Collections.singletonList(p));
    }

    @Test
    @Description("Изменение place после пересохранения")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9922")
    public void reSave() {
        Group group = bannerRule.getCurrentGroup();
        group.getPhrases().get(0).setPrice(nextDouble(1400, 1800));
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(group, bannerRule.getMediaType());
        GroupsParameters groupRequest = GroupsParameters.forExistingCamp(CLIENT, bannerRule.getCampaignId(), group);
        bannerRule.saveGroup(groupRequest);
        Integer actualPlace = cmdRule.apiSteps().phrasesFakeSteps().getBannerPhraseParams(phraseId).getPlace();
        assertThat("place != 0", actualPlace, not(equalTo(0)));
    }


    @Test
    @Description("Изменение place после запроса ajax")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9923")
    public void changePrice() {
        cmdRule.cmdSteps().phrasesSteps().changePriceAjaxUpdateShowCondition(
                nextDouble(1400, 1800),
                phraseId,
                bannerRule.getCampaignId(),
                bannerRule.getGroupId(),
                CLIENT);

        Integer actualPlace = cmdRule.apiSteps().phrasesFakeSteps().getBannerPhraseParams(phraseId).getPlace();
        assertThat("place != 0", actualPlace, not(equalTo(0)));
    }

    public Group getGroup() {
        Group savingGroup = GroupsFactory.getDefaultTextGroup();
        savingGroup.setAdGroupID(String.valueOf(bannerRule.getGroupId()));
        savingGroup.getBanners().get(0).withBid(bannerRule.getBannerId());
        return savingGroup;
    }
}
