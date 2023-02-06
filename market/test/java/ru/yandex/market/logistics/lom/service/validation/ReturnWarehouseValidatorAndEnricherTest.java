package ru.yandex.market.logistics.lom.service.validation;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.LmsModelFactory;
import ru.yandex.market.logistics.lom.converter.EnumConverter;
import ru.yandex.market.logistics.lom.converter.lms.AddressLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.ContactLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.DeliveryTypeLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.LogisticsPointLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.PartnerExternalParamLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.PartnerTypeLmsConverter;
import ru.yandex.market.logistics.lom.converter.lms.ScheduleDayLmsConverter;
import ru.yandex.market.logistics.lom.entity.Contact;
import ru.yandex.market.logistics.lom.entity.Location;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.Phone;
import ru.yandex.market.logistics.lom.entity.WarehouseWorkTime;
import ru.yandex.market.logistics.lom.entity.embedded.Address;
import ru.yandex.market.logistics.lom.entity.embedded.Fio;
import ru.yandex.market.logistics.lom.entity.embedded.TimeInterval;
import ru.yandex.market.logistics.lom.entity.enums.LocationType;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.LogisticsPointValidatorAndEnricher;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.ReturnWarehouseValidatorAndEnricher;
import ru.yandex.market.logistics.lom.lms.client.LmsLomLightClient;
import ru.yandex.market.logistics.lom.lms.model.LogisticsPointLightModel;
import ru.yandex.market.logistics.lom.service.partner.LogisticsPointsService;
import ru.yandex.market.logistics.lom.service.partner.LogisticsPointsServiceImpl;
import ru.yandex.market.logistics.lom.service.partner.PartnerServiceImpl;
import ru.yandex.market.logistics.lom.utils.PlatformClientUtils;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Валидация и обогащение возвратного склада")
class ReturnWarehouseValidatorAndEnricherTest extends AbstractTest {
    private static final long RETURN_SORTING_CENTER_ID = 1;
    private static final LogisticsPointFilter FILTER = LogisticsPointFilter.newBuilder()
        .partnerIds(Set.of(RETURN_SORTING_CENTER_ID))
        .type(PointType.WAREHOUSE)
        .active(true)
        .build();

