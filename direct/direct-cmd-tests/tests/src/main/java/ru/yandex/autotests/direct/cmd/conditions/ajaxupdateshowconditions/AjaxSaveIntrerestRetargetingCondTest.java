package ru.yandex.autotests.direct.cmd.conditions.ajaxupdateshowconditions;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.interest.TargetInterests;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.retargeting.RetargetingHelper;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BidsRetargetingRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка удаления/остановки интереса через ajaxUpdateShowConditions")
@Stories(TestFeatures.Conditions.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.CONDITIONS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.MOBILE)
public class AjaxSaveIntrerestRetargetingCondTest {
    private static final String CLIENT = Logins.CLIENT_WITH_INTERESTS;

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    private MobileBannersRule bannersRule;
    @Rule
    public DirectCmdRule cmdRule;

    private Long categoryId;
    private Long retId;

    public AjaxSaveIntrerestRetargetingCondTest() {
        categoryId = RetargetingHelper.getRandomTargetCategoryId();
        bannersRule = new MobileBannersRule()
                .overrideGroupTemplate(new Group().withTargetInterests(
                        Collections.singletonList(
                                new TargetInterests()
                                        .withTargetCategoryId(categoryId)
                                        .withRetId(0)
                                        .withPriceContext(0.78d)
                                        .withAutobudgetPriority(3))
                        )
                )
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        retId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bidsSteps()
                .getBidsRetargetingRecordByPid(bannersRule.getGroupId()).get(0).getRetId();

    }

    @Test
    @Description("Создание условия ретаргетинга (cmd = ajaxSaveRetargetingCond)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10648")
    public void testAddAtAjaxSaveRetargetingCond() {
        String expectedPriceContext = "0.88";
        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withCid(String.valueOf(bannersRule.getCampaignId()))
                .withInterest(String.valueOf(bannersRule.getGroupId()), new AjaxUpdateShowConditionsObjects()
                        .withEdited(
                                String.valueOf(retId),
                                new AjaxUpdateShowConditions().withPriceContext(expectedPriceContext)
                        ))
                .withUlogin(CLIENT);
        ErrorResponse response = cmdRule.cmdSteps().ajaxUpdateShowConditionsSteps().postAjaxUpdateShowConditions(request);
        assumeThat("нет ошибок при сохранении условия", response.getError(), nullValue());


        List<BidsRetargetingRecord> bidsRetargeting = TestEnvironment.newDbSteps().bidsSteps()
                .getBidsRetargetingRecordByPid(bannersRule.getGroupId());

        assertThat("Изменения сохранились", bidsRetargeting.get(0).getPriceContext().toString(), equalTo(expectedPriceContext));
    }
}
