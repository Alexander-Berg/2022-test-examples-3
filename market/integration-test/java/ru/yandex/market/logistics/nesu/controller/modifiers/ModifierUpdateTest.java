package ru.yandex.market.logistics.nesu.controller.modifiers;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.dto.modifier.DeliveryOptionModifierRequestDto;
import ru.yandex.market.logistics.nesu.dto.modifier.ModifierConditionRequestDto;
import ru.yandex.market.logistics.nesu.jobs.producer.ModifierUploadTaskProducer;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.utils.ShopAvailableDeliveriesUtils;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Обновление модификаторов опций доставки")
@DatabaseSetup({
    "/repository/shop-deliveries-availability/setup.xml",
    "/controller/modifier/modifier_setup.xml",
    "/controller/modifier/modifier_available_directly_delivery.xml",
    "/controller/modifier/updated_modifier_addition_delivery_service.xml",
})
class ModifierUpdateTest extends ModifierWriteBaseTest {

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private ModifierUploadTaskProducer producer;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(producer, lmsClient);
    }

    @BeforeEach
    void setup() {
        doNothing().when(producer).produceTask(anyLong());
    }

    @Test
    @DisplayName("Обновление модификатора")
    @ExpectedDatabase(
        value = "/controller/modifier/updated_modifier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void updateSuccessWithErasure() throws Exception {
        DeliveryOptionModifierRequestDto request = new DeliveryOptionModifierRequestDto();
        request.setIsDeliveryServiceEnabled(false)
            .setActive(true);

        updateModifier(1L, 1L, request)
            .andExpect(status().isOk());

        verify(producer).produceTask(1);
    }

    @Test
    @DisplayName("Модификатор с существующей СД, которая стала невалидной")
    void updateModifierWithExistingInvalidDeliveryService() throws Exception {
        when(lmsClient.searchPartners(
            createPartnerFilter(Set.of(3L, 5L)))
        ).thenReturn(List.of(
            LmsFactory.createPartner(3L, PartnerType.DELIVERY)
        ));

        DeliveryOptionModifierRequestDto updateModifierWithInvalidNewPartner = defaultModifier();
        updateModifierWithInvalidNewPartner.setCondition(
            (ModifierConditionRequestDto) new ModifierConditionRequestDto().setDeliveryServiceIds(Set.of(3L, 5L))
        );

        updateModifier(1L, 1L, updateModifierWithInvalidNewPartner)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [5]"));

        verify(lmsClient).searchPartners(SearchPartnerFilter.builder().setIds(Set.of(3L, 5L)).build());
    }

    @Test
    @DisplayName("Не найден модификатор")
    void updateModifierNotFound() throws Exception {
        updateModifier(2L, 1L, defaultModifier())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [DELIVERY_OPTION_MODIFIER] with ids [2]"));
    }

    @Test
    @DisplayName("Не найден сендер")
    void updateSenderNotFound() throws Exception {
        updateModifier(1L, 2L, defaultModifier())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [2]"));
    }

    @Test
    @DisplayName("Модификатор другого сендера")
    void updateModifierNoSenderAccess() throws Exception {
        updateModifier(1L, 11L, defaultModifier())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [DELIVERY_OPTION_MODIFIER] with ids [1]"));
    }

    @Test
    @DisplayName("Не найдена СД при обновлении")
    void updateDeliveryServiceNotFound() throws Exception {
        when(lmsClient.searchPartners(
            createPartnerFilter(Set.of(3L)))
        ).thenReturn(List.of(
            LmsFactory.createPartner(3L, PartnerType.DELIVERY)
        ));

        DeliveryOptionModifierRequestDto modifier = defaultModifier();
        modifier.setCondition(
            (ModifierConditionRequestDto) new ModifierConditionRequestDto().setDeliveryServiceIds(Set.of(3L, 100L))
        );
        updateModifier(1L, 1L, modifier)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(PARTNER_VALIDATION_ERROR_MESSAGE));

        verify(lmsClient).searchPartners(createPartnerFilter(Set.of(3L)));
        verify(lmsClient).searchPartners(LmsFactory.createPartnerFilter(Set.of(100L), null));
        verify(lmsClient).searchPartners(ShopAvailableDeliveriesUtils.OWN_DELIVERY_FILTER);
    }

    @Nonnull
    private SearchPartnerFilter createPartnerFilter(Set<Long> ids) {
        return SearchPartnerFilter.builder()
            .setIds(ids)
            .build();
    }

    @Nonnull
    private ResultActions updateModifier(
        long modifierId,
        long senderId,
        DeliveryOptionModifierRequestDto request
    ) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.PUT, "/back-office/settings/modifiers/" + modifierId, request)
                .param("shopId", "1")
                .param("senderId", String.valueOf(senderId))
                .param("userId", "1")
        );
    }

    @Nonnull
    @Override
    protected ResultActions createOrUpdateModifier(DeliveryOptionModifierRequestDto request) throws Exception {
        return updateModifier(1L, 1L, request);
    }
}
