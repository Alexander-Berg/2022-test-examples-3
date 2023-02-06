package ru.yandex.autotests.direct.cmd.banners.additions.callouts.negative;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.banners.additions.callouts.CalloutsTestHelper;
import ru.yandex.autotests.direct.cmd.data.banners.additions.BannersAdditionsErrorsResponse;
import ru.yandex.autotests.direct.cmd.data.banners.additions.CalloutsErrorsTextResource;
import ru.yandex.autotests.direct.cmd.data.banners.additions.SaveBannersAdditionsRequest;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorData;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Ошибки при сохранении дополнений")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_BANNERS_ADDITIONS)
public class SaveClientCalloutsErrorsTest {

    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();

    public String ulogin = "at-direct-banners-callouts-10";

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private CalloutsTestHelper helper;

    @Before
    public void setUp() {
        helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), "");
        helper.clearCalloutsForClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9113")
    public void calloutExceedsMaxLength() {
        String calloutExceedsMaxLength = RandomUtils.getString(CalloutsTestHelper.MAX_CALLOUT_LENGTH + 1);
        BannersAdditionsErrorsResponse resp = cmdRule.cmdSteps().bannersAdditionsSteps()
                .saveBannersAdditionsError(
                        SaveBannersAdditionsRequest.defaultCalloutsRequest(ulogin, calloutExceedsMaxLength)
                );

        assertThat("Получили ошибку", getFirstErrorText(resp),
                containsString(CalloutsErrorsTextResource.MAX_LENGTH_EXCEEDED.getErrorText()));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9114")
    public void calloutWithInvalidSymbols() {
        String calloutWithInvalidSymbols = "expectedCallout>>";
        BannersAdditionsErrorsResponse resp = cmdRule.cmdSteps().bannersAdditionsSteps()
                .saveBannersAdditionsError(
                        SaveBannersAdditionsRequest.defaultCalloutsRequest(ulogin, calloutWithInvalidSymbols)
                );

        assertThat("Получили ошибку", getFirstErrorText(resp),
                equalTo(CalloutsErrorsTextResource.INVALID_SYMBOLS.getErrorText()));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9115")
    public void emptyCallout() {
        String callout = "";
        BannersAdditionsErrorsResponse resp = cmdRule.cmdSteps().bannersAdditionsSteps()
                .saveBannersAdditionsError(
                        SaveBannersAdditionsRequest.defaultCalloutsRequest(ulogin, callout)
                );

        assertThat("Получили ошибку", getFirstErrorText(resp),
                equalTo(CalloutsErrorsTextResource.EMPTY_TEXT.getErrorText()));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9116")
    public void saveDuplicateCallouts() {
        String text = "expectedCallout";
        BannersAdditionsErrorsResponse resp = cmdRule.cmdSteps().bannersAdditionsSteps()
                .saveBannersAdditionsError(SaveBannersAdditionsRequest.defaultCalloutsRequest(ulogin, text, text));
        assertThat("Получили ошибку", getFirstErrorText(resp),
                equalTo(CalloutsErrorsTextResource.CALLOUTS_MUST_BE_UNIQUE.getErrorText()));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9117")
    public void tryToSaveMoreThanMaxCallouts() {
        String[] callouts = IntStream.range(0, CalloutsTestHelper.MAX_CALLOUTS_FOR_CLIENT + 1)
                .boxed()
                .map(String::valueOf)
                .toArray(String[]::new);

        BannersAdditionsErrorsResponse resp = cmdRule.cmdSteps().bannersAdditionsSteps()
                .saveBannersAdditionsError(SaveBannersAdditionsRequest.defaultCalloutsRequest(ulogin, callouts));

        String error = getFirstErrorText(resp);

        assertThat("Получили ошибку", error, equalTo(CalloutsErrorsTextResource.NUMBER_MORE_THAN_MAX.getErrorText()));
    }

    private String getFirstErrorText(BannersAdditionsErrorsResponse resp) {
        List<List<ErrorData>> errors = Optional.ofNullable(resp.getCallouts())
                .orElseThrow(() -> new DirectCmdStepsException("В ответе отсутствуют текстовые дополнения"))
                .getCalloutsErrors();
        DirectCmdStepsException ex = new DirectCmdStepsException("В ответе отсутствуют ошибки по дополнениям");

        return Optional.ofNullable(errors)
                .orElseThrow(() -> ex)
                .stream()
                .filter(e -> e != null)
                .findFirst()
                .orElseThrow(() -> ex)
                .stream()
                .findFirst()
                .orElseThrow(() -> ex)
                .getDescription();
    }

}
