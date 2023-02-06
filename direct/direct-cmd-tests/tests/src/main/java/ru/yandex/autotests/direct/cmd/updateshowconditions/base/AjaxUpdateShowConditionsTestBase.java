package ru.yandex.autotests.direct.cmd.updateshowconditions.base;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory;
import ru.yandex.autotests.direct.cmd.data.strategy.AjaxSaveAutoBudgetRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.autotests.directapi.darkside.model.bslogs.AutoBudgetPriority;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.direct.cmd.updateshowconditions.AjaxUpdateShowConditionsHelper.MAX_PHRASES_IN_GROUP;
import static ru.yandex.autotests.direct.cmd.updateshowconditions.AjaxUpdateShowConditionsHelper.buildRequestWithMaxAvailablePhrases;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;


public abstract class AjaxUpdateShowConditionsTestBase {
    private static final String ULOGIN = "at-direct-add-plus-phrases";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = getBannerRule().withUlogin(ULOGIN);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    private String phraseBase = "новая фраза ПЗ ";

    protected abstract BannersRule getBannerRule();

    public void addOnePhrase() {
        String expectedNormPhrase = "новый пз фраза";
        String expectedNumWords = "3";
        AjaxUpdateShowConditions phaseToSend = new AjaxUpdateShowConditions()
                .withPhrase("новая фраза пз")
                .withPrice(PhrasesFactory.getDefaultPhrase().getPrice().toString())
                .withPriceContext(PhrasesFactory.getDefaultPhrase().getPriceContext().toString());

        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withGroupPhrases(bannersRule.getGroupId().toString(),
                        new AjaxUpdateShowConditionsObjects().withAdded(phaseToSend))
                .withUlogin(ULOGIN);

        List<Phrase> actualPhrase =
                cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditionsAndGetPhases(request);

        assumeThat("получили ответ по одной фразе", actualPhrase, hasSize(1));

        Phrase expectedPhrase = new Phrase()
                .withPhrase(phaseToSend.getPhrase())
                .withPremium(null)
                .withGuarantee(null);

        assertThat("получили верный ответ по фразе", actualPhrase.get(0), beanDiffer(expectedPhrase)
                .useCompareStrategy(onlyExpectedFields()));
    }

    public void phraseWithDifferentContextAndSearchPrices() {
        AjaxUpdateShowConditions phaseToSend = new AjaxUpdateShowConditions()
                .withPhrase("Новая фраза ПЗ")
                .withPriceContext(PhrasesFactory.getDefaultPhrase().getPrice().toString())
                .withPrice(PhrasesFactory.getDefaultPhrase().getPriceContext().toString());

        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withGroupPhrases(bannersRule.getGroupId().toString(),
                        new AjaxUpdateShowConditionsObjects().withAdded(phaseToSend))
                .withUlogin(ULOGIN);

        List<Phrase> actualPhrases =
                cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditionsAndGetPhases(request);

        assumeThat("получили ответ по одной фразе", actualPhrases, hasSize(1));

        Phrase expectedPhrase = new Phrase()
                .withPhrase(phaseToSend.getPhrase())
                .withPremium(null)
                .withGuarantee(null);

        assertThat("получили верный ответ по фразе", actualPhrases.get(0), beanDiffer(expectedPhrase)
                .useCompareStrategy(onlyExpectedFields()));

    }

    public void phraseForAutobudgetStrategy() {
        AjaxSaveAutoBudgetRequest ajaxSaveAutoBudgetRequest = new AjaxSaveAutoBudgetRequest().
                withCid(bannersRule.getCampaignId().toString()).
                withJsonStrategy(CmdStrategyBeans.getStrategyBean(Strategies.AVERAGE_PRICE_DEFAULT)).
                withuLogin(ULOGIN);
        cmdRule.cmdSteps().strategySteps().postAjaxSaveAutobudget(ajaxSaveAutoBudgetRequest);

        AutoBudgetPriority priority = AutoBudgetPriority.MEDIUM;

        //после смены стратегии на автобюджетную, нужно выставить значение автобюджета у фразы
        Group current = bannersRule.getCurrentGroup();
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(current, bannersRule.getMediaType());
        current.getPhrases().get(0).setAutobudgetPriority(Double.valueOf(priority.transportValue()));

        bannersRule.saveGroup(GroupsParameters.forExistingCamp(ULOGIN, bannersRule.getCampaignId(), current));

        AjaxUpdateShowConditions phaseToSend = new AjaxUpdateShowConditions()
                .withPhrase("Новая фраза ПЗ")
                .withAutobudgetPriority(priority.transportValue());

        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withGroupPhrases(bannersRule.getGroupId().toString(),
                        new AjaxUpdateShowConditionsObjects().withAdded(phaseToSend))
                .withUlogin(ULOGIN);

        List<Phrase> actualPhrase =
                cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditionsAndGetPhases(request);

        assumeThat("получили ответ по одной фразе", actualPhrase, hasSize(1));

        Phrase expectedPhrase = new Phrase()
                .withPhrase(phaseToSend.getPhrase())
                .withPremium(null)
                .withGuarantee(null);

        assertThat("получили верный ответ по фразе", actualPhrase.get(0), beanDiffer(expectedPhrase)
                .useCompareStrategy(onlyExpectedFields()));
    }

