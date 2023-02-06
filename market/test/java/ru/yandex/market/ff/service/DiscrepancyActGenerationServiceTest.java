package ru.yandex.market.ff.service;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.service.implementation.DiscrepancyActGenerationService;

/**
 * Функциональные тесты для {@link DiscrepancyActGenerationService}.
 */
class DiscrepancyActGenerationServiceTest extends ActGenerationServiceTest {

    @Autowired
    private DiscrepancyActGenerationService discrepancyActGenerationService;

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateDiscrepancyAct() throws IOException {
        assertPdfActGeneration(8, "discrepancy.txt", discrepancyActGenerationService::generateReport);
    }

    // following 3 tests covers same cases as old tests DivergenceActGenerationServiceTest
    // and SecondaryReceptionAdditionalActGenerationServiceTest
//    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateSmallDivergenceAct() throws IOException {
        assertPdfActGeneration(1, "div-small.txt", discrepancyActGenerationService::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateBigDivergenceAct() throws IOException {
        assertPdfActGeneration(2, "div-big.txt", discrepancyActGenerationService::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateVeryBigDivergenceAct() throws IOException {
        assertPdfActGeneration(3, "div-verybig.txt", discrepancyActGenerationService::generateReport);
    }
}
