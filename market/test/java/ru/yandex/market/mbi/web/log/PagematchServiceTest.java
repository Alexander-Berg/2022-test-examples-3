package ru.yandex.market.mbi.web.log;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Тесты сервиса подготовки списка урлов с именами метрик.
 *
 * @author stani on 06.03.18.
 */
class PagematchServiceTest {

    private PagematchService pagematchService;

    @BeforeEach
    void setUp() {
        pagematchService = new PagematchService("mbi-web",
                Collections.singletonList(() -> Arrays.asList(
                        "/surveys/{surveyId}/passed", "/pagematch", "/getPagematch", "/pagematch")));
    }

    @Test
    void testPagematch() {
        String[] expected = new String[]{
                "getPagematch\t/getPagematch\tmbi-web",
                "pagematch\t/pagematch\tmbi-web",
                "surveys_surveyId_passed\t/surveys/<surveyId>/passed\tmbi-web"
        };
        Assertions.assertArrayEquals(expected, pagematchService.getPagematch().split("\n"));
    }

}
