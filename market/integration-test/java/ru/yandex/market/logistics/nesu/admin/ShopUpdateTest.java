package ru.yandex.market.logistics.nesu.admin;

import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.enums.ShopStatus;
import ru.yandex.market.logistics.nesu.model.dto.ShopUpdateDto;
import ru.yandex.market.logistics.nesu.service.marketid.MarketIdService;
import ru.yandex.market.logistics.nesu.utils.MarketIdFactory;

import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/shop/before/prepare_update.xml")
class ShopUpdateTest extends AbstractContextualTest {

    @Autowired
    private MarketIdService marketIdService;

    @BeforeEach
    void setup() {
        when(marketIdService.findAccountById(10001L)).thenReturn(Optional.of(MarketIdFactory.marketAccount()));
    }

    @Test
    @DisplayName("Обновить статус магазина OFF -> ACTIVE, магазин может быть активирован")
    @ExpectedDatabase(
        value = "/repository/shop/after/off_active.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateOffActiveNeedSettings() throws Exception {
        updateExec(50001L, ShopStatus.ACTIVE)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-update/off_active.json"));
    }

    @Test
    @DisplayName("Обновить статус магазина OFF -> ACTIVE, магазин не может быть активирован")
    @ExpectedDatabase(
        value = "/repository/shop/after/off_active_need_settings.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateOffActive() throws Exception {
        updateExec(50002L, ShopStatus.ACTIVE)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-update/off_need_settings.json"));
    }

    @Test
    @DisplayName("Обновить статус магазина ACTIVE -> OFF")
    @ExpectedDatabase(
        value = "/repository/shop/after/active_off.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateActiveOff() throws Exception {
        updateExec(50003L, ShopStatus.OFF)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-update/active_off.json"));
    }

    @Test
    @DisplayName("Обновить статус магазина NEED_SETTINGS -> OFF")
    @ExpectedDatabase(
        value = "/repository/shop/after/need_settings_off.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateNeedSettingsOff() throws Exception {
        updateExec(50004L, ShopStatus.OFF)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-update/need_settings_off.json"));
    }

    @Test
    @DisplayName("Валидация дто")
    void getShopNotFound() throws Exception {
        updateExec(50001L, null)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "shopStatus",
                "must not be null",
                "shopUpdateDto",
                "NotNull"
            )));
    }

    @ParameterizedTest(name = "[{index}]: Из {0} в {1}")
    @MethodSource
    @DisplayName("Невалидная смена статуса")
    void invalidStatusChange(ShopStatus oldStatus, ShopStatus newStatus, Long shopId) throws Exception {
        updateExec(shopId, newStatus)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                String.format("Cannot change shop %d status from %s to %s", shopId, oldStatus, newStatus)
            ));
    }

    @Nonnull
    private static Stream<Arguments> invalidStatusChange() {
        return Stream.of(
            Arguments.of(ShopStatus.OFF, ShopStatus.NEED_SETTINGS, 50001L),
            Arguments.of(ShopStatus.NEED_SETTINGS, ShopStatus.ACTIVE, 50004L),
            Arguments.of(ShopStatus.ACTIVE, ShopStatus.NEED_SETTINGS, 50003L)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Обновление на такой же статус - валидная ситуация")
    void validStatusNotChange(ShopStatus status, Long shopId, String responsePath) throws Exception {
        updateExec(shopId, status)
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> validStatusNotChange() {
        return Stream.of(
            Arguments.of(ShopStatus.OFF, 50001L, "controller/admin/shop-update/off_off.json"),
            Arguments.of(ShopStatus.ACTIVE, 50003L, "controller/admin/shop-update/active_active.json"),
            Arguments.of(
                ShopStatus.NEED_SETTINGS,
                50004L,
                "controller/admin/shop-update/need_settings_need_settings.json"
            )
        );
    }

    @Nonnull
    private ResultActions updateExec(long shopId, ShopStatus shopStatus) throws Exception {
        return mockMvc.perform(request(
            HttpMethod.PUT,
            "/admin/shops/" + shopId,
            ShopUpdateDto.builder().shopStatus(shopStatus).build()
        ));
    }
}
