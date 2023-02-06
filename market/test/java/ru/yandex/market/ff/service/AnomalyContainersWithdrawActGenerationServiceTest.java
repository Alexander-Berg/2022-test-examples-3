package ru.yandex.market.ff.service;

import java.io.IOException;
import java.io.InputStream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.ff.client.enums.DocumentType;
import ru.yandex.market.ff.dbqueue.service.docu.BuildAnomalyContainersWithdrawActService;
import ru.yandex.market.ff.model.dbqueue.BuildAnomalyContainersWithdrawActPayload;
import ru.yandex.market.ff.service.registry.RegistryChainStateService;
import ru.yandex.market.ff.service.registry.RegistryService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;

/**
 * Функциональные тесты для {@link AnomalyContainersWithdrawActGenerationServiceTest}.
 */
public class AnomalyContainersWithdrawActGenerationServiceTest extends ActGenerationServiceTest {

    @Autowired
    private RegistryService registryService;
    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;
    @Autowired
    private RegistryChainStateService registryChainStateService;
    @Autowired
    private FulfillmentInfoService fulfillmentInfoService;
    @Autowired
    private ConcreteEnvironmentParamService concreteEnvironmentParamService;

    @Transactional
    @Test
    @DatabaseSetup("classpath:service/pdf-report/requests-with-supply-additional.xml")
    void generateAnomalyContainersWithdrawAct() throws IOException {

        RequestDocumentService requestDocumentService = mock(RequestDocumentService.class);

        BuildAnomalyContainersWithdrawActService actGenerationServiceTest =
                new BuildAnomalyContainersWithdrawActService(
                        registryService,
                        shopRequestFetchingService,
                        requestDocumentService,
                        registryChainStateService,
                        fulfillmentInfoService,
                        concreteEnvironmentParamService
                );

        ArgumentCaptor<InputStream> argInpStream = ArgumentCaptor.forClass(InputStream.class);
        Mockito.when(requestDocumentService.create(anyLong(), argInpStream.capture(), any(DocumentType.class), any()))
                .thenReturn(null);


        assertPdfActGeneration(2, "anomaly-container-withdraw-act.txt", (r) -> {
            actGenerationServiceTest.processPayload(new BuildAnomalyContainersWithdrawActPayload(r.getId()));
            return argInpStream.getValue();
        });
    }

}
