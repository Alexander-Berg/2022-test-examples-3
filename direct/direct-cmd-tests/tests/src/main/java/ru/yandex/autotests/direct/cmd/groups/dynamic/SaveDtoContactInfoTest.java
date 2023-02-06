package ru.yandex.autotests.direct.cmd.groups.dynamic;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.data.commons.banner.OrgDetails;
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

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение визитки в динамической группе")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(CmdTag.EDIT_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class SaveDtoContactInfoTest extends DtoBaseTest {

    private static final String VCARD_TAMPLATE_FULL = CmdBeans.COMMON_REQUEST_VCARD_FULL;

    private ContactInfo contactInfo;

    @Override
    protected Group getDynamicGroup() {
        Group savingGroup = super.getDynamicGroup();
        contactInfo = BeanLoadHelper.loadCmdBean(VCARD_TAMPLATE_FULL, ContactInfo.class);
        savingGroup.getBanners().get(0).withContactInfo(contactInfo);
        return savingGroup;
    }

    @Test
    @Description("Сохранение визитки в динамической группе")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9788")
    public void checkSaveContactInfoInDynamicGroup() {
        EditDynamicAdGroupsResponse createdGroup = getCreatedGroup();
        Group actualGroup = createdGroup.getCampaign().getGroups().get(0);
        Banner actualBanner = actualGroup.getBanners().get(0);
        ContactInfo actualContactInfo = actualBanner.getContactInfo();
        contactInfo.withAutoBound(actualContactInfo.getAutoBound());

        // переводим в формат ответа
        contactInfo.withOrgDetails(new OrgDetails(null, contactInfo.getOGRN()));
        contactInfo.withOGRN(null);
        contactInfo.withAutoPoint(null);

        CompareStrategy onlyExpected = DefaultCompareStrategies.onlyExpectedFields();
        assertThat("сохраненная визитка соответствует ожидаемой",
                actualContactInfo, BeanDifferMatcher.beanDiffer(contactInfo).useCompareStrategy(onlyExpected));
    }
}
