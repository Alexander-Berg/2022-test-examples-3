package ru.yandex.autotests.directintapi.tests.runppccampautoprice;

import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.steps.retargeting.RetargetingHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BidsRetargetingRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.darkside.tags.StageTag;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.cmd.data.interest.TargetInterestsFactory.defaultTargetInterest;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Tag(StageTag.RELEASE)
@Features(FeatureNames.PPC_CAMP_AUTO_PRICE)
@Description("Вызов скрипта PpcCampAutoPrice.pm для интереса")
public class RunPpcCampAutoPriceIgnoreInterestTest {
    private static final String CLIENT = "at-direct-interests";
    private final String expectedPriceContext;

    private int shard;
    private String clientId;

    @ClassRule
    public static DirectCmdRule stepsClassRule = DirectCmdRule.defaultClassRule();

    private MobileBannersRule bannersRule;
    @Rule
    public DirectCmdRule cmdRule;
    private Long categoryId;
    private Long retId;

    public RunPpcCampAutoPriceIgnoreInterestTest() {
        expectedPriceContext = "0.88";
        categoryId = RetargetingHelper.createTargetCategory(CLIENT, null).getCategoryId();
        bannersRule = MobileBannersRule.mobileBannersRuleNewType()
                .overrideGroupTemplate(new Group().withTargetInterests(
                        Collections.singletonList(
                                defaultTargetInterest(categoryId).withPriceContext(Double.valueOf(expectedPriceContext))
                        )
                )).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        retId = TestEnvironment.newDbSteps().bidsSteps()
                .getBidsRetargetingRecordByPid(bannersRule.getGroupId()).get(0).getRetId();
    }


    @Test
    public void runPpcCampAutoPrice() {
        cmdRule.apiSteps().getDarkSideSteps().getRunScriptSteps().runPpcCampAutoPrice(
                TestEnvironment.newDbSteps().getCurrentPpcShard(),
                Math.toIntExact(bannersRule.getCampaignId())
        );
        List<BidsRetargetingRecord> bidsRetargeting = TestEnvironment.newDbSteps().bidsSteps()
                .getBidsRetargetingRecordByPid(bannersRule.getGroupId());

        assertThat("Изменения сохранились", bidsRetargeting.get(0).getPriceContext().toString(), equalTo(expectedPriceContext));

    }

    @After
    public void after() {
        if (categoryId != null) {
            TestEnvironment.newDbSteps().interestSteps()
                    .deleteTargetingCategoriesRecords(categoryId);
        }
    }

}
