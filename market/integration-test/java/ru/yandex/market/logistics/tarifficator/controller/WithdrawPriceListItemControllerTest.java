package ru.yandex.market.logistics.tarifficator.controller;

import java.math.BigDecimal;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.dto.WithdrawPriceListItemSearchDto;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;
import ru.yandex.market.logistics.tarifficator.util.ValidationUtil;

import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/withdraw/price-list-items/before/search_prepare.xml")
@ParametersAreNonnullByDefault
public class WithdrawPriceListItemControllerTest extends AbstractContextualTest {

    @DisplayName("Поиск элемента заборного прайс-листа")
    @MethodSource("searchArgument")
    @ParameterizedTest(name = "{index} : {0}")
    void searchWithdrawPriceListItem(
        String displayName,
        WithdrawPriceListItemSearchDto searchDto,
        String responsePath
    ) throws Exception {
        searchItems(searchDto)
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Значение volume не равно minVolume и maxVolume",
                new WithdrawPriceListItemSearchDto().setLocationZoneId(1L).setVolume(BigDecimal.valueOf(0.5)),
                "controller/withdraw/price-list-items/response/id_3.json"
            ),
            Arguments.of(
                "Значение volume равно minVolume",
                new WithdrawPriceListItemSearchDto().setLocationZoneId(1L).setVolume(BigDecimal.valueOf(0.3)),
                "controller/withdraw/price-list-items/response/id_2.json"
            ),
            Arguments.of(
                "Значение volume равно maxVolume",
                new WithdrawPriceListItemSearchDto().setLocationZoneId(1L).setVolume(BigDecimal.valueOf(1)),
                "controller/withdraw/price-list-items/response/id_3.json"
            )
        );
    }

    @Test
    @DisplayName("Элемент заборного прайс-листа не найден")
    void withdrawPriceListItemNotFound() throws Exception {
        searchItems(new WithdrawPriceListItemSearchDto().setLocationZoneId(10L).setVolume(BigDecimal.valueOf(0.5)))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage(
                "Failed to find [WITHDRAW_PRICE_LIST_ITEM] with locationZoneId [10] and volume [0.5]"
            ));
    }

    @MethodSource("invalidRequestProvider")
    @ParameterizedTest(name = "{index} : {0}")
    void validateDto(
        String displayName,
        WithdrawPriceListItemSearchDto searchDto,
        ResultMatcher resultMatcher
    ) throws Exception {
        searchItems(searchDto)
            .andExpect(status().isBadRequest())
            .andExpect(resultMatcher);
    }

    @Nonnull
    private static Stream<Arguments> invalidRequestProvider() {
        return Stream.of(
            Arguments.of(
                "Не указана зона локации",
                defaultWithdrawPriceListItemSearchDto().setLocationZoneId(null),
                ValidationUtil.fieldValidationError("locationZoneId", "must not be null")
            ),
            Arguments.of(
                "Не указан объём",
                defaultWithdrawPriceListItemSearchDto().setVolume(null),
                ValidationUtil.fieldValidationError("volume", "must not be null")
            )
        );
    }

    @Nonnull
    private ResultActions searchItems(WithdrawPriceListItemSearchDto searchDto) throws Exception {
        return mockMvc.perform(TestUtils.request(
            HttpMethod.PUT,
            "/withdraw-price-list-item",
            searchDto
        ));
    }

    @Nonnull
    private static WithdrawPriceListItemSearchDto defaultWithdrawPriceListItemSearchDto() {
        return new WithdrawPriceListItemSearchDto()
            .setLocationZoneId(1L)
            .setVolume(BigDecimal.valueOf(0.5));
    }
}
