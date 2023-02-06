package ru.yandex.market.mbi.web.log;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbi.web.log.suppliers.MvcUrlsSupplier;
import ru.yandex.market.mbi.web.log.suppliers.ServantletUrlsSupplier;

/**
 * Тест произвольной конфигурации {@link PagematchController}
 *
 * @author stani on 07.03.18.
 */
class PagematchControllerTest {

    private PagematchController pagematchController;

    @BeforeEach
    void setUp() {
        MvcUrlsSupplier mvcUrlsSupplier = Mockito.mock(MvcUrlsSupplier.class);
        Mockito.when(mvcUrlsSupplier.get()).thenReturn(Arrays.asList(
                "/surveys/{surveyId}/passed", "/pagematch", "/pagematch"));

        ServantletUrlsSupplier servantletUrlsSupplier = Mockito.mock(ServantletUrlsSupplier.class);
        Mockito.when(servantletUrlsSupplier.get()).thenReturn(Collections.singletonList("/getPagematch"));

        PagematchService pagematchService = new PagematchService("mbi-partner",
                Arrays.asList(mvcUrlsSupplier, servantletUrlsSupplier));

        pagematchController = new PagematchController(pagematchService);
    }

    @Test
    void testPagematch() {
        String pagematch = pagematchController.pagematch();

        String[] expected = new String[]{
                "getPagematch\t/getPagematch\tmbi-partner",
                "pagematch\t/pagematch\tmbi-partner",
                "surveys_surveyId_passed\t/surveys/<surveyId>/passed\tmbi-partner"
        };
        Assertions.assertArrayEquals(expected, pagematch.split("\n"));
    }

}
