package ru.yandex.market.billing.tasks.delivery;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseFilter;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.ExtendedShipmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.misc.algo.ht.examples.OpenHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.yt.YtUtil.stringNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.longNode;
import static ru.yandex.market.mbi.yt.YtUtilTest.yPathHasPath;

@ExtendWith(MockitoExtension.class)
class LoadDeliveryServicesExecutorFunctionalTest extends FunctionalTest {
    @Autowired
    private LMSClient lmsClient;

    @Autowired
    @Qualifier("loadDeliveryServicesPgExecutor")
    private Executor loadDeliveryServicesExecutor;

    @Autowired
    @Qualifier("logisticPartnerRelationsYtCluster")
    private YtCluster logisticPartnerRelationsYtCluster;

    @Mock
    private Yt yt;

    @Mock
    private YtTables ytTables;


    @AfterEach
    void onAfter() {
        verifyNoMoreInteractions(logisticPartnerRelationsYtCluster);
        verifyNoMoreInteractions(yt);
    }

    @Test
    @DbUnitDataSet(
            before = {"db/deliveryDataBefore.csv", "db/searchBusinessWarehouses.before.csv"},
            after = "db/testDoJobWithSearchBusinessWarehouses.after.csv"
    )
    void testLoadRelationsFromYT() {
        initLmsPartners();
        initYt("//home/market/testing/delivery/logistics_management_system/dropship_logistic_movements", List.of(
                // выбрасывается
                buildRelation(101L, 777L, 780L, null, ShipmentType.WITHDRAW.name(), "active", null),
                buildRelation(102L, 778L, 779L, null, ShipmentType.WITHDRAW.name(), "active", "15:00:00"),
                // обновляется
                buildRelation(103L, 666L, 667L, 666L, ShipmentType.IMPORT.name(), "active", ""),
                // добавляются
                buildRelation(104L, 667L, 780L, 667L, ShipmentType.WITHDRAW.name(), "active", "12:00:00"),
                buildRelation(105L, 780L, 666L, null, null, "active", "12:00:00"),
                buildRelation(106L, 779L, 10000L, null, null, "active", "12:00:00"), // to_partner не того типа (например, служба доставки)
                buildRelation(107L, 667L, 780L, 667L, ShipmentType.WITHDRAW.name(), "inactive", "12:00:00")
        ));

        when(logisticPartnerRelationsYtCluster.getYt()).thenReturn(yt);

        loadDeliveryServicesExecutor.doJob(null);

        verifyLmsPartners();
        verify(logisticPartnerRelationsYtCluster).getYt();
        verify(yt).tables();
    }

    private static YTreeMapNodeImpl buildRelation(Object... args) {
        int i = 0;
        final YTreeMapNodeImpl node = new YTreeMapNodeImpl(new OpenHashMap<>());
        Optional.ofNullable(args[i++]).ifPresent(v -> node.put("movement_id", longNode((Long) v)));
        Optional.ofNullable(args[i++]).ifPresent(v -> node.put("from_partner_id", longNode((Long) v)));
        Optional.ofNullable(args[i++]).ifPresent(v -> node.put("to_partner_id", longNode((Long) v)));
        Optional.ofNullable(args[i++]).ifPresent(v -> node.put("to_logistics_point_id", longNode((Long) v)));
        Optional.ofNullable(args[i++]).ifPresent(v -> node.put("shipment_type", stringNode((String) v)));
        Optional.ofNullable(args[i++]).ifPresent(v -> node.put("movement_status", stringNode((String) v)));
        Optional.ofNullable(args[i]).ifPresent(v -> node.put("cutoff_time", stringNode((String) v)));
        return node;
    }

    @SuppressWarnings("unchecked")
    private void initYt(final String tablePath, final List<YTreeMapNodeImpl> ytReturnValue) {
        doAnswer(invocation -> {
            final Consumer<YTreeMapNode> consumer = invocation.getArgument(2);
            ytReturnValue.forEach(consumer);
            return null;
        }).when(ytTables).read(
                argThat(ytPath -> yPathHasPath(ytPath, tablePath)),
                any(YTableEntryType.class),
                any(Consumer.class)
        );

        when(yt.tables()).thenReturn(ytTables);
        when(logisticPartnerRelationsYtCluster.getYt()).thenReturn(yt);
    }

    private void verifyLmsPartners() {
        verify(lmsClient).getBusinessWarehouses(
                eq(BusinessWarehouseFilter.newBuilder()
                        .types(Set.of(PartnerType.FULFILLMENT, PartnerType.DROPSHIP, PartnerType.SUPPLIER,
                                PartnerType.DROPSHIP_BY_SELLER, PartnerType.SORTING_CENTER))
                        .build()),
                eq(0),
                eq(500));
        verify(lmsClient).searchPartners(
                eq(SearchPartnerFilter.builder().setTypes(Set.of(PartnerType.DELIVERY)).build()));
    }

