package ru.yandex.market.ff.service.implementation;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.exception.FulfillmentWorkflowException;
import ru.yandex.market.ff.model.bo.Warehouse;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class LmsClientWarehouseServiceTest extends IntegrationTest {

    @Autowired
    private LmsClientWarehouseService lmsClientWarehouseService;

    @Test
    void fetchAndConvertWarehouse() {
        when(lmsClient.getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .partnerIds(Collections.singleton(123L))
            .type(PointType.WAREHOUSE)
            .build())))
            .thenReturn(Collections.singletonList(getWarehouseLogisticsPointResponse()));

        Warehouse warehouse = lmsClientWarehouseService.getWarehouse(123);

        verify(lmsClient).getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .partnerIds(Collections.singleton(123L))
            .type(PointType.WAREHOUSE)
            .build()));
        verifyNoMoreInteractions(lmsClient);

        assertions.assertThat(warehouse)
            .as("Asserting that the warehouse is valid")
            .isEqualToComparingFieldByFieldRecursively(getExpectedWarehouse());
    }

    @Test
    void fetchAndConvertWarehouseWithoutPhone() {
        when(lmsClient.getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .partnerIds(Collections.singleton(123L))
            .type(PointType.WAREHOUSE)
            .build())))
            .thenReturn(Collections.singletonList(getWarehouseLogisticsPointResponseWithoutPhone()));

        Warehouse warehouse = lmsClientWarehouseService.getWarehouse(123);

        verify(lmsClient).getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .partnerIds(Collections.singleton(123L))
            .type(PointType.WAREHOUSE)
            .build()));
        verifyNoMoreInteractions(lmsClient);

        assertions.assertThat(warehouse)
            .as("Asserting that the warehouse is valid")
            .isEqualToComparingFieldByFieldRecursively(getExpectedWarehouseWithoutPhone());
    }

    @Test
    void fetchAndConvertWarehouseWithoutId() {
        when(lmsClient.getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .partnerIds(Collections.singleton(123L))
            .type(PointType.WAREHOUSE)
            .build())))
            .thenReturn(Collections.singletonList(getWarehouseLogisticsPointResponseWithoutId()));

        Warehouse warehouse = lmsClientWarehouseService.getWarehouse(123);

        verify(lmsClient).getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .partnerIds(Collections.singleton(123L))
            .type(PointType.WAREHOUSE)
            .build()));
        verifyNoMoreInteractions(lmsClient);

        assertions.assertThat(warehouse)
            .as("Asserting that the warehouse is valid")
            .isEqualToComparingFieldByFieldRecursively(getExpectedWarehouseWithoutId());
    }

    @Test
    void fetchAndConvertWarehouseWithoutAddress() {
        when(lmsClient.getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .partnerIds(Collections.singleton(123L))
            .type(PointType.WAREHOUSE)
            .build())))
            .thenReturn(Collections.singletonList(getWarehouseLogisticsPointResponseWithoutAddress()));

        assertThrows(FulfillmentWorkflowException.class, () -> lmsClientWarehouseService.getWarehouse(123));

        verify(lmsClient).getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .partnerIds(Collections.singleton(123L))
            .type(PointType.WAREHOUSE)
            .build()));
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    void fetchAndConvertWarehouseWithoutName() {
        when(lmsClient.getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .partnerIds(Collections.singleton(123L))
            .type(PointType.WAREHOUSE)
            .build())))
            .thenReturn(Collections.singletonList(getWarehouseLogisticsPointResponseWithoutName()));

        assertThrows(FulfillmentWorkflowException.class, () -> lmsClientWarehouseService.getWarehouse(123));

        verify(lmsClient).getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .partnerIds(Collections.singleton(123L))
            .type(PointType.WAREHOUSE)
            .build()));
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    void fetchAndConvertWarehouseWhenNoLogisticPointsFound() {
        when(lmsClient.getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .partnerIds(Collections.singleton(123L))
            .type(PointType.WAREHOUSE)
            .build())))
            .thenReturn(Collections.emptyList());

        assertThrows(FulfillmentWorkflowException.class, () -> lmsClientWarehouseService.getWarehouse(123));

        verify(lmsClient).getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .partnerIds(Collections.singleton(123L))
            .type(PointType.WAREHOUSE)
            .build()));
        verifyNoMoreInteractions(lmsClient);
    }

    private Warehouse getExpectedWarehouse() {
        return Warehouse.builder().setId(1234L).setName("Rostov")
            .setAddress(ru.yandex.market.ff.model.bo.Address.builder()
                .setCity("Котельники").setStreet("Яничкин проезд").setNumber("7").setOther("терминал БД-6").build())
            .setPhone("+79165678901")
            .build();
    }

    private Warehouse getExpectedWarehouseWithoutPhone() {
        return Warehouse.builder().setId(1234L).setName("Rostov")
            .setAddress(ru.yandex.market.ff.model.bo.Address.builder()
                .setCity("Котельники").setStreet("Яничкин проезд").setNumber("7").setOther("терминал БД-6").build())
            .build();
    }

    private Warehouse getExpectedWarehouseWithoutId() {
        return Warehouse.builder().setName("Rostov")
            .setAddress(ru.yandex.market.ff.model.bo.Address.builder()
                .setCity("Котельники").setStreet("Яничкин проезд").setNumber("7").setOther("терминал БД-6").build())
            .setPhone("+79165678901")
            .build();
    }

    private LogisticsPointResponse getWarehouseLogisticsPointResponse() {
        return LogisticsPointResponse.newBuilder().id(1234L).name("Rostov").address(getAddress())
            .phones(Collections.singleton(getPhone())).build();
    }

    private LogisticsPointResponse getWarehouseLogisticsPointResponseWithoutPhone() {
        return LogisticsPointResponse.newBuilder().id(1234L).name("Rostov").address(getAddress()).build();
    }

    private LogisticsPointResponse getWarehouseLogisticsPointResponseWithoutId() {
        return LogisticsPointResponse.newBuilder().name("Rostov").address(getAddress())
            .phones(Collections.singleton(getPhone())).build();
    }

    private LogisticsPointResponse getWarehouseLogisticsPointResponseWithoutAddress() {
        return LogisticsPointResponse.newBuilder().id(1234L).name("Rostov").phones(Collections.singleton(getPhone()))
            .build();
    }

    private LogisticsPointResponse getWarehouseLogisticsPointResponseWithoutName() {
        return LogisticsPointResponse.newBuilder().id(1234L).address(getAddress())
            .phones(Collections.singleton(getPhone())).build();
    }

    private Address getAddress() {
        return Address.newBuilder()
            .settlement("Котельники")
            .street("Яничкин проезд")
            .house("7")
            .comment("терминал БД-6")
            .build();
    }

    private Phone getPhone() {
        return new Phone("+79165678901", null, null, null);
    }
}
