package ru.yandex.market.pers.shopinfo.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.pers.shopinfo.test.context.FunctionalTest;

/**
 * Тест на работоспособность метода shopinfo /pagematch.
 *
 * @author stani on 05.03.18.
 */
public class PagematchControllerTest extends FunctionalTest {

    @Test
    public void testPagematch() {
        ResponseEntity<String> response = FunctionalTestHelper.get(urlBasePrefix + "/pagematch");
        Assertions.assertTrue(response.getBody().split("\n").length > 1);
    }
}
