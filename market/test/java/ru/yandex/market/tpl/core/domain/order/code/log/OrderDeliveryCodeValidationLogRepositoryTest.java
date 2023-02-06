package ru.yandex.market.tpl.core.domain.order.code.log;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.code.OrderCodeValidationDto;
import ru.yandex.market.tpl.api.model.order.code.OrderCodeValidationResponceDto;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class OrderDeliveryCodeValidationLogRepositoryTest extends TplAbstractTest {

    private static final OrderCodeValidationResponceDto SUCCESS_VALIDATION_RESULT =
            new OrderCodeValidationResponceDto(true);
    private final TestUserHelper userHelper;
    private final TransactionTemplate transactionTemplate;
    private final OrderDeliveryCodeValidationLogRepository validationLogRepository;

    private User user;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(1L);
    }

    @DisplayName("Сохранение кода валидации выдачи заказа")
    @Test
    public void saveValidationLogTest() {
        OrderDeliveryCodeValidationLogEntry logEntry = new OrderDeliveryCodeValidationLogEntry();
        OrderCodeValidationDto validationDto = OrderCodeValidationDto.builder()
                .userId(user.getId())
                .multiOrderId("1")
                .code("code")
                .build();
        logEntry.init(user, validationDto, SUCCESS_VALIDATION_RESULT);

        val savedLogEntry = transactionTemplate.execute(t -> validationLogRepository.save(logEntry));

        assertThat(savedLogEntry).isNotNull();
        assertThat(savedLogEntry.getValidationCode()).isEqualTo(validationDto.getCode());
        assertThat(savedLogEntry.getLogTime()).isNotNull();
        assertThat(savedLogEntry.getMultiOrderId()).isEqualTo(validationDto.getMultiOrderId());
        assertThat(savedLogEntry.isValidCode()).isEqualTo(SUCCESS_VALIDATION_RESULT.isValid());
    }
}
