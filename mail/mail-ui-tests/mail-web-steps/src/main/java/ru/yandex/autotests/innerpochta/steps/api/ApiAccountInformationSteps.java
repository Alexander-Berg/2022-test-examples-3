package ru.yandex.autotests.innerpochta.steps.api;

import com.google.gson.Gson;
import io.restassured.mapper.ObjectMapperType;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.account.AccountInformation;
import ru.yandex.autotests.innerpochta.steps.beans.account.Email;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.accountInformationHandler;

/**
 * @author mabelpines
 */
public class ApiAccountInformationSteps {

    public RestAssuredAuthRule auth;
    Gson gson = new Gson();

    public ApiAccountInformationSteps withAuth(RestAssuredAuthRule auth) {
        this.auth = auth;
        return this;
    }

    @Step("Вызов api-метода: account-information. Получаем алиасы пользователя")
    public List<String> getUserAliases() {
        List<String> userAliases = new ArrayList<>();
        AccountInformation accInfo = accountInformationHandler().withAuth(auth).callAccountInformation()
            .as(AccountInformation.class, ObjectMapperType.GSON);
        for(Email email : accInfo.getEmail()){
            userAliases.add(buildEmail(email));
        }
        return userAliases;
    }

    @Step("Вызов api-метода: account-information. Получаем uid пользователя")
    public String getUserUid() {
        return accountInformationHandler().withAuth(auth).callAccountInformation()
            .as(AccountInformation.class, ObjectMapperType.GSON).getUid();
    }

    public String buildEmail(Email email){
        return new StringBuilder(email.getLogin())
                .append("@").append(email.getDomain()).toString();
    }
}
