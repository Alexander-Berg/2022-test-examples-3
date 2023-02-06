package ru.yandex.market.ff.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.ff.model.entity.RequestSubTypeEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.RequestItemRepository;
import ru.yandex.market.ff.repository.RequestLegalInfoRepository;
import ru.yandex.market.ff.service.implementation.ConcreteEnvironmentParamServiceImpl;
import ru.yandex.market.ff.service.implementation.WithdrawActGenerationService;
import ru.yandex.market.ff.service.implementation.drivers.DaasServiceImpl;
import ru.yandex.market.ff.service.implementation.drivers.DriversBookletActGenerationService;
import ru.yandex.market.ff.service.implementation.drivers.parsers.PlaceholdersParser;
import ru.yandex.market.ff.service.implementation.drivers.pdf.PdfGenerator;
import ru.yandex.market.ff.service.implementation.drivers.processors.CssProcessor;
import ru.yandex.market.ff.service.implementation.drivers.processors.HtmlPageProcessor;
import ru.yandex.market.ff.service.registry.RegistryService;
import ru.yandex.market.ff.util.FileContentUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для {@link WithdrawActGenerationService}.
 */
public class WithdrawActGenerationServiceTest extends ActGenerationServiceTest {

    private static final String DAAS_RESPONSE_JSON = "service/drivers/json/withdrawal_response.json";
    private static final String DAAS_EMPTY_RESPONSE_JSON = "service/drivers/json/response_empty.json";
    private static final String CSS = "service/drivers/css/css.css";

    @Autowired
    private RequestItemRepository requestItemRepository;

    @Autowired
    private RequestLegalInfoRepository requestLegalInfoRepository;

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @Autowired
    private HtmlPageProcessor htmlPageProcessor;

    @Autowired
    private PdfGenerator pdfGenerator;

    @Autowired
    private RequestSubTypeService requestSubTypeService;

    private DaasServiceImpl mockDaasService;
    private CssProcessor mockCssProcessor;

    @Autowired
    private RegistryService registryService;

    private RequestSubTypeEntity mySubType;

    private ConcreteEnvironmentParamService mockConcreteEnvironmentParamService;
    private DriversBookletActGenerationService spyDriversBookletActGenerationService;

    private WithdrawActGenerationService withdrawActGenerationService;
    private PlaceholdersParser placeholdersParser;

    @BeforeEach
    public void setFields() {
        mockCssProcessor = mock(CssProcessor.class);
        mockDaasService = mock(DaasServiceImpl.class);
        requestSubTypeService = mock(RequestSubTypeService.class);

        mySubType = mock(RequestSubTypeEntity.class);
        when(mySubType.isUseRegistryToGenerateActOfWithdrawal()).thenReturn(false);
        when(requestSubTypeService.getEntityByRequestTypeAndSubtype(any())).thenReturn(mySubType);

        mockConcreteEnvironmentParamService = mock(ConcreteEnvironmentParamServiceImpl.class);

        CalendaringServiceClientWrapperService calendaringServiceClientWrapperService =
                mock(CalendaringServiceClientWrapperService.class);

        placeholdersParser = spy(new PlaceholdersParser(calendaringServiceClientWrapperService,
                shopRequestFetchingService, requestSubTypeService));

        spyDriversBookletActGenerationService = spy(new DriversBookletActGenerationService(
                mockConcreteEnvironmentParamService, mockDaasService,
                mockCssProcessor, htmlPageProcessor, placeholdersParser, pdfGenerator));
        withdrawActGenerationService = new WithdrawActGenerationService(requestItemRepository,
                requestLegalInfoRepository, spyDriversBookletActGenerationService, mockConcreteEnvironmentParamService,
                requestSubTypeService, registryService);
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-withdrawal.xml")
    @Transactional
    void generateWithdrawActFromRegistry() throws IOException {
        when(mySubType.isUseRegistryToGenerateActOfWithdrawal()).thenReturn(true);
        mockCss();
        setEnvParams();
        doThrow(RuntimeException.class).when(placeholdersParser)
                .getParsedHtml(any(), any(ShopRequest.class), anyBoolean());
        assertPdfActGeneration(1, "withdraw-small-with-undefined.txt", withdrawActGenerationService::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-withdrawal.xml")
    void generateSmallWithdrawAct() throws IOException {
        mockCss();
        setEnvParams();
        doThrow(RuntimeException.class).when(placeholdersParser)
                .getParsedHtml(any(), any(ShopRequest.class), anyBoolean());
        assertPdfActGeneration(1, "withdraw-small.txt", withdrawActGenerationService::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-withdrawal.xml")
    void generateSmallWithdrawActForInvalidWarehouse() throws IOException {
        mockCss();
        setDaasServiceMockResponse(DAAS_EMPTY_RESPONSE_JSON);
        setEnvParams();
        assertPdfActGeneration(11, "withdraw-small-empty.txt",
                withdrawActGenerationService::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-withdrawal.xml")
    void generateOldActWhenNoWarehousesSelected() throws IOException {
        when(mockConcreteEnvironmentParamService.getAllowedWarehousesInWithdrawalActToDeployNewDriverBooklet())
                .thenReturn(Collections.emptyList());
        assertPdfActGeneration(1, "withdraw-small.txt", withdrawActGenerationService::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-withdrawal.xml")
    void generateOldActWhenDriverBookletException() throws IOException {
        when(mockConcreteEnvironmentParamService.getAllowedWarehousesInWithdrawalActToDeployNewDriverBooklet())
                .thenReturn(Collections.singletonList("147"));
        doThrow(RuntimeException.class).when(spyDriversBookletActGenerationService)
                .generateReport(any(ShopRequest.class));
        assertPdfActGeneration(1, "withdraw-small.txt", withdrawActGenerationService::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-withdrawal.xml")
    void generateBigWithdrawAct() throws IOException {
        mockCss();
        setDaasServiceMockResponse(DAAS_RESPONSE_JSON);
        setEnvParams();
        assertPdfActGeneration(2, "withdraw-big.txt", withdrawActGenerationService::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-withdrawal.xml")
    void generateVeryBigWithdrawAct() throws IOException {
        mockCss();
        setDaasServiceMockResponse(DAAS_RESPONSE_JSON);
        setEnvParams();
        assertPdfActGeneration(3, "withdraw-verybig.txt", withdrawActGenerationService::generateReport);
    }

    private void setDaasServiceMockResponse(String jsonFile) throws IOException {
        when(mockDaasService.getHtmlDocumentComponent(anyString(), anyString()))
                .thenReturn(FileContentUtils.getFileContent(jsonFile));
    }

    private void setEnvParams() {
        when(mockConcreteEnvironmentParamService.getAllowedWarehousesInWithdrawalActToDeployNewDriverBooklet())
                .thenReturn(Arrays.asList("147", "100"));
    }

    private void mockCss() throws IOException {
        when(mockCssProcessor.loadCss(any(ShopRequest.class), anyString()))
                .thenReturn(FileContentUtils.getFileContent(CSS));
    }
}
