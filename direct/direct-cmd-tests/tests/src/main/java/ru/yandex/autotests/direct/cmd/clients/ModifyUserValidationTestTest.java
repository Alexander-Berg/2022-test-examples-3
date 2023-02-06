package ru.yandex.autotests.direct.cmd.clients;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.clients.ModifyUserModel;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.steps.modifyuser.ModifyUserSteps;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Валидация при сохранении настроек пользователя (контроллер modifyUser)")
@Stories(TestFeatures.Client.MODIFY_USER)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.MODIFY_USER)
@Tag(ObjectTag.USER)
public class ModifyUserValidationTestTest {

    private static final String TEMPLATE_2_NAME = "cmd.modifyUser.model.forModifyUserTest-2";

    private static final String CLIENT = "at-direct-backend-modifyuser-2";
    private static final String AGENCY_CLIENT = Logins.AGENCY;

    private static final String INVALID_PHONE = "1";
    private static final String INVALID_EMAIL = "ololo.yandex.ru";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    private ModifyUserModel modifyUserModel;

    @Test
    @Description("Невозможность отредактировать ФИО клиента, у которого более 1 типа обслуживания")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9573")
    public void testDenyFIOEditingAtAgencyClient() {
        final String client = AGENCY_CLIENT;
        String expectedFio = cmdRule.cmdSteps().modifyUserSteps().getModifyUser(client).getFio();

        modifyUserModel = BeanLoadHelper.loadCmdBean(TEMPLATE_2_NAME, ModifyUserModel.class);
        modifyUserModel.withFio(randomAlphabetic(10)).withUlogin(client);

        cmdRule.darkSideSteps().getClientFakeSteps().enableToCreateSelfCampaigns(client);

        cmdRule.cmdSteps().modifyUserSteps().postModifyUser(modifyUserModel);

        ModifyUserModel afterEditing = cmdRule.cmdSteps().modifyUserSteps().getModifyUser(client);
        String actualFio = afterEditing.getFio();

        assertThat("ФИО пользователя не изменилось после редактирования",
                actualFio, equalTo(expectedFio));
    }

    @Test
    @Description("Невозможность отредактировать клиента с пустым полем ФИО")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9574")
    public void testEmptyFIOValidation() {
        modifyUserModel = BeanLoadHelper.loadCmdBean(TEMPLATE_2_NAME, ModifyUserModel.class);
        modifyUserModel.withFio(null).withUlogin(CLIENT);

        ErrorResponse response = cmdRule.cmdSteps().modifyUserSteps().postModifyUserInvalidData(modifyUserModel);

        assertThat("Ответ содержит сообщение об ошибке",
                response.getError(), equalTo(ModifyUserSteps.ERR_EMPTY_NAME));
    }

    @Test
    @Description("Невозможность отредактировать клиента с неправильным номером телефона")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9575")
    public void testInvalidPhoneValidation() {
        modifyUserModel = BeanLoadHelper.loadCmdBean(TEMPLATE_2_NAME, ModifyUserModel.class);
        modifyUserModel.withPhone(INVALID_PHONE).withUlogin(CLIENT);

        ErrorResponse response = cmdRule.cmdSteps().modifyUserSteps().postModifyUserInvalidData(modifyUserModel);

        assertThat("Ответ содержит сообщение об ошибке",
                response.getError(), equalTo(ModifyUserSteps.ERR_INVALID_PHONE + INVALID_PHONE));
    }

    @Test
    @Description("Невозможность отредактировать клиента с неправильным email")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9576")
    public void testInvalidEmailValidation() {
        modifyUserModel = BeanLoadHelper.loadCmdBean(TEMPLATE_2_NAME, ModifyUserModel.class);
        modifyUserModel.withEmail(INVALID_EMAIL).withUlogin(CLIENT);

        ErrorResponse response = cmdRule.cmdSteps().modifyUserSteps().postModifyUserInvalidData(modifyUserModel);

        assertThat("Ответ содержит сообщение об ошибке",
                response.getError(), equalTo(ModifyUserSteps.ERR_INVALID_EMAIL + INVALID_EMAIL));
    }
}
