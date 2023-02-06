package ru.yandex.market.tpl.core.domain.order.code;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.code.OrderCodeValidationDto;
import ru.yandex.market.tpl.api.model.order.code.OrderCodeValidationResponceDto;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CHECK_USER_WHEN_VALIDATE_VERIFICATION_CODE_ENABLED;
import static ru.yandex.market.tpl.core.domain.order.code.util.ModelBuilderTestUtil.buildOrderWithVerificationCode;

@ExtendWith(MockitoExtension.class)
class OrderDeliveryCodeValidationServiceUnitTest {

    private static final int USER_ID = 123;
    private static final String VERIFICATION_CODE_1 = "1111";
    private static final String VERIFICATION_CODE_2 = "2222";
    private static final String NOT_VALID_CODE = "-1";
    private static final String MULTI_ORDER_ID = "1000";
    private static final String NOT_EXIST_MULTI_ORDER_ID = "-1000";
    @InjectMocks
    private OrderDeliveryCodeValidationService validationService;
    @Mock
    private OrderDeliveryUserValidationService userValidationService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @BeforeEach
    void setUp() {
        when(configurationProviderAdapter.isBooleanEnabled(CHECK_USER_WHEN_VALIDATE_VERIFICATION_CODE_ENABLED))
                .thenReturn(true);
    }

    @DisplayName("Успешная валидация мультизаказа по одному из кодов для получения посылок")
    @Test
    void validateSuccess() {
        long taskId = Long.parseLong(MULTI_ORDER_ID);
        when(userValidationService.isValidUserByTask(USER_ID, taskId)).thenReturn(true);
        when(orderRepository.findAllOrdersInMultiOrderWithProperties(taskId))
                .thenReturn(
                        List.of(
                                buildOrderWithVerificationCode(VERIFICATION_CODE_1),
                                buildOrderWithVerificationCode(VERIFICATION_CODE_2)
                        )
                );
        OrderCodeValidationDto validationDto = OrderCodeValidationDto.builder()
                .code(VERIFICATION_CODE_1)
                .multiOrderId(MULTI_ORDER_ID)
                .userId(USER_ID)
                .build();

        OrderCodeValidationResponceDto validate = validationService.validate(validationDto);

        assertThat(validate.isValid()).isTrue();
        verify(userValidationService, atLeastOnce()).isValidUserByTask(USER_ID, taskId);
    }

    @DisplayName("Успешная валидация мультизаказа по одному из кодов для получения посылок. " +
            "Флаг проверки курьера выключен. ")
    @Test
    void validateSuccessButFlagUserValidateOff() {
        when(configurationProviderAdapter.isBooleanEnabled(CHECK_USER_WHEN_VALIDATE_VERIFICATION_CODE_ENABLED))
                .thenReturn(false);
        long taskId = Long.parseLong(MULTI_ORDER_ID);
        when(orderRepository.findAllOrdersInMultiOrderWithProperties(taskId))
                .thenReturn(
                        List.of(
                                buildOrderWithVerificationCode(VERIFICATION_CODE_1),
                                buildOrderWithVerificationCode(VERIFICATION_CODE_2)
                        )
                );
        OrderCodeValidationDto validationDto = OrderCodeValidationDto.builder()
                .code(VERIFICATION_CODE_1)
                .multiOrderId(MULTI_ORDER_ID)
                .userId(USER_ID)
                .build();

        OrderCodeValidationResponceDto validate = validationService.validate(validationDto);

        assertThat(validate.isValid()).isTrue();
        verify(userValidationService, never()).isValidUserByTask(USER_ID, taskId);
    }

    @DisplayName("Неправильный код не проходит валидацию для выдачи")
    @Test
    void validateNotValidCode() {
        long taskId = Long.parseLong(MULTI_ORDER_ID);
        when(userValidationService.isValidUserByTask(USER_ID, taskId)).thenReturn(true);
        when(orderRepository.findAllOrdersInMultiOrderWithProperties(taskId))
                .thenReturn(
                        List.of(
                                buildOrderWithVerificationCode(VERIFICATION_CODE_1),
                                buildOrderWithVerificationCode(VERIFICATION_CODE_2)
                        )
                );
        OrderCodeValidationDto validationDto = OrderCodeValidationDto.builder()
                .code(NOT_VALID_CODE)
                .multiOrderId(MULTI_ORDER_ID)
                .userId(USER_ID)
                .build();

        OrderCodeValidationResponceDto validate = validationService.validate(validationDto);

        assertThat(validate.isValid()).isFalse();
    }

    @DisplayName("В запросе пришел неаутальный external order id")
    @Test
    void validateExternalOrderIdNotExist() {
        long taskId = Long.parseLong(NOT_EXIST_MULTI_ORDER_ID);
        when(userValidationService.isValidUserByTask(USER_ID, taskId)).thenReturn(true);
        when(orderRepository.findAllOrdersInMultiOrderWithProperties(taskId))
                .thenReturn(List.of());
        OrderCodeValidationDto validationDto = OrderCodeValidationDto.builder()
                .code(VERIFICATION_CODE_1)
                .multiOrderId(NOT_EXIST_MULTI_ORDER_ID)
                .userId(USER_ID)
                .build();

        OrderCodeValidationResponceDto validate = validationService.validate(validationDto);

        assertThat(validate.isValid()).isFalse();
    }

    @DisplayName("У заказа не существует проверочного кода")
    @Test
    void validateOrderWithoutVerificationCode() {
        long taskId = Long.parseLong(MULTI_ORDER_ID);
        when(userValidationService.isValidUserByTask(USER_ID, taskId)).thenReturn(true);
        when(orderRepository.findAllOrdersInMultiOrderWithProperties(taskId))
                .thenReturn(List.of());
        OrderCodeValidationDto validationDto = OrderCodeValidationDto.builder()
                .code(VERIFICATION_CODE_1)
                .multiOrderId(MULTI_ORDER_ID)
                .userId(USER_ID)
                .build();

        OrderCodeValidationResponceDto validate = validationService.validate(validationDto);

        assertThat(validate.isValid()).isFalse();
    }

    @DisplayName("У заказа другой курьер, не такой как в запросе")
    @Test
    void validateOrderCodeButUserFromAnotherTask() {
        long taskId = Long.parseLong(MULTI_ORDER_ID);
        when(userValidationService.isValidUserByTask(USER_ID, taskId)).thenReturn(false);
        OrderCodeValidationDto validationDto = OrderCodeValidationDto.builder()
                .code(VERIFICATION_CODE_1)
                .multiOrderId(MULTI_ORDER_ID)
                .userId(USER_ID)
                .build();

        OrderCodeValidationResponceDto validate = validationService.validate(validationDto);

        assertThat(validate.isValid()).isFalse();
    }
}
