package ru.yandex.market.tpl.core.service.usershift;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.tpl.api.model.user.UserStatus;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.ORDERS_IN_SHIFT_AMOUNT_FOR_GRADE_UP;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SECOND_SHIFT_MULTIPLIER;

class UserShiftMultiplierServiceTest {


    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @Mock
    private UserShiftRepository userShiftRepository;
    @Mock
    private UserPropertyService userPropertyService;
    @Mock
    private SortingCenterPropertyService sortingCenterPropertyService;

    @InjectMocks
    private UserShiftMultiplierService unit;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldDoNothingIfUserIsNotNewbie() {
        var user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

        unit.calculateAndSetShiftMultiplier(user, SortingCenter.DEFAULT_SC_ID);

        verifyNoInteractions(configurationProviderAdapter);
        verifyNoInteractions(userShiftRepository);
        verifyNoInteractions(userPropertyService);
        verifyNoInteractions(sortingCenterPropertyService);
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"NEWBIE", "NOT_ACTIVE"})
    void shouldSuccessfullyCalculateUserShiftMultiplier(UserStatus status) {
        Long userId = 100L;

        var user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getStatus()).thenReturn(status);

        when(userShiftRepository.getUserShiftsCountWithCompletedTasksCountGreaterThen(
                eq(userId), any()
        )).thenReturn(Optional.of(1));

        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.RECALCULATE_MULTIPLIERS_ENABLED),
                eq(SortingCenter.DEFAULT_SC_ID)
        )).thenReturn(true);

        when(configurationProviderAdapter.getValueAsInteger(eq(ORDERS_IN_SHIFT_AMOUNT_FOR_GRADE_UP))).thenReturn(Optional.empty());
        when(configurationProviderAdapter.getValueAsDouble(eq(SECOND_SHIFT_MULTIPLIER))).thenReturn(Optional.empty());

        unit.calculateAndSetShiftMultiplier(user, SortingCenter.DEFAULT_SC_ID);

        var expectedMultiplier = BigDecimal.valueOf(1.5);

        verify(userPropertyService).addPropertyToUser(eq(user), eq(UserProperties.TRAVEL_TIME_MULTIPLIER_NONE), eq(expectedMultiplier));
        verify(userPropertyService).addPropertyToUser(eq(user), eq(UserProperties.SHARED_SERVICE_TIME_MULTIPLIER_NONE), eq(expectedMultiplier));
        verify(userPropertyService).addPropertyToUser(eq(user), eq(UserProperties.SERVICE_TIME_MULTIPLIER_NONE), eq(expectedMultiplier));

        verify(userPropertyService).addPropertyToUser(eq(user), eq(UserProperties.TRAVEL_TIME_MULTIPLIER_CAR), eq(expectedMultiplier));
        verify(userPropertyService).addPropertyToUser(eq(user), eq(UserProperties.SHARED_SERVICE_TIME_MULTIPLIER_CAR), eq(expectedMultiplier));
        verify(userPropertyService).addPropertyToUser(eq(user), eq(UserProperties.SERVICE_TIME_MULTIPLIER_CAR), eq(expectedMultiplier));
    }
}