    private void initLmsPartners() {
        doReturn(new PageResult<BusinessWarehouseResponse>()
                .setData(getWarehouses())
                .setPage(0)
                .setTotalPages(1))
                .when(lmsClient).getBusinessWarehouses(
                        eq(BusinessWarehouseFilter.newBuilder()
                                .types(Set.of(PartnerType.FULFILLMENT, PartnerType.DROPSHIP, PartnerType.SUPPLIER,
                                        PartnerType.DROPSHIP_BY_SELLER, PartnerType.SORTING_CENTER))
                                .build()),
                        any(),
                        any());

        when(lmsClient.searchPartners(SearchPartnerFilter.builder()
                .setTypes(Sets.newHashSet(PartnerType.DELIVERY))
                .build()))
                .thenReturn(getLmsDeliveryServices());
    }

    private static List<BusinessWarehouseResponse> getWarehouses() {
        return ImmutableList.<BusinessWarehouseResponse>builder()
                .add(buildBusinessFulfillmentService(PartnerType.FULFILLMENT, PartnerStatus.ACTIVE, 1, "Крутой ФФ", 777,
                        "Cool FF", null, null, null, null))
                .add(buildBusinessFulfillmentService(PartnerType.DROPSHIP, PartnerStatus.ACTIVE, 1, "Крутой Дропшип",
                        778, "Cool dropship", "Logistics point dropship", ExtendedShipmentType.EXPRESS,
                        Address.newBuilder()
                                .settlement("Москва")
                                .region("Москва")
                                .street("Льва Толстого")
                                .postCode("111111")
                                .locationId(213)
                                .build(),
                        List.of(new PartnerExternalParam(PartnerExternalParamType.DROPSHIP_EXPRESS.name(), null, "1"),
                                new PartnerExternalParam(PartnerExternalParamType.IS_DROPOFF.name(),
                                        "Является ли служба дропоффом", "1"))))
                .add(buildBusinessFulfillmentService(PartnerType.SUPPLIER, PartnerStatus.ACTIVE, null,
                        "Крутой Кроссдок", 779, "Cool crossdock", null, ExtendedShipmentType.IMPORT,
                        Address.newBuilder().settlement("Рязань").build(), List.of()))
                .add(buildBusinessFulfillmentService(PartnerType.FULFILLMENT, PartnerStatus.ACTIVE, 1, null, 780,
                        "Null name", null, null, Address.newBuilder().build(), null))
                .addAll(getBusinessSortingCenters())
                .build();
    }

    private static BusinessWarehouseResponse buildBusinessFulfillmentService(
            PartnerType partnerType, PartnerStatus status, Integer homeRegionId, String humanReadableId,
            Integer marketDeliveryServiceId, String name, String logisticsPointName, ExtendedShipmentType shipmentType,
            Address address, List<PartnerExternalParam> params) {
        return BusinessWarehouseResponse.newBuilder()
                .partnerStatus(status)
                .locationId(homeRegionId)
                .readableName(humanReadableId)
                .partnerId(Long.valueOf(marketDeliveryServiceId))
                .name(name)
                .logisticsPointName(logisticsPointName)
                .partnerType(partnerType)
                .partnerParams(params)
                .address(address)
                .shipmentType(shipmentType)
                .build();
    }

    private static List<BusinessWarehouseResponse> getBusinessSortingCenters() {
        return List.of(
                BusinessWarehouseResponse.newBuilder()
                        .partnerStatus(PartnerStatus.ACTIVE)
                        .locationId(2)
                        .readableName("Обновляемый СЦ")
                        .partnerId(666L)
                        .name("2")
                        .partnerType(PartnerType.SORTING_CENTER)
                        .build(),
                BusinessWarehouseResponse.newBuilder()
                        .partnerStatus(PartnerStatus.ACTIVE)
                        .locationId(213)
                        .readableName("Второй СЦ")
                        .partnerId(667L)
                        .name("2")
                        .partnerType(PartnerType.SORTING_CENTER)
                        .partnerParams(List.of(
                                new PartnerExternalParam(PartnerExternalParamType.SHIPMENT_TIME_LIMIT.name(),
                                        "\"Отсечка\". Время до которого можно принимать заказы с ближайшей датой " +
                                                "отгрузки.",
                                        "21:00:00"
                                )
                        ))
                        .build()
        );
    }

    private static List<PartnerResponse> getLmsDeliveryServices() {
        return List.of(
                PartnerResponse.newBuilder()
                        .status(PartnerStatus.ACTIVE)
                        .readableName("НоваяСлужба")
                        .id(123456)
                        .name("НоваяСлужба")
                        .partnerType(PartnerType.DELIVERY)
                        .rating(3)
                        .build(),
                PartnerResponse.newBuilder()
                        .status(PartnerStatus.ACTIVE)
                        .readableName("some_updated_carrier")
                        .id(9999999)
                        .name("some_updated_carrier")
                        .partnerType(PartnerType.DELIVERY)
                        .rating(7)
                        .build()
        );
    }

}
