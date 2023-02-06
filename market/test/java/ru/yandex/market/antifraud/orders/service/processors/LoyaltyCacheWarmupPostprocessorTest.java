package ru.yandex.market.antifraud.orders.service.processors;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.service.LoyaltyDataService;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author dzvyagin
 */
public class LoyaltyCacheWarmupPostprocessorTest {

    @Test
    public void process() {
        LoyaltyDataService loyaltyDataService = mock(LoyaltyDataService.class);
        OrderCheckPostprocessor postprocessor = new LoyaltyCacheWarmupPostprocessor(loyaltyDataService);
        postprocessor.process(
                MultiCartRequestDto.builder().buyer(OrderBuyerRequestDto.builder().uid(123L).build()).build(),
                null,
                null);
        verify(loyaltyDataService).warmupCache(eq(123L));
    }

}
