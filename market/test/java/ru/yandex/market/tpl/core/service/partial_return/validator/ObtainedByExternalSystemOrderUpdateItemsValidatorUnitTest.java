package ru.yandex.market.tpl.core.service.partial_return.validator;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.UpdateItemsInstancesPurchaseStatusRequestDto;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrder;
import ru.yandex.market.tpl.core.domain.partial_return_order.state_processing_lrm.PartialReturnStateProcessingLrm;
import ru.yandex.market.tpl.core.domain.partial_return_order.state_processing_lrm.PartialReturnStateProcessingLrmQueryService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.TestTplApiRequestFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ObtainedByExternalSystemOrderUpdateItemsValidatorUnitTest {
    public static final String EXISTED_EXTERNAL_ORDER_ID_1 = "EXISTED_EXTERNAL_ORDER_ID_1";
    public static final String EXISTED_EXTERNAL_ORDER_ID_2 = "EXISTED_EXTERNAL_ORDER_ID_2";
    @Mock
    private PartialReturnStateProcessingLrmQueryService processingLrmQueryService;
    @InjectMocks
    private ObtainedByExternalSystemOrderUpdateItemsValidator validator;

    @Test
    void validate_success() {
        //given
        Set<String> externalOrderIds = Set.of(EXISTED_EXTERNAL_ORDER_ID_1, EXISTED_EXTERNAL_ORDER_ID_2);
        when(processingLrmQueryService.findAllByExternalOrderIds(eq(externalOrderIds)))
                .thenReturn(Collections.emptyList());

        //then
        validator.validate(UpdateItemsInstancesPurchaseStatusRequestDto
                .builder()
                .orders(List.of(
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(EXISTED_EXTERNAL_ORDER_ID_1),
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(EXISTED_EXTERNAL_ORDER_ID_2))
                )
                .build(), null
        );
    }

    @Test
    void validate_failure() {
        //given
        Set<String> externalOrderIds = Set.of(EXISTED_EXTERNAL_ORDER_ID_1, EXISTED_EXTERNAL_ORDER_ID_2);
        PartialReturnStateProcessingLrm processingLrm = buildPartialReturnProcessingLrm(EXISTED_EXTERNAL_ORDER_ID_1);
        when(processingLrmQueryService.findAllByExternalOrderIds(eq(externalOrderIds)))
                .thenReturn(Collections.singletonList(processingLrm));

        //then
        assertThrows(TplInvalidActionException.class, () -> validator.validate(UpdateItemsInstancesPurchaseStatusRequestDto
                .builder()
                .orders(List.of(
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(EXISTED_EXTERNAL_ORDER_ID_1),
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(EXISTED_EXTERNAL_ORDER_ID_2))
                )
                .build(), mock(User.class)
        ));
    }

    private PartialReturnStateProcessingLrm buildPartialReturnProcessingLrm(String externalOrderId) {
        return PartialReturnStateProcessingLrm
                .builder()
                .partialReturnOrder(PartialReturnOrder
                        .builder()
                        .order(buildOrder(externalOrderId))
                        .build())
                .build();
    }

    private Order buildOrder(String externalOrderId) {
        Order mockedOrder = mock(Order.class);
        when(mockedOrder.getExternalOrderId()).thenReturn(externalOrderId);
        return mockedOrder;
    }
}
