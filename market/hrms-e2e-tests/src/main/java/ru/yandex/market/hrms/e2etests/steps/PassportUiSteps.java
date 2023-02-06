package ru.yandex.market.hrms.e2etests.steps;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;

import ru.yandex.market.hrms.e2etests.pageobjects.passport.AuthPage;
import ru.yandex.market.hrms.e2etests.pageobjects.passport.ProfilePage;

import static com.codeborne.selenide.Selenide.open;

public class PassportUiSteps {
    @Property("hrms.user")
    private String user = "robot-market-hrms-at";

    @Property("hrms.password")
    private String password;

    public PassportUiSteps() {
        PropertyLoader.newInstance().populate(this);

        //Если пароль не задан в jvm-args, читаем его из переменных окружения
        if(password == null) {
            password = System.getenv("HRMSPassword");
        }

        if (password == null) {
            throw new IllegalStateException("Password is empty");
        }
    }

    @Step("Логинимся в паспорте")
    public ProfilePage login() {
        return Allure.step("Открываем страницу паспорта и логинимся", () ->
                open("https://passport.yandex-team.ru/", AuthPage.class)
                        .SendKeysLogin(user)
                        .SendKeysPassword(password)
                        .ClickSignInButton()
        );
    }
}
