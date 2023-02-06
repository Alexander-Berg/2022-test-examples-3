package ru.yandex.market.logistics.nesu.controller.business;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics4shops.client.api.PartnerMappingApi;
import ru.yandex.market.logistics4shops.client.model.DeletePartnerMappingRequest;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;

@ParametersAreNonnullByDefault
@DisplayName("Деактивация бизнес-склада")
@DatabaseSetup("/controller/business/before/prepare_deactivation.xml")
class DeactivateBusinessWarehouseTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private PartnerMappingApi partnerMappingApi;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, mbiApiClient, partnerMappingApi);
    }

    @Test
    @DisplayName("Успех")
    @ExpectedDatabase(
        value = "/controller/business/after/success_deactivate.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() throws Exception {
        deactivate(1L).andExpect(status().isOk());
        verify(lmsClient).deactivateBusinessWarehouse(1000L);
        verify(mbiApiClient).deletePartnerFulfillmentLinks(1L, Set.of(1000L));
        verify(partnerMappingApi).deletePartnerMapping(
            new DeletePartnerMappingRequest().mbiPartnerId(1L).lmsPartnerId(1000L)
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Ошибки")
    @MethodSource
    void deactivationFail(
        @SuppressWarnings("unused") String name,
        Long shopId,
        HttpStatus expectedStatus,
        String message
    ) throws Exception {
        deactivate(shopId).andExpect(status().is(expectedStatus.value())).andExpect(errorMessage(message));
    }

    @Nonnull
    private static Stream<Arguments> deactivationFail() {
        return Stream.of(
            Arguments.of(
                "Нет настроек",
                2L,
                HttpStatus.BAD_REQUEST,
                "No ShopPartnerSetting for shopId=2 with type=DROPSHIP"
            ),
            Arguments.of(
                "Несколько настроек с одним партнером разных типов",
                3L,
                HttpStatus.BAD_REQUEST,
                "There are more than one ShopPartnerSetting for shopId=3"
            ),
            Arguments.of(
                "Несколько настроек с разными партнерами одного типа",
                4L,
                HttpStatus.BAD_REQUEST,
                "There are more than one ShopPartnerSetting for shopId=4"
            ),
            Arguments.of(
                "Магазин не существует",
                5L,
                HttpStatus.NOT_FOUND,
                "Failed to find [SHOP] with ids [5]"
            )
        );
    }

    @Nonnull
    private ResultActions deactivate(Long shopId) throws Exception {
        return mockMvc.perform(put("/internal/business-warehouse/{shopId}/deactivate", shopId));
    }
}
