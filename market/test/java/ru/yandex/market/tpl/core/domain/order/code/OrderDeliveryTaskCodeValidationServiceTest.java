package ru.yandex.market.tpl.core.domain.order.code;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.OrderChequeRemoteBatchDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.code.OrderCodeValidationDto;
import ru.yandex.market.tpl.api.model.order.code.OrderCodeValidationResponceDto;
import ru.yandex.market.tpl.common.util.exception.TplException;
import ru.yandex.market.tpl.common.util.validation.ValidRequestBodyList;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.code.exception.OrderDeliveryTaskNotValidOrderCodeException;
import ru.yandex.market.tpl.core.domain.task.TaskOrderDeliveryRepository;
import ru.yandex.market.tpl.core.domain.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.order.code.exception.OrderDeliveryTaskNotValidOrderCodeException.MESSAGE;
import static ru.yandex.market.tpl.core.domain.order.code.util.ModelBuilderTestUtil.buildB2bCustomersOrder;
import static ru.yandex.market.tpl.core.domain.order.code.util.ModelBuilderTestUtil.buildOrder;
import static ru.yandex.market.tpl.core.domain.order.code.util.ModelBuilderTestUtil.buildOrderDeliveryTask;
import static ru.yandex.market.tpl.core.domain.order.code.util.ModelBuilderTestUtil.buildUser;

@ExtendWith(MockitoExtension.class)
class OrderDeliveryTaskCodeValidationServiceTest {

    public static final long USER_ID = 1L;
    public static final String VERIFICATION_CODE = "verificationCode";
    public static final User USER = buildUser(USER_ID);
    public static final String NOT_VALID_VERIFICATION_CODE = "notValidVerificationCode";
    @InjectMocks
    private OrderDeliveryTaskCodeValidationService validationService;
    @Mock
    private OrderDeliveryCodeValidationService codeValidationService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TaskOrderDeliveryRepository taskOrderDeliveryRepository;

    @DisplayName("В проверке чеков не пришли чеки")
    @Test
    void validateEmptyCheckList() {
        ValidRequestBodyList<OrderChequeRemoteBatchDto> chequesDto = new ValidRequestBodyList<>(List.of());
        when(taskOrderDeliveryRepository.findAllById(eq(List.of()))).thenReturn(List.of());

        List<Long> validTaskIds = validationService.validateCodeAndGetValidTask(chequesDto, USER);

        assertThat(validTaskIds).isEmpty();
        verify(taskOrderDeliveryRepository, atLeastOnce()).findAllById(anyList());
        verify(orderRepository, never()).findAllWithProperties(anySet());
        verify(codeValidationService, never()).validate(any(OrderCodeValidationDto.class));
    }

    @DisplayName("Проверка заданий без orderDeliveryTask")
    @Test
    void validateNonDeliveryTaskByCheque() {
        List<OrderChequeRemoteBatchDto> chequeRemoteBatchList = List.of(
                buildOrderChequeRemoteBatchDto(1L, null, OrderChequeType.SELL),
                buildOrderChequeRemoteBatchDto(2L, null, OrderChequeType.SELL)
        );
        List<Long> taskIdsByCheque =
                chequeRemoteBatchList.stream()
                        .map(OrderChequeRemoteBatchDto::getTaskId)
                        .collect(Collectors.toList());
        ValidRequestBodyList<OrderChequeRemoteBatchDto> chequesDto = new ValidRequestBodyList<>(chequeRemoteBatchList);
        when(taskOrderDeliveryRepository.findAllById((anyList())))
                .thenReturn(List.of());

        List<Long> validTaskIds = validationService.validateCodeAndGetValidTask(chequesDto, USER);

        assertThat(validTaskIds).containsExactlyElementsOf(taskIdsByCheque);
        verify(taskOrderDeliveryRepository, atLeastOnce()).findAllById(anyList());
        verify(orderRepository, never()).findAllWithProperties(anySet());
        verify(codeValidationService, never()).validate(any(OrderCodeValidationDto.class));
    }

    @DisplayName("В проверке чеков не пришли заказы юр.лицам (b2b)")
    @Test
    void validateChequeWithoutB2bCustomersOrder() {
        List<OrderChequeRemoteBatchDto> chequeRemoteBatchList = List.of(
                buildOrderChequeRemoteBatchDto(1L, null, OrderChequeType.SELL),
                buildOrderChequeRemoteBatchDto(2L, null, OrderChequeType.SELL)
        );
        List<Long> taskIdsByCheque = chequeRemoteBatchList.stream()
                .map(OrderChequeRemoteBatchDto::getTaskId)
                .collect(Collectors.toList());
        ValidRequestBodyList<OrderChequeRemoteBatchDto> chequesDto = new ValidRequestBodyList<>(chequeRemoteBatchList);
        when(taskOrderDeliveryRepository.findAllById(eq(taskIdsByCheque)))
                .thenReturn(
                        List.of(
                                buildOrderDeliveryTask(1L, 10L, 100L),
                                buildOrderDeliveryTask(2L, 20L, 200L)
                        )
                );
        when(orderRepository.findAllWithProperties(anySet()))
                .thenReturn(
                        List.of(
                                buildOrder(10L),
                                buildOrder(20L)
                        )
                );

        List<Long> validTaskIds = validationService.validateCodeAndGetValidTask(chequesDto, USER);

        assertThat(validTaskIds).containsExactlyElementsOf(taskIdsByCheque);
        verify(taskOrderDeliveryRepository, atLeastOnce()).findAllById(anyList());
        verify(orderRepository, atLeastOnce()).findAllWithProperties(anySet());
        verify(codeValidationService, never()).validate(any(OrderCodeValidationDto.class));
    }

