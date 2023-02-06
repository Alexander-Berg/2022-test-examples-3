package ru.yandex.market.logistics.nesu.controller.internal.registration;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.model.ConfigureShopDto;
import ru.yandex.market.logistics.nesu.jobs.producer.PushMarketIdToLmsProducer;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Конфигурирование магазина")
abstract class BaseShopConfigureTest extends AbstractContextualTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    protected PushMarketIdToLmsProducer pushMarketIdToLmsProducer;

    @BeforeEach
    void setup() {
        doNothing().when(pushMarketIdToLmsProducer).produceTask(anyLong(), anyLong());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(pushMarketIdToLmsProducer);
    }

    @Test
    @DisplayName("Магазин не существует")
    void shopNotFound() throws Exception {
        configureShop()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [1]"));
    }

    @Test
    @DisplayName("Невозможно сконфигурировать DaaS магазин")
    @DatabaseSetup("/controller/shop-registration/after_daas_registration.xml")
    @ExpectedDatabase(
        value = "/controller/shop-registration/after_daas_registration.xml",
        assertionMode = NON_STRICT
    )
    void daasFailure() throws Exception {
        configureShop()
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Shop with role DAAS cannot be configured"));
    }

    @Test
    @DisplayName("Передан идентификатор null как магазин")
    void nullShop() throws Exception {
        configureShop(null, defaultRequest())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("For input string: \"null\""));
    }

    @Test
    @DisplayName("Ранняя регистрация. Больше одной настройки магазина с партнером")
    @DatabaseSetup("/controller/shop-registration/two_settings.xml")
    void twoSettings() throws Exception {
        configureShop(3L, defaultRequest())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Cannot define shopPartnerSetting for shop 3 and type SUPPLIER. Found more than one setting"
            ));
    }

    @ParameterizedTest
    @MethodSource
    @DatabaseSetup(value = {
        "/controller/shop-registration/dropship_not_configured.xml",
        "/controller/shop-registration/supplier_not_configured.xml",
        "/controller/shop-registration/dbs_not_configured.xml",
    })
    @DisplayName("Ранняя регистрация. Невалидный запрос")
    void earlyRegisterValidation(
        @SuppressWarnings("unused") String name,
        Long shopId,
        List<String> fields
    ) throws Exception {
        configureShop(shopId, ConfigureShopDto.builder().build())
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                fields.stream()
                    .map(field -> fieldError(field, "must not be null", "configureShopDto", "NotNull"))
                    .collect(Collectors.toList())
            ));
    }

    @Nonnull
    private static Stream<Arguments> earlyRegisterValidation() {
        return Stream.of(
            Arguments.of("DBS", 4L, List.of("balanceClientId", "balanceContractId", "balancePersonId", "marketId")),
            Arguments.of("DROPSHIP", 2L, List.of("balanceClientId", "marketId")),
            Arguments.of("SUPPLIER", 3L, List.of("balanceClientId", "marketId"))
        );
    }

    @Nonnull
    protected ResultActions configureShop(@Nullable Long shopId, ConfigureShopDto request) throws Exception {
        return mockMvc.perform(
            post("/internal/shops/" + shopId + "/configure")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );
    }

    @Nonnull
    protected ResultActions configureShop() throws Exception {
        return configureShop(1L, defaultRequest());
    }

    @Nonnull
    protected ConfigureShopDto defaultRequest() {
        return configureShopDtoBuilder().build();
    }

    @Nonnull
    protected ConfigureShopDto.ConfigureShopDtoBuilder configureShopDtoBuilder() {
        return ConfigureShopDto.builder()
            .marketId(1L)
            .balanceClientId(250L)
            .balanceContractId(260L)
            .balancePersonId(200L);
    }
}
