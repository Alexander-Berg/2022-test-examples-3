package ru.yandex.autotests.direct.httpclient.banners.phrases.ajaxupdatephrasesandprices;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.phrases.AjaxUpdateShowConditionsBean;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.direct.httpclient.util.beanmapper.BeanMapper;
import ru.yandex.autotests.direct.httpclient.util.mappers.AjaxUpdatePhraseMappingBuilder;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

/**
 * Created by shmykov on 27.05.15.
 * TESTIRT-4965
 */
@Aqua.Test
@Description("Проверки контроллера ajaxUpdateShowConditions для нескольких фраз")
@Stories(TestFeatures.Phrases.AJAX_UPDATE_PHRASES_AND_PRICES)
@Features(TestFeatures.PHRASES)
@Tag(TrunkTag.YES)
public class AjaxUpdateShowConditionsMultiplePhrasesTest extends AjaxUpdateShowConditionsTestBase {

    public AjaxUpdateShowConditionsMultiplePhrasesTest() {
        super(GroupsFactory.getDefaultTextGroup().withPhrases(
                Arrays.asList(
                        PhrasesFactory.getDefaultPhrase().withPhrase("some1"),
                        PhrasesFactory.getDefaultPhrase().withPhrase("some2")
                )
        ));
    }

    @Test
    @Description("Удаление непоследней фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10258")
    public void deleteNotLastPhraseTest() {
        Long phraseIdToDelete = bannersRule.getCurrentGroup().getPhrases()
                .stream()
                .filter(p -> p.getPhrase().equals("some1"))
                .map(Phrase::getId)
                .findFirst()
                .orElseThrow(() -> new AssumptionException("не удалось определить id фразы к удалению"));

        groupPhrases.setDeleted(String.valueOf(phraseIdToDelete));
        jsonPhrases.setGroupPhrases(String.valueOf(adgroupId), groupPhrases);
        response =
                cmdRule.oldSteps().ajaxUpdatePhrasesAndPricesSteps().ajaxUpdateShowConditions(csrfToken, requestParams);

        Group actualGroup = bannersRule.getCurrentGroup();
        assertThat("у баннера при проверке через апи конкретное число фраз", actualGroup.getPhrases(),
                hasSize(equalTo(1)));
        assertThat("id оставшейся фразы соответствует ожиданиям", actualGroup.getPhrases().get(0).getPhrase(),
                equalTo("some2"));
    }

    @Test
    @Description("Остановка непоследней фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10255")
    public void suspendNotLastPhraseTest() {
        phraseBean = new AjaxUpdateShowConditionsBean();
        phraseBean.setIsSuspended("1");
        phrasesMap.put(String.valueOf(firstPhraseId), phraseBean);
        groupPhrases.setEdited(phrasesMap);
        jsonPhrases.setGroupPhrases(String.valueOf(adgroupId), groupPhrases);
        response =
                cmdRule.oldSteps().ajaxUpdatePhrasesAndPricesSteps().ajaxUpdateShowConditions(csrfToken, requestParams);

        Group actualBanner = cmdRule.cmdSteps().groupsSteps()
                        .getGroup(CLIENT_LOGIN, bannersRule.getCampaignId(), bannersRule.getGroupId());
        assertThat("фраза остановилась", actualBanner.getPhrases().get(0).getIsSuspended(), equalTo("1"));
    }

    @Test
    @Description("Склейка двух одинаковых фраз")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10257")
    public void gluePhrasesTest() {
        List<Phrase> phrases = bannersRule.getCurrentGroup().getPhrases();
        Phrase phrase = phrases.get(0);
        phraseBean = new AjaxUpdateShowConditionsBean()
                .withPhrase(phrase.getPhrase())
                .withIsSuspended("0")
                .withPrice(phrase.getPrice().toString());

        phrasesMap.put(String.valueOf(phrases.get(1).getId()), phraseBean);
        groupPhrases.setEdited(phrasesMap);
        jsonPhrases.setGroupPhrases(String.valueOf(adgroupId), groupPhrases);
        response =
                cmdRule.oldSteps().ajaxUpdatePhrasesAndPricesSteps().ajaxUpdateShowConditions(csrfToken, requestParams);
        Group actualGroup = cmdRule.cmdSteps().groupsSteps()
                .getGroup(CLIENT_LOGIN, bannersRule.getCampaignId(), bannersRule.getGroupId());

        assumeThat("в ответе осталась только одна фраза", actualGroup.getPhrases(), hasSize(1));

        AjaxUpdateShowConditionsBean actualPhrase =
                BeanMapper.map(actualGroup.getPhrases().get(0), AjaxUpdateShowConditionsBean.class,
                        new AjaxUpdatePhraseMappingBuilder(User.get(CLIENT_LOGIN).getCurrency()));

        assertThat("фраза при проверке через api соответствует ожиданиям",
                actualPhrase, beanDiffer(phraseBean).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Проверка фразы в ответе сервера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10256")
    public void ajaxUpdateMultiplePhrasesResponseTest() {

        AjaxUpdateShowConditionsBean anotherPhraseBean =
                new PropertyLoader<>(AjaxUpdateShowConditionsBean.class)
                        .getHttpBean("anotherPhraseForAjaxUpdatePhrasesAndPrices");
        phrasesMap.put(String.valueOf(firstPhraseId), phraseBean);
        phrasesMap.put(String.valueOf(secondPhraseId), anotherPhraseBean);
        groupPhrases.setEdited(phrasesMap);
        jsonPhrases.setGroupPhrases(String.valueOf(adgroupId), groupPhrases);
        response =
                cmdRule.oldSteps().ajaxUpdatePhrasesAndPricesSteps().ajaxUpdateShowConditions(csrfToken, requestParams);

        AjaxUpdateShowConditionsBean firstActualPhrase = getPhraseFromResponse(response, String.valueOf(firstPhraseId));
        AjaxUpdateShowConditionsBean secondActualPhrase =
                getPhraseFromResponse(response, String.valueOf(secondPhraseId));
        assertThat("фраза в ответе совпадает с ожидаемой", firstActualPhrase, beanEquivalent(phraseBean));
        assertThat("фраза в ответе совпадает с ожидаемой", secondActualPhrase, beanEquivalent(anotherPhraseBean));
    }
}
