package ru.yandex.market.sc.internal.controller.external;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.user.repository.UserRepository;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.controller.dto.user.HermesUserRequestDto;
import ru.yandex.market.sc.internal.controller.dto.user.HermesUserResponseDto;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.sc.internal.util.ScIntControllerCaller;
import ru.yandex.market.tpl.common.util.JacksonUtil;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class HermesUserControllerTest {

    private final TestFactory testFactory;
    private final UserRepository userRepository;
    private final ScIntControllerCaller caller;

    @SpyBean(name = "blackboxClient")
    BlackboxClient blackboxClient;

    @SpyBean(name = "innerBlackboxClient")
    BlackboxClient innerBlackboxClient;

    private static final Long SORTING_CENTER_ID = 1L;
    private static final Long USER_UID = 12398791L;

    @BeforeEach
    void init() {
        var sc = testFactory.storedSortingCenter(SORTING_CENTER_ID);
        testFactory.setSortingCenterProperty(sc, SortingCenterPropertiesKey.THIRD_PARTY_SC, false);
        testFactory.setSortingCenterProperty(sc, SortingCenterPropertiesKey.HERMES_USER_MANAGEMENT, true);

        var sc2 = testFactory.storedSortingCenter(20L);
        testFactory.setSortingCenterProperty(sc2, SortingCenterPropertiesKey.THIRD_PARTY_SC, false);
        testFactory.setSortingCenterProperty(sc2, SortingCenterPropertiesKey.HERMES_USER_MANAGEMENT, true);

        doReturn(USER_UID).when(blackboxClient).getUidForLogin(any());
        doReturn(USER_UID).when(innerBlackboxClient).getUidForLogin(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Создание пользователя")
    void createUser() {
        var hermesStockmanDto = new HermesUserRequestDto();
        hermesStockmanDto.setEmail("ivanov.i.i@yandex.ru");
        hermesStockmanDto.setName("Ivanov Ivan Ivanovich");
        hermesStockmanDto.setStaffLogin("ivanov-ii");
        hermesStockmanDto.setRole(UserRole.STOCKMAN);
        hermesStockmanDto.setSortingCenterId(SORTING_CENTER_ID);

        caller.createOrUpdateStockman(hermesStockmanDto)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty());

        var user = testFactory.findUserByUid(USER_UID);
        assertThat(user.getEmail()).isEqualTo("ivanov.i.i@yandex.ru");
        assertThat(user.getName()).isEqualTo("Ivanov Ivan Ivanovich");
        assertThat(user.getStaffLogin()).isEqualTo("ivanov-ii");
        assertThat(user.getRole().name()).isEqualTo(UserRole.STOCKMAN.name());
        assertThat(user.getSortingCenter().getId()).isEqualTo(SORTING_CENTER_ID);
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    @SneakyThrows
    @DisplayName("Создание удаленного пользователя в другом СЦ")
    void recoveryUserDeletedAnotherSc() {
        var sc2 = testFactory.getSortingCenterById(20L);

        var hermesStockmanDto = new HermesUserRequestDto();
        hermesStockmanDto.setEmail("ivanov.i.i@yandex.ru");
        hermesStockmanDto.setName("Ivanov Ivan Ivanovich");
        hermesStockmanDto.setStaffLogin("ivanov-ii");
        hermesStockmanDto.setRole(UserRole.STOCKMAN);
        hermesStockmanDto.setSortingCenterId(SORTING_CENTER_ID);

        var content = caller.createOrUpdateStockman(hermesStockmanDto)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var response = JacksonUtil.fromString(content, HermesUserResponseDto.class);

        caller.deleteStockman(response.getId())
                .andExpect(status().isOk());

        // создаем пользователя в другом СЦ
        hermesStockmanDto.setSortingCenterId(sc2.getId());
        caller.createOrUpdateStockman(hermesStockmanDto)
                .andExpect(status().isOk());

        var user = userRepository.findByIdOrThrow(response.getId());
        assertThat(user.getSortingCenter()).isEqualTo(sc2);
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    @SneakyThrows
    @DisplayName("Не включен hrms флаг")
    void exceptionWhenHermesFeatureDisabled() {
        var hermesStockmanDto = new HermesUserRequestDto();
        hermesStockmanDto.setEmail("ivanov.i.i@yandex.ru");
        hermesStockmanDto.setName("Ivanov Ivan Ivanovich");
        hermesStockmanDto.setStaffLogin("ivanov-ii");
        hermesStockmanDto.setRole(UserRole.STOCKMAN);
        hermesStockmanDto.setSortingCenterId(SORTING_CENTER_ID);

        testFactory.setSortingCenterProperty(SORTING_CENTER_ID, SortingCenterPropertiesKey.THIRD_PARTY_SC, true);
        testFactory.setSortingCenterProperty(SORTING_CENTER_ID,
                SortingCenterPropertiesKey.HERMES_USER_MANAGEMENT, false);

        caller.createOrUpdateStockman(hermesStockmanDto)
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    @DisplayName("Обновление пользователя")
    void updateUser() {
        var sc = testFactory.getSortingCenterById(SORTING_CENTER_ID);
        var user = new User(sc, USER_UID, "ivanov.i.i@yandex.ru", "Иван Иванов", UserRole.STOCKMAN, "ivanov-ii");
        user = userRepository.saveAndFlush(user);

        HermesUserRequestDto hermesStockmanDto = new HermesUserRequestDto();
        hermesStockmanDto.setEmail("ivanov.i.i@yandex.ru");
        hermesStockmanDto.setName("Petrov Petr Petrovich");
        hermesStockmanDto.setStaffLogin("petrov-pp");
        hermesStockmanDto.setRole(UserRole.SENIOR_STOCKMAN);
        hermesStockmanDto.setSortingCenterId(sc.getId());

        caller.createOrUpdateStockman(hermesStockmanDto)
                .andExpect(status().isOk())
                .andExpect(content().json(JacksonUtil.toString(new HermesUserResponseDto(user.getId()))));

        var updatedUser = testFactory.findUserByUid(USER_UID);
        assertThat(updatedUser.getEmail()).isEqualTo("ivanov.i.i@yandex.ru");
        assertThat(updatedUser.getName()).isEqualTo("Petrov Petr Petrovich");
        assertThat(updatedUser.getStaffLogin()).isEqualTo("petrov-pp");
        assertThat(updatedUser.getRole().name()).isEqualTo(UserRole.SENIOR_STOCKMAN.name());
        assertThat(updatedUser.getSortingCenter().getId()).isEqualTo(SORTING_CENTER_ID);
        assertThat(updatedUser.isDeleted()).isFalse();
    }

    @Test
    @SneakyThrows
    @DisplayName("Удаление пользователя")
    void deleteUser() {
        var hermesStockmanDto = new HermesUserRequestDto();
        hermesStockmanDto.setEmail("ivanov.i.i@yandex.ru");
        hermesStockmanDto.setName("Ivanov Ivan Ivanovich");
        hermesStockmanDto.setStaffLogin("ivanov-ii");
        hermesStockmanDto.setRole(UserRole.STOCKMAN);
        hermesStockmanDto.setSortingCenterId(SORTING_CENTER_ID);

        var sc = testFactory.getSortingCenterById(SORTING_CENTER_ID);
        var user = testFactory.storedUser(sc, USER_UID, UserRole.STOCKMAN, "ivanov-ii");

        caller.deleteStockman(user.getId())
                .andExpect(status().isOk());

        var newUser = testFactory.findUserByUid(USER_UID);
        assertThat(newUser.isDeleted()).isTrue();
    }

    @Test
    @SneakyThrows
    @DisplayName("Проверка валидации email-домена (корректный домен)")
    void validateEmailDomainCorrect() {
        var hermesStockmanDto = new HermesUserRequestDto();
        hermesStockmanDto.setEmail("user@hrms-sc.ru");
        hermesStockmanDto.setName("Ivanov Ivan Ivanovich");
        hermesStockmanDto.setStaffLogin("ivanov-ii");
        hermesStockmanDto.setRole(UserRole.STOCKMAN);
        hermesStockmanDto.setSortingCenterId(SORTING_CENTER_ID);

        caller.createOrUpdateStockman(hermesStockmanDto)
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @DisplayName("Проверка валидации email-домена (некорректный домен)")
    void validateEmailDomainNotCorrect() {
        var hermesStockmanDto = new HermesUserRequestDto();
        hermesStockmanDto.setEmail("user@not-correct-domain.ru");
        hermesStockmanDto.setName("Ivanov Ivan Ivanovich");
        hermesStockmanDto.setStaffLogin("ivanov-ii");
        hermesStockmanDto.setRole(UserRole.STOCKMAN);
        hermesStockmanDto.setSortingCenterId(SORTING_CENTER_ID);

        caller.createOrUpdateStockman(hermesStockmanDto)
                .andExpect(status().isBadRequest());
    }
}
