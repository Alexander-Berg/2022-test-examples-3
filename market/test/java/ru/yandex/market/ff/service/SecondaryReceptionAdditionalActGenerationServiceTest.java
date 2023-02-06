package ru.yandex.market.ff.service;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.service.implementation.SecondaryReceptionAdditionalActGenerationService;

/**
 * Функциональные тесты для {@link SecondaryReceptionAdditionalActGenerationService}.
 */
public class SecondaryReceptionAdditionalActGenerationServiceTest extends ActGenerationServiceTest {

    @Autowired
    private SecondaryReceptionAdditionalActGenerationService actGenerationServiceTest;

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateBigSecondaryReceptionAct() throws IOException {
        assertPdfActGeneration(3, "sra-additional.txt", actGenerationServiceTest::generateReport);
    }

}
