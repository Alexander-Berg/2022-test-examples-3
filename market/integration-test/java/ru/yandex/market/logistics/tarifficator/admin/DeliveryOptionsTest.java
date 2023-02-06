package ru.yandex.market.logistics.tarifficator.admin;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.admin.dto.AdminDeliveryOptionSearchRequestDto;
import ru.yandex.market.logistics.tarifficator.admin.enums.AdminTariffTag;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.NULL_VALIDATION_INFO;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.createArgumentsForValidation;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.fieldValidationFrontError;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.getNotEmptyValidationInfo;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.getPositiveValidationInfo;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск опций доставки через админку")
@DatabaseSetup(
    {
        "/tags/tags.xml",
        "/tariffs/courier_without_active_price_lists_1.xml",
    }
)
@DatabaseSetup(
    value = {
        "/tariffs/pick_up_100.xml",
        "/tariffs/post_200.xml",
        "/tariffs/post_400_big_dimension_weight.xml",
        "/tariffs/pick_up_500.xml",
        "/tariffs/post_900.xml",
    },
    type = DatabaseOperation.INSERT
)
class DeliveryOptionsTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получение страницы для поиска опций")
    void getDeliveryOptionsSearchPage() throws Exception {
        mockMvc.perform(get("/admin/delivery-options"))
            .andExpect(TestUtils.jsonContent("controller/delivery-options/response/get_options_search_detail.json"));
    }

    @Test
    @DisplayName("Поиск опций")
    void searchOptions() throws Exception {
        getOptions(validRequest(null, null))
            .andExpect(TestUtils.jsonContent("controller/delivery-options/response/get_options_grid.json"));
    }

    @Test
    @DisplayName("Поиск опций по всем тарифам")
    void searchOption1s() throws Exception {
        AdminDeliveryOptionSearchRequestDto dto = validRequest(null, null);
        dto.setTariffIds(null);
        getOptions(dto)
            .andExpect(TestUtils.jsonContent("controller/delivery-options/response/get_options_all_tariffs.json"));
    }

    @Test
    @DisplayName("Поиск опций по тегу")
    void searchOptionsByTag() throws Exception {
        AdminDeliveryOptionSearchRequestDto dto = validRequest(null, null);
        dto.setTariffIds(null);
        dto.setTariffTags(Set.of(AdminTariffTag.DAAS));
        getOptions(dto)
            .andExpect(TestUtils.jsonContent("controller/delivery-options/response/get_admin_options_by_tag.json"));
    }

    @Test
    @DisplayName("Поиск опций по нескольким тегам")
    void searchOptionsByTags() throws Exception {
        AdminDeliveryOptionSearchRequestDto dto = validRequest(null, null);
        dto.setTariffIds(Set.of(1L, 200L));
        dto.setTariffTags(Set.of(AdminTariffTag.DAAS, AdminTariffTag.BERU_CROSSDOCK));
        getOptions(dto)
            .andExpect(TestUtils.jsonContent(
                "controller/delivery-options/response/get_admin_options_by_multiple_tags.json"
            ));
    }

    @Test
    @DisplayName("Поиск опций по совпадающей правой границе")
    void searchOptionsExactRightValue() throws Exception {
        AdminDeliveryOptionSearchRequestDto dto = validRequest(null, null);
        dto.setWeight(BigDecimal.valueOf(20));
        getOptions(dto).andExpect(TestUtils.jsonContent("controller/delivery-options/response/get_options_grid.json"));
    }

    @Nonnull
    private ResultActions getOptions(AdminDeliveryOptionSearchRequestDto requestDto) throws Exception {
        return mockMvc.perform(TestUtils.request(
            HttpMethod.POST,
            "/admin/delivery-options/search",
            requestDto
        ));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("validateRequestDtoArguments")
    @DisplayName("Валидация поиска опций")
    void searchValidation(
        @SuppressWarnings("unused") String testName,
        String fieldPath,
        String fieldError,
        AdminDeliveryOptionSearchRequestDto dto
    ) throws Exception {
        getOptions(dto)
            .andExpect(status().isBadRequest())
            .andExpect(fieldValidationFrontError(fieldPath, fieldError));
    }

    private static Stream<Arguments> validateRequestDtoArguments() {
        return Stream.of(
            createArgumentsForValidation(
                NULL_VALIDATION_INFO,
                AdminDeliveryOptionSearchRequestDto.class,
                DeliveryOptionsTest::validRequest
            ),
            createArgumentsForValidation(
                getPositiveValidationInfo(BigDecimal.valueOf(-1)),
                AdminDeliveryOptionSearchRequestDto.class,
                DeliveryOptionsTest::validRequest
            ),
            createArgumentsForValidation(
                getPositiveValidationInfo(BigDecimal.valueOf(-1)),
                AdminDeliveryOptionSearchRequestDto.class,
                DeliveryOptionsTest::validRequest
            ),
            createArgumentsForValidation(
                getNotEmptyValidationInfo(Set.of()),
                AdminDeliveryOptionSearchRequestDto.class,
                DeliveryOptionsTest::validRequest
            )
        )
            .flatMap(Function.identity());
    }

    @Nonnull
    protected static AdminDeliveryOptionSearchRequestDto validRequest(@Nullable Field field, @Nullable Object value) {
        AdminDeliveryOptionSearchRequestDto build = AdminDeliveryOptionSearchRequestDto.builder()
            .tariffIds(ImmutableSet.of(1L, 100L, 200L, 300L))
            .locationFrom(213)
            .locationTo(197)
            .date(LocalDateTime.of(2019, 8, 22, 11, 0, 0).toInstant(ZoneOffset.UTC))
            .weight(new BigDecimal("19"))
            .length(50)
            .width(30)
            .height(10)
            .isPublic(false)
            .build();
        TestUtils.setFieldValue(build, field, value);
        return build;
    }
}
