package ru.yandex.market.logistics.nesu.controller.internal;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.enums.ShopStatus;
import ru.yandex.market.logistics.nesu.client.model.filter.ShopWithSendersFilter;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/shop/shop_with_senders.xml")
class InternalControllerShopWithSendersSearchTest extends AbstractContextualTest {
    @Autowired
    private ObjectMapper objectMapper;

    @JpaQueriesCount(1)
    @SneakyThrows
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArguments")
    @DisplayName("Успешный поиск магазинов")
    void searchShopWithSendersTest(
        @SuppressWarnings("unused") String caseName,
        ShopWithSendersFilter filter,
        String responsePath
    ) {
        search(filter)
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Triple.of("По shop-id", filter().shopIds(Set.of(1L, 2L)), "shop_with_two_senders_response"),
            Triple
                .of(
                    "По market-id",
                    filter().marketIds(Set.of(201L, 202L)),
                    "shop_with_two_senders_response"
                ),
            Triple.of("По статусу", filter().status(ShopStatus.ACTIVE), "shop_with_two_senders_response"),
            Triple.of(
                "По нескольким полям",
                filter()
                    .status(ShopStatus.NEED_SETTINGS)
                    .shopIds(Set.of(3L))
                    .marketIds(Set.of(203L)),
                "shop_with_senders_inactive_response"
            ),
            Triple.of("Пустой фильтр", filter(), "all_shop_with_senders_response")
        )
            .map(triple -> Arguments.of(
                triple.getLeft(),
                triple.getMiddle().build(),
                "controller/shop/" + triple.getRight() + ".json"
            ));
    }

    @SneakyThrows
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgumentsValidation")
    @DisplayName("Валидация фильтра")
    void searchShopWithSendersValidationTest(
        @SuppressWarnings("unused") String caseName,
        ShopWithSendersFilter filter,
        ValidationErrorData error
    ) {
        search(filter)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    @Nonnull
    private static Stream<Arguments> searchArgumentsValidation() {
        return Stream.of(
            Triple.of(
                "Null shop-id",
                filter().shopIds(Sets.newHashSet(1L, null)),
                fieldError(
                    "shopIds",
                    "must not contain nulls",
                    "shopWithSendersFilter",
                    "NotNullElements"
                )
            ),
            Triple.of(
                "Null market-id",
                filter().marketIds(Sets.newHashSet(1L, null)),
                fieldError(
                    "marketIds",
                    "must not contain nulls",
                    "shopWithSendersFilter",
                    "NotNullElements"
                )
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle().build(), triple.getRight()));
    }

    @Test
    @DisplayName("Пустой результат")
    void searchShopWithSendersEmptyTest() throws Exception {
        search(ShopWithSendersFilter.builder().shopIds(Set.of(42L)).build())
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY, true));
    }

    private static ShopWithSendersFilter.ShopWithSendersFilterBuilder filter() {
        return ShopWithSendersFilter.builder();
    }

    @Nonnull
    private ResultActions search(ShopWithSendersFilter filter) throws Exception {
        return mockMvc.perform(put("/internal/shops/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(filter)));
    }
}
