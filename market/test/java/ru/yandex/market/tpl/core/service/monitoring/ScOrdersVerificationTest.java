package ru.yandex.market.tpl.core.service.monitoring;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.sc.ScManager;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrdersVerificationLog;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrdersVerificationLogRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.external.delivery.sc.SortCenterDirectClient;
import ru.yandex.market.tpl.core.external.delivery.sc.dto.ScRoutingResult;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class ScOrdersVerificationTest extends TplAbstractTest {

    private final ScOrdersVerificationLogRepository logRepository;

    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final ShiftManager shiftManager;
    private final Clock clock;
    private final ScRoutingMonitoringService monitoringService;
    private final ConfigurationService configurationService;
    private final JdbcTemplate jdbcTemplate;
    private final ScManager scManager;

    private final SortCenterDirectClient mockedScClient;
    private Shift shift;


    @BeforeEach
    void setUp() {
        LocalDate shiftDate = LocalDate.now(clock);
        shift = shiftManager.findOrCreate(shiftDate, SortingCenter.DEFAULT_SC_ID);
        Mockito.reset(mockedScClient);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(mockedScClient);
    }

    @Test
    void verify_whenMismatch() {
        //given
        configurationService.mergeValue(ConfigurationProperties.VALID_ROUTING_MONITORING_SC_GAP.getName(), 0);
        CreateResult userShiftWithOrder1 = createUserShiftWithOrder(1L);
        CreateResult userShiftWithOrder2 = createUserShiftWithOrder(2L);

        when(mockedScClient.getRoutingResult(eq(shift.getShiftDate()), eq(shift.getSortingCenter().getToken())))
                .thenReturn(Optional.of(new ScRoutingResult(
                        List.of(new ScRoutingResult.OrderCourier(userShiftWithOrder1.getOrder().getExternalOrderId(),
                                String.valueOf(userShiftWithOrder1.getUserShift().getUser().getUid()))))));

        //when
        monitoringService.verificationRouting(shift.getShiftDate(), shift.getSortingCenter().getId(), null);

        //then
        List<ScOrdersVerificationLog> logs = logRepository.findAll();
        assertThat(logs).hasSize(1);
        ScOrdersVerificationLog scOrdersVerificationLog = logs.get(0);

        assertThat(scOrdersVerificationLog.getIsValid()).isFalse();
        assertThat(scOrdersVerificationLog.getShiftId()).isEqualTo(shift.getId());
        assertThat(scOrdersVerificationLog.getMismatchOrderExternalIds()).contains(userShiftWithOrder2.getOrder()
                .getExternalOrderId());
        assertThat(scOrdersVerificationLog.getMismatch().compareTo(BigDecimal.valueOf(1.0))).isEqualTo(0);
    }

    @Test
    void verify_whenNotMismatch() {
        //given
        CreateResult userShiftWithOrder1 = createUserShiftWithOrder(1L);
        CreateResult userShiftWithOrder2 = createUserShiftWithOrder(2L);

        when(mockedScClient.getRoutingResult(eq(shift.getShiftDate()), eq(shift.getSortingCenter().getToken())))
                .thenReturn(Optional.of(new ScRoutingResult(
                        List.of(
                                new ScRoutingResult.OrderCourier(userShiftWithOrder1.getOrder().getExternalOrderId(),
                                        String.valueOf(userShiftWithOrder1.getUserShift().getUser().getUid())),
                                new ScRoutingResult.OrderCourier(userShiftWithOrder2.getOrder().getExternalOrderId(),
                                        String.valueOf(userShiftWithOrder2.getUserShift().getUser().getUid()))
                        ))));

        //when
        monitoringService.verificationRouting(shift.getShiftDate(), shift.getSortingCenter().getId(), null);

        //then
        List<ScOrdersVerificationLog> logs = logRepository.findAll();
        assertThat(logs).hasSize(1);
        ScOrdersVerificationLog scOrdersVerificationLog = logs.get(0);

        assertThat(scOrdersVerificationLog.getIsValid()).isTrue();
        assertThat(scOrdersVerificationLog.getShiftId()).isEqualTo(shift.getId());
        assertThat(scOrdersVerificationLog.getMismatchOrderExternalIds()).isEmpty();
        assertThat(scOrdersVerificationLog.getMismatch().compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }

    private CreateResult createUserShiftWithOrder(Long uid) {
        var user = userHelper.findOrCreateUser(uid, LocalDate.now(clock));
        UserShift userShift = userHelper.createEmptyShift(user, shift);

        var order = orderGenerateService.createOrder();
        userHelper.addDeliveryTaskToShift(user, userShift, order);
        return CreateResult.of(userShift, order);
    }

    @Value(staticConstructor = "of")
    public static class CreateResult {
        private UserShift userShift;
        private Order order;
    }
}
