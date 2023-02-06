package ru.yandex.market.admin.service.remote;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.mapping.UIDeliveryServiceType;
import ru.yandex.market.admin.ui.model.supplier.UIFulfillmentService;
import ru.yandex.market.admin.ui.model.supplier.UISupplier;
import ru.yandex.market.admin.ui.model.supplier.UISupplierSearch;
import ru.yandex.market.admin.ui.service.PassportUIService;
import ru.yandex.market.admin.ui.service.SortOrder;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для {@link RemoteSupplierUIService}.
 *
 * @author avetokhin 20.08.18.
 */
class RemoteSupplierUIServiceTest extends FunctionalTest {

    private static final UISupplier SUPPLIER_1 = ffSupplier(1, "Supplier1", -1, 100, 10);
    private static final UISupplier SUPPLIER_2 = ffSupplier(2, "Supplier2", 101, 200, 20);

    private static final UIFulfillmentService FF_1 =
            ffService(101, "Marschroute FF", false, UIDeliveryServiceType.FULFILLMENT);
    private static final UIFulfillmentService FF_1_WITH_INLET =
            ffService(101, "Marschroute FF", false, UIDeliveryServiceType.FULFILLMENT, 1000);
    private static final UIFulfillmentService FF_2 =
            ffService(102, "Rostov FF", false, UIDeliveryServiceType.FULFILLMENT);

    private static final Set<UIDeliveryServiceType> FF_TYPES = Sets.newHashSet(UIDeliveryServiceType.FULFILLMENT,
            UIDeliveryServiceType.CROSSDOCK, UIDeliveryServiceType.DROPSHIP);

    @Autowired
    private RemoteSupplierUIService supplierUIService;

    @Autowired
    private PassportUIService remotePassportService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    private static UIFulfillmentService ffService(long id, String name, boolean isExpress, UIDeliveryServiceType type,
                                                  long... inletIds) {
        var service = new UIFulfillmentService();
        service.setField(UIFulfillmentService.ID, id);
        service.setField(UIFulfillmentService.NAME, name);
        service.setField(UIFulfillmentService.INLET_IDS,
                Arrays.stream(inletIds).mapToObj(String::valueOf).collect(Collectors.joining(", ")));
        service.setField(UIFulfillmentService.IS_EXPRESS, isExpress);
        service.setField(UIFulfillmentService.WAREHOUSE_TYPE, type);
        return service;
    }

    private static UISupplierSearch ffSupplierSearch(long id, String name, long businessId) {
        var supplier = new UISupplierSearch();
        supplier.setField(UISupplierSearch.ID, id);
        supplier.setField(UISupplierSearch.NAME, String.valueOf(name));
        supplier.setField(UISupplierSearch.BUSINESS_ID,businessId);
        return supplier;
    }

    private static UISupplier ffSupplier(long id, String name, long managerId, long campaignId, long clientId) {
        var supplier = new UISupplier();
        supplier.setField(UISupplier.ID, id);
        supplier.setField(UISupplier.NAME, name);
        supplier.setField(UISupplier.MANAGER_ID, managerId);
        supplier.setField(UISupplier.CAMPAIGN_ID, campaignId);
        supplier.setField(UISupplier.CLIENT_ID, clientId);
        return supplier;
    }

    @Test
    @DbUnitDataSet(before = "RemoteSupplierUIServiceTest.before.csv")
    void getSupplier() {
        assertThat(supplierUIService.getSupplier(1L)).isEqualTo(SUPPLIER_1);
        assertThat(supplierUIService.getSupplier(2L)).isEqualTo(SUPPLIER_2);
    }

    @DisplayName("Поиск ФФ сервисов. Постраничный, страница 1")
    @Test
    @DbUnitDataSet(before = "RemoteSupplierUIServiceTest.before.csv")
    void searchFulfillmentServicesPage1() {
        var services = supplierUIService.searchFulfillmentServices(null, 0, 1, FF_TYPES);
        assertThat(services).containsExactlyInAnyOrder(
                FF_1
        );
    }

    @DisplayName("Поиск ФФ сервисов. Постраничный, страница 2")
    @Test
    @DbUnitDataSet(before = "RemoteSupplierUIServiceTest.before.csv")
    void searchFulfillmentServicesPage2() {
        var services = supplierUIService.searchFulfillmentServices(null, 1, 2, FF_TYPES);
        assertThat(services).containsExactlyInAnyOrder(
                FF_2
        );
    }

    @DisplayName("Поиск ФФ сервисов. По названию. Регистронезависимый")
    @Test
    @DbUnitDataSet(before = "RemoteSupplierUIServiceTest.before.csv")
    void searchFulfillmentServicesByName() {
        var services = supplierUIService.searchFulfillmentServices("ros", 0, 10, FF_TYPES);
        assertThat(services).containsExactlyInAnyOrder(
                FF_2
        );
    }

    @DisplayName("Поиск ФФ сервисов. По ID")
    @Test
    @DbUnitDataSet(before = "RemoteSupplierUIServiceTest.before.csv")
    void searchFulfillmentServicesById() {
        var services = supplierUIService.searchFulfillmentServices("101", 0, 10, FF_TYPES);
        assertThat(services).containsExactlyInAnyOrder(
                FF_1
        );
    }

    @Test
    @DbUnitDataSet(before = "RemoteSupplierUIServiceTest.before.csv")
    void getAvailableFulfillmentServices() {
        var services = supplierUIService.getAvailableFulfillmentServices(1L);
        assertThat(services).containsExactlyInAnyOrder(
                FF_1_WITH_INLET,
                FF_2
        );

        services = supplierUIService.getAvailableFulfillmentServices(2L);
        assertThat(services).containsExactlyInAnyOrder(
                FF_1_WITH_INLET
        );
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteSupplierUIServiceTest.before.csv",
            after = "RemoteSupplierUIServiceTest.after.save.csv")
    void saveSupplierFulfillmentServicesLink() {
        long marketId = 123;

        // mock ответа ЛМС на запрос о складе
        var responseBuilder
                = EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class);
        responseBuilder.marketId(marketId);
        responseBuilder.id(102L);
        var getPartnerResp = responseBuilder.build();
        when(lmsClient.getPartner(102L)).thenReturn(Optional.of(getPartnerResp));

        // mock ответа MarketID на запрос о ид поставщика
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            var marketAccount = MarketAccount.newBuilder().setMarketId(marketId).build();
            var response = GetByPartnerResponse.newBuilder()
                    .setMarketId(marketAccount).setSuccess(true).build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(any(), any());

        supplierUIService.saveSupplierFulfillmentServicesLink(2L, 102L);
    }

    @Test
    @DbUnitDataSet(
            before = "RemoteSupplierUIServiceTest.after.save.csv",
            after = "RemoteSupplierUIServiceTest.removeSupplierLink.after.csv")
    void removeSupplierFulfillmentServicesLink() {
        supplierUIService.removeSupplierFulfillmentServicesLink(2L, 102L);
    }

    @Test
    @DbUnitDataSet(before = "RemoteSupplierUIServiceTest.before.csv")
    void searchSuppliers() {
        var suppliers = supplierUIService.searchSuppliers("supp", UIFulfillmentService.ID,
                SortOrder.DESC, 0, 1);
        var supplier = ffSupplierSearch(2, "Supplier2", 10);
        assertThat(suppliers).containsExactlyInAnyOrder(supplier);
    }
}
