package ru.yandex.market.logistics.lom.service.validation;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.LmsModelFactory;
import ru.yandex.market.logistics.lom.converter.lms.AddressLmsConverter;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.embedded.Address;
import ru.yandex.market.logistics.lom.entity.embedded.PickupPoint;
import ru.yandex.market.logistics.lom.entity.enums.DeliveryType;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.PickupAddressValidatorAndEnricher;
import ru.yandex.market.logistics.lom.lms.client.LmsLomLightClient;
import ru.yandex.market.logistics.lom.lms.model.LogisticsPointLightModel;
import ru.yandex.market.logistics.lom.service.partner.LogisticsPointsServiceImpl;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Валидация и обогащение адреса ПВЗ")
class PickupAddressValidatorAndEnricherTest extends AbstractTest {
    private static final long LOGISTICS_POINT_ID = 100500L;
    private final LMSClient lmsClient = mock(LMSClient.class);
    private final LmsLomLightClient lmsLomLightClient = mock(LmsLomLightClient.class);
    private final PickupAddressValidatorAndEnricher validatorAndEnricher = new PickupAddressValidatorAndEnricher(
        new LogisticsPointsServiceImpl(lmsLomLightClient),
        new AddressLmsConverter()
    );

    @Test
    @DisplayName("Тип доставки курьером, валидация проходит успешно, обогащение отсутствует")
    void courierDeliveryType() {
        Order order = new Order().setDeliveryType(DeliveryType.COURIER);
        ValidateAndEnrichResults results = doValidateAndEnrich(order);
        softly.assertThat(results.isValidationPassed()).isTrue();
    }

    @Test
    @DisplayName("Данные по ПВЗ уже обогащены")
    void alreadyEnriched() {
        Order order = createOrder();
        order.getPickupPoint()
            .setExternalId("100500")
            .setAddress(
                new Address()
                    .setCountry("Россия")
                    .setLocality("Новосибирск")
                    .setRegion("Новосибирская область")
            );
        ValidateAndEnrichResults results = doValidateAndEnrich(order);
        softly.assertThat(results.isValidationPassed()).isTrue();
    }

    @Test
    @DisplayName("Логистическая точка не найдена")
    void logisticsPointNotFound() {
        Order order = createOrder();
        when(lmsClient.getLogisticsPoint(LOGISTICS_POINT_ID)).thenReturn(Optional.empty());
        ValidateAndEnrichResults results = doValidateAndEnrich(order);
        softly.assertThat(results.getErrorMessage())
            .isEqualTo("Delivery type is POST but no logistics point found with id 100500");
    }

    @Test
    @DisplayName("Адрес логистической точки null")
    void addressNull() {
        Order order = createOrder();
        when(lmsLomLightClient.getLogisticsPoint(LOGISTICS_POINT_ID))
            .thenReturn(
                Optional.of(LogisticsPointLightModel.build(createPickupPointResponseBuilder().address(null).build()))
            );
        ValidateAndEnrichResults results = doValidateAndEnrich(order);
        softly.assertThat(results.getErrorMessage())
            .isEqualTo("Null logistics point address for logistics point with id = 100500");
    }

    @Test
    @DisplayName("Внешний идентификатор логистическй точки null")
    void externalIdNull() {
        Order order = createOrder();
        when(lmsLomLightClient.getLogisticsPoint(LOGISTICS_POINT_ID))
            .thenReturn(Optional.of(
                LogisticsPointLightModel.build(createPickupPointResponseBuilder().externalId(null).build())
            ));
        ValidateAndEnrichResults results = doValidateAndEnrich(order);
        softly.assertThat(results.getErrorMessage())
            .isEqualTo("Null logistics point external id for logistics point with id = 100500");
    }

    @Test
    @DisplayName("Успех валидации")
    void validationSucceeded() {
        Order order = createOrder();
        LogisticsPointResponse pickupPoint = createPickupPointResponseBuilder().build();
        when(lmsLomLightClient.getLogisticsPoint(LOGISTICS_POINT_ID))
            .thenReturn(Optional.of(LogisticsPointLightModel.build(pickupPoint)));

        ValidateAndEnrichContext context = new ValidateAndEnrichContext();
        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(context.getPickupPointData())
            .isEqualToComparingFieldByFieldRecursively(pickupPoint);
    }

    @Nonnull
    private ValidateAndEnrichResults doValidateAndEnrich(Order order) {
        ValidateAndEnrichContext context = new ValidateAndEnrichContext();
        return validatorAndEnricher.validateAndEnrich(order, context);
    }

    @Nonnull
    private Order createOrder() {
        return new Order()
            .setDeliveryType(DeliveryType.POST)
            .setPickupPoint(createPickupPoint());
    }

    @Nonnull
    private PickupPoint createPickupPoint() {
        return new PickupPoint().setPickupPointId(LOGISTICS_POINT_ID);
    }

    @Nonnull
    private LogisticsPointResponse.LogisticsPointResponseBuilder createPickupPointResponseBuilder() {
        return LmsModelFactory.createPickupPointResponseBuilder(LOGISTICS_POINT_ID);
    }
}
