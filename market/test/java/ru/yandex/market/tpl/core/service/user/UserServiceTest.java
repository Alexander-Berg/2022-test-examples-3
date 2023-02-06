package ru.yandex.market.tpl.core.service.user;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.user.UserRegistrationStatus;
import ru.yandex.market.tpl.api.model.usershift.UserShiftRegistrationStatusDto;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserPropertyEntity;
import ru.yandex.market.tpl.core.domain.user.UserPropertyRepository;
import ru.yandex.market.tpl.core.domain.user.UserService;
import ru.yandex.market.tpl.core.domain.user.UserSmartphone;
import ru.yandex.market.tpl.core.domain.user.UserSmartphoneRepository;
import ru.yandex.market.tpl.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class UserServiceTest extends TplAbstractTest {
    private final UserService userService;
    private final TestUserHelper testUserHelper;
    private final UserSmartphoneRepository userSmartphoneRepository;
    private final UserPropertyRepository userPropertyRepository;

    private static final String NEED_CLEAR_OFFLINE_SCHEDULER_NAME =
            UserPropsType.NEED_CLEAR_OFFLINE_SCHEDULER.getName();

    private User user;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(9001L);
    }

    @Test
    void updateUserRegistration() {
        UserRegistrationStatus status = UserRegistrationStatus.SELF_EMPLOYED_REGISTRATION_PROCESSING;
        UserShiftRegistrationStatusDto userShiftRegistrationStatusDto =
                userService.updateUserRegistration(user, status);
        assertThat(userShiftRegistrationStatusDto.getStatus())
                .isEqualTo(UserShiftStatus.SELF_EMPLOYED_REGISTRATION_PROCESSING);
        User newUser = userService.getById(user.getId());
        assertThat(newUser.getRegistrationStatus()).isEqualTo(status);
    }

    @Test
    void saveUserSmartphone_WhenSmartphoneNotExists() {
        assertThat(userSmartphoneRepository.findByUser(user).isEmpty()).isTrue();

        userService.updateUserSmartphone(UserCommand.UpdateUserSmartphoneCommand.builder()
                .userId(user.getId())
                .osSmartphone("os")
                .modelSmartphone("model")
                .courierAppVersion("version")
                .build());

        UserSmartphone userSmartphone = userSmartphoneRepository.findByUser(user).get();
        assertThat(userSmartphone.getOsSmartphone()).isEqualTo("os");
    }

    @Test
    void saveUserSmartphone_WhenSmartphoneExists() {
        assertThat(userSmartphoneRepository.findByUser(user).isEmpty()).isTrue();

        userService.updateUserSmartphone(UserCommand.UpdateUserSmartphoneCommand.builder()
                .userId(user.getId())
                .osSmartphone("os")
                .modelSmartphone("model")
                .courierAppVersion("version")
                .build());

        assertThat(userSmartphoneRepository.findByUser(user).isEmpty()).isFalse();

        userService.updateUserSmartphone(UserCommand.UpdateUserSmartphoneCommand.builder()
                .userId(user.getId())
                .osSmartphone("os1")
                .modelSmartphone("model1")
                .courierAppVersion("version1")
                .build());

        UserSmartphone userSmartphone = userSmartphoneRepository.findByUser(user).get();
        assertThat(userSmartphone.getOsSmartphone()).isEqualTo("os1");
    }

    @Test
    void disableClearOfflineSchedulerProperty() {
        assertThat(user.getProperties().get(UserPropsType.NEED_CLEAR_OFFLINE_SCHEDULER.getName())).isNull();
        assertThat(userPropertyRepository.findByName(NEED_CLEAR_OFFLINE_SCHEDULER_NAME).isEmpty()).isTrue();

        userPropertyRepository.save(
                UserPropertyEntity.builder()
                        .user(user)
                        .type(TplPropertyType.BOOLEAN)
                        .name(NEED_CLEAR_OFFLINE_SCHEDULER_NAME)
                        .value("true")
                        .build()
        );

        var properties =
                userPropertyRepository.findByName(NEED_CLEAR_OFFLINE_SCHEDULER_NAME);

        assertThat(properties.size()).isEqualTo(1);
        assertThat(properties.get(0).getValue()).isEqualTo("true");

        userService.upsertUserProperty(user.getId(), Map.of(NEED_CLEAR_OFFLINE_SCHEDULER_NAME,
                "false"));

        var updatedProperties =
                userPropertyRepository.findByName(NEED_CLEAR_OFFLINE_SCHEDULER_NAME);

        assertThat(updatedProperties.get(0).getValue()).isEqualTo("false");
    }

}
