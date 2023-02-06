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
import ru.yandex.market.tpl.core.test.TestTplOrderFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UitSetEqualityOrderUpdateItemsValidatorUnitTest {

    public static final String EXISTED_EXTERNAL_ORDER_ID_1 = "EXISTED_EXTERNAL_ORDER_ID_1";
    public static final String EXISTED_EXTERNAL_ORDER_ID_2 = "EXISTED_EXTERNAL_ORDER_ID_2";

    @Mock
    private UserShiftRepository userShiftRepository;
    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private UitSetEqualityOrderUpdateItemsValidator validator;

    @Test
    void validate_success_allOrdersSame() {
        //given
        User mockedUser = mock(User.class);
        UserShift mockedUserShift = mock(UserShift.class);

        when(userShiftRepository.findCurrentShift(mockedUser)).thenReturn(Optional.of(mockedUserShift));
        List<Order> toBeReturned = List.of(
                TestTplOrderFactory.buildOrder(EXISTED_EXTERNAL_ORDER_ID_2, List.of("uit2_1", "uit2_2")),
                TestTplOrderFactory.buildOrder(EXISTED_EXTERNAL_ORDER_ID_1, List.of("uit1_1", "uit1_2"))
        );
        doReturn(toBeReturned).when(orderRepository).findAllByUserShifts(eq(Collections.singletonList(mockedUserShift)));


        //then
        validator.validate(buildRequest(), mockedUser);
    }

    @Test
    void validate_failure_ordersDifferent() {
        //given
        User mockedUser = mock(User.class);
        UserShift mockedUserShift = mock(UserShift.class);

        when(userShiftRepository.findCurrentShift(mockedUser)).thenReturn(Optional.of(mockedUserShift));
        List<Order> toBeReturned = List.of(
                TestTplOrderFactory.buildOrder(EXISTED_EXTERNAL_ORDER_ID_2, List.of("uit2_1", "uit2_2"))
        );
        doReturn(toBeReturned).when(orderRepository).findAllByUserShifts(eq(Collections.singletonList(mockedUserShift)));


        //then
        assertThrows(TplInvalidActionException.class, () -> validator.validate(buildRequest(), mockedUser));
    }

    @Test
    void validate_failure_ordersUitsDifferent() {
        //given
        User mockedUser = mock(User.class);
        UserShift mockedUserShift = mock(UserShift.class);

        when(userShiftRepository.findCurrentShift(mockedUser)).thenReturn(Optional.of(mockedUserShift));
        List<Order> toBeReturned = List.of(
                TestTplOrderFactory.buildOrder(EXISTED_EXTERNAL_ORDER_ID_2, List.of("uit2_1", "uit2_2")),
                TestTplOrderFactory.buildOrder(EXISTED_EXTERNAL_ORDER_ID_1, List.of("uit1_2"))
        );
        doReturn(toBeReturned).when(orderRepository).findAllByUserShifts(eq(Collections.singletonList(mockedUserShift)));


        //then
        assertThrows(TplInvalidActionException.class, () -> validator.validate(buildRequest(), mockedUser));
    }

    @Test
    void validate_failure_ordersSetsTheSame_butDifferent() {
        //given
        User mockedUser = mock(User.class);
        UserShift mockedUserShift = mock(UserShift.class);

        when(userShiftRepository.findCurrentShift(mockedUser)).thenReturn(Optional.of(mockedUserShift));
        List<Order> toBeReturned = List.of(
                TestTplOrderFactory.buildOrder(EXISTED_EXTERNAL_ORDER_ID_2, List.of("uit2_1", "uit2_2", "uit1_1")),
                TestTplOrderFactory.buildOrder(EXISTED_EXTERNAL_ORDER_ID_1, List.of("uit1_2"))
        );
        doReturn(toBeReturned).when(orderRepository).findAllByUserShifts(eq(Collections.singletonList(mockedUserShift)));


        //then
        assertThrows(TplInvalidActionException.class, () -> validator.validate(buildRequest(), mockedUser));
    }

    private UpdateItemsInstancesPurchaseStatusRequestDto buildRequest() {
        return UpdateItemsInstancesPurchaseStatusRequestDto
                .builder()
                .orders(List.of(
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(EXISTED_EXTERNAL_ORDER_ID_1,
                                List.of("uit1_1", "uit1_2"),
                                null,
                                null),
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(EXISTED_EXTERNAL_ORDER_ID_2, List.of(
                                "uit2_1"), List.of("uit2_2"), null))
                )
                .build();
    }
}