    @DisplayName("В проверке чеков пришли заказы юр.лицам (b2b) и код валидный")
    @Test
    void validateChequeWithB2bCustomersOrderAndValidVerificationCode() {
        List<OrderChequeRemoteBatchDto> chequeRemoteBatchList = List.of(
                buildOrderChequeRemoteBatchDto(1L, null, OrderChequeType.SELL),
                buildOrderChequeRemoteBatchDto(2L, VERIFICATION_CODE, OrderChequeType.SELL)
        );
        List<Long> taskIdsByCheque = chequeRemoteBatchList.stream()
                .map(OrderChequeRemoteBatchDto::getTaskId)
                .collect(Collectors.toList());
        ValidRequestBodyList<OrderChequeRemoteBatchDto> chequesDto = new ValidRequestBodyList<>(chequeRemoteBatchList);
        when(taskOrderDeliveryRepository.findAllById(eq(taskIdsByCheque)))
                .thenReturn(
                        List.of(
                                buildOrderDeliveryTask(1L, 10L, 100L),
                                buildOrderDeliveryTask(2L, 20L, 200L)
                        )
                );
        when(orderRepository.findAllWithProperties(anySet()))
                .thenReturn(
                        List.of(
                                buildB2bCustomersOrder(10L, null),
                                buildB2bCustomersOrder(20L, VERIFICATION_CODE)
                        )
                );
        OrderCodeValidationDto codeValidationDto = OrderCodeValidationDto.builder()
                .code(VERIFICATION_CODE)
                .multiOrderId(String.valueOf(200L))
                .userId(USER_ID)
                .build();
        when(codeValidationService.validate(codeValidationDto)).thenReturn(new OrderCodeValidationResponceDto(true));

        List<Long> validTaskIds = validationService.validateCodeAndGetValidTask(chequesDto, USER);

        assertThat(validTaskIds).containsExactlyElementsOf(taskIdsByCheque);
        verify(taskOrderDeliveryRepository, atLeastOnce()).findAllById(anyList());
        verify(orderRepository, atLeastOnce()).findAllWithProperties(anySet());
        verify(codeValidationService, atLeastOnce()).validate(any(OrderCodeValidationDto.class));
    }

    @DisplayName("В проверке чеков пришли заказы юр.лицам (b2b) и есть невалидный код")
    @Test
    void validateChequeWithB2bCustomersOrderAndNotValidVerificationCode() {
        long taskIdWithNotValidCode = 3L;
        List<OrderChequeRemoteBatchDto> chequeRemoteBatchList = List.of(
                buildOrderChequeRemoteBatchDto(1L, null, OrderChequeType.SELL),
                buildOrderChequeRemoteBatchDto(2L, VERIFICATION_CODE, OrderChequeType.SELL),
                buildOrderChequeRemoteBatchDto(taskIdWithNotValidCode, NOT_VALID_VERIFICATION_CODE,
                        OrderChequeType.SELL)
        );
        List<Long> taskIdsByCheque = chequeRemoteBatchList.stream()
                .map(OrderChequeRemoteBatchDto::getTaskId)
                .collect(Collectors.toList());
        ValidRequestBodyList<OrderChequeRemoteBatchDto> chequesDto = new ValidRequestBodyList<>(chequeRemoteBatchList);
        when(taskOrderDeliveryRepository.findAllById(eq(taskIdsByCheque)))
                .thenReturn(
                        List.of(
                                buildOrderDeliveryTask(1L, 10L, 100L),
                                buildOrderDeliveryTask(2L, 20L, 200L),
                                buildOrderDeliveryTask(taskIdWithNotValidCode, 30L, 300L)
                        )
                );
        when(orderRepository.findAllWithProperties(anySet()))
                .thenReturn(
                        List.of(
                                buildOrder(10L),
                                buildB2bCustomersOrder(20L, VERIFICATION_CODE),
                                buildB2bCustomersOrder(30L, VERIFICATION_CODE)
                        )
                );
        OrderCodeValidationDto codeDtoWithValidCode = OrderCodeValidationDto.builder()
                .code(VERIFICATION_CODE)
                .multiOrderId(String.valueOf(200L))
                .userId(USER_ID)
                .build();
        OrderCodeValidationDto codeDtoWithNotValidCode = OrderCodeValidationDto.builder()
                .code(NOT_VALID_VERIFICATION_CODE)
                .multiOrderId(String.valueOf(300L))
                .userId(USER_ID)
                .build();
        when(codeValidationService.validate(codeDtoWithNotValidCode)).thenReturn(new OrderCodeValidationResponceDto(false));
        when(codeValidationService.validate(codeDtoWithValidCode)).thenReturn(new OrderCodeValidationResponceDto(true));

        try {
            List<Long> validTaskIds = validationService.validateCodeAndGetValidTask(chequesDto, USER);
        } catch (Exception exception) {
            assertThat(exception).getClass().equals(OrderDeliveryTaskNotValidOrderCodeException.class);
            assertEquals(
                    TplException.interpolate(MESSAGE, taskIdWithNotValidCode),
                    exception.getMessage()
            );
        }

        verify(taskOrderDeliveryRepository, atLeastOnce()).findAllById(anyList());
        verify(orderRepository, atLeastOnce()).findAllWithProperties(anySet());
        verify(codeValidationService, times(2)).validate(any(OrderCodeValidationDto.class));

    }