    public void addPhrasesWithGroupCopy() {
        String lastPhrase = phraseBase + MAX_PHRASES_IN_GROUP;

        AjaxUpdateShowConditionsRequest request =
                buildRequestWithMaxAvailablePhrases(phraseBase, bannersRule.getGroupId())
                        .withCopyGroupIfOversized("1")
                        .withUlogin(ULOGIN);

        List<Phrase> phrasesInResponse =
                cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditionsAndGetPhases(request);

        assumeThat("получили ответ по всем фразам", phrasesInResponse, hasSize(MAX_PHRASES_IN_GROUP));

        List<Group> groups = cmdRule.cmdSteps().groupsSteps().getGroups(ULOGIN, bannersRule.getCampaignId());

        assumeThat("добавилась 2я группа", groups, hasSize(2));

        List<Phrase> phrasesInGroup1 = groups.get(0).getPhrases().stream()
                .sorted(Comparator.comparing(Phrase::getPhrase))
                .collect(Collectors.toList());

        List<Phrase> phrasesInGroup2 = groups.get(1).getPhrases();

        assumeThat("в первой группе максимальное количество фраз", phrasesInGroup1, hasSize(MAX_PHRASES_IN_GROUP));

        assumeThat("во второй группе 1на фраза", phrasesInGroup2, hasSize(1));

        List<String> actualPhrasesGroup1 = phrasesInGroup1.stream()
                .map(Phrase::getPhrase)
                .sorted(String::compareTo)
                .collect(Collectors.toList());

        String actualPhraseInGroup2 = phrasesInGroup2.get(0).getPhrase();


        List<String> expectedPhasesInGroup1 = request.getJsonPhrases().get(bannersRule.getGroupId().toString())
                .getAdded().stream()
                .map(AjaxUpdateShowConditions::getPhrase)
                .collect(Collectors.toList());

        expectedPhasesInGroup1.add(bannersRule.getGroup().getPhrases().get(0).getPhrase());
        expectedPhasesInGroup1.remove(lastPhrase);

        expectedPhasesInGroup1 = expectedPhasesInGroup1.stream()
                .sorted(String::compareTo)
                .collect(Collectors.toList());

        assertThat("в первой группе содержатся добавленные и старые фразы до лимита в " + MAX_PHRASES_IN_GROUP,
                actualPhrasesGroup1, beanDiffer(expectedPhasesInGroup1));

        assertThat("в новой созданной группе содержатся одна последняя фраза", actualPhraseInGroup2, equalTo(lastPhrase));

    }

    public void addOnePhraseTwice() {
        AjaxUpdateShowConditions phaseToSend = new AjaxUpdateShowConditions()
                .withPhrase("новая фраза ПЗ")
                .withPrice(PhrasesFactory.getDefaultPhrase().getPrice().toString())
                .withPriceContext(PhrasesFactory.getDefaultPhrase().getPriceContext().toString());

        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withGroupPhrases(bannersRule.getGroupId().toString(),
                        new AjaxUpdateShowConditionsObjects().withAdded(phaseToSend))
                .withUlogin(ULOGIN);

        List<Phrase> actualPhrases =
                cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditionsAndGetPhases(request);

        assumeThat("получили ответ по одной фразе", actualPhrases, hasSize(1));

        actualPhrases =
                cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditionsAndGetPhases(request);

        assumeThat("получили все так же ответ по одной фразе", actualPhrases, hasSize(1));

        List<String> actualGroupPhrases = cmdRule.cmdSteps().groupsSteps()
                .getPhrases(ULOGIN, bannersRule.getCampaignId(), bannersRule.getGroupId())
                .stream()
                .map(Phrase::getPhrase)
                .sorted()
                .collect(Collectors.toList());

        List<String> expectedPhrases = new ArrayList<>();
        expectedPhrases.add(bannersRule.getGroup().getPhrases().get(0).getPhrase());
        expectedPhrases.add(phaseToSend.getPhrase());
        expectedPhrases = expectedPhrases.stream()
                .sorted()
                .collect(Collectors.toList());

        assertThat("в группе присутствуют 2 фразы", actualGroupPhrases, beanDiffer(expectedPhrases));
    }


}
