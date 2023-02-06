package ru.yandex.market.tpl.tms.executor.user;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.utils.TimeZoneUtil;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyEntity;
import ru.yandex.market.tpl.core.domain.user.UserPropertyRepository;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RequiredArgsConstructor
class DisableNeedClearOfflineSchedulerExecutorTest extends TplTmsAbstractTest {

    private final DisableNeedClearOfflineSchedulerExecutor executor;
    private final UserPropertyRepository userPropertyRepository;
    private final TestUserHelper testUserHelper;
    private final Clock clock;

    private User user;
    private static final Long UID = 1234L;
    private static final String NEED_CLEAR_OFFLINE_SCHEDULER = UserProperties.NEED_CLEAR_OFFLINE_SCHEDULER.getName();


    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(UID);
        ClockUtil.initFixed(clock, LocalDateTime.ofInstant(Instant.now(), TimeZoneUtil.DEFAULT_OFFSET));
    }

    @Test
    void testCanUpdatePropertiesToFalse() throws Exception {
        assertThat(userPropertyRepository.findByName(NEED_CLEAR_OFFLINE_SCHEDULER).isEmpty()).isTrue();

        var expiryTime = LocalDateTime.ofInstant(clock.instant(), TimeZoneUtil.DEFAULT_OFFSET).plusMinutes(6);

        userPropertyRepository.save(
                UserPropertyEntity.builder()
                        .user(user)
                        .name(UserProperties.NEED_CLEAR_OFFLINE_SCHEDULER.getName())
                        .type(TplPropertyType.BOOLEAN)
                        .value("true")
                        .build()
        );

        ClockUtil.initFixed(clock, expiryTime);

        executor.doRealJob(null);

        var properties = userPropertyRepository.findByName(NEED_CLEAR_OFFLINE_SCHEDULER);

        assertThat(properties.size()).isEqualTo(1);
        assertThat(properties.get(0).getValue()).isEqualTo("false");
    }

    @Test
    void testDoesNotUpdatePropertiesToFalse_whenNotExpired() throws Exception {
        assertThat(userPropertyRepository.findByName(NEED_CLEAR_OFFLINE_SCHEDULER).isEmpty()).isTrue();

        var nonExpiryTime = LocalDateTime.ofInstant(clock.instant(), clock.getZone()).plusMinutes(4);
        userPropertyRepository.save(
                UserPropertyEntity.builder()
                        .user(user)
                        .name(NEED_CLEAR_OFFLINE_SCHEDULER)
                        .type(TplPropertyType.BOOLEAN)
                        .value("true")
                        .build()
        );

        ClockUtil.initFixed(clock, nonExpiryTime);

        executor.doRealJob(null);

        var properties = userPropertyRepository.findByName(NEED_CLEAR_OFFLINE_SCHEDULER);

        assertThat(properties.size()).isEqualTo(1);
        assertThat(properties.get(0).getValue()).isEqualTo("true");
    }
}
