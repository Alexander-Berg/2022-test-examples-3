package ru.yandex.autotests.direct.cmd.banners.additions.callouts.negative;

import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.banners.additions.callouts.CalloutsTestHelper;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.banners.additions.CalloutsErrorsTextResource;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Привязка невалидных текстовых дополнений к текстовым баннерам")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
public class SaveDynamicBannersCalloutsErrorsTest extends SaveBannersCalloutsErrorsTestBase {
    public SaveDynamicBannersCalloutsErrorsTest(String[] callouts, String errorText) {
        super(callouts, errorText);
    }

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {new String[]{RandomUtils.getString(CalloutsTestHelper.MAX_CALLOUT_LENGTH + 1)},
                        CalloutsErrorsTextResource.MAX_LENGTH_EXCEEDED.getErrorText()},
                {new String[]{"expectedCallout>>"}, CalloutsErrorsTextResource.INVALID_SYMBOLS_SHORT.getErrorText()},
                {new String[]{"callout1", "callout1"}, CalloutsErrorsTextResource.REPEATABLE_ELEMENTS.getErrorText()},
                {new String[]{""}, CalloutsErrorsTextResource.EMPTY_FIELD.getErrorText()},
                {new String[]{"callout1", RandomUtils.getString(CalloutsTestHelper.MAX_CALLOUT_LENGTH + 1)},
                        CalloutsErrorsTextResource.MAX_LENGTH_EXCEEDED.getErrorText()},
                {IntStream.range(0, CalloutsTestHelper.MAX_CALLOUTS_FOR_BANNER + 1)
                        .boxed()
                        .map(String::valueOf)
                        .toArray(String[]::new), CalloutsErrorsTextResource.ILLEGAL_CALLOUTS_NUMBER.getErrorText()}
        });
    }

    @Override
    protected CampaignTypeEnum getCampType() {
        return CampaignTypeEnum.DTO;
    }

    @Override
    protected String getUlogin() {
        return "at-direct-banners-callouts-7";
    }

    @Override
    protected String saveAndGetError(String... callouts) {
        GroupsParameters request = helper.getRequestForDynamic(helper.newDynamicGroupAndSet(callouts));

        return cmdRule.cmdSteps().groupsSteps()
                .postSaveDynamicAdGroupsInvalidData(request).getErrors().getGroupErrors().getArrayErrors().get(0)
                .getObjectErrors().getBannersErrors().getArrayErrors().get(0).getObjectErrors().getCallouts().get(0)
                .getText();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9118")
    public void errorOnSave() {
        super.errorOnSave();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9119")
    public void calloutsNotSaved() {
        super.calloutsNotSaved();
    }
}