    @DisplayName("При регистрации чека возврата, не делаем проверку кода у b2b заказа" +
            "и проверка в отбивке чеков возращает идентификаторы заданий(taskId).")
    @Test
    void validateWhenOrderReturnRegisterOrderChequeReturnTest() {

        List<OrderChequeRemoteBatchDto> chequeRemoteBatchList = List.of(
                buildOrderChequeRemoteBatchDto(1L, null, OrderChequeType.RETURN),
                buildOrderChequeRemoteBatchDto(2L, null, OrderChequeType.RETURN)
        );
        List<Long> taskIdsByCheque = chequeRemoteBatchList.stream()
                .map(OrderChequeRemoteBatchDto::getTaskId)
                .collect(Collectors.toList());
        ValidRequestBodyList<OrderChequeRemoteBatchDto> chequesDto = new ValidRequestBodyList<>(chequeRemoteBatchList);
        when(taskOrderDeliveryRepository.findAllById(eq(taskIdsByCheque)))
                .thenReturn(
                        List.of(
                                buildOrderDeliveryTask(1L, 10L, 100L),
                                buildOrderDeliveryTask(2L, 20L, 100L)
                        )
                );
        when(orderRepository.findAllWithProperties(anySet()))
                .thenReturn(
                        List.of(
                                buildB2bCustomersOrder(10L, VERIFICATION_CODE),
                                buildB2bCustomersOrder(20L, VERIFICATION_CODE)
                        )
                );

        List<Long> validTaskIds = validationService.validateCodeAndGetValidTask(chequesDto, USER);

        assertThat(validTaskIds).containsExactlyElementsOf(taskIdsByCheque);
        verify(taskOrderDeliveryRepository, atLeastOnce()).findAllById(anyList());
        verify(orderRepository, atLeastOnce()).findAllWithProperties(anySet());
        verify(codeValidationService, never()).validate(any(OrderCodeValidationDto.class));
    }

    @DisplayName("При регистрации чека возврата, не делаем проверку кода у b2b заказа" +
            "и проверка в отбивке чеков возращает идентификаторы заданий(taskId)." +
            "В отбивке чека мультизаказ из b2b и b2c заказа.")
    @Test
    void validateWhenOrderReturnRegisterOrderChequeReturnWhenMultiB2bAndB2cTest() {

        List<OrderChequeRemoteBatchDto> chequeRemoteBatchList = List.of(
                buildOrderChequeRemoteBatchDto(1L, null, OrderChequeType.RETURN),
                buildOrderChequeRemoteBatchDto(2L, VERIFICATION_CODE, OrderChequeType.RETURN)
        );
        List<Long> taskIdsByCheque = chequeRemoteBatchList.stream()
                .map(OrderChequeRemoteBatchDto::getTaskId)
                .collect(Collectors.toList());
        ValidRequestBodyList<OrderChequeRemoteBatchDto> chequesDto = new ValidRequestBodyList<>(chequeRemoteBatchList);
        when(taskOrderDeliveryRepository.findAllById(eq(taskIdsByCheque)))
                .thenReturn(
                        List.of(
                                buildOrderDeliveryTask(1L, 10L, 100L),
                                buildOrderDeliveryTask(2L, 20L, 100L)
                        )
                );
        when(orderRepository.findAllWithProperties(anySet()))
                .thenReturn(
                        List.of(
                                buildOrder(10L),
                                buildB2bCustomersOrder(20L, VERIFICATION_CODE)
                        )
                );

        List<Long> validTaskIds = validationService.validateCodeAndGetValidTask(chequesDto, USER);

        assertThat(validTaskIds).containsExactlyElementsOf(taskIdsByCheque);
        verify(taskOrderDeliveryRepository, atLeastOnce()).findAllById(anyList());
        verify(orderRepository, atLeastOnce()).findAllWithProperties(anySet());
        verify(codeValidationService, never()).validate(any(OrderCodeValidationDto.class));
    }

    private OrderChequeRemoteBatchDto buildOrderChequeRemoteBatchDto(Long taskId, String passCode,
                                                                     OrderChequeType chequeType) {
        OrderChequeRemoteBatchDto check = new OrderChequeRemoteBatchDto();
        check.setTaskId(taskId);
        check.setPassCode(passCode);
        check.setChequeType(chequeType);
        return check;
    }

}
