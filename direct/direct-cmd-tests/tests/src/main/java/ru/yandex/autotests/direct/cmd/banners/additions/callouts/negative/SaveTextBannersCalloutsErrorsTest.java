package ru.yandex.autotests.direct.cmd.banners.additions.callouts.negative;

import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.banners.additions.callouts.CalloutsTestHelper;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.CalloutsErrorEnum;
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
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
public class SaveTextBannersCalloutsErrorsTest extends SaveBannersCalloutsErrorsTestBase {
    public SaveTextBannersCalloutsErrorsTest(String[] callouts, String errorText) {
        super(callouts, errorText);
    }

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {new String[]{RandomUtils.getString(CalloutsTestHelper.MAX_CALLOUT_LENGTH + 1)},
                        CalloutsErrorEnum.TOO_LONG_CALLOUT.toString()},
                {new String[]{"expectedCallout>>"}, CalloutsErrorEnum.WRONG_SYMBOLS.toString()},
                {new String[]{"callout1", "callout1"}, CalloutsErrorEnum.UNIQUE_CALLOUTS.toString()},
                {new String[]{""}, CalloutsErrorEnum.TEXT_NOT_FOUND.toString()},
                {new String[]{"callout1", RandomUtils.getString(CalloutsTestHelper.MAX_CALLOUT_LENGTH + 1)},
                        CalloutsErrorEnum.TOO_LONG_CALLOUT.toString()},
                {IntStream.range(0, CalloutsTestHelper.MAX_CALLOUTS_FOR_BANNER + 1)
                        .boxed()
                        .map(String::valueOf)
                        .toArray(String[]::new), CalloutsErrorEnum.MAX_CALLOUTS.toString()}
        });
    }

    @Override
    protected CampaignTypeEnum getCampType() {
        return CampaignTypeEnum.TEXT;
    }

    @Override
    protected String getUlogin() {
        return "at-direct-banners-callouts-7";
    }

    @Override
    protected String saveAndGetError(String... callouts) {
        GroupsParameters request = helper.getRequestFor(helper.newGroupAndSet(callouts));

        return cmdRule.cmdSteps().groupsSteps()
                .postSaveTextAdGroupsInvalidData(request).getError();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9120")
    public void errorOnSave() {
        super.errorOnSave();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9121")
    public void calloutsNotSaved() {
        super.calloutsNotSaved();
    }


}
