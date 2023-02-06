package ru.yandex.market.sc.api.controller;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.userPassword.GeneratePasswordResponseDto;
import ru.yandex.market.sc.core.domain.userPassword.ValidatePasswordRequestDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author merak1t
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserPasswordControllerTest extends BaseApiControllerTest {

    @MockBean
    private Clock clock;

    private SortingCenter sortingCenter;
    private TestControllerCaller controllerCaller;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        testFactory.storedUser(sortingCenter, UID);
        testFactory.setupMockClock(clock);
        testFactory.increaseScOrderId();
        controllerCaller = TestControllerCaller.createCaller(mockMvc);
    }

    @DisplayName("success генерация пароля старшим смены")
    @Test
    @SneakyThrows
    void generatePasswordByMaster() {
        var masterStockman = testFactory.storedUser(sortingCenter, 666, UserRole.MASTER_STOCKMAN);
        controllerCaller.generatePassword(masterStockman).andReturn().getResponse();

        var generateResponse = controllerCaller.generatePassword(masterStockman)
                .andExpect(status().is2xxSuccessful());
        var generateDtoResp = readContentAsClass(generateResponse, GeneratePasswordResponseDto.class);

        assertThat(generateDtoResp.password()).isNotNull();
    }

    @DisplayName("success поиск существующего пароля старшего смены")
    @Test
    @SneakyThrows
    void createAndFindPasswordByMaster() {
        var masterStockman = testFactory.storedUser(sortingCenter, 666, UserRole.MASTER_STOCKMAN);

        var generateResponse = controllerCaller.generatePassword(masterStockman)
                .andExpect(status().is2xxSuccessful());
        var generateDtoResp = readContentAsClass(generateResponse, GeneratePasswordResponseDto.class);

        assertThat(generateDtoResp.password()).isNotNull();

        var generateResponse2 = controllerCaller.generatePassword(masterStockman)
                .andExpect(status().is2xxSuccessful());
        var generateDtoResp2 = readContentAsClass(generateResponse2, GeneratePasswordResponseDto.class);

        assertThat(generateDtoResp2.password()).isEqualTo(generateDtoResp.password());
    }

    @DisplayName("fail генерация пароля кладовщиками")
    @Test
    @SneakyThrows
    void generatePasswordByStockman() {

        var stockman = testFactory.storedUser(sortingCenter, 222, UserRole.STOCKMAN);
        var seniorStockman = testFactory.storedUser(sortingCenter, 333, UserRole.SENIOR_STOCKMAN);


        controllerCaller.generatePassword(stockman)
                .andExpect(status().is4xxClientError());
        controllerCaller.generatePassword(seniorStockman)
                .andExpect(status().is4xxClientError());
    }

    @DisplayName("success сканирование пароля")
    @Test
    @SneakyThrows
    void validatePassword() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().get();
        var stockman = testFactory.storedUser(sortingCenter, 222, UserRole.STOCKMAN);
        var masterStockman = testFactory.storedUser(sortingCenter, 666, UserRole.MASTER_STOCKMAN);

        var generateResponse = controllerCaller.generatePassword(masterStockman)
                .andExpect(status().is2xxSuccessful());
        var generateDtoResp = readContentAsClass(generateResponse, GeneratePasswordResponseDto.class);
        assertThat(generateDtoResp.password()).isNotNull();

        var request = new ValidatePasswordRequestDto(generateDtoResp.password(), order.getExternalId(), null, null);
        controllerCaller.validatePassword(request, stockman)
                .andExpect(status().is2xxSuccessful());
    }

    @DisplayName("fail сканирование использованного пароля")
    @Test
    @SneakyThrows
    void validateOldPassword() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().get();
        var stockman = testFactory.storedUser(sortingCenter, 222, UserRole.STOCKMAN);
        var masterStockman = testFactory.storedUser(sortingCenter, 666, UserRole.MASTER_STOCKMAN);

        var generateResponse = controllerCaller.generatePassword(masterStockman)
                .andExpect(status().is2xxSuccessful());
        var generateDtoResp = readContentAsClass(generateResponse, GeneratePasswordResponseDto.class);
        assertThat(generateDtoResp.password()).isNotNull();

        var request = new ValidatePasswordRequestDto(generateDtoResp.password(), order.getExternalId(), null, null);
        controllerCaller.validatePassword(request, stockman)
                .andExpect(status().is2xxSuccessful());

        controllerCaller.validatePassword(request, stockman)
                .andExpect(status().is4xxClientError());
    }

    @DisplayName("fail сканирование использованного пароля другим кладовщиком")
    @Test
    @SneakyThrows
    void validateOldPassword2() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().get();
        var stockman = testFactory.storedUser(sortingCenter, 222, UserRole.STOCKMAN);
        var stockman2 = testFactory.storedUser(sortingCenter, 333, UserRole.STOCKMAN);
        var masterStockman = testFactory.storedUser(sortingCenter, 666, UserRole.MASTER_STOCKMAN);

        var generateResponse = controllerCaller.generatePassword(masterStockman)
                .andExpect(status().is2xxSuccessful());
        var generateDtoResp = readContentAsClass(generateResponse, GeneratePasswordResponseDto.class);
        assertThat(generateDtoResp.password()).isNotNull();

        var request = new ValidatePasswordRequestDto(generateDtoResp.password(), order.getExternalId(), null, null);
        controllerCaller.validatePassword(request, stockman)
                .andExpect(status().is2xxSuccessful());

        controllerCaller.validatePassword(request, stockman2)
                .andExpect(status().is4xxClientError());

        var generateResponse2 = controllerCaller.generatePassword(masterStockman)
                .andExpect(status().is2xxSuccessful());
        var generateDtoResp2 = readContentAsClass(generateResponse2, GeneratePasswordResponseDto.class);
        assertThat(generateDtoResp2.password()).isNotNull();

        var request2 = new ValidatePasswordRequestDto(generateDtoResp2.password(), order.getExternalId(), null, null);

        controllerCaller.validatePassword(request2, stockman2)
                .andExpect(status().is2xxSuccessful());

        controllerCaller.validatePassword(request2, stockman)
                .andExpect(status().is4xxClientError());
    }

    @DisplayName("fail сканирование не верного пароля")
    @Test
    @SneakyThrows
    void validateWrongPassword() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().get();
        var stockman = testFactory.storedUser(sortingCenter, 222, UserRole.STOCKMAN);

        var request = new ValidatePasswordRequestDto("cheaterStockman", order.getExternalId(), null, null);
        controllerCaller.validatePassword(request, stockman)
                .andExpect(status().is4xxClientError());
    }
}
