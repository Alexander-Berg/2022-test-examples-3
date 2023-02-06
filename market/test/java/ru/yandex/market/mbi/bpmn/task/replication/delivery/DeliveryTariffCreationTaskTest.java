package ru.yandex.market.mbi.bpmn.task.replication.delivery;


import java.math.BigDecimal;
import java.util.List;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.model.enums.shop.CourierTariffType;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.RegionGroupDto;
import ru.yandex.market.logistics.tarificator.open.api.client.api.model.ShopRegionGroupsDto;
import ru.yandex.market.mbi.bpmn.FunctionalTest;
import ru.yandex.market.mbi.bpmn.client.RetryableTarifficatorClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeliveryTariffCreationTaskTest extends FunctionalTest {

    @Autowired
    private DeliveryTariffCreationTask deliveryTariffCreationTask;

    @Autowired
    private RetryableTarifficatorClient tarifficatorClient;

    @Test
    @DisplayName("Создание дефолтного тарифа в тарификаторе")
    void testDefaultDeliveryTariffCreation() throws Exception {
        long shopId = 112;
        long uid = 123;

        DelegateExecution delegateExecution = Mockito.mock(DelegateExecution.class);

        when(delegateExecution.getVariable("partnerId")).thenReturn(shopId);
        when(delegateExecution.getVariable("uid")).thenReturn(uid);

        when(tarifficatorClient.getRegionGroups(anyLong()))
                .thenReturn(new ShopRegionGroupsDto()
                        .regionsGroups(List.of(new RegionGroupDto()
                                .selfRegion(true)
                                .id(999L))));

        deliveryTariffCreationTask.execute(delegateExecution);

        verify(tarifficatorClient).getRegionGroups(shopId);

        verify(tarifficatorClient).createTariff(eq(shopId), eq(uid), eq(999L), argThat(
                tariff -> tariff.getTariffType() == CourierTariffType.UNIFORM &&
                        tariff.getOptionsGroups() != null &&
                        tariff.getOptionsGroups().size() == 1 &&
                        tariff.getOptionsGroups().get(0).getHasDelivery() &&
                        tariff.getOptionsGroups().get(0).getOptions() != null &&
                        tariff.getOptionsGroups().get(0).getOptions().size() == 1 &&
                        BigDecimal.valueOf(99).equals(tariff.getOptionsGroups().get(0).getOptions().get(0).getCost()) &&
                        tariff.getOptionsGroups().get(0).getOptions().get(0).getDaysFrom().equals(4) &&
                        tariff.getOptionsGroups().get(0).getOptions().get(0).getDaysTo().equals(4) &&
                        tariff.getOptionsGroups().get(0).getOptions().get(0).getOrderBeforeHour().equals(13)));
    }

    @Test
    @DisplayName("Тарификатор не вернул региональные группы для магазина")
    void testCreateDeliveryTariffNoRegionGroups() {
        long shopId = 112;
        long uid = 123;

        DelegateExecution delegateExecution = Mockito.mock(DelegateExecution.class);

        when(delegateExecution.getVariable("partnerId")).thenReturn(shopId);
        when(delegateExecution.getVariable("uid")).thenReturn(uid);

        when(tarifficatorClient.getRegionGroups(anyLong()))
                .thenReturn(new ShopRegionGroupsDto());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> deliveryTariffCreationTask.execute(delegateExecution)
        );

        assertEquals(
                String.format("Failed to find self delivery region group for shop %s", shopId),
                exception.getMessage()
        );

        verify(tarifficatorClient).getRegionGroups(shopId);
    }
}
