package ru.yandex.market.tpl.core.domain.order.code;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.order.code.util.ModelBuilderTestUtil;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDeliveryUserValidationServiceTest {

    public static final long TASK_ID = 1L;
    public static final int USER_ID = 100;
    @InjectMocks
    private OrderDeliveryUserValidationService userValidationService;
    @Mock
    private UserShiftRepository userShiftRepository;

    @DisplayName("Happy path, таска относится к курьеру")
    @Test
    void checkValidUserByTask() {
        when(userShiftRepository.findByTaskIdWithUser(TASK_ID))
                .thenReturn(Optional.of(ModelBuilderTestUtil.buildUserShift(USER_ID)));

        boolean validUserByTask = userValidationService.isValidUserByTask(USER_ID, TASK_ID);

        assertThat(validUserByTask).isTrue();
    }

    @DisplayName("Не найдено смены у курьера с этой таской")
    @Test
    void validateTaskFromAnotherUser() {
        when(userShiftRepository.findByTaskIdWithUser(TASK_ID)).thenReturn(Optional.empty());

        boolean validUserByTask = userValidationService.isValidUserByTask(USER_ID, TASK_ID);

        assertThat(validUserByTask).isFalse();
    }
}
