package ru.yandex.market.logistics.nesu.controller.modifiers;

import java.math.BigDecimal;
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
import ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils;
import ru.yandex.market.logistics.nesu.dto.enums.DeliveryType;
import ru.yandex.market.logistics.nesu.dto.enums.NumericValueOperationType;
import ru.yandex.market.logistics.nesu.dto.modifier.DeliveryOptionModifierRequestDto;
import ru.yandex.market.logistics.nesu.dto.modifier.ModifierConditionRequestDto;
import ru.yandex.market.logistics.nesu.jobs.producer.ModifierUploadTaskProducer;
import ru.yandex.market.logistics.nesu.model.entity.ServiceType;
import ru.yandex.market.logistics.nesu.utils.ShopAvailableDeliveriesUtils;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartnerFilter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Создание модификаторов опций доставки")
@DatabaseSetup({
    "/repository/shop-deliveries-availability/setup.xml",
    "/controller/modifier/modifier_setup.xml"
})
class ModifierCreateTest extends ModifierWriteBaseTest {
    private static final long AVAILABLE_DIRECTLY_DELIVERY_SERVICE_ID = 3L;
    private static final long AVAILABLE_VIA_SC_DELIVERY_SERVICE_ID = 4L;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private ModifierUploadTaskProducer producer;

    @BeforeEach
    void setup() {
        doNothing().when(producer).produceTask(anyLong());
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(producer, lmsClient);
    }

    @Test
    @DisplayName("Сохранение полностью заполненного модификатора, СД доступна напрямую")
    @ExpectedDatabase(
        value = "/controller/modifier/modifier_available_directly_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createFullModifierAvailableDirectlyDs() throws Exception {
        createFullModifier(AVAILABLE_DIRECTLY_DELIVERY_SERVICE_ID);
        verify(lmsClient).searchPartners(createPartnerFilter(Set.of(3L), null));
    }

    @Test
    @DisplayName("Сохранение полностью заполненного модификатора, СД доступна через СЦ")
    @ExpectedDatabase(
        value = "/controller/modifier/modifier_available_via_sc_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createFullModifierAvailableViaScDs() throws Exception {
        createFullModifier(AVAILABLE_VIA_SC_DELIVERY_SERVICE_ID);
        verify(lmsClient).searchPartners(createPartnerFilter(Set.of(4L), null));
        verify(lmsClient).searchPartnerRelation(ShopAvailableDeliveriesUtils.SORTING_CENTER_RELATION_FILTER);
    }

    @Test
    @DisplayName("Сохранение полностью заполненного модификатора, собственная СЦ")
    @ExpectedDatabase(
        value = "/controller/modifier/modifier_own_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createFullModifierOwnDs() throws Exception {
        createFullModifier(TestOwnDeliveryUtils.OWN_PARTNER_ID);
    }

    private void createFullModifier(long deliveryServiceId) throws Exception {
        ShopAvailableDeliveriesUtils.mockShopAvailableDeliveries(lmsClient);
        ModifierConditionRequestDto condition = new ModifierConditionRequestDto();
        condition.setDestinations(Set.of(213, 40))
            .setPriceRange(createRange(0L, 10L))
            .setPricePercent(BigDecimal.valueOf(37L))
            .setWeightRange(createRange(1L, 11L))
            .setChargeableWeightRange(createRange(2L, 12L))
            .setItemDimensionRange(createRange(3L, 13L))
            .setDeliveryType(DeliveryType.COURIER)
            .setDeliveryServiceIds(Set.of(deliveryServiceId));
        DeliveryOptionModifierRequestDto request = new DeliveryOptionModifierRequestDto();
        request.setCondition(condition)
            .setCostRule(createRule(
                NumericValueOperationType.MULTIPLY,
                new BigDecimal("1.25"),
                createRange(100L, 500L)
            ))
            .setTimeRule(createRule(NumericValueOperationType.ADD, BigDecimal.ONE, createRange(5L, 15L)))
            .setIsDeliveryServiceEnabled(false)
            .setPaidByCustomerServices(Set.of(ServiceType.SORT, ServiceType.INSURANCE))
            .setActive(true);
        createModifier(request)
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verify(producer).produceTask(1L);
        verify(lmsClient).searchPartners(ShopAvailableDeliveriesUtils.OWN_DELIVERY_FILTER);
    }

    @Test
    @DisplayName("Не найдена СД")
    void createDeliveryServiceNotFound() throws Exception {
        ShopAvailableDeliveriesUtils.mockShopAvailableDeliveries(lmsClient);
        DeliveryOptionModifierRequestDto request = defaultModifier()
            .setCondition(
                (ModifierConditionRequestDto) new ModifierConditionRequestDto()
                    .setDeliveryServiceIds(Set.of(100L))
            );

        createModifier(request)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(PARTNER_VALIDATION_ERROR_MESSAGE));

        verify(lmsClient).searchPartners(createPartnerFilter(Set.of(100L), null));
        verify(lmsClient).searchPartners(ShopAvailableDeliveriesUtils.OWN_DELIVERY_FILTER);
    }

    @Test
    @DisplayName("Не найден сендер")
    void createSenderNotFound() throws Exception {
        createModifier(2, defaultModifier())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [2]"));
    }

    @Nonnull
    private ResultActions createModifier(DeliveryOptionModifierRequestDto request) throws Exception {
        return createModifier(1, request);
    }

    @Nonnull
    private ResultActions createModifier(long senderId, DeliveryOptionModifierRequestDto request) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.POST, "/back-office/settings/modifiers", request)
                .param("shopId", "1")
                .param("senderId", String.valueOf(senderId))
                .param("userId", "1")
        );
    }

    @Nonnull
    @Override
    protected ResultActions createOrUpdateModifier(DeliveryOptionModifierRequestDto request) throws Exception {
        return createModifier(request);
    }
}
