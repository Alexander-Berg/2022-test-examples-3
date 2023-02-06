package ru.yandex.market.abo.gen.modify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.exception.ExceptionalShopReason;
import ru.yandex.market.abo.core.exception.ExceptionalShopsService;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.Ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 12/4/13
 */
class TicketCheckMethodModifierTest {
    @Mock
    ShopInfoService shopInfoService;
    @Mock
    ExceptionalShopsService exceptionalShopsService;
    @Mock
    Ticket ticket;
    @InjectMocks
    TicketCheckMethodModifier ticketModifier;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(shopInfoService.getShopInfo(anyLong())).thenReturn(createShop(false));
    }

    @Test
    void testUsualTicketWithRussianShop() {
        when(shopInfoService.getShopCountry(anyLong())).thenReturn((long) Regions.RUSSIA);

        ticketModifier.modifyTicket(ticket);

        verify(ticket, never()).setCheckMethod(any());
    }

    @Test
    void testUsualTicketWithNotRussianShop() {
        when(shopInfoService.getShopCountry(anyLong())).thenReturn((long) Regions.KAZAKHSTAN);
        var captor = ArgumentCaptor.forClass(CheckMethod.class);

        ticketModifier.modifyTicket(ticket);

        verify(ticket).setCheckMethod(captor.capture());
        assertEquals(CheckMethod.DEFAULT, captor.getValue());
    }

    @ParameterizedTest
    @ValueSource(longs = {Regions.RUSSIA, Regions.KAZAKHSTAN})
    void testAutoOrder(long region) {
        when(shopInfoService.getShopCountry(anyLong())).thenReturn((long) region);
        when(ticket.getCheckMethod()).thenReturn(CheckMethod.AUTO_ORDER);

        ticketModifier.modifyTicket(ticket);

        verify(ticket, never()).setCheckMethod(any());
    }

    @Test
    void testException() {
        when(exceptionalShopsService.shopHasException(anyLong(), eq(ExceptionalShopReason.ONLY_VISUAL_CHECK)))
                .thenReturn(true);

        ticketModifier.modifyTicket(ticket);

        verify(ticket).setCheckMethod(CheckMethod.BY_SIGHT);
    }

    @Test
    void testSmb() {
        when(shopInfoService.getShopInfo(anyLong())).thenReturn(createShop(true));

        ticketModifier.modifyTicket(ticket);

        verify(ticket).setCheckMethod(CheckMethod.DEFAULT);
    }

    private static ShopInfo createShop(boolean smb) {
        var shop = new ShopInfo();
        shop.setSmb(smb);
        return shop;
    }
}
