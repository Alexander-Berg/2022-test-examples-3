package ru.yandex.market.ff.service;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.service.implementation.ReceptionTransferAdditionalActGenerationService;

/**
 * Функциональные тесты для {@link ReceptionTransferAdditionalActGenerationService}.
 */
public class ReceptionTransferAdditionalActGenerationServiceTest extends ActGenerationServiceTest {

    @Autowired
    private ReceptionTransferAdditionalActGenerationService actGenerationService;

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateSmallDivergenceAct() throws IOException {
        assertPdfActGeneration(3, "rta-additional.txt", actGenerationService::generateReport);
    }

}
