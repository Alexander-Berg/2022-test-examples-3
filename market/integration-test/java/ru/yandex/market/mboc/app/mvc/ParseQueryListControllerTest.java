package ru.yandex.market.mboc.app.mvc;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.mboc.app.BaseWebIntegrationTestClass;

/**
 * Tests of {@link ParseQueryListController}.
 * Тест проверяет, что если указать в мультипараметр через запятую: ids=1,2,3,4 то все корректно распарсится в список.
 */
public class ParseQueryListControllerTest extends BaseWebIntegrationTestClass {
    @Test
    public void parseIds() throws Exception {
        MvcResult mvcResult = getJson("/integration-test-api/parse-ids?ids=1,2,3,4,5");
        Assertions.assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo("Count: 5");
    }
}
