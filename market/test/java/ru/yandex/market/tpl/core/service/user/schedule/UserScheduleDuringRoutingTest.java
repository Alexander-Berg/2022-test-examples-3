package ru.yandex.market.tpl.core.service.user.schedule;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.schedule.UserScheduleRuleDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.routing.schedule.RoutingScheduleRule;
import ru.yandex.market.tpl.core.domain.routing.schedule.RoutingScheduleRuleRepository;
import ru.yandex.market.tpl.core.domain.routing.schedule.RoutingScheduleRuleUtil;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author kukabara
 */
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UserScheduleDuringRoutingTest {

    private final TestUserHelper userHelper;
    private final UserScheduleService userScheduleService;
    private final RoutingScheduleRuleRepository routingScheduleRuleRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    @MockBean
    private Clock clock;
    private User user;
    private RoutingScheduleRule routingScheduleRule;
    private Company company;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock);
        company = userHelper.findOrCreateSuperCompany();
        user = userHelper.findOrCreateUserWithoutSchedule(1L, company.getName());

        routingScheduleRule = RoutingScheduleRuleUtil.routingScheduleRule();
        routingScheduleRuleRepository.save(routingScheduleRule);
    }

    @Test
    void shouldCreateBeforeRouting() {
        LocalDateTime time = LocalDateTime.of(LocalDate.now(clock),
                routingScheduleRule.getPreRoutingStartTime().minusHours(1));
        ClockUtil.initFixed(clock, time);

        createRule();
    }

    @Test
    void shouldCreateAfterRoutingWithFlag() {
        LocalDateTime time = LocalDateTime.of(LocalDate.now(clock),
                routingScheduleRule.getPreRoutingStartTime().plusHours(1));
        ClockUtil.initFixed(clock, time);

        createRule();
    }

    @Test
    void shouldCreateAfterRoutingToFuture() {
        LocalDateTime time = LocalDateTime.of(LocalDate.now(clock),
                routingScheduleRule.getPreRoutingStartTime().minusHours(1));
        ClockUtil.initFixed(clock, time);

        createRule(LocalDate.now(clock).plusDays(2));
    }

    private UserScheduleRuleDto createRule() {
        return createRule(LocalDate.now(clock).plusDays(1));
    }

    private UserScheduleRuleDto createRule(LocalDate now) {
        return userScheduleService.createRule(user.getId(),
                UserScheduleTestHelper.ruleDto(UserScheduleType.ALWAYS_WORKS, now, null),
                company.isSuperCompany());
    }

}
