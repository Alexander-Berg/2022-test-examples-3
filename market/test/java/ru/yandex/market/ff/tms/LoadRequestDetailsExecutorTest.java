package ru.yandex.market.ff.tms;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.framework.history.wrapper.HistoryAgent;
import ru.yandex.market.ff.repository.NewMovementFlowRequestsRepository;
import ru.yandex.market.ff.service.LgwRequestService;
import ru.yandex.market.ff.service.RequestConfigsService;
import ru.yandex.market.ff.service.RequestDetailsService;
import ru.yandex.market.ff.tms.exception.TmsJobException;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.CompositeId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundUnitDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundUnitDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferDetailsItem;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для {@link LoadRequestDetailsExecutor}.
 *
 * @author avetokhin 24/01/18.
 */
class LoadRequestDetailsExecutorTest extends IntegrationTest {

    private static final Partner PARTNER = new Partner(121L);
    private static final long SUPPLIER_ID = 1;
    private static final long SECOND_SUPPLIER_ID = 2;
    public static final ResourceId RESOURCE_ID1 = ResourceId.builder()
            .setYandexId("1")
            .setPartnerId("11")
            .build();
    public static final ResourceId RESOURCE_ID2 = ResourceId.builder()
            .setYandexId("2")
            .setPartnerId("22")
            .build();

    @Autowired
    private RequestDetailsService requestDetailsService;

    @Autowired
    private LgwRequestService lgwRequestService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private NewMovementFlowRequestsRepository newMovementFlowRequestsRepository;

    @Autowired
    private RequestConfigsService requestConfigsService;

    private LoadRequestDetailsExecutor executor;

    @Autowired
    private HistoryAgent historyAgent;

    @BeforeEach
    void init() {
        executor = new LoadRequestDetailsExecutor(lgwRequestService, requestDetailsService, transactionTemplate,
                Executors.newSingleThreadExecutor(), newMovementFlowRequestsRepository,
                requestConfigsService, historyAgent);

        InboundUnitDetails details1 = inboundUnitDetails("art1", 4, 4, 0, SUPPLIER_ID, 1);
        InboundUnitDetails details2 = inboundUnitDetails("art2", 3, 3, 0, SUPPLIER_ID, 1);
        InboundUnitDetails details3 = inboundUnitDetails("art3", 0, 3, 1, SUPPLIER_ID, 1);
        InboundDetails inboundDetails1 = new InboundDetails(
                RESOURCE_ID1, Arrays.asList(details1, details2, details3));

        when(fulfillmentClient.getInboundDetails(RESOURCE_ID1, PARTNER))
                .thenReturn(inboundDetails1);

        InboundUnitDetails details4 = inboundUnitDetails("art1", 4, 4, 2, SUPPLIER_ID, 1);
        InboundUnitDetails details5 = inboundUnitDetails("art2", 3, 2, 1, SUPPLIER_ID, 1);
        InboundDetails inboundDetails2 = new InboundDetails(
                RESOURCE_ID1, Arrays.asList(details4, details5));

        when(fulfillmentClient.getInboundDetails(RESOURCE_ID2, PARTNER))
                .thenReturn(inboundDetails2);

        ResourceId resourceIdOutbound1 = ResourceId.builder()
                .setYandexId("3")
                .setPartnerId("33")
                .build();
        OutboundUnitDetails details6 = outboundUnitDetails("art3", 4, 3, SUPPLIER_ID);
        OutboundUnitDetails details7 = outboundUnitDetails("art4", 2, 2, SUPPLIER_ID);
        OutboundDetails outboundDetails1 = new OutboundDetails(
                resourceIdOutbound1, Arrays.asList(details6, details7));

        when(fulfillmentClient.getOutboundDetails(resourceIdOutbound1, PARTNER))
                .thenReturn(outboundDetails1);

        ResourceId resourceIdOutbound2 = ResourceId.builder()
                .setYandexId("4")
                .setPartnerId("44")
                .build();
        OutboundUnitDetails details8 = outboundUnitDetails("art3", 4, 3, SUPPLIER_ID);
        OutboundUnitDetails details9 = outboundUnitDetails("art4", 2, 2, SUPPLIER_ID);
        OutboundDetails outboundDetails2 = new OutboundDetails(
                resourceIdOutbound2, Arrays.asList(details8, details9));

        when(fulfillmentClient.getOutboundDetails(resourceIdOutbound2, PARTNER))
                .thenReturn(outboundDetails2);

        ResourceId resourceIdTransfer = ResourceId.builder()
                .setYandexId("5")
                .setPartnerId("44")
                .build();
        TransferDetailsItem details10 = transferUnitDetails("art3", 4, 3, SUPPLIER_ID);
        TransferDetailsItem details11 = transferUnitDetails("art4", 2, 2, SUPPLIER_ID);
        TransferDetails transferDetails = new TransferDetails(
                resourceIdTransfer, Arrays.asList(details10, details11));

        when(fulfillmentClient.getTransferDetails(resourceIdTransfer, PARTNER))
                .thenReturn(transferDetails);

        ResourceId resourceIdInboundWithUndeclaredSurplus = ResourceId.builder()
                .setYandexId("7")
                .setPartnerId("77")
                .build();
        InboundUnitDetails details12 = inboundUnitDetails("art5", 4, 4, 0, SUPPLIER_ID, 0);
        InboundUnitDetails details13 = inboundUnitDetails("art2", 0, 1, 0, SUPPLIER_ID, 1);
        InboundDetails inboundDetails = new InboundDetails(
                resourceIdInboundWithUndeclaredSurplus, Arrays.asList(details12, details13));

        when(fulfillmentClient.getInboundDetails(resourceIdInboundWithUndeclaredSurplus, PARTNER))
                .thenReturn(inboundDetails);

        ResourceId resourceIdOutbound3 = ResourceId.builder()
                .setYandexId("8")
                .setPartnerId("88")
                .build();
        OutboundUnitDetails details14 = outboundUnitDetails("art3", 4, 3, SUPPLIER_ID);
        OutboundUnitDetails details15 = outboundUnitDetails("art4", 2, 2, SUPPLIER_ID);
        OutboundUnitDetails details16 = outboundUnitDetails("art4", 2, 2, SECOND_SUPPLIER_ID);
        OutboundUnitDetails details17 = outboundUnitDetails("art5", 3, 1, SECOND_SUPPLIER_ID);
        OutboundDetails outboundDetails3 = new OutboundDetails(
                resourceIdOutbound3, Arrays.asList(details14, details15, details16, details17));

        when(fulfillmentClient.getOutboundDetails(resourceIdOutbound3, PARTNER))
                .thenReturn(outboundDetails3);
    }

