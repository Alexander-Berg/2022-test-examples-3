package ru.yandex.autotests.direct.cmd.firstaid;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.direct.cmd.steps.firsthelp.FirstHelpHelper.prepareCampForOptimizinhAccept;
import static ru.yandex.autotests.direct.httpclient.data.textresources.firstAid.AcceptOptimizeErrors.SOMEONE_ELSE_CAMPAIGN;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Чужой клиент не может подвердить первую помощь")
@Stories(TestFeatures.FirstAid.ACCEPT_OPTIMIZE)
@Features(TestFeatures.FIRST_AID)
@Tag(CmdTag.ACCEPT_OPTIMIZE)
@Tag(CampTypeTag.TEXT)
public class AcceptOptimizeRoleTest {
    public static final String CLIENT = "at-direct-b-firstaid-c8";
    public static final String ANOTHER_CLIENT = Logins.DEFAULT_CLIENT2;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    private Long optimizeRequestId;

    @Before
    public void before() {
        optimizeRequestId = prepareCampForOptimizinhAccept(cmdRule,
                bannersRule.getCampaignId(),
                bannersRule.getGroupId(),
                CLIENT
        );
    }
    @Test
    @Description("Проверяем валидацию при параметре cid, принадлежащей другому клиенту")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10825")
    public void anotherClientCidValidationTest() {
        ErrorResponse errorResponse =
                cmdRule.cmdSteps().firstHelpSteps().acceptOptimizeWithError(
                        bannersRule.getCampaignId(),
                        bannersRule.getCurrentGroup().getPhrases().stream().map(Phrase::getId).collect(toList()),
                        optimizeRequestId,
                        ANOTHER_CLIENT
                );
        assertThat("ошибка соответсвует ожиданиям", errorResponse.getError(), equalTo(SOMEONE_ELSE_CAMPAIGN.toString()));
    }
}
