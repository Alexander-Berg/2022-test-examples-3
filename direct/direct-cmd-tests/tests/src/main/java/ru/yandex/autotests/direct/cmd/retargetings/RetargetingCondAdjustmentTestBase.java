package ru.yandex.autotests.direct.cmd.retargetings;

import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public abstract class RetargetingCondAdjustmentTestBase {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    protected TextBannersRule bannersRule = new TextBannersRule().withUlogin(getClient());
    protected Long retargetingId;

    protected abstract String getClient();

    @Description("Проверяем получение связанных с ретаргетингом кампаний")
    public void checkGetUsedCampaignsTest() {
        RetargetingCondition response = cmdRule.cmdSteps().retargetingSteps()
                .getShowRetargetingCond(getClient()).get(retargetingId);
        assumeThat("кампания есть в списке", response.getCampaigns(), Matchers.hasSize(1));
        assertThat("кампания соответствует ожиданию", response.getCampaigns().get(0).getCid(),
                equalTo(bannersRule.getCampaignId().toString()));
    }
}
