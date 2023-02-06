package ru.yandex.market.tpl.core.service.task;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnQueryService;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderBox;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderQueryService;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.service.dropoff.MovementCargoCollector;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReturnItemsCollectorUnitTest {

    public static final long USER_ID = 10L;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MovementCargoCollector movementCargoCollector;
    @Mock
    private ClientReturnQueryService clientReturnQueryService;
    @Mock
    private PartialReturnOrderQueryService partialReturnOrderQueryService;
    @Mock
    private SortingCenterPropertyService sortingCenterPropertyService;
    @InjectMocks
    private ReturnItemsCollector returnItemsCollector;

    @Test
    void collectReturnItems() {
        //given
        User mockedUser = mock(User.class);
        when(mockedUser.getId()).thenReturn(USER_ID);
        UserShift userShift = mock(UserShift.class);
        when(userShift.getUser()).thenReturn(mockedUser);

        List<PartialReturnOrderBox> expectedPartialList = List.of(PartialReturnOrderBox.builder().build());
        List<ClientReturn> expectedClientList = List.of(ClientReturn.builder().build());
        Map<Long, List<DropoffCargo>> expectedMovementMap = Map.of(1L, List.of(new DropoffCargo()));
        List<Order> expectedOrderList = List.of(mock(Order.class));

        when(partialReturnOrderQueryService.findAllPartialReturnOrdersByUserShift(userShift))
                .thenReturn(expectedPartialList);
        when(clientReturnQueryService.getClientReturns(userShift))
                .thenReturn(expectedClientList);
        when(movementCargoCollector.collectFailedReturnCargosMap(userShift))
                .thenReturn(expectedMovementMap);
        when(orderRepository.findCurrentUserOrders(eq(USER_ID)))
                .thenReturn(expectedOrderList);
        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.SKIP_DIRECT_DROPOFF_SCAN_ON_SC_ENABLED), any())).thenReturn(false);
        //when
        ReturnItemsCollector.ReturnItems returnItems = returnItemsCollector.collectReturnItems(userShift);

        //then
        assertNotNull(returnItems);

        assertEquals(expectedPartialList, returnItems.getPartialReturnOrdersBoxes());
        assertEquals(expectedClientList, returnItems.getClientReturns());
        assertEquals(expectedMovementMap.entrySet(), returnItems.getDropOffReturnsByMovement());
        assertEquals(expectedOrderList, returnItems.getOrders());
    }

    @Test
    void isAnyItemToReturn_whenEmpty() {
        //given
        User mockedUser = mock(User.class);
        when(mockedUser.getId()).thenReturn(USER_ID);
        UserShift userShift = mock(UserShift.class);
        when(userShift.getUser()).thenReturn(mockedUser);

        //when
        boolean anyItemToReturn = returnItemsCollector.isAnyItemToReturn(userShift);

        //then
        assertFalse(anyItemToReturn);
    }

    @Test
    void isAnyItemToReturn_whenNotEmpty() {
        //given
        User mockedUser = mock(User.class);
        when(mockedUser.getId()).thenReturn(USER_ID);
        UserShift userShift = mock(UserShift.class);
        when(userShift.getUser()).thenReturn(mockedUser);

        when(movementCargoCollector.collectFailedReturnCargosMap(userShift))
                .thenReturn(Map.of(1L, List.of()));

        //when
        boolean anyItemToReturn = returnItemsCollector.isAnyItemToReturn(userShift);

        //then
        assertTrue(anyItemToReturn);
    }
}
