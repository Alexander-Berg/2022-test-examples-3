package ru.yandex.autotests.direct.httpclient.banners.phrases.ajaxapplyrejectcorrections;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections.AjaxApplyRejectCorrectionsPhrase;
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.httpclient.data.phrases.ajaxapplyrejectcorrections.CorrectionType.UNGLUED;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

/**
 * Created by shmykov on 11.06.15.
 * TESTIRT-4999
 */
@Aqua.Test
@Description("Проверки контроллера ajaxApplyRejectCorrection при расклейке")
@Stories(TestFeatures.Phrases.AJAX_UPDATE_PHRASES_AND_PRICES)
@Features(TestFeatures.PHRASES)
@Tag(TrunkTag.YES)
public class AjaxApplyRejectUnglueCorrectionTest extends AjaxApplyRejectCorrectionsTestBase {

    private static ArrayList phrases;
    private long expectedPhraseId;

    private AjaxApplyRejectCorrectionsPhrase testPhrase;
    @BeforeClass
    public static void beforeClass() {
        phrases = new ArrayList<>();
        phrases.add(PhrasesFactory.getDefaultPhrase().withPhrase("красная площадь"));
        phrases.add(PhrasesFactory.getDefaultPhrase().withPhrase("красная"));
    }

    public AjaxApplyRejectUnglueCorrectionTest() {
        super(phrases);
    }

    @Override
    protected void setPhrasesInRequest() {
        testPhrase = loader.getHttpBean("phraseForUnglue");
        expectedPhraseId = bannersRule.getCurrentGroup().getPhrases().get(1).getId();
        testPhrase.setPhraseId(String.valueOf(expectedPhraseId));
        jsonPhrases.addGroupPhrasesForUnglue(bannersRule.getGroupId().toString(), testPhrase);

        requestParams.setCorrection(UNGLUED.toString());
        requestParams.setIsRejected("0");
    }

    @Test
    @Description("Проверка принятия склеивания минус слов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10250")
    public void ajaxApplyMinusWordsUnglueCorrectionsTest() {
        response = cmdRule.oldSteps().ajaxApplyRejectCorrections().AjaxApplyRejectCorrections(csrfToken, requestParams);
        Phrase expectedPhrase = new Phrase()
                .withPhrase("красная -площадь")
                .withId(expectedPhraseId);
        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyFields(
                newPath("phrase"),
                newPath("id")
        );
        assertThat("фраза не изменилась",
                bannersRule.getCurrentGroup().getPhrases().stream().filter(x -> x.getId().equals(expectedPhraseId))
                        .findFirst()
                        .orElseThrow(() -> new DirectCmdStepsException("ожидмаемая группа не найдена")),
                BeanDifferMatcher.beanDiffer(expectedPhrase).useCompareStrategy(compareStrategy));
    }

    @Test
    @Description("Проверка отклонения корректировки стоп слов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10249")
    public void ajaxRejectMinusWordsUnglueCorrectionsTest() {
        requestParams.setIsRejected("1");
        response = cmdRule.oldSteps().ajaxApplyRejectCorrections().AjaxApplyRejectCorrections(csrfToken, requestParams);
        Phrase expectedPhrase =  new Phrase()
                .withPhrase("красная")
                .withId(expectedPhraseId);
        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyFields(
                newPath("phrase"),
                newPath("id")
        );
        assertThat("фраза не изменилась",
                bannersRule.getCurrentGroup().getPhrases().stream().filter(x -> x.getId().equals(expectedPhraseId))
                        .findFirst()
                        .orElseThrow(() -> new DirectCmdStepsException("ожидмаемая группа не найдена")),
                BeanDifferMatcher.beanDiffer(expectedPhrase).useCompareStrategy(compareStrategy));
    }
}
