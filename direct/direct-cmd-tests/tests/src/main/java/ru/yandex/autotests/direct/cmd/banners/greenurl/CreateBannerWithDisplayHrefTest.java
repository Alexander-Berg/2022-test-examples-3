package ru.yandex.autotests.direct.cmd.banners.greenurl;

import java.util.Arrays;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Создание баннера с отображаемой ссылкой (saveTextAdGroups)")
@Stories(TestFeatures.Banners.DISPLAY_HREF)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class CreateBannerWithDisplayHrefTest extends DisplayHrefBaseTest {

    @Test
    @Description("Создание баннера с отображаемой ссылкой (saveTextAdGroups)")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9194")
    public void testCreateBannerWithDisplayHref() {
        assertThat("отображаемая ссылка сохранилась правильно", getDisplayHref(), equalTo(DISPLAY_HREF));
    }

    @Test
    @Description("Статус модерации отображаемой ссылки при создании баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9195")
    public void testCreateBannerWithDisplayHrefModeration() {
        assertThat("статус модерации отобр. ссылки при создании баннера соответствует ожидаемому",
                Arrays.asList(Status.READY, Status.SENDING, Status.SENT), hasItem(getDisplayHrefStatusModerate()));
    }

    @Override
    protected String getDisplayHrefToCreateBannerWith() {
        return DISPLAY_HREF;
    }

    @Override
    protected String getDisplayHrefToAddToCreatedBanner() {
        return null;
    }
}