    /**
     * Флаг загрузки излишков включен. Опознанные излишки добавляются к поставке.
     */
    @Test
    @DatabaseSetup(value = "classpath:tms/load-request-details/before-update-details-with-extras.xml")
    @ExpectedDatabase(value = "classpath:tms/load-request-details/after-update-details-with-extras.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void executeSuccessfullyExtraItemsTurnedOn() {
        executor.doJob(null);
    }

    @Test
    @DatabaseSetup(
            value = "classpath:tms/load-request-details/before-update-details-with-two-internal-errors-for-item.xml")
    @ExpectedDatabase(
            value = "classpath:tms/load-request-details/after-update-details-with-two-internal-errors-for-item.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void executeSuccessfullyWithTwoInternalErrorsForOneItem() {
        InboundUnitDetails details1 = inboundUnitDetails("art1", 4, 2, 0, SUPPLIER_ID, 0);
        InboundDetails inboundDetails1 = new InboundDetails(RESOURCE_ID1, Collections.singletonList(details1));

        when(fulfillmentClient.getInboundDetails(RESOURCE_ID1, PARTNER))
                .thenReturn(inboundDetails1);
        executor.doJob(null);
    }

    /**
     * Синк деталей для изъятия утилизации с подизъятиями.
     */
    @Test
    @DatabaseSetup(value = "classpath:tms/load-request-details/before-with-sub-requests.xml")
    @ExpectedDatabase(value = "classpath:tms/load-request-details/after-with-sub-requests.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void executeSuccessfullyWithSubRequests() {
        executor.doJob(null);
    }

    /**
     * В деталях не хватает информации по одной из строк поставки. Выбрасывается ошибка.
     */
    @Test
    @DatabaseSetup("classpath:tms/load-request-details/before-update-details-not-all-details.xml")
    void executeWithNoDetailsFoundError() {
        final TmsJobException e =
                Assertions.assertThrows(TmsJobException.class, () -> executor.doJob(null));
        assertThat(e.getMessage(),
                containsString("Details not found for request [2], item with article [artXXX]"));
    }

