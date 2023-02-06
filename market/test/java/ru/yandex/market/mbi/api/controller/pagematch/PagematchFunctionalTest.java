package ru.yandex.market.mbi.api.controller.pagematch;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.api.config.FunctionalTest;


/**
 * Функциональный тест на ручку /pagematch.
 *
 * @author Vadim Lyalin
 */
class PagematchFunctionalTest extends FunctionalTest {

    @Test
    void testPagematch() {
        ResponseEntity<String> response = FunctionalTestHelper.get("http://localhost:" + port + "/pagematch");
        MatcherAssert.assertThat(
                response.getBody(),
                Matchers.containsString("abo_cutoff_datasource_id_open\t/abo-cutoff/<datasource_id>/open\tmbi-api")
        );
    }
}
