package ru.yandex.autotests.direct.httpclient.banners.editgroups.savetextadgroups;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Created by f1nal
 * on 03.07.15.
 * TESTIRT-6117
 */

@Issue("TESTIRT-6117")
@Aqua.Test
@Description("Позитивная проверка сохранения динамических баннеров через контроллер saveDynamicAdGroups")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(TrunkTag.YES)
public class DynamicBannersSaveDynamicAdGroupsSavePositiveTest extends DynamicBannersSaveDynamicAdGroupsPositiveBase {

    protected void editBanner(String beanName) {
        return;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10122")
    public void editDynamicBannerPositiveTest() {
        super.editDynamicBannerPositiveTest();
    }
}
