package ru.yandex.autotests.direct.cmd.phrases;

import java.util.Collections;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.fake.BannerPhraseFakeInfo;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка предупреждений при сохранении фраз")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.PHRASES)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.PHRASE)
@Tag(CampTypeTag.TEXT)
public class PhraseWarningsTest {
    protected static final String CLIENT = "at-backend-phrase-1";
    private static final int WORD_MAX_SIZE = 4096;
    private static final int MAX_WORD_COUNT = 7;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected TextBannersRule bannerRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(bannerRule);
    private BannerPhraseFakeInfo bannerPhraseFakeInfo;
    private int campaignId;
    private long phraseId;

    @Before
    public void before() {
        campaignId = bannerRule.getCampaignId().intValue();
        phraseId = cmdRule.cmdSteps().groupsSteps()
                .getPhrases(CLIENT, bannerRule.getCampaignId(), bannerRule.getGroupId())
                .get(0).getId();
        bannerPhraseFakeInfo = cmdRule.apiSteps().phrasesFakeSteps().getBannerPhraseParams(phraseId);
    }

    @Test
    @Description("сохраняем группу со слишком длинной фразой")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9926")
    public void saveGroupWithTooLongPhrase() {
        bannerPhraseFakeInfo.setPhrase(RandomStringUtils.randomAlphabetic(WORD_MAX_SIZE + 1));
        cmdRule.apiSteps().phrasesFakeSteps()
                .updateBannerPhrasesParams(Collections.singletonList(bannerPhraseFakeInfo));

        ShowCampResponse actualResponse =
                cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, String.valueOf(campaignId));
        assertThat("полученный флаг предупрежедения 'does_phrase_exceed_max_length' совпадает с ожидаемым",
                actualResponse.getGroups().get(0).getPhraseExceedMaxLength(), equalTo("1"));
    }

    @Test
    @Description("сохраняем группу со слишком большим количеством слов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9927")
    public void saveGroupWithTooMuchWords() {
        bannerPhraseFakeInfo.setPhrase(createPhraseWithTooMuchWords());
        cmdRule.apiSteps().phrasesFakeSteps()
                .updateBannerPhrasesParams(Collections.singletonList(bannerPhraseFakeInfo));

        ShowCampResponse actualResponse =
                cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, String.valueOf(campaignId));
        assertThat("полученный флаг предупрежедения 'does_phrase_exceed_max_word' совпадает с ожидаемым",
                actualResponse.getGroups().get(0).getPhraseExceedMaxWordsCount(), equalTo("1"));
    }

    @Test
    @Description("сохраняем группу со слишком длинной фразой и слишком большим числом слов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9928")
    public void saveGroupWithTooLongPhraseAndTooMuchWords() {
        bannerPhraseFakeInfo
                .setPhrase(createPhraseWithTooMuchWords() + RandomStringUtils.randomAlphabetic(WORD_MAX_SIZE + 1));
        cmdRule.apiSteps().phrasesFakeSteps()
                .updateBannerPhrasesParams(Collections.singletonList(bannerPhraseFakeInfo));

        ShowCampResponse actualResponse =
                cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, String.valueOf(campaignId));
        assertThat("полученный флаг предупрежедения 'does_phrase_exceed_max_length' совпадает с ожидаемым",
                actualResponse.getGroups().get(0).getPhraseExceedMaxLength(), equalTo("1"));
        assertThat("полученный флаг предупрежедения 'does_phrase_exceed_max_word' совпадает с ожидаемым",
                actualResponse.getGroups().get(0).getPhraseExceedMaxWordsCount(), equalTo("1"));
    }

    private String createPhraseWithTooMuchWords() {
        String phrase = "";
        for (int i = 0; i < MAX_WORD_COUNT; i++) {
            phrase += RandomStringUtils.randomAlphabetic(2) + " ";
        }
        phrase += RandomStringUtils.randomAlphabetic(2);
        return phrase;
    }

}
