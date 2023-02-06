package ru.yandex.autotests.innerpochta;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.utils.rules.AccountRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.AskValidationTests.DONE;
import static ru.yandex.autotests.innerpochta.GetQueriesForUserNotInBase.*;
import static ru.yandex.autotests.innerpochta.NegativeCases.REQUEST_ID;
import static ru.yandex.autotests.innerpochta.utils.oper.GetAll.getAll;
import static ru.yandex.autotests.innerpochta.utils.oper.Remove.remove;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateParams.updateOneParamsSetting;
import static ru.yandex.autotests.innerpochta.utils.oper.UpdateProfile.updateProfile;
import static ru.yandex.autotests.innerpochta.utils.SettingsApiObj.settings;

/**
 * Created with IntelliJ IDEA.
 * User: angrybird
 * Date: 9.12.15
 * Time: 16:27
 */
@Aqua.Test
@Title("Ручки update для юзера без данных в базе")
@Description("Ручки update ищут юзера в базе, если его нет, сначала инитят дефолтно, затем апдейтят")
@Features("Общее")
@Stories("Update настроек")
public class UpdateQueriesForUserNotInBase {

    public static final String CUSTOM_SKIN_NAME = "neo";
    public static final String CUSTOM_MESSAGES_AVATARS = "off";

    @ClassRule
    public static AccountRule accInfo = new AccountRule().with(TskvTest.class);

    @Rule
    public TestRule chain = RuleChain.outerRule(new LogConfigRule());
    private DefaultHttpClient httpClient = new DefaultHttpClient();


    @Test
    @Title("update_params init and then update user without data in database")
    public void updateParamsInitUserWithoutDataInDatabase() {
        remove(settings(accInfo.uid())).post().via(httpClient);

        getAll(settings(accInfo.uid())).get().via(httpClient)
                .statusCodeShouldBe(OK_200)
                .assertResponse("should return default params before update",
                        allOf(containsString(DEF_MESSAGES_AVATARS),
                                not(containsString(REQUEST_ID))));

        updateOneParamsSetting(accInfo.uid(), messagesAvatarsParam, "off").post().via(httpClient)
                .statusCodeShouldBe(OK_200)
                .assertResponse("should update default params without errors",
                        allOf(containsString(DONE),
                                not(containsString(REQUEST_ID))));

        getAll(settings(accInfo.uid())).get().via(httpClient)
                .statusCodeShouldBe(OK_200)
                .assertResponse("should custom param after update",
                        allOf(containsString(CUSTOM_MESSAGES_AVATARS),
                                not(containsString(REQUEST_ID))));
    }

    @Test
    @Title("update_profile init and then update user without data in database")
    public void updateProfileInitUserWithoutDataInDatabase() {
        remove(settings(accInfo.uid())).post().via(httpClient);

        getAll(settings(accInfo.uid())).get().via(httpClient)
                .statusCodeShouldBe(OK_200)
                .assertResponse("should return default params before update",
                        allOf(containsString(DEF_SKIN_NAME),
                                not(containsString(REQUEST_ID))));

        updateProfile(settings(accInfo.uid()).set(true, skinNameParam, CUSTOM_SKIN_NAME)).post().via(httpClient)
                .statusCodeShouldBe(OK_200)
                .assertResponse("should update default params without errors",
                        allOf(containsString(DONE),
                                not(containsString(REQUEST_ID))));

        getAll(settings(accInfo.uid())).get().via(httpClient)
                .statusCodeShouldBe(OK_200)
                .assertResponse("should custom param after update",
                        allOf(containsString(CUSTOM_SKIN_NAME),
                                not(containsString(REQUEST_ID))));
    }
}
