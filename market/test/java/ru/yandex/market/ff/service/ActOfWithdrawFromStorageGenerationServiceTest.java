package ru.yandex.market.ff.service;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.RequestStatusHistoryRepository;
import ru.yandex.market.ff.service.implementation.ActOfWithdrawFromStorageGenerationService;
import ru.yandex.market.ff.service.implementation.ConcreteEnvironmentParamServiceImpl;
import ru.yandex.market.ff.service.implementation.UtilizationTransferOutboundMappingService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActOfWithdrawFromStorageGenerationServiceTest extends ActGenerationServiceTest {

    @Autowired
    private RequestItemService requestItemService;

    @Autowired
    private RequestStatusHistoryRepository requestStatusHistoryRepository;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private UtilizationTransferOutboundMappingService utilizationTransferOutboundMappingService;

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @Autowired
    private FulfillmentInfoService fulfillmentInfoService;

    @Autowired
    private TransferOutboundService transferOutboundService;

    @Autowired
    private RequestSubTypeService requestSubTypeService;

    private ActOfWithdrawFromStorageGenerationService actOfWithdrawFromStorageGenerationService;

    private ConcreteEnvironmentParamService mockConcreteEnvironmentParamService;

    @BeforeEach
    public void before() {
        mockConcreteEnvironmentParamService = mock(ConcreteEnvironmentParamServiceImpl.class);
        actOfWithdrawFromStorageGenerationService =
                new ActOfWithdrawFromStorageGenerationService(requestItemService, requestStatusHistoryRepository,
                        supplierService, utilizationTransferOutboundMappingService, shopRequestFetchingService,
                        mockConcreteEnvironmentParamService, fulfillmentInfoService, transferOutboundService,
                        requestSubTypeService);
        when(mockConcreteEnvironmentParamService.getActOfWithdrawFromStorageReasonForForceTransfer())
                .thenReturn("п.2.9 Договора на оказание услуг маркетплейса Яндекс.Маркета");
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/act-of-withdraw-from-storage-requests.xml")
    void generateActOfWithdrawFromStorageForUtilizationWithdraw() throws IOException {
        assertPdfActGeneration(1, "aws-withdraw.txt", actOfWithdrawFromStorageGenerationService::generateReport);
    }

    /**
     * Состояние базы:
     * 1) Есть 2 трансфера с id 2 и 3. В трансфере 2 товары 1 (10 штук) и 2 (10 штук).
     * В трансфере 3 товары 1 (10 штук), 2 (10 штук) и 3 (10 штук)
     * 2) Есть 2 отгруженных изъятия в статусах 7 и 10. В первом изъятии отгружены товары 1 (10 штук) и 3 (10 штук).
     * Во втором изъятии отгружен товар 1 (5 штук). Оба изъятия связаны с трансферами.
     * 3) Есть 3 изъятия в статусах отменено, невалидное и отклонено сервисом с различными комбинациями товаров.
     * Эти изъятия не должны никак влиять на логику работы.
     * <p>
     * Документ генерим для трансфера 3.
     * В результате ожидаем, что в документе не будет строки со 2 товаром (так как он не отгружен в изъятиях),
     * а также будет товар 1 (5 штук), так как в трансфер 2 попадает 10 штук, а в трансфер 3 попадает 5 штук и
     * товар 3 в количестве 10 штук, так как все 10 штук отгружены в изъятиях.
     */
    @Test
    @DatabaseSetup("classpath:service/pdf-report/act-of-withdraw-from-storage-requests.xml")
    void generateActOfWithdrawFromStorageForForceUtilizationTransfer() throws IOException {
        assertPdfActGeneration(3, "aws-force-transfer.txt", actOfWithdrawFromStorageGenerationService::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/act-of-withdraw-from-storage-requests.xml")
    void generateActOfWithdrawFromStorageForSupplierUtilizationTransfer() throws IOException {
        assertPdfActGeneration(2, "aws-supplier-transfer.txt",
                actOfWithdrawFromStorageGenerationService::generateReport);
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/act-of-withdraw-from-storage-fails.xml")
    void generateActOfWithdrawForNotProcessedOutbound() {
        assertException(1, "It should be processed status for document generation");
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/act-of-withdraw-from-storage-fails.xml")
    void generateActOfWithdrawForIncorrectRequestType() {
        assertException(2, "Act of withdraw from storage generation is possible only " +
                "for utilization withdraw or transfer to utilization");
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/act-of-withdraw-from-storage-fails.xml")
    void generateActOfWithdrawForTransferWithoutLinkedOutbounds() {
        assertException(3, "There are no linked outbounds for transfer 3");
    }

    @Test
    @DatabaseSetup("classpath:service/pdf-report/act-of-withdraw-from-storage-fails.xml")
    void generateActOfWithdrawForTransferWithoutLinkedProcessedOutbounds() {
        assertException(4, "There are no linked processed outbounds for transfer 4");
    }

    private void assertException(long requestId, String message) {
        ShopRequest request = requestService.getRequestOrThrow(requestId);
        try {
            actOfWithdrawFromStorageGenerationService.generateReport(request);
            assertions.fail("Exception should be thrown");
        } catch (Exception e) {
            assertions.assertThat(e.getMessage()).isEqualTo(message);
        }
    }
}
