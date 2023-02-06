package ru.yandex.market.logistics.tarifficator.admin.tariff;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.admin.dto.TariffCreateDto;
import ru.yandex.market.logistics.tarifficator.admin.enums.AdminDeliveryMethod;
import ru.yandex.market.logistics.tarifficator.model.enums.TariffType;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.NOT_BLANK_VALIDATION_INFO;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.NULL_VALIDATION_INFO;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.createArgumentsForValidation;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.fieldValidationFrontError;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.getSizeBetweenValidationInfo;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание тарифа через админку")
class CreateTariffTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @MethodSource("invalidRequestProvider")
    @DisplayName("Валидация параметров")
    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    void validateDto(
        @SuppressWarnings("unused") String testName,
        String fieldPath,
        String fieldError,
        TariffCreateDto tariffCreateDto
    ) throws Exception {
        createTariff(tariffCreateDto)
            .andExpect(status().isBadRequest())
            .andExpect(fieldValidationFrontError(fieldPath, fieldError));
    }

    private static Stream<Arguments> invalidRequestProvider() {
        return Stream.of(
            createArgumentsForValidation(
                NULL_VALIDATION_INFO,
                TariffCreateDto.class,
                CreateTariffTest::validRequest
            ),
            createArgumentsForValidation(
                NOT_BLANK_VALIDATION_INFO,
                TariffCreateDto.class,
                CreateTariffTest::validRequest
            ),
            createArgumentsForValidation(
                getSizeBetweenValidationInfo(0, 128, "a".repeat(129)),
                TariffCreateDto.class,
                CreateTariffTest::validRequest
            )
        )
            .flatMap(Function.identity());
    }

    @Test
    @DisplayName("Создание тарифа общего характера")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/create_tariff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createTariffGeneral() throws Exception {
        mockLmsSearchPartner(PartnerType.DELIVERY);
        createTariff(validRequest(null, null))
            .andExpect(status().isOk())
            .andExpect(content().string(Long.toString(1L)));
    }

    @Test
    @DisplayName("Создание тарифа собственной доставки")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/create_tariff_own_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createTariffOwnDelivery() throws Exception {
        mockLmsSearchPartner(PartnerType.OWN_DELIVERY);
        createTariff(validRequest(null, null).setType(TariffType.OWN_DELIVERY))
            .andExpect(status().isOk())
            .andExpect(content().string(Long.toString(1L)));
    }
    @Test
    @DisplayName("Создание тарифа курьерской службы маркета")
    @ExpectedDatabase(
        value = "/controller/admin/tariffs/after/create_tariff_market_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createTariffMarketCourier() throws Exception {
        mockLmsSearchPartner(PartnerType.DELIVERY);
        createTariff(validRequest(null, null).setType(TariffType.MARKET_COURIER))
            .andExpect(status().isOk())
            .andExpect(content().string(Long.toString(1L)));
    }

    @Test
    @DisplayName("Создание тарифа с нарушением ограничения уникальности")
    @DatabaseSetup("/controller/admin/tariffs/after/create_tariff.xml")
    void createTariffWithUniqueConstraintViolation() throws Exception {
        mockLmsSearchPartner(PartnerType.DELIVERY);
        createTariff(validRequest(null, null))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Tariff with name = 'Название тарифа' already exists for partner with id = 1"));
    }

    @Test
    @DisplayName("Создание тарифа с несуществующим партнёром")
    void createTariffWithNotExistingPartner() throws Exception {
        createTariff(validRequest(null, null))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [[1]]"));
    }

    @Test
    @DisplayName("Создание тарифа с неподходящим партнёром")
    void createTariffWithIncompatiblePartner() throws Exception {
        mockLmsSearchPartner(PartnerType.FULFILLMENT);
        createTariff(validRequest(null, null))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Partner type mismatch: tariff type 'GENERAL'"
                + " is not compatible with partner type 'FULFILLMENT' (id = 1)"));
    }

    private void mockLmsSearchPartner(PartnerType partnerType) {
        doReturn(List.of(PartnerResponse.newBuilder()
            .id(1)
            .partnerType(partnerType)
            .build()
        ))
            .when(lmsClient)
            .searchPartners(eq(SearchPartnerFilter.builder()
                .setIds(Set.of(1L))
                .build()));
    }

    @Nonnull
    private ResultActions createTariff(TariffCreateDto tariffCreateDto) throws Exception {
        return mockMvc.perform(TestUtils.request(HttpMethod.POST, "/admin/tariffs", tariffCreateDto));
    }

    @Nonnull
    private static TariffCreateDto validRequest(@Nullable Field field, @Nullable Object value) {
        TariffCreateDto dto = new TariffCreateDto()
            .setPartnerId(1L)
            .setDeliveryMethod(AdminDeliveryMethod.PICKUP)
            .setName("Название тарифа")
            .setType(TariffType.GENERAL)
            .setCode("Код тарифа")
            .setDescription("Описание тарифа")
            .setEnabled(false);
        TestUtils.setFieldValue(dto, field, value);
        return dto;
    }
}
