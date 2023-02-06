package ru.yandex.autotests.direct.cmd.groups.dynamic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Cохранение параметров ссылки дто")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@RunWith(Parameterized.class)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(CmdTag.EDIT_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
@Tag(TrunkTag.YES)
public class SaveDtoBannerDomainParamsTest extends DtoBaseTest {
    private static final String RANDOM_PARAM = randomAlphabetic(1024);

    @Parameterized.Parameter(0)
    public String hrefParams;

    @Parameterized.Parameter(1)
    public String expectedHrefParams;

    @Parameterized.Parameters(name = "Параметры : {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"utm_source=yandex-direct&term={region_name}", "utm_source=yandex-direct&term={region_name}"},
                {"", null},
                {"123", "123"},
                {"?type={source_type}&source={source}&added={addphrases}& block={position_type}&pos={position}" +
                        "&key={keyword}&campaign={campaign_id}& retargeting={retargeting_id}&ad={ad_id}" +
                        "&phrase={phrase_id}&gbid={gbid}& device={device_type}&region={region_id}" +
                        "&region_name={region_name}",
                        "type={source_type}&source={source}&added={addphrases}& block={position_type}&pos={position}" +
                                "&key={keyword}&campaign={campaign_id}& retargeting={retargeting_id}&ad={ad_id}" +
                                "&phrase={phrase_id}&gbid={gbid}& device={device_type}&region={region_id}" +
                                "&region_name={region_name}"},
                {RANDOM_PARAM, RANDOM_PARAM},
        });
    }

    @Test
    @Description("Cохранение параметров ссылки дто")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9787")
    public void checkHrefParams() {
        Group actualGroup = getCreatedGroup().getCampaign().getGroups().get(0);
        assertThat("параметры ссылки соотвествуют ожидаемым", actualGroup.getHrefParams(), equalTo(expectedHrefParams));
    }

    @Override
    protected Group getDynamicGroup() {
        Group group = super.getDynamicGroup();
        group.setHrefParams(hrefParams);
        return group;
    }
}
