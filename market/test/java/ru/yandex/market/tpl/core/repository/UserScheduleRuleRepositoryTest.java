package ru.yandex.market.tpl.core.repository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.schedule.UserScheduleRuleDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleStatus;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.api.model.user.UserRegistrationStatus;
import ru.yandex.market.tpl.api.model.user.UserStatus;
import ru.yandex.market.tpl.api.model.user.UserType;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleTestHelper;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SELF_EMPLOYED_USER_SCHEDULE_RULE_STATUS_PLANNED_ENABLED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserScheduleRuleRepositoryTest extends TplAbstractTest {
    private final TestUserHelper userHelper;
    private final UserScheduleService service;
    private final UserRepository userRepository;
    private final Clock clock;
    private final UserScheduleRuleRepository userScheduleRuleRepository;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    @Test
    void createRule_selfEmployedPlannedDisabled() {
        configurationServiceAdapter.mergeValue(SELF_EMPLOYED_USER_SCHEDULE_RULE_STATUS_PLANNED_ENABLED, false);

        UserScheduleType type = UserScheduleType.TWO_TWO;
        LocalDate from = LocalDate.now(clock);
        LocalDate to = LocalDate.now(clock).plusDays(3);
        boolean isSuper = true;

        User user = userHelper.findOrCreateUserWithoutSchedule(2657564L);
        user.setRegistrationStatus(UserRegistrationStatus.READY_TO_BE_SELF_EMPLOYED);
        user.setUserType(UserType.SELF_EMPLOYED);
        userRepository.save(user);
        UserScheduleRuleDto rule = service.createRule(
                user.getId(),
                UserScheduleTestHelper.ruleDto(type, from, to),
                isSuper
        );

        transactionTemplate.execute((status) -> {
            UserScheduleRule result = userScheduleRuleRepository.getById(rule.getId());

            assertThat(result.getUserScheduleStatus()).isEqualTo(UserScheduleStatus.READY);

            return null;
        });
    }

    @Test
    void testFindRulesWithSelfEmployed() {
        configurationServiceAdapter.mergeValue(SELF_EMPLOYED_USER_SCHEDULE_RULE_STATUS_PLANNED_ENABLED, true);
        userHelper.sortingCenter(SortingCenter.DEFAULT_SC_ID);

        UserScheduleType type = UserScheduleType.TWO_TWO;
        LocalDate from = LocalDate.now(clock);
        LocalDate to = LocalDate.now(clock).plusDays(3);
        boolean isSuper = true;

        User userPartner = userHelper.findOrCreateUserWithoutSchedule(324625L);
        userPartner.setRegistrationStatus(null);
        userPartner.setUserType(UserType.PARTNER);
        userRepository.save(userPartner);
        service.createRule(userPartner.getId(), UserScheduleTestHelper.ruleDto(type, from, to), isSuper);

        User userSelfEmployedNotRegistration = userHelper.findOrCreateUserWithoutSchedule(2657564L);
        userSelfEmployedNotRegistration.setRegistrationStatus(UserRegistrationStatus.READY_TO_BE_SELF_EMPLOYED);
        userSelfEmployedNotRegistration.setUserType(UserType.SELF_EMPLOYED);
        userRepository.save(userSelfEmployedNotRegistration);
        service.createRule(userSelfEmployedNotRegistration.getId(), UserScheduleTestHelper.ruleDto(type, from, to),
                isSuper);

        User userSelfEmployedNullRegistration = userHelper.findOrCreateUserWithoutSchedule(2657556L);
        userSelfEmployedNullRegistration.setRegistrationStatus(null);
        userSelfEmployedNullRegistration.setUserType(UserType.SELF_EMPLOYED);
        userRepository.save(userSelfEmployedNullRegistration);
        service.createRule(userSelfEmployedNullRegistration.getId(), UserScheduleTestHelper.ruleDto(type, from, to),
                isSuper);

        User userSelfEmployedReadySlot = userHelper.findOrCreateUserWithoutSchedule(2657556132L);
        userSelfEmployedReadySlot.setRegistrationStatus(UserRegistrationStatus.SELF_EMPLOYED);
        userSelfEmployedReadySlot.setUserType(UserType.SELF_EMPLOYED);
        userRepository.save(userSelfEmployedReadySlot);
        var slot = service.createRule(userSelfEmployedReadySlot.getId(), UserScheduleTestHelper.ruleDto(type, from, to),
                isSuper);
        transactionTemplate.execute(status -> {
            var schedule = userScheduleRuleRepository.getById(slot.getId());
            schedule.setUserScheduleStatus(UserScheduleStatus.READY);
            userScheduleRuleRepository.save(schedule);
            return status;
        });

        User userSelfEmployedNotReadySlot = userHelper.findOrCreateUserWithoutSchedule(26575563284L);
        userSelfEmployedNotReadySlot.setRegistrationStatus(UserRegistrationStatus.SELF_EMPLOYED);
        userSelfEmployedNotReadySlot.setUserType(UserType.SELF_EMPLOYED);
        userRepository.save(userSelfEmployedNotReadySlot);
        service.createRule(userSelfEmployedNotReadySlot.getId(), UserScheduleTestHelper.ruleDto(type, from, to),
                isSuper);

        User userWithoutType = userHelper.findOrCreateUserWithoutSchedule(26576598L);
        userWithoutType.setRegistrationStatus(UserRegistrationStatus.SELF_EMPLOYED);
        userWithoutType.setUserType(null);
        userRepository.save(userWithoutType);
        service.createRule(userWithoutType.getId(), UserScheduleTestHelper.ruleDto(type, from, to),
                isSuper);

        User userWithoutSlotStatus = userHelper.findOrCreateUserWithoutSchedule(26576598436822L);
        userWithoutSlotStatus.setUserType(UserType.PARTNER);
        userRepository.save(userWithoutSlotStatus);
        var slotWithoutStatus = service.createRule(userWithoutSlotStatus.getId(),
                UserScheduleTestHelper.ruleDto(type, from, to), isSuper);
        transactionTemplate.execute(status -> {
            var schedule = userScheduleRuleRepository.getById(slotWithoutStatus.getId());
            schedule.setUserScheduleStatus(null);
            userScheduleRuleRepository.save(schedule);
            return status;
        });

        User userInReviewStatus = userHelper.findOrCreateUserWithoutSchedule(265765984362L);
        userInReviewStatus.setUserType(UserType.PARTNER);
        UserUtil.setStatus(userInReviewStatus, UserStatus.REVIEW, Instant.now());
        userRepository.save(userInReviewStatus);
        var slotInReviewStatus = service.createRule(userInReviewStatus.getId(),
                UserScheduleTestHelper.ruleDto(type, from, to), isSuper);
        transactionTemplate.execute(status -> {
            var schedule = userScheduleRuleRepository.getById(userInReviewStatus.getId());
            schedule.setUserScheduleStatus(UserScheduleStatus.READY);
            userScheduleRuleRepository.save(schedule);
            return status;
        });

        User userInInternshipStatus = userHelper.findOrCreateUserWithoutSchedule(2657684362L);
        userInInternshipStatus.setUserType(UserType.PARTNER);
        UserUtil.setStatus(userInInternshipStatus, UserStatus.REVIEW, Instant.now());
        UserUtil.setStatus(userInInternshipStatus, UserStatus.INTERNSHIP, Instant.now());
        userRepository.save(userInInternshipStatus);
        var slotInInternshipStatus = service.createRule(userInInternshipStatus.getId(),
                UserScheduleTestHelper.ruleDto(type, from, to), isSuper);
        transactionTemplate.execute(status -> {
            var schedule = userScheduleRuleRepository.getById(userInInternshipStatus.getId());
            schedule.setUserScheduleStatus(UserScheduleStatus.READY);
            userScheduleRuleRepository.save(schedule);
            return status;
        });

        transactionTemplate.execute(status -> {
            List<User> result = userScheduleRuleRepository.findActiveRulesForInterval(from, to)
                    .stream()
                    .map(UserScheduleRule::getUser)
                    .collect(Collectors.toList());
            assertThat(result).hasSize(4);
            assertThat(result).contains(userPartner);
            assertThat(result).contains(userWithoutType);
            assertThat(result).contains(userSelfEmployedReadySlot);
            assertThat(result).contains(userWithoutSlotStatus);

            List<UserScheduleRule> userPartnerRules =
                    userScheduleRuleRepository.findLastActiveRules(userPartner.getId(), Set.of(type));
            assertThat(userPartnerRules).hasSize(1);

            List<UserScheduleRule> userSelfEmployedNotRegistrationRules =
                    userScheduleRuleRepository.findLastActiveRules(userSelfEmployedNotRegistration.getId(),
                            Set.of(type));
            assertThat(userSelfEmployedNotRegistrationRules).hasSize(0);

            List<UserScheduleRule> userSelfEmployedNullRegistrationRules =
                    userScheduleRuleRepository.findLastActiveRules(userSelfEmployedNullRegistration.getId(),
                            Set.of(type));
            assertThat(userSelfEmployedNullRegistrationRules).hasSize(0);

            List<UserScheduleRule> userSelfEmployedReadySlotRules =
                    userScheduleRuleRepository.findLastActiveRules(userSelfEmployedReadySlot.getId(),
                            Set.of(type));
            assertThat(userSelfEmployedReadySlotRules).hasSize(1);

            List<UserScheduleRule> userSelfEmployedNotReadySlotRules =
                    userScheduleRuleRepository.findLastActiveRules(userSelfEmployedNotReadySlot.getId(),
                            Set.of(type));
            assertThat(userSelfEmployedNotReadySlotRules).hasSize(0);

            List<UserScheduleRule> userWithoutTypeRules =
                    userScheduleRuleRepository.findLastActiveRules(userWithoutType.getId(), Set.of(type));
            assertThat(userWithoutTypeRules).hasSize(1);

            List<UserScheduleRule> userWithoutSlotStatusRules =
                    userScheduleRuleRepository.findLastActiveRules(userWithoutSlotStatus.getId(), Set.of(type));
            assertThat(userWithoutSlotStatusRules).hasSize(1);

            return status;
        });
    }
}
