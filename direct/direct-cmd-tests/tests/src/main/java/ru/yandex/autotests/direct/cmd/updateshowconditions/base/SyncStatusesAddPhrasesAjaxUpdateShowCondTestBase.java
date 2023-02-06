package ru.yandex.autotests.direct.cmd.updateshowconditions.base;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.phrases.PhrasesFactory;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BidsStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.Bids;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BidsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.allure.annotations.Description;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

public abstract class SyncStatusesAddPhrasesAjaxUpdateShowCondTestBase {

    private static final String ULOGIN = "at-direct-add-plus-phrases";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = getBannerRule().withUlogin(ULOGIN);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private String phraseText = "Новая фраза ПЗ";

    protected abstract BannersRule getBannerRule();

    @Before
    public void before() {
        cmdRule.darkSideSteps().getGroupsFakeSteps().makeGroupFullyModerated(bannersRule.getGroupId());
        cmdRule.darkSideSteps().getBannerPhrasesFakeSteps().setPhraseStatusBsSyncedYes(bannersRule.getGroupId());
        for (Phrase phrase : bannersRule.getCurrentGroup().getPhrases()) {
            cmdRule.darkSideSteps().getBannerPhrasesFakeSteps().makeKeywordModerated(phrase.getId());
        }
        setPhraseAutobudgetPriority();

        AjaxUpdateShowConditions phaseToSend = new AjaxUpdateShowConditions()
                .withPhrase(phraseText)
                .withPrice(PhrasesFactory.getDefaultEngPhrase().getPrice().toString())
                .withPriceContext(PhrasesFactory.getDefaultEngPhrase().getPriceContext().toString());

        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withGroupPhrases(bannersRule.getGroupId().toString(),
                        new AjaxUpdateShowConditionsObjects().withAdded(phaseToSend))
                .withUlogin(ULOGIN);

        cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditionsAndGetPhases(request);
    }

    @Test
    @Description("Статусы синхронизации группы с БК и модерацией сбрасываются после добавления фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10628")
    public void groupStatusesAfterPhraseAdded() {
        Group actualGroup = bannersRule.getCurrentGroup();

        Group expectedGroup = new Group()
                .withStatusBsSynced(StatusBsSynced.NO.toString())
                .withStatusModerate(StatusModerate.READY.toString())
                .withStatusPostModerate(StatusModerate.NO.toString());

        assertThat("статусы группы соответствуют ожиданиям", actualGroup,
                beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Статусы синхронизации не изменились для существующей фразы в группе")
//    @Ignore("https://st.yandex-team.ru/DIRECT-61104")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10626")
    public void oldPhraseStatusesAfterPhraseAdded() {
        TestEnvironment.newDbSteps().useShardForLogin(ULOGIN);
        BidsRecord actualPhraseRecord = TestEnvironment.newDbSteps().bidsSteps().getBidsByCid(bannersRule.getCampaignId())
                .stream()
                .filter(p -> bannersRule.getGroup().getPhrases().get(0).getPhrase().equals(p.getPhrase()))
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("сохраненная фраза не найдена в базе данных"));

        Map<String, Object> actualPhrase = new HashMap<>();
        actualPhrase.put(Bids.BIDS.STATUSMODERATE.getName(), actualPhraseRecord.getStatusmoderate());

        Map<String, Object> expectedPhrase = new HashMap<>();
        expectedPhrase.put(Bids.BIDS.STATUSMODERATE.getName(), BidsStatusmoderate.Yes);

        assertThat("статусы исходной фразы соответствуют ожиданиям", actualPhrase,
                beanDiffer(expectedPhrase).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    @Description("Новая фраза создается несинхронизированной")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10627")
    public void newPhraseStatusesAfterPhraseAdded() {
        TestEnvironment.newDbSteps().useShardForLogin(ULOGIN);
        BidsRecord actualPhraseRecord = TestEnvironment.newDbSteps().bidsSteps().getBidsByCid(bannersRule.getCampaignId())
                .stream()
                .filter(p -> phraseText.equalsIgnoreCase(p.getPhrase()))
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("новая фраза не найдена в базе данных"));

        Map<String, Object> actualPhrase = new HashMap<>();
        actualPhrase.put(Bids.BIDS.STATUSMODERATE.getName(), actualPhraseRecord.getStatusmoderate());

        Map<String, Object> expectedPhrase = new HashMap<>();
        expectedPhrase.put(Bids.BIDS.STATUSMODERATE.getName(), BidsStatusmoderate.New);

        assertThat("статусы соответствуют ожиданиям", actualPhrase,
                beanDiffer(expectedPhrase).useCompareStrategy(onlyExpectedFields()));
    }

    /**
     * Устанавливаем первой добавленной фразе autobudgetPriority = 3,
     * чтобы после добавления новой фразы это значение не поменялось и статус не сбросился.
     * Это баг, который решили не чинить: https://st.yandex-team.ru/DIRECT-61104
     */
    private void setPhraseAutobudgetPriority() {
        TestEnvironment.newDbSteps().useShardForLogin(ULOGIN);
        BidsRecord phraseRecord = TestEnvironment.newDbSteps().bidsSteps().getBidsByCid(bannersRule.getCampaignId())
                .stream()
                .filter(p -> bannersRule.getGroup().getPhrases().get(0).getPhrase().equals(p.getPhrase()))
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("сохраненная фраза не найдена в базе данных"));
        phraseRecord.setAutobudgetpriority(3);
        TestEnvironment.newDbSteps().bidsSteps().updateBids(phraseRecord);
    }

}