    private final LmsLomLightClient lmsLomLightClient = mock(LmsLomLightClient.class);
    private final LogisticsPointsService logisticsPointsService = new LogisticsPointsServiceImpl(lmsLomLightClient);
    private final ReturnWarehouseValidatorAndEnricher enricher = new ReturnWarehouseValidatorAndEnricher(
        logisticsPointsService,
        new LogisticsPointValidatorAndEnricher(
            logisticsPointsService,
            new LogisticsPointLmsConverter(
                new AddressLmsConverter(),
                new ScheduleDayLmsConverter(),
                new ContactLmsConverter()
            ),
            new PartnerServiceImpl(
                lmsLomLightClient,
                new DeliveryTypeLmsConverter(),
                new PartnerTypeLmsConverter(new EnumConverter()),
                new PartnerExternalParamLmsConverter()
            )
        )
    );

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsLomLightClient);
    }

    @Test
    @DisplayName("Склад возвратного сортировочного центра не найден")
    void returnSortingCenterWarehouseNotFound() {
        Order order = new Order().setReturnSortingCenterId(RETURN_SORTING_CENTER_ID);
        when(lmsLomLightClient.getLogisticsPoints(refEq(FILTER))).thenReturn(List.of());
        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());

        softly.assertThat(results.isValidationPassed()).isFalse();
        softly.assertThat(results.getErrorMessage()).isEqualTo(String.format(
            "Return warehouse was not set, because there are no active warehouses for partner with id %s",
            RETURN_SORTING_CENTER_ID
        ));

        verify(lmsLomLightClient).getLogisticsPoints(refEq(FILTER));
    }

    @Test
    @DisplayName("Успех обогащения заказа (ядо)")
    void enrichingSucceeded() {
        LogisticsPointResponse returnSortingCenterWarehouse = LmsModelFactory.createLogisticsPointResponse(
            1L,
            RETURN_SORTING_CENTER_ID,
            "Склад возвратного СЦ",
            PointType.WAREHOUSE
        )
            .instruction("Инструкция, как проехать")
            .build();

        when(lmsLomLightClient.getLogisticsPoints(refEq(FILTER))).thenReturn(
            List.of(LogisticsPointLightModel.build(returnSortingCenterWarehouse))
        );

        Order order = new Order().setReturnSortingCenterId(RETURN_SORTING_CENTER_ID);
        order.setPlatformClient(PlatformClient.YANDEX_DELIVERY);
        ValidateAndEnrichContext context = new ValidateAndEnrichContext();
        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, context);

        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(context.getReturnWarehouse())
            .usingRecursiveComparison()
            .isEqualTo(returnSortingCenterWarehouse);

        softly.assertThat(results.getOrderModifier().apply(order).getReturnSortingCenterWarehouse())
            .usingRecursiveComparison()
            .isEqualTo(createReturnSortingCenterWarehouse("Инструкция, как проехать"));

        verify(lmsLomLightClient).getLogisticsPoints(FILTER);
    }

    @ParameterizedTest
    @MethodSource("nonYaDoPlatforms")
    @DisplayName("Успех обогащения заказа (не ядо)")
    void successNonYado(PlatformClient platformClient) {
        LogisticsPointResponse returnSortingCenterWarehouse = LmsModelFactory.createLogisticsPointResponse(
            1L,
            RETURN_SORTING_CENTER_ID,
            "Склад возвратного СЦ",
            PointType.WAREHOUSE
        ).build();

        when(lmsLomLightClient.getLogisticsPoints(refEq(FILTER))).thenReturn(
            List.of(LogisticsPointLightModel.build(returnSortingCenterWarehouse))
        );

        Order order = new Order().setReturnSortingCenterId(RETURN_SORTING_CENTER_ID);
        order.setPlatformClient(platformClient);
        ValidateAndEnrichContext context = new ValidateAndEnrichContext();
        context.setMarketAccountFromLegalName("Ololo");
        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, context);

        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(context.getReturnWarehouse())
            .usingRecursiveComparison()
            .isEqualTo(returnSortingCenterWarehouse);

        softly.assertThat(results.getOrderModifier().apply(order).getReturnSortingCenterWarehouse())
            .usingRecursiveComparison()
            .isEqualTo(createReturnSortingCenterWarehouse("Комментарий, как проехать"));
        verify(lmsLomLightClient).getLogisticsPoints(FILTER);
    }

    private Location createReturnSortingCenterWarehouse(String instruction) {
        return new Location()
            .setType(LocationType.WAREHOUSE)
            .setAddress(
                new Address()
                    .setBuilding("")
                    .setCountry("Россия")
                    .setGeoId(1)
                    .setHouse("1")
                    .setHousing("1")
                    .setLocality("Новосибирск")
                    .setRegion("Регион")
                    .setRoom("")
                    .setSettlement("Новосибирск")
                    .setStreet("Николаева")
                    .setZipCode("649220")
            )
            .setInstruction(instruction)
            .setPhones(Set.of(new Phone().setNumber("+7 923 243 5555").setAdditional("777")))
            .setWarehouseExternalId("externalId")
            .setWarehouseId(1L)
            .setWarehouseWorkTime(
                Set.of(new WarehouseWorkTime().setDay(1).setInterval(
                    new TimeInterval().setFrom(LocalTime.of(10, 0)).setTo(LocalTime.of(18, 0))
                ))
            )
            .setContact(new Contact()
                .setFio(
                    new Fio()
                        .setLastName("Иванов")
                        .setMiddleName("Иванович")
                        .setFirstName("Иван")
                )
            );
    }

    private static Set<PlatformClient> nonYaDoPlatforms() {
        return Arrays.stream(PlatformClient.values())
            .filter(Predicate.not(PlatformClientUtils::isYaDo))
            .collect(Collectors.toSet());
    }
}
