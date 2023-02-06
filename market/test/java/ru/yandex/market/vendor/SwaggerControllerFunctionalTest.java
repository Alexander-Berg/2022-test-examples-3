package ru.yandex.market.vendor;


import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Test;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hamcrest.MatcherAssert.assertThat;

class SwaggerControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    /**
     * Тест проверяет работоспособность swagger-а
     */
    @Test
    void shouldLoadSpringContextWithoutAnyErrors() {
        String response = FunctionalTestHelper.get(baseUrl + "/v2/api-docs");
        assertThat("Swagger response is empty", isNotEmpty(response));
        JsonAssert.assertJsonNotEquals("{}", response, when(IGNORING_ARRAY_ORDER));
    }

}
