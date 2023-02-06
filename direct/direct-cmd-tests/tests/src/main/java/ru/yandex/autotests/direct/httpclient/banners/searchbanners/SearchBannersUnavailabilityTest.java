package ru.yandex.autotests.direct.httpclient.banners.searchbanners;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.*;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.CommonErrorsResource;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.direct.httpclient.data.Logins.AGENCY;

/**
 * Created by shmykov on 16.06.15.
 * TESTIRT-5018
 */
@Aqua.Test
@Description("Проверка контроллера SearchBanners для ролей, не имеющих к нему доступ")
@Stories(TestFeatures.Banners.SEARCH_BANNERS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CmdTag.SEARCH_BANNERS)
@Tag(OldTag.YES)
@RunWith(Parameterized.class)
public class SearchBannersUnavailabilityTest extends SearchBannersTestBase {

    private String description;

    public SearchBannersUnavailabilityTest(String description, String userLogin) {
        super(userLogin);
        this.description = description;
    }

    @Parameterized.Parameters(name = "Роль: {0}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"Клиент", CLIENT_LOGIN}
        };
        return Arrays.asList(data);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10276")
    public void searchBannersAvailabilityTest() {
        AllureUtils.changeTestCaseTitle(description + ", проверка недоступности контроллера");
        response = cmdRule.oldSteps().searchBannersSteps().searchBanners(requestParams, csrfToken);
        cmdRule.oldSteps().commonSteps().checkDirectResponseErrorCMDText(response, CommonErrorsResource.NO_RIGHTS_FOR_OPERATION.toString());
    }
}
