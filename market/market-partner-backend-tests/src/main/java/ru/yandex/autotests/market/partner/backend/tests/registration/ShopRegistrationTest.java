package ru.yandex.autotests.market.partner.backend.tests.registration;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.partner.backend.beans.registration.ShopRegistrationResponse;
import ru.yandex.autotests.market.partner.backend.steps.registration.ShopsRegistrationSteps;
import ru.yandex.autotests.market.partner.backend.util.query.registration.ShopRegistrationRequest;
import ru.yandex.autotests.market.partner.backend.util.query.registration.ShopRegistrationUtils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Элементарный тест, который проверяет регистрацию магазина.
 * Нужен из-за того, что функциональные тесты в основном репозитории не могут проверить
 * часть запросов в БД из-за ограничений H2 и эти запросы мокаются.
 * Один раз получили проблему с этими запросами в проде, данный тест в будущем будет такие проблемы отлавливать.
 *
 * @author Kirill Lakhtin (klaktin@yandex-team.ru)
 */
@Aqua.Test(title = "Тесты для ручки POST /register-shop")
@Features("POST /register-shop")
@Issue("https://st.yandex-team.ru/MBI-25958")
public class ShopRegistrationTest {
    private static final ShopsRegistrationSteps steps = new ShopsRegistrationSteps();

    /**
     * Выполнить запрос на создание магазина, убедиться, что он завершился статусом 200.
     */
    @Test
    public void testRegistration() {
        long uid = steps.registerNewUser();
        ShopRegistrationRequest request = ShopRegistrationUtils.createTestShopRequest(uid);
        ShopRegistrationResponse response = steps.sendShopRegistrationRequest(request);
        assertThat(response.getResult().getOwnerId()).isEqualTo(uid);
    }

}
