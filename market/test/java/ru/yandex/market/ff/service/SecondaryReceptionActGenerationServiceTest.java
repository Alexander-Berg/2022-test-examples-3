package ru.yandex.market.ff.service;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.service.implementation.SecondaryReceptionActGenerationService;
/**
 * Функциональные тесты для {@link SecondaryReceptionActGenerationService}.
 */
public class SecondaryReceptionActGenerationServiceTest extends ActGenerationServiceTest {

    @Autowired
    private SecondaryReceptionActGenerationService actGenerationServiceTest;

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateSmallSecondaryReceptionAct() throws IOException {
        assertPdfActGeneration(1, "sra-small.txt", actGenerationServiceTest::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateBigSecondaryReceptionAct() throws IOException {
        assertPdfActGeneration(2, "sra-big.txt", actGenerationServiceTest::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateVeryBigSecondaryReceptionAct() throws IOException {
        assertPdfActGeneration(3, "sra-verybig.txt", actGenerationServiceTest::generateReport);
    }
}
