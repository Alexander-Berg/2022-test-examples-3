package ru.yandex.market.ff.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.bo.ItemCountDetails;
import ru.yandex.market.ff.model.bo.ItemDetails;
import ru.yandex.market.ff.model.bo.RequestStatusInfo;
import ru.yandex.market.ff.model.bo.RequestStatusResult;
import ru.yandex.market.ff.model.bo.SupplierSkuKeyWithOrderId;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.Identifier;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.SupplierBusinessType;
import ru.yandex.market.ff.model.enums.IdentifierType;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.CompositeId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferDetailsItem;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatusEvent;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.TransferStatusType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FulfillmentRequestTransferServiceTest extends IntegrationTest {

    private static final Long REQ_ID_1 = 1L;
    private static final Long REQ_ID_2 = 2L;
    private static final Long REQ_ID_3 = 3L;
    private static final Long REQ_ID_4 = 4L;
    private static final Long REQ_ID_5 = 5L;
    private static final Long REQ_ID_6 = 6L;


    private static final String REQ_EXT_ID_1 = "11";
    private static final String REQ_EXT_ID_2 = "22";
    private static final String REQ_EXT_ID_3 = "33";
    private static final String REQ_EXT_ID_4 = "44";
    private static final String REQ_EXT_ID_5 = "55";
    private static final String REQ_EXT_ID_6 = "66";

    private static final LocalDateTime FIXED_DT = LocalDateTime.of(2017, 1, 1, 10, 0, 0, 0);
    private static final String FIXED_DT_STR = "2017-01-01T10:00:00";
    private static final Long SERVICE_ID_1 = 555L;
    private static final Partner PARTNER_1 = partner(SERVICE_ID_1);

    private static final ResourceId RES_ID_1 = ResourceId.builder()
        .setYandexId(REQ_ID_1.toString())
        .setPartnerId(REQ_EXT_ID_1)
        .build();
    private static final ResourceId RES_ID_2 = ResourceId.builder()
        .setYandexId(REQ_ID_2.toString())
        .setPartnerId(REQ_EXT_ID_2)
        .build();
    private static final ResourceId RES_ID_3 = ResourceId.builder()
        .setYandexId(REQ_ID_3.toString())
        .setPartnerId(REQ_EXT_ID_3)
        .build();
    private static final ResourceId RES_ID_4 = ResourceId.builder()
        .setYandexId(REQ_ID_4.toString())
        .setPartnerId(REQ_EXT_ID_4)
        .build();
    private static final ResourceId RES_ID_5 = ResourceId.builder()
        .setYandexId(REQ_ID_5.toString())
        .setPartnerId(REQ_EXT_ID_5)
        .build();
    private static final ResourceId RES_ID_6 = ResourceId.builder()
        .setYandexId(REQ_ID_6.toString())
        .setPartnerId(REQ_EXT_ID_6)
        .build();

    private static final long SUPPLIER_ID = 42;

    @Autowired
    private LgwRequestService service;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    /**
     * Проверка запроса к LGW для получения актуальных статусов трансферов.
     */
    @Test
    void getRequestsStatusesTransfer() {
        final TransferStatus status1 =
            transferStatus(RES_ID_1, TransferStatusType.PROCESSING, new DateTime(FIXED_DT_STR));
        final TransferStatus status2 =
            transferStatus(RES_ID_2, TransferStatusType.COMPLETED, new DateTime(FIXED_DT_STR));
        final TransferStatus status3 =
            transferStatus(RES_ID_3, TransferStatusType.ERROR, new DateTime(FIXED_DT_STR));
        final TransferStatus status4 =
            transferStatus(RES_ID_4, TransferStatusType.NEW, new DateTime(FIXED_DT_STR));
        final TransferStatus status5 =
            transferStatus(RES_ID_5, TransferStatusType.UNKNOWN, new DateTime(FIXED_DT_STR));
        final TransferStatus status6 =
            transferStatus(RES_ID_6, TransferStatusType.ACCEPTED, new DateTime(FIXED_DT_STR));

        when(fulfillmentClient.getTransfersStatus(Arrays.asList(RES_ID_1, RES_ID_2, RES_ID_3, RES_ID_4, RES_ID_5,
            RES_ID_6), PARTNER_1))
            .thenReturn(Arrays.asList(status1, status2, status3, status4, status5, status6));

        final RequestType requestType = RequestType.TRANSFER;
        final Map<Long, RequestStatusInfo> requestsStatuses = service.getRequestsStatuses(Arrays.asList(
            request(REQ_ID_1, REQ_EXT_ID_1, requestType, SERVICE_ID_1),
            request(REQ_ID_2, REQ_EXT_ID_2, requestType, SERVICE_ID_1),
            request(REQ_ID_3, REQ_EXT_ID_3, requestType, SERVICE_ID_1),
            request(REQ_ID_4, REQ_EXT_ID_4, requestType, SERVICE_ID_1),
            request(REQ_ID_5, REQ_EXT_ID_5, requestType, SERVICE_ID_1),
            request(REQ_ID_6, REQ_EXT_ID_6, requestType, SERVICE_ID_1)),
            false);

        assertThat(requestsStatuses, notNullValue());
        assertThat(requestsStatuses.get(REQ_ID_1),
            equalTo(new RequestStatusInfo(RequestStatusResult.of(RequestStatus.IN_PROGRESS), FIXED_DT)));

        assertThat(requestsStatuses.get(REQ_ID_2),
            equalTo(new RequestStatusInfo(RequestStatusResult.of(RequestStatus.PROCESSED), FIXED_DT)));

        assertThat(requestsStatuses.get(REQ_ID_3),
            equalTo(new RequestStatusInfo(RequestStatusResult.of(RequestStatus.REJECTED_BY_SERVICE), FIXED_DT)));

        assertThat(requestsStatuses.get(REQ_ID_4),
            equalTo(new RequestStatusInfo(RequestStatusResult.of(RequestStatus.ACCEPTED_BY_SERVICE), FIXED_DT)));

        assertThat(requestsStatuses.get(REQ_ID_5),
            equalTo(new RequestStatusInfo(RequestStatusResult.of("Unknown fulfillment status -1"), FIXED_DT)));

        assertThat(requestsStatuses.get(REQ_ID_6),
            equalTo(new RequestStatusInfo(RequestStatusResult.of(RequestStatus.ACCEPTED_BY_SERVICE), FIXED_DT)));
    }

    /**
     * Проверка запроса к LGW для получения истории статусов поставок.
     */
    @Test
    void getTransferStatusesHistory() {
        final List<TransferStatusEvent> statuses = Arrays.asList(
            transferEvent(TransferStatusType.NEW, new DateTime("2017-01-01T10:00:00")),
            transferEvent(TransferStatusType.ACCEPTED, new DateTime("2017-01-01T10:05:00")),
            transferEvent(TransferStatusType.PROCESSING, new DateTime("2017-01-01T10:07:00")),
            transferEvent(TransferStatusType.COMPLETED, new DateTime("2017-01-01T10:10:00"))
        );
        final TransferStatusHistory transferHistory = new TransferStatusHistory(statuses, RES_ID_1);
        when(fulfillmentClient.getTransferHistory(RES_ID_1, PARTNER_1)).thenReturn(transferHistory);

        final List<RequestStatusInfo> history =
            service.getRequestStatusesHistory(request(REQ_ID_1, REQ_EXT_ID_1, RequestType.TRANSFER, SERVICE_ID_1),
                false);

        assertThat(history, notNullValue());
        assertThat(history, hasSize(4));
        assertThat(history.get(0).getRequestStatusResult(),
            equalTo(RequestStatusResult.of(RequestStatus.ACCEPTED_BY_SERVICE)));
        assertThat(history.get(0).getDate(), equalTo(LocalDateTime.of(2017, 1, 1, 10, 0, 0)));

        assertThat(history.get(1).getRequestStatusResult(),
            equalTo(RequestStatusResult.of(RequestStatus.ACCEPTED_BY_SERVICE)));
        assertThat(history.get(1).getDate(), equalTo(LocalDateTime.of(2017, 1, 1, 10, 5, 0)));

        assertThat(history.get(2).getRequestStatusResult(),
            equalTo(RequestStatusResult.of(RequestStatus.IN_PROGRESS)));
        assertThat(history.get(2).getDate(), equalTo(LocalDateTime.of(2017, 1, 1, 10, 7, 0)));

        assertThat(history.get(3).getRequestStatusResult(),
            equalTo(RequestStatusResult.of(RequestStatus.PROCESSED)));
        assertThat(history.get(3).getDate(), equalTo(LocalDateTime.of(2017, 1, 1, 10, 10, 0)));
    }


    @Test
    @DatabaseSetup("classpath:tms/load-request-details/before-load-transfer-details-test.xml")
    void getTransferDetails() {
        final ResourceId resourceId = new ResourceId.ResourceIdBuilder()
            .setYandexId(REQ_ID_1.toString())
            .setPartnerId(REQ_EXT_ID_1)
            .build();

        final List<TransferDetailsItem> items = Lists.newArrayList(
            transferDetailsItem("art1", 1, 1),
            transferDetailsItem("art2", 2, 2),
            transferDetailsItemWithInstances("art3", 3, 2)
        );
        final TransferDetails transferDetails = transferDetails(RES_ID_1, items);

        when(fulfillmentClient.getTransferDetails(resourceId, PARTNER_1))
            .thenReturn(transferDetails);

        ShopRequest request = request(REQ_ID_1, REQ_EXT_ID_1, RequestType.TRANSFER, SERVICE_ID_1);
        final Map<SupplierSkuKeyWithOrderId, ItemDetails> details = service.getRequestDetails(request);
        verify(fulfillmentClient).getTransferDetails(resourceId, PARTNER_1);

        assertThat(details, notNullValue());
        assertThat(details.size(), equalTo(3));
        assertThat(details.get(new SupplierSkuKeyWithOrderId(SUPPLIER_ID, "art1")),
            equalTo(new ItemDetails(new ItemCountDetails("art1", 1, 1, 0, 0))));
        assertThat(details.get(new SupplierSkuKeyWithOrderId(SUPPLIER_ID, "art2")),
            equalTo(new ItemDetails(new ItemCountDetails("art2", 2, 2, 0, 0))));
        assertThat(details.get(new SupplierSkuKeyWithOrderId(SUPPLIER_ID, "art3")),
                equalTo(new ItemDetails(
                        new ItemCountDetails("art3", 3, 2, 0, 0),
                        Set.of(Identifier.builder()
                                .itemId(3L)
                                .type(IdentifierType.RECEIVED)
                                .identifiers(RegistryUnitId.of(
                                        UnitPartialId.builder()
                                                .type(RegistryUnitIdType.PALLET_ID)
                                                .value("pallet-one")
                                                .build(),
                                        UnitPartialId.builder()
                                                .type(RegistryUnitIdType.CIS)
                                                .value("cis-one")
                                                .build(),
                                        UnitPartialId.builder()
                                                .type(RegistryUnitIdType.PALLET_ID)
                                                .value("pallet-two")
                                                .build(),
                                        UnitPartialId.builder()
                                                .type(RegistryUnitIdType.CIS)
                                                .value("cis-two")
                                                .build()
                                )).build()))
                        ));
    }

    private static TransferStatus transferStatus(final ResourceId resourceId, final TransferStatusType statusType,
                                                 final DateTime dateTime) {
        return new TransferStatus(resourceId, new TransferStatusEvent(statusType, dateTime));
    }

    private static TransferStatusEvent transferEvent(final TransferStatusType statusType,
                                                     final DateTime dateTime) {
        return new TransferStatusEvent(statusType, dateTime);
    }

    private static ShopRequest request(long id, String externalId, RequestType requestType, long serviceId) {
        final ShopRequest request = new ShopRequest();
        request.setId(id);
        request.setServiceRequestId(externalId);
        request.setType(requestType);
        request.setServiceId(serviceId);
        request.setSupplier(supplier());
        return request;
    }

    private static Supplier supplier() {
        return new Supplier(
            SUPPLIER_ID,
            "test_supplier",
            "test_organization",
            0L,
            SupplierType.FIRST_PARTY,
            new SupplierBusinessType()
        );
    }

    private static TransferDetails transferDetails(ResourceId resourceId, List<TransferDetailsItem> items) {
        return new TransferDetails(resourceId, items);
    }

    private static TransferDetailsItem transferDetailsItem(String article, int declared, int actual) {
        final UnitId unitId = new UnitId(null, SUPPLIER_ID, article);
        return new TransferDetailsItem(unitId, declared, actual);
    }

    private static TransferDetailsItem transferDetailsItemWithInstances(String article, int declared, int actual) {
        TransferDetailsItem transferDetailsItem = transferDetailsItem(article, declared, actual);
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

    private static Partner partner(long id) {
        return new Partner(id);
    }

}
