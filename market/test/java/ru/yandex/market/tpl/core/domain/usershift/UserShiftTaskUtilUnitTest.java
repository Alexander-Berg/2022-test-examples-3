package ru.yandex.market.tpl.core.domain.usershift;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.api.model.movement.MovementStatus;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.task.DropoffCargoDeliveryTaskHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.external.routing.api.RoutingAddress;
import ru.yandex.market.tpl.core.external.routing.api.RoutingGeoPoint;
import ru.yandex.market.tpl.core.external.routing.api.RoutingLocationType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResponseRoutePoint;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.COURIER_WITH_ENABLED_LOT_FLOW_IDS;

@ExtendWith(SpringExtension.class)
class UserShiftTaskUtilUnitTest {
    public static final long SC_ID = 111L;
    public static final long ENABLED_USER_ID = 1L;
    public static final long DISABLED_USER_ID = 10L;
    @Mock
    private SortingCenterPropertyService sortingCenterPropertyService;
    @Mock
    private UserShiftCommandService userShiftCommandService;
    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @Mock
    private DropoffCargoDeliveryTaskHelper dropoffCargoDeliveryTaskHelper;
    @Mock
    private CollectDropshipTaskFactory collectDropshipTaskFactory;
    @InjectMocks
    private UserShiftTaskUtil userShiftTaskUtil;

    @Test
    void createCollectDropShipTask_whenUserEnabled() {
        //given
        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.DROPSHIP_LOTS_ENABLED), eq(SC_ID))).thenReturn(true);
        when(configurationProviderAdapter.getValueAsLongs(COURIER_WITH_ENABLED_LOT_FLOW_IDS))
                .thenReturn(Set.of(1L, 2L, 3L));
        var userShift = buildMockedUserShift();
        var movement = buildMovement();
        var rp = buildRp();
        var user = buildMockedUser(ENABLED_USER_ID);
        userShift.setUser(user);

        //when
        userShiftTaskUtil.createCollectDropShipTask(movement, userShift, rp);

        //then
        verify(userShiftCommandService, times(1)).addLockerDeliveryTask(any(), any());
        verify(userShiftCommandService, never()).addCollectDropshipTask(any(), any());
    }


    @Test
    void createCollectDropShipTask_whenAnyUserEnabled() {
        //given
        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.DROPSHIP_LOTS_ENABLED), eq(SC_ID))).thenReturn(true);

        var userShift = buildMockedUserShift();
        var movement = buildMovement();
        var rp = buildRp();
        var user = buildMockedUser(ENABLED_USER_ID);
        userShift.setUser(user);

        //when
        userShiftTaskUtil.createCollectDropShipTask(movement, userShift, rp);

        //then
        verify(userShiftCommandService, times(1)).addLockerDeliveryTask(any(), any());
        verify(userShiftCommandService, never()).addCollectDropshipTask(any(), any());
    }

    @NotNull
    private Movement buildMovement() {
        Movement movement = new Movement();
        movement.setStatus(MovementStatus.CREATED);
        return movement;
    }

    @Test
    void createCollectDropShipTask_whenUserDisabled() {
        //given
        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.DROPSHIP_LOTS_ENABLED), eq(SC_ID))).thenReturn(true);
        when(configurationProviderAdapter.getValueAsLongs(COURIER_WITH_ENABLED_LOT_FLOW_IDS))
                .thenReturn(Set.of(1L, 2L, 3L));
        UserShift userShift = buildMockedUserShift();
        var movement = buildMovement();
        var rp = buildRp();
        var user = buildMockedUser(DISABLED_USER_ID);
        userShift.setUser(user);
        //when
        userShiftTaskUtil.createCollectDropShipTask(movement, userShift, rp);

        //then
        verify(userShiftCommandService, never()).addLockerDeliveryTask(any(), any());
        verify(userShiftCommandService, times(1)).addCollectDropshipTask(any(), any());
    }

    @Test
    void createCollectDropShipTask_whenEmptyEnabledList() {
        //given
        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.DROPSHIP_LOTS_ENABLED), eq(SC_ID))).thenReturn(true);
        when(configurationProviderAdapter.getValueAsLongs(COURIER_WITH_ENABLED_LOT_FLOW_IDS))
                .thenReturn(Set.of());
        UserShift userShift = buildMockedUserShift();
        var movement = buildMovement();
        var rp = buildRp();
        var user = buildMockedUser(DISABLED_USER_ID);
        userShift.setUser(user);
        //when
        userShiftTaskUtil.createCollectDropShipTask(movement, userShift, rp);

        //then
        verify(userShiftCommandService, times(1)).addLockerDeliveryTask(any(), any());
        verify(userShiftCommandService, never()).addCollectDropshipTask(any(), any());
    }

    @Test
    void createCollectDropShipTask_whenScDisabled() {
        //given
        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.DROPSHIP_LOTS_ENABLED), eq(SC_ID))).thenReturn(false);
        UserShift userShift = buildMockedUserShift();
        var movement = buildMovement();
        var rp = buildRp();

        //when
        userShiftTaskUtil.createCollectDropShipTask(movement, userShift, rp);

        //then
        verify(userShiftCommandService, never()).addLockerDeliveryTask(any(), any());
        verify(userShiftCommandService, times(1)).addCollectDropshipTask(any(), any());
    }

    @NotNull
    private RoutingResponseRoutePoint buildRp() {
        return new RoutingResponseRoutePoint(Instant.now(), Instant.now(),
                new RoutingAddress("", RoutingGeoPoint.ofLatLon(BigDecimal.ONE, BigDecimal.ONE)),
                List.of(), RoutingLocationType.delivery
        );
    }

    private UserShift buildMockedUserShift() {
        var sortingCenter = new SortingCenter();
        sortingCenter.setId(SC_ID);

        var shift = mock(Shift.class);
        when(shift.getSortingCenter()).thenReturn(sortingCenter);

        var us = new UserShift();
        us.setShift(shift);
        return us;
    }

    @NotNull
    private User buildMockedUser(Long userId) {
        var user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        return user;
    }
}
