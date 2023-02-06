package ru.yandex.travel.orders.services.promo;

import java.util.EnumSet;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.commons.proto.EDisplayOrderType;
import ru.yandex.travel.orders.entities.promo.DiscountApplicationConfig;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DefaultPromoDiscountCalculator#applicableByMaxOrders(DiscountApplicationConfig, Long, EDisplayOrderType)}
 */
public class DefaultPromoDiscountCalculatorMaxOrdersRuleTest {
    private DefaultPromoDiscountCalculator calculator;
    private UserOrderCounterService service;
    private Long passportId;
    private DiscountApplicationConfig cfg;

    @Before
    public void setUp() {
        service = mock(UserOrderCounterService.class);
        calculator = new DefaultPromoDiscountCalculator(service);
        passportId = System.currentTimeMillis();

        cfg = new DiscountApplicationConfig();
        cfg.setFirstOrderOnlyFor(EnumSet.of(EDisplayOrderType.DT_HOTEL));
    }

    @Test
    public void shouldBeIgnoredIfNoMaxConfirmedHotelsDefined() {
        cfg.setMaxConfirmedHotelOrders(null);
        cfg.setFirstOrderOnlyFor(null);

        assertTrue(calculator.applicableByMaxOrders(cfg, passportId, EDisplayOrderType.DT_HOTEL));

        verifyNoMoreInteractions(service);
    }

    @Test
    public void shouldFailWhenNoPassportIdIsProvided() {
        assertFalse(calculator.applicableByMaxOrders(cfg, null, EDisplayOrderType.DT_HOTEL));

        verifyNoMoreInteractions(service);
    }

    @Test
    public void shouldPassForTheFirstOrder() {
        assertTrue(calculator.applicableByMaxOrders(cfg, passportId, EDisplayOrderType.DT_HOTEL));

        verify(service).userHasOrdersConfirmed(passportId, EDisplayOrderType.DT_HOTEL);
        verifyNoMoreInteractions(service);
    }

    @Test
    public void shouldNotPassForTheSecond() {
        when(service.userHasOrdersConfirmed(passportId, EDisplayOrderType.DT_HOTEL)).thenReturn(true);

        assertFalse(calculator.applicableByMaxOrders(cfg, passportId, EDisplayOrderType.DT_HOTEL));

        verify(service).userHasOrdersConfirmed(passportId, EDisplayOrderType.DT_HOTEL);
        verifyNoMoreInteractions(service);
    }
}
