package ru.yandex.autotests.direct.cmd.banners.additions.callouts.negative;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.banners.additions.callouts.CalloutsTestHelper;
import ru.yandex.autotests.direct.cmd.data.banners.additions.Additions;
import ru.yandex.autotests.direct.cmd.data.banners.additions.BannersAdditionsErrorsResponse;
import ru.yandex.autotests.direct.cmd.data.banners.additions.CalloutsErrorsTextResource;
import ru.yandex.autotests.direct.cmd.data.banners.additions.DeleteBannersAdditionsRequest;
import ru.yandex.autotests.direct.cmd.data.banners.additions.SaveBannersAdditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Callout;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Ошибки при удалении дополнений")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.DELETE_BANNERS_ADDITIONS)
public class DeleteClientCalloutsErrorsTest {

    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();

    public String ulogin = "at-direct-banners-callouts-10";

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private CalloutsTestHelper helper;

    private Callout savedCallout;

    private String callout = "callout1";

    @Before
    public void setUp() {
        helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), "");
        helper.clearCalloutsForClient();
        List<Callout> callouts = cmdRule.cmdSteps().bannersAdditionsSteps()
                .saveBannersAdditions(
                        SaveBannersAdditionsRequest.defaultCalloutsRequest(ulogin, callout)
                ).getCallouts();
        assumeThat("Уточнение сохранилось", callouts, hasSize(1));
        savedCallout = callouts.get(0);
    }


    @Test
    @Description("Нельзя удалить уточнение по тексту")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9110")
    public void deleteСalloutByOnlyText() {
        DeleteBannersAdditionsRequest request = new DeleteBannersAdditionsRequest()
                .withAdditions(new Additions().withCallouts(
                        (Callout) new Callout()
                                .withCalloutText(savedCallout.getCalloutText())));

        BannersAdditionsErrorsResponse error =
                cmdRule.cmdSteps().bannersAdditionsSteps().deleteClientCalloutsError(request);

        assertThat("Получили ошибку", error.getError(),
                equalTo(CalloutsErrorsTextResource.ERROR_EMPTY_CALLOUT_ID.getErrorText()));
    }

    @Test
    @Description("Удаление невалидного id дополнения одновременно с валидным не должно ничего удалить")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9111")
    public void deleteCalloutByInvalidIdShouldNotDeleteCorrectCallouts() {
        DeleteBannersAdditionsRequest request = new DeleteBannersAdditionsRequest()
                .withAdditions(new Additions().withCallouts(
                        (Callout) new Callout()
                                .withAdditionsItemId(savedCallout.getAdditionsItemId()),
                        (Callout) new Callout()
                                .withAdditionsItemId(542345264L)
                ))
                .withUlogin(ulogin);

        cmdRule.cmdSteps().bannersAdditionsSteps().deleteClientCalloutsError(request);

        List<Callout> actualCallouts = cmdRule.cmdSteps()
                .bannersAdditionsSteps().getCallouts(ulogin).getCallouts();

        assertThat("Дополнение не удалилось в случае ошибки", actualCallouts, hasSize(1));
    }

    @Test
    @Description("Удаление невалидного id дополнения должно приводить к ошибке")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9112")
    public void deleteCalloutByInvalidIdError() {
        Long invalidId = 542345264L;
        DeleteBannersAdditionsRequest request = new DeleteBannersAdditionsRequest()
                .withAdditions(new Additions().withCallouts(
                        (Callout) new Callout()
                                .withAdditionsItemId(savedCallout.getAdditionsItemId()),
                        (Callout) new Callout()
                                .withAdditionsItemId(invalidId)
                ))
                .withUlogin(ulogin);

        BannersAdditionsErrorsResponse response =
                cmdRule.cmdSteps().bannersAdditionsSteps().deleteClientCalloutsError(request);
        assumeThat("Результат выполнения удаления не успешный", response.getSuccess(), equalTo("0"));

        assumeThat("Получили ошибки по 2м дополнениям", response.getCallouts().getCalloutsErrors(), hasSize(2));

        assertThat("Получили ошибку для 2го дополнения",
                response.getCallouts().getCalloutsErrors().get(1).get(0).getDescription(),
                equalTo(CalloutsErrorsTextResource.ERROR_INVALID_CALLOUT_ID.getErrorText() + ": " + invalidId)
        );

    }

}
