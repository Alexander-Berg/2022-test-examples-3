package ru.yandex.autotests.direct.cmd.conditions;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;

@Aqua.Test
@Description("Проверка статусов при добавлении первого условия (фраза)")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@Tag("TESTIRT-8612")
public class AddFirstPhraseToGroupTest extends AddFirstConditionToGroupTestBase {

    private Phrase expectedPhrase;

    @Override
    public BannersRule getBannersRule() {
        return new TextBannersRule().overrideGroupTemplate(new Group().withPhrases(emptyList()));
    }

    @Before
    public void before() {
        assumeThat("у группы нет условий", bannersRule.getCurrentGroup(),
                beanDiffer(new Group().withPhrases(emptyList()).withRetargetings(emptyList()))
                        .useCompareStrategy(onlyFields(newPath("phrases"), newPath("retargetings"))));
        expectedPhrase = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_PHRASE_DEFAULT2, Phrase.class)
                .withIsSuspended(isSuspended);
        expectedGroup = bannersRule.getCurrentGroup();
        expectedGroup.withPhrases(singletonList(expectedPhrase));
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(expectedGroup, CampaignTypeEnum.TEXT);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10863")
    public void addFistPhraseToDraftGroupTest() {
        addFistConditionToDraftGroupTest();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10862")
    public void addFistPhraseToActiveGroupTest() {
        addFistConditionToActiveGroupTest();
    }

    @Override
    protected Group getExpectedGroupDraft() {
        return super.getExpectedGroupDraft()
                .withPhrases(singletonList(new Phrase()
                        .withPhrase(expectedPhrase.getPhrase())
                        .withGuarantee(null)
                        .withPremium(null)
                        .withIsSuspended(isSuspended)
                        .withStatusModerate(StatusModerate.NEW.toString())
                ));
    }

    @Override
    protected Group getExpectedGroupActive() {
        return super.getExpectedGroupActive()
                .withStatusModerate(StatusModerate.READY.toString())
                .withPhrases(singletonList(new Phrase()
                        .withPhrase(expectedPhrase.getPhrase())
                        .withGuarantee(null)
                        .withPremium(null)
                        .withIsSuspended(isSuspended)
                        .withStatusModerate(StatusModerate.NEW.toString())
                ));
    }
}
