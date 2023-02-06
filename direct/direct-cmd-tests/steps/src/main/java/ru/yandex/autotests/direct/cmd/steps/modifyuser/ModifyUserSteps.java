package ru.yandex.autotests.direct.cmd.steps.modifyuser;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.clients.ModifyUserModel;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

public class ModifyUserSteps extends DirectBackEndSteps {

    public static final String ERR_EMPTY_NAME = "поле Имя не заполнено";
    public static final String ERR_INVALID_PHONE = "неправильный формат номера телефона: ";
    public static final String ERR_INVALID_EMAIL = "неправильный формат адреса: ";

    @Step("POST cmd = modifyUser (сохранение настроек пользователя)")
    public void postModifyUser(ModifyUserModel request) {
       post(CMD.MODIFY_USER, request.withSave("yes"), Void.class);
    }

    @Step("POST cmd = modifyUser с невалидными данными (сохранение настроек пользователя)")
    public ErrorResponse postModifyUserInvalidData(ModifyUserModel request) {
       return post(CMD.MODIFY_USER, request.withSave("yes"), ErrorResponse.class);
    }

    @Step("GET cmd = modifyUser (получение настроек пользователя)")
    public ModifyUserModel getModifyUser(String login) {
        return get(CMD.MODIFY_USER, new BasicDirectRequest().withUlogin(login), ModifyUserModel.class);
    }

    @Step("POST cmd = modifyUser (сохранение настроек пользователя) с проверкой")
    public void postModifyUserWithVerification(ModifyUserModel request) {
        postModifyUser(request);
        ModifyUserModel expected = request.toResponse();
        ModifyUserModel actual = getModifyUser(request.getUlogin());
        assumeThat("изменили настройки пользователя",
                actual, beanDiffer(expected).useCompareStrategy(buildCompareStrategy()));
    }

    private CompareStrategy buildCompareStrategy() {
        return DefaultCompareStrategies.allFieldsExcept(
                newPath("clientID"),
                newPath("uid"),
                newPath("currency"),
                newPath("defaultFeedCountLimit"),
                newPath("defaultFeedMaxFileSize"),
                newPath("forceMulticurrencyTeaser"),
                newPath("modifyConvertAllowed"),
                newPath("noDisplayHrefs"));
    }
}
