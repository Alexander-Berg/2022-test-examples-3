package ru.yandex.autotests.direct.cmd.groups.dynamic;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.SiteLink;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.editdynamicadgroups.EditDynamicAdGroupsResponse;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.List;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение сайтлинков в динамической группе")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(CmdTag.EDIT_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class SaveDtoSitelinksTest extends DtoBaseTest {

    private static final String SITELINKS_TAMPLATE_FULL = CmdBeans.COMMON_REQUEST_SITELINKS_FULL;

    private List<SiteLink> sitelinks;

    @Override
    protected Group getDynamicGroup() {
        Group savingGroup = super.getDynamicGroup();
        sitelinks = Arrays.asList(BeanLoadHelper.loadCmdBean(SITELINKS_TAMPLATE_FULL, SiteLink[].class));
        savingGroup.getBanners().get(0).withSiteLinks(sitelinks);
        return savingGroup;
    }

    @Test
    @Description("Сохранение сайтлинков в динамической группе")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9790")
    public void checkSaveSitelinksInDynamicGroup() {
        EditDynamicAdGroupsResponse createdGroup = getCreatedGroup();
        Group actualGroup = createdGroup.getCampaign().getGroups().get(0);
        Banner actualBanner = actualGroup.getBanners().get(0);
        List<SiteLink> actualSitelinks = actualBanner.getSiteLinks();

        CompareStrategy onlyExpected = DefaultCompareStrategies.onlyExpectedFields();
        assertThat("сохраненные сайтлинки соответствует ожидаемым",
                actualSitelinks, BeanDifferMatcher.beanDiffer(sitelinks).useCompareStrategy(onlyExpected));
    }
}
