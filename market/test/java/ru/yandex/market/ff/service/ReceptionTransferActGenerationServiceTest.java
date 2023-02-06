package ru.yandex.market.ff.service;

import java.io.IOException;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.implementation.ConcreteEnvironmentParamServiceImpl;
import ru.yandex.market.ff.service.implementation.ReceptionTransferActGenerationService;
import ru.yandex.market.ff.service.implementation.drivers.DriversBookletActGenerationService;
import ru.yandex.market.ff.service.implementation.drivers.OldDriversBookletActGenerationService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для {@link ReceptionTransferActGenerationService}.
 */
public class ReceptionTransferActGenerationServiceTest extends ActGenerationServiceTest {

    @Autowired
    private ShopRequestRepository shopRequestRepository;

    @Autowired
    private RequestItemRepository requestItemRepository;

    @Autowired
    private FulfillmentInfoService fulfillmentInfoService;

    @Autowired
    private OldDriversBookletActGenerationService oldDriversBookletGenerator;

    @Autowired
    private DriversBookletActGenerationService driversBookletGenerator;

    private ConcreteEnvironmentParamService mockConcreteEnvironmentParamService;

    private ReceptionTransferActGenerationService actGenerationServiceTest;

    @BeforeEach
    public void setFields() {
        mockConcreteEnvironmentParamService = mock(ConcreteEnvironmentParamServiceImpl.class);
        actGenerationServiceTest = new ReceptionTransferActGenerationService(requestItemRepository,
                fulfillmentInfoService,
                driversBookletGenerator, oldDriversBookletGenerator, mockConcreteEnvironmentParamService);
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateSmallReceptionTransferAct() throws IOException {
        setEnvParams();
        assertPdfActGeneration(1, "rta-small.txt", actGenerationServiceTest::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateBigReceptionTransferAct() throws IOException {
        setEnvParams();
        assertPdfActGeneration(2, "rta-big.txt", actGenerationServiceTest::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateBigReceptionTransferActWithException() {
        Assertions.assertThrows(RuntimeException.class,
                () -> actGenerationServiceTest.generateReport(shopRequestRepository.getOne(2L)));
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateVeryBigReceptionTransferAct() throws IOException {
        setEnvParams();
        assertPdfActGeneration(3, "rta-verybig.txt", actGenerationServiceTest::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests.xml")
    void generateXDocReceptionTransferAct() throws IOException {
        setEnvParams();
        assertPdfActGeneration(6, "rta-xdoc.txt", actGenerationServiceTest::generateReport);
    }

    private void setEnvParams() {
        when(mockConcreteEnvironmentParamService.getAllowedWarehousesToDeployNewDriverBooklet())
                .thenReturn(Collections.emptyList());
    }
}
