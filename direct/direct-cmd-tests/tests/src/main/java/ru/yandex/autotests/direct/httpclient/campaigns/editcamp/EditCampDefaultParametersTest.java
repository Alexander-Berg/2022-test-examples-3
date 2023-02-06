package ru.yandex.autotests.direct.httpclient.campaigns.editcamp;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.campaigns.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.httpclient.util.PropertyLoader;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 14.04.15
 *         https://st.yandex-team.ru/TESTIRT-4958
 */

@Aqua.Test
@Description("Проверка параметров по умолчанию контроллера editCamp при создании новой кампании")
@Stories(TestFeatures.Campaigns.EDIT_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CampTypeTag.TEXT)
@Tag(CmdTag.EDIT_CAMP)
@Tag(OldTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class EditCampDefaultParametersTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private User client = User.get("at-direct-backend-c");
    private EditCampResponse expectedEditCampResponse;

    @Before
    public void before() {

        cmdRule.oldSteps().onPassport().authoriseAs(client.getLogin(), client.getPassword());
        PropertyLoader<EditCampResponse> loader = new PropertyLoader<>(EditCampResponse.class);
        expectedEditCampResponse = loader.getHttpBean("defaultEditCampResponceForNewCamp");

    }

    @Test
    @Description("Проверяем параметры по умолчанию для новой кампании в ответе контроллера editCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10329")
    public void editCampNewCampTest() {
        EditCampResponse editCampResponse =
                cmdRule.oldSteps().onEditCamp().getEditCampResponseForNewCamp(client.getLogin());
        assertThat("Ответ контроллера совпадает с ожидаемым", editCampResponse,
                beanEquivalent(expectedEditCampResponse));
    }
}
