package ru.yandex.market.tpl.core.service.partial_return.validator;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.UpdateItemsInstancesPurchaseStatusRequestDto;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.test.TestTplApiRequestFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserAccessibilityOrderUpdateItemsValidatorUnitTest {
    public static final String EXISTED_EXTERNAL_ORDER_ID_1 = "EXISTED_EXTERNAL_ORDER_ID_1";
    public static final String EXISTED_EXTERNAL_ORDER_ID_2 = "EXISTED_EXTERNAL_ORDER_ID_2";
    @Mock
    private UserShiftRepository userShiftRepository;
    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private UserAccessibilityOrderUpdateItemsValidator validator;

    @Test
    void validate_success_allOrdersSame() {
        //given
        User mockedUser = mock(User.class);
        UserShift mockedUserShift = mock(UserShift.class);

        when(userShiftRepository.findCurrentShift(mockedUser)).thenReturn(Optional.of(mockedUserShift));
        List<Order> toBeReturned = List.of(
                buildOrder(EXISTED_EXTERNAL_ORDER_ID_2),
                buildOrder(EXISTED_EXTERNAL_ORDER_ID_1)
        );
        doReturn(toBeReturned).when(orderRepository).findAllByUserShifts(eq(Collections.singletonList(mockedUserShift)));


        //then
        validator.validate(UpdateItemsInstancesPurchaseStatusRequestDto
                .builder()
                .orders(List.of(
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(EXISTED_EXTERNAL_ORDER_ID_1),
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(EXISTED_EXTERNAL_ORDER_ID_2))
                )
                .build(), mockedUser);
    }
    @Test
    void validate_success_requestedOrdersAllInUserShift() {
        //given
        User mockedUser = mock(User.class);
        UserShift mockedUserShift = mock(UserShift.class);

        when(userShiftRepository.findCurrentShift(mockedUser)).thenReturn(Optional.of(mockedUserShift));
        List<Order> toBeReturned = List.of(
                buildOrder(EXISTED_EXTERNAL_ORDER_ID_2),
                buildOrder(EXISTED_EXTERNAL_ORDER_ID_1)
        );
        doReturn(toBeReturned).when(orderRepository).findAllByUserShifts(eq(Collections.singletonList(mockedUserShift)));


        //then
        validator.validate(UpdateItemsInstancesPurchaseStatusRequestDto
                .builder()
                .orders(List.of(
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(EXISTED_EXTERNAL_ORDER_ID_1),
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(EXISTED_EXTERNAL_ORDER_ID_2))
                )
                .build(), mockedUser);
    }

    @Test
    void validate_failure() {
        //given
        User mockedUser = mock(User.class);
        UserShift mockedUserShift = mock(UserShift.class);

        when(userShiftRepository.findCurrentShift(mockedUser)).thenReturn(Optional.of(mockedUserShift));
        List<Order> toBeReturned = List.of(
                buildOrder(EXISTED_EXTERNAL_ORDER_ID_1)
        );
        doReturn(toBeReturned).when(orderRepository).findAllByUserShifts(eq(Collections.singletonList(mockedUserShift)));


        //then
        assertThrows(TplInvalidActionException.class, ()->validator.validate(UpdateItemsInstancesPurchaseStatusRequestDto
                .builder()
                .orders(List.of(
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(EXISTED_EXTERNAL_ORDER_ID_1),
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(EXISTED_EXTERNAL_ORDER_ID_2))
                )
                .build(), mockedUser));
    }

    private Order buildOrder(String externalOrderId) {
        Order mockedOrder = mock(Order.class);
        when(mockedOrder.getExternalOrderId()).thenReturn(externalOrderId);
        return mockedOrder;
    }
}
