package ru.yandex.market.abo.core.premod.helper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.framework.message.MessageService;
import ru.yandex.common.framework.message.MessageTemplate;
import ru.yandex.market.abo.clch.ClchService;
import ru.yandex.market.abo.core.message.Messages;
import ru.yandex.market.abo.core.premod.PremodManager;
import ru.yandex.market.abo.core.premod.PremodTicketService;
import ru.yandex.market.abo.core.premod.helper.clone.model.DecisionParams;
import ru.yandex.market.abo.core.premod.model.CloneType;
import ru.yandex.market.abo.core.premod.model.PremodItem;
import ru.yandex.market.abo.core.premod.model.PremodItemStatus;
import ru.yandex.market.abo.core.premod.model.PremodItemType;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.premod.model.PremodTicketStatus;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.util.ErrorMessageException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 03/07/2020.
 */
public class CloneItemHelperUnitTest {
    private static final long ITEM_ID = 42342L;
    private static final long TICKET_ID = 12342L;
    private static final long USER_ID = 432235L;
    private static final Long SESSION_ID = 76786698L;
    private static final long SHOP_ID = 53463576L;

    @InjectMocks
    CloneItemHelper cloneItemHelper;
    @Mock
    ShopInfoService shopInfoService;
    @Mock
    PremodManager premodManager;
    @Mock
    ClchService clchService;
    @Mock
    MessageService messageService;
    @Mock
    PremodTicketService premodTicketService;

    @Mock
    PremodItem premodItem;
    @Mock
    PremodTicket ticket;
    @Mock
    ShopInfo shopInfo;
    @Mock
    MessageTemplate template;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(clchService.loadShopsBySessionId(anyLong())).thenReturn(List.of(11L, 111L, 1111L));
        when(shopInfoService.getShopInfo(SHOP_ID)).thenReturn(shopInfo);
        when(messageService.createMessageTemplate(anyInt(), anyMap())).thenReturn(template);
        when(premodTicketService.loadPremodTicket(TICKET_ID)).thenReturn(ticket);
        when(premodItem.getTicketId()).thenReturn(TICKET_ID);
    }

    @ParameterizedTest
    @EnumSource(value = CloneType.class)
    void approveClone(CloneType cloneType) {
        cloneItemHelper.approveClone(premodItem, SHOP_ID, decisionParams(cloneType), "");
        if (cloneType.canTurnOn()) {
            verify(premodItem).setStatus(PremodItemStatus.PASS);
            verify(premodManager).updatePremodItem(premodItem);
        } else {
            verify(premodItem).setStatus(PremodItemStatus.FAILED);
            verify(premodManager).markCloneByCloneItem(premodItem);
            verify(messageService).createMessageTemplate(
                    eq(Messages.PREMODERATION_CLONE_CAN_NOT_TURN), anyMap());
        }
    }

    @Test
    void validateItem() {
        assertThrows(IllegalStateException.class, () -> {
            when(premodItem.getType()).thenReturn(PremodItemType.SHOP_INFO_COLLECTED);
            cloneItemHelper.validatePremodParams(ticket, premodItem);
        });

        when(premodItem.getType()).thenReturn(PremodItemType.CLONE);
        assertThrows(ErrorMessageException.class, () -> {
            when(premodItem.getStatus()).thenReturn(PremodItemStatus.PASS);
            cloneItemHelper.validatePremodParams(ticket, premodItem);
        });

        when(premodItem.getStatus()).thenReturn(PremodItemStatus.NEW);

        assertThrows(ErrorMessageException.class, () -> cloneItemHelper.validatePremodParams(null, premodItem));
        assertThrows(ErrorMessageException.class, () -> {
            when(ticket.getStatus()).thenReturn(PremodTicketStatus.WAITING_FOR_APPROVE);
            cloneItemHelper.validatePremodParams(ticket, premodItem);
        });

        when(ticket.getStatus()).thenReturn(PremodTicketStatus.IN_PROGRESS);
        cloneItemHelper.validatePremodParams(ticket, premodItem);
    }

    @Test
    @SuppressWarnings("unchecked")
    void validateCluster() {
        assertThrows(ErrorMessageException.class, () -> cloneItemHelper.validateClusterLeaders("1, 3, null, 4", ""));

        when(shopInfoService.loadShopInfosById(any())).thenReturn(Collections.emptyList());
        assertThrows(ErrorMessageException.class, () -> cloneItemHelper.validateClusterLeaders("1, 2, 3", "4, 5, 6"));

        when(shopInfoService.loadShopInfosById(any())).then(inv -> {
            Collection<Long> shopIds = (Collection<Long>) inv.getArguments()[0];
            return shopIds.stream().map(id -> {
                ShopInfo shop = new ShopInfo();
                shop.setId(id);
                shop.setName("name-" + id);
                return shop;}).collect(Collectors.toList());
        });
        cloneItemHelper.validateClusterLeaders("1, 2, 3", "4, 5, 6");
    }

    private static DecisionParams decisionParams(CloneType cloneType) {
        return new DecisionParams(ITEM_ID, cloneType, USER_ID, SESSION_ID, "1, 2, 3", "why not", "5, 8, 13", "indeed");
    }
}
