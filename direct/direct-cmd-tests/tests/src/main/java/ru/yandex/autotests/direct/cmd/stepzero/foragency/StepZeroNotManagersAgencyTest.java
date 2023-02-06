package ru.yandex.autotests.direct.cmd.stepzero.foragency;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroErrorsResource;
import ru.yandex.autotests.direct.cmd.data.stepzero.StepZeroRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка невозможности вызова контроллера StepZero под менеджером для чужого агенства")
@Stories(TestFeatures.StepZero.STEP_ZERO_FOR_AGENCY)
@Features(TestFeatures.STEP_ZERO)
@Tag(CmdTag.STEP_ZERO)
public class StepZeroNotManagersAgencyTest {
    private static final String NOT_MANAGERS_AGENCY = "at-agency-banners";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(Logins.TRANSFER_MANAGER);


    private StepZeroRequest request;

    @Before
    public void before() {
        request = new StepZeroRequest()
                .withForAgency(NOT_MANAGERS_AGENCY);
    }

    @Test
    @Description("Проверка, что контроллер недоступен для чужого агенства")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10005")
    public void controllerUnavailabilityForNotManagersAgencyTest() {
        ErrorResponse response = cmdRule.cmdSteps().stepZeroSteps().getStepZero(request);
        assertThat("нет прав для выполнения операции", response.getError(),
                containsString(TextResourceFormatter.resource(StepZeroErrorsResource.NOT_YOUR_AGENCY).toString()));
    }

}
