package ru.yandex.market.hrms.e2etests.login;

import io.qameta.allure.Description;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.hrms.e2etests.AbstractSelenuimTest;

@DisplayName("HRMS e2e Tests")
public class LoginTest extends AbstractSelenuimTest {

    @Test
    @DisplayName("Тест логина")
    @Description("""
            - Логинимся в паспорт
            - Заходим в hrms
            - Проверяем, что мы залогинены""")
    public void LoginToPassportAndCheckThatHRMSOpensTest() {
        hrmsUi.checkLoginWasSuccessfull();
    }
}