    /**
     * В деталях не хватает информации по одной из строк поставки, количество которой 0, строка игнорируется.
     */
    @Test
    @DatabaseSetup("classpath:tms/load-request-details/before-update-details-no-details-by-zero-item.xml")
    @ExpectedDatabase(
            value = "classpath:tms/load-request-details/after-update-details-no-details-by-zero-item.xml",
            assertionMode = NON_STRICT
    )
    void executeWithNoDetailsByZeroItem() {
        final ResourceId resourceId = ResourceId.builder()
                .setYandexId("2")
                .setPartnerId("22")
                .build();

        final OutboundUnitDetails details1 = outboundUnitDetails("art1", 3, 3, 1);
        final OutboundUnitDetails details2 = outboundUnitDetails("art2", 4, 4, 1);
        final OutboundDetails outboundDetails = new OutboundDetails(resourceId, Arrays.asList(details1, details2));
        when(fulfillmentClient.getOutboundDetails(resourceId, PARTNER)).thenReturn(outboundDetails);

        executor.doJob(null);
    }

    /**
     * В деталях не хватает информации по одной из строк поставки,
     * по которой имеется ошибка валидации, строка игнорируется.
     */
    @Test
    @DatabaseSetup("classpath:tms/load-request-details/before-update-details-no-details-by-error-item.xml")
    @ExpectedDatabase(
            value = "classpath:tms/load-request-details/after-update-details-no-details-by-error-item.xml",
            assertionMode = NON_STRICT
    )
    void executeWithNoDetailsByErrorItem() {
        final ResourceId resourceId = ResourceId.builder()
                .setYandexId("2")
                .setPartnerId("22")
                .build();

        final OutboundUnitDetails details1 = outboundUnitDetails("art1", 3, 3, 1);
        final OutboundUnitDetails details2 = outboundUnitDetails("art2", 4, 4, 1);
        final OutboundDetails outboundDetails = new OutboundDetails(resourceId, Arrays.asList(details1, details2));
        when(fulfillmentClient.getOutboundDetails(resourceId, PARTNER)).thenReturn(outboundDetails);

        executor.doJob(null);
    }

    /**
     * В заявке клиентского возврата может оказаться так, что есть товары с одинаковым артикулом,
     * но от разных поставщиков.
     *
     * <p>Тест проверяет, что в такой ситуации информация о количестве товаров обновится без ошибок.</p>
     */
    @Test
    @DatabaseSetup("classpath:tms/load-request-details/before-customer-return-request.xml")
    @ExpectedDatabase(
            value = "classpath:tms/load-request-details/after-customer-return-request.xml",
            assertionMode = NON_STRICT
    )
    void executeCustomerReturnRequestTypeWithSameArticleButDifferentSupplierId() {
        final ResourceId resourceId = ResourceId.builder()
                .setYandexId("11743")
                .setPartnerId("11111")
                .build();

        final InboundUnitDetails details1 = inboundUnitDetails("108", 1, 1, 0, 1, 1);
        final InboundUnitDetails details2 = inboundUnitDetails("108", 2, 2, 0, 2, 1);
        final InboundDetails inboundDetails = new InboundDetails(resourceId, Arrays.asList(details1, details2));
        when(fulfillmentClient.getInboundDetails(resourceId, PARTNER)).thenReturn(inboundDetails);

        executor.doJob(null);
    }

