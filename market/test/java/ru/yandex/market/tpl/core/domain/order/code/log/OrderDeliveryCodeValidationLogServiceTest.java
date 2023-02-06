package ru.yandex.market.tpl.core.domain.order.code.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.code.OrderCodeValidationDto;
import ru.yandex.market.tpl.api.model.order.code.OrderCodeValidationResponceDto;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDeliveryCodeValidationLogServiceTest {

    private static final OrderCodeValidationResponceDto SUCCESS_VALIDATION_RESULT =
            new OrderCodeValidationResponceDto(true);
    private static final Long USER_ID = 100L;
    @InjectMocks
    private OrderDeliveryCodeValidationLogService validationLogService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderDeliveryCodeValidationLogRepository validationLogRepository;
    private User user;

    @BeforeEach
    void setUp() {
        Mockito.reset(userRepository);
        user = Mockito.mock(User.class);
        when(userRepository.findByIdOrThrow(USER_ID)).thenReturn(user);
    }

    @DisplayName("Сохранение введенного кода валидации выдачи заказа")
    @Test
    public void savedLogEntryTest() {
        OrderCodeValidationDto validationDto = OrderCodeValidationDto.builder()
                .multiOrderId("1")
                .userId(USER_ID)
                .code("code")
                .build();
        when(validationLogRepository.save(any(OrderDeliveryCodeValidationLogEntry.class))).thenReturn(new OrderDeliveryCodeValidationLogEntry());

        validationLogService.saveValidationCode(validationDto, SUCCESS_VALIDATION_RESULT, user);

        OrderDeliveryCodeValidationLogEntry logEntry = new OrderDeliveryCodeValidationLogEntry();
        logEntry.init(userRepository.findByIdOrThrow(USER_ID), validationDto, SUCCESS_VALIDATION_RESULT);
        verify(validationLogRepository, atLeastOnce()).save(logEntry);
    }
}
