package ru.yandex.market.partner.mvc.controller.credit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.credit.CreditTemplateValidator;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тест на {@link CreditMinPriceLimitController}.
 *
 * @author serenitas
 */
public class CreditMinPriceLimitControllerTest extends FunctionalTest {

    private static final String URL = "/credits/minPriceLimit";

    @Autowired
    private EnvironmentService environmentService;

    @Test
    void testGet() {
        environmentService.setValue(CreditTemplateValidator.CREDITS_MIN_PRICE_LIMIT, "5000");
        ResponseEntity<String> entity = FunctionalTestHelper.get(baseUrl + URL);
        JsonTestUtil.assertEquals(entity, "5000");
    }

}