    /**
     * Проверяет корректный расчет недостачи для трех кейсов.
     * 1. Article '107'. Заявлено 10, годного 4, 0 дефекта, ожидаем 6 в недостаче.
     * 2. Article '108'. Заявлено 10, годного 8, 2 дефекта, ожидаем 0 недостачи.
     * 3. Article '109'. Заявлено 10, годного 10, 2 излишка, ожидаем 0 недостачи.
     */
    @Test
    @DatabaseSetup("classpath:tms/load-request-details/before-shortage-calc.xml")
    @ExpectedDatabase(
            value = "classpath:tms/load-request-details/after-shortage-calc.xml",
            assertionMode = NON_STRICT
    )
    void correctShortageCalculation() {
        ResourceId resourceId = ResourceId.builder()
                .setYandexId("11743")
                .setPartnerId("11111")
                .build();

        InboundUnitDetails details1 = inboundUnitDetails("107", 10, 4, 0, 1, 0);
        InboundUnitDetails details2 = inboundUnitDetails("108", 10, 8, 2, 1, 0);
        InboundUnitDetails details3 = inboundUnitDetails("109", 10, 10, 0, 1, 2);
        InboundDetails inboundDetails = new InboundDetails(resourceId, Arrays.asList(details1, details2, details3));
        when(fulfillmentClient.getInboundDetails(resourceId, PARTNER)).thenReturn(inboundDetails);

        executor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/load-request-details/before-not-full-details-for-transfer.xml")
    @ExpectedDatabase(
            value = "classpath:tms/load-request-details/after-not-full-details-for-transfer.xml",
            assertionMode = NON_STRICT
    )
    void executeForTransferWithNotFullDetails() {
        ResourceId resourceIdTransfer = ResourceId.builder()
                .setYandexId("5")
                .setPartnerId("44")
                .build();
        TransferDetailsItem details = transferUnitDetails("art3", 4, 3, SUPPLIER_ID);
        TransferDetails transferDetails = new TransferDetails(resourceIdTransfer, List.of(details));
        when(fulfillmentClient.getTransferDetails(resourceIdTransfer, PARTNER)).thenReturn(transferDetails);

        executor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:tms/load-request-details/before-not-full-details-for-transfer.xml")
    @ExpectedDatabase(
            value = "classpath:tms/load-request-details/after-not-full-details-for-transfer-with-identifiers.xml",
            assertionMode = NON_STRICT
    )
    void executeForTransferWithNotFullDetailsWithIdentifiers() {
        ResourceId resourceIdTransfer = ResourceId.builder()
                .setYandexId("5")
                .setPartnerId("44")
                .build();
        TransferDetailsItem details = transferDetailsItemWithInstances("art3", 4, 3, SUPPLIER_ID);
        TransferDetails transferDetails = new TransferDetails(resourceIdTransfer, List.of(details));
        when(fulfillmentClient.getTransferDetails(resourceIdTransfer, PARTNER)).thenReturn(transferDetails);

        executor.doJob(null);
    }

    @Test
    @DatabaseSetup(
            "classpath:tms/load-request-details/before-not-full-details-for-transfer-with-existing-identifiers.xml")
    @ExpectedDatabase(
            value =
                "classpath:tms/load-request-details/after-not-full-details-for-transfer-with-existing-identifiers.xml",
            assertionMode = NON_STRICT
    )
    void executeForTransferWithNotFullDetailsWithExistingIdentifiers() {
        ResourceId resourceIdTransfer = ResourceId.builder()
                .setYandexId("5")
                .setPartnerId("44")
                .build();
        TransferDetailsItem details = transferDetailsItemWithInstances("art3", 4, 3, SUPPLIER_ID);
        TransferDetails transferDetails = new TransferDetails(resourceIdTransfer, List.of(details));
        when(fulfillmentClient.getTransferDetails(resourceIdTransfer, PARTNER)).thenReturn(transferDetails);

        executor.doJob(null);
    }

    private static InboundUnitDetails inboundUnitDetails(
            String article, int declared, int actual, int defect, long vendorId, int surplus) {

        final UnitId unitId = new UnitId(null, vendorId, article);
        return new InboundUnitDetails(unitId, declared, actual, defect, surplus);
    }

    private static OutboundUnitDetails outboundUnitDetails(String article, int declared, int actual, long vendorId) {
        final UnitId unitId = new UnitId(null, vendorId, article);
        return new OutboundUnitDetails(unitId, declared, actual);
    }

    private static TransferDetailsItem transferUnitDetails(String article, int declared, int actual, long vendorId) {
        final UnitId unitId = new UnitId(null, vendorId, article);
        return new TransferDetailsItem(unitId, declared, actual);
    }

    private static TransferDetailsItem transferDetailsItemWithInstances(String article, int declared, int actual,
                                                                        long vendorId) {
        TransferDetailsItem transferDetailsItem = transferUnitDetails(article, declared, actual, vendorId);
        List<CompositeId> instances = List.of(
                new CompositeId(List.of(
                        new PartialId(PartialIdType.CIS, "cis-one"),
                        new PartialId(PartialIdType.PALLET_ID, "pallet-one")
                )),
                new CompositeId(List.of(
                        new PartialId(PartialIdType.CIS, "cis-two"),
                        new PartialId(PartialIdType.PALLET_ID, "pallet-two")
                )));
        transferDetailsItem.setInstances(instances);
        return transferDetailsItem;
    }


}
