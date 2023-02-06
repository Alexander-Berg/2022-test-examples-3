package ru.yandex.market.logistics.cs.checkouter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import ru.yandex.market.logistics.cs.AbstractTest;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceDeliveryDescriptor;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceDeliveryDescriptorExtractor;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceDeliveryDescriptorExtractorImpl;
import ru.yandex.market.logistics.cs.domain.exception.NoLogisticDateException;
import ru.yandex.market.logistics.cs.logbroker.checkouter.SimpleCombinatorRoute;
import ru.yandex.market.logistics.cs.service.PartnerCargoTypeFactorService;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.DeliveryRoute;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Бизнес требования к получению дат для сервиса из комбинаторного маршрута")
class ServiceDeliveryExtractorTest extends AbstractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long PARTNER_ID_1 = 54509L;
    private static final long SERVICE_ID_1_1 = 2675520L;
    private static final long SERVICE_ID_1_2 = 2675521L;
    private static final long SERVICE_ID_1_3 = 2675522L;
    private static final long SERVICE_ID_1_4 = 2675524L;
    private static final long PARTNER_ID_2 = 75735L;
    private static final long SERVICE_ID_2 = 3584125L;
    private static final long PARTNER_ID_3 = 1005429L;
    private static final long SERVICE_ID_3_1 = 5672380L;
    private static final long SERVICE_ID_3_2 = 5672255L;
    private static final long SERVICE_ID_3_3 = 4792434L;
    private static final List<Integer> CARGO_TYPE_1 = List.of(100);
    public static final List<Integer> CARGO_TYPES_2_3 = List.of(200, 300);
    public static final List<Integer> ALL_CARGO_TYPES = List.of(100, 200, 300);
    private static final LocalDate DATE_3 = LocalDate.of(2021, 7, 3);
    private static final LocalDate DATE_4 = LocalDate.of(2021, 7, 4);
    private static final LocalDate DATE_5 = LocalDate.of(2021, 7, 5);
    private static final LocalDate DATE_6 = LocalDate.of(2021, 7, 6);


    @Mock
    private PartnerCargoTypeFactorService partnerCargoTypeFactorService;
    private ServiceDeliveryDescriptorExtractor serviceDeliveryDescriptorExtractor;

    @BeforeEach
    void setup() {
        serviceDeliveryDescriptorExtractor = new ServiceDeliveryDescriptorExtractorImpl(partnerCargoTypeFactorService);
    }

    @Test
    @DisplayName("Нет полей logistic_date и start_time")
    void nullableStartTimeAndLogisticDate() {
        softly.assertThatThrownBy(() -> serviceDeliveryDescriptorExtractor.extract(
                combinatorRoute(deliveryRoute("json/route/nullable_logistic_date_and_start_time.json"))
            ))
            .isInstanceOf(NoLogisticDateException.class)
            .hasMessage("Both of logistic_date and start_time for service '" + SERVICE_ID_1_1 + "' are null");
    }

    @Test
    @DisplayName("Нулевой logistic_date и ненулевой start_time")
    void zeroLogisticDateAndNonnullStartTime() {
        List<ServiceDeliveryDescriptor> descriptors = serviceDeliveryDescriptorExtractor.extract(
            combinatorRoute(deliveryRoute("json/route/zero_logistic_date_and_nonnull_start_time.json"))
        );
        for (var descriptor : descriptors) {
            softly.assertThat(descriptor.getDay()).isEqualTo(DATE_4);
        }
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Разбор маршрутов")
    @MethodSource(value = "testData")
    @SuppressWarnings("unused")
    void extractorTest(String displayName, String jsonPath, List<ServiceDeliveryDescriptor> expected) {
        doReturn(1.0).when(partnerCargoTypeFactorService).getMaxFactor(any(), any());
        softly.assertThat(serviceDeliveryDescriptorExtractor.extract(combinatorRoute(deliveryRoute(jsonPath))))
            .containsExactlyElementsOf(expected);
    }

    /*
     * Все даты, которые должны измениться, будут = LocalDate.of(2001, 7, 4)
     */
    @Nonnull
    private static Stream<Arguments> testData() {
        return Stream.of(
            Arguments.of(
                "Берем логистическую дату, если она есть или дату начала",
                "json/route/without_sc_ff.json",
                List.of(
                    new ServiceDeliveryDescriptor(722372L, LocalDate.of(2020, 6, 10), 1),
                    new ServiceDeliveryDescriptor(724995L, LocalDate.of(2020, 6, 8), 1),
                    new ServiceDeliveryDescriptor(722373L, LocalDate.of(2020, 6, 12), 1),
                    new ServiceDeliveryDescriptor(725089L, LocalDate.of(2020, 6, 11), 1),
                    new ServiceDeliveryDescriptor(725090L, LocalDate.of(2020, 6, 11), 1),
                    new ServiceDeliveryDescriptor(725091L, LocalDate.of(2020, 6, 11), 1),
                    new ServiceDeliveryDescriptor(722566L, LocalDate.of(2020, 6, 14), 1),
                    new ServiceDeliveryDescriptor(722567L, LocalDate.of(2020, 6, 14), 1),
                    new ServiceDeliveryDescriptor(32956L, LocalDate.of(2020, 6, 15), 1)
                )
            ),
            Arguments.of(
                "Для FULFILLMENT берем дату с следующего MOVEMENT сегмента",
                "json/route/with_ff_with_logistic_date_in_next_segment.json",
                List.of(
                    new ServiceDeliveryDescriptor(5044056L, LocalDate.of(2001, 7, 4), 5),
                    new ServiceDeliveryDescriptor(5044058L, LocalDate.of(2021, 7, 2), 5),
                    new ServiceDeliveryDescriptor(5044057L, LocalDate.of(2001, 7, 4), 5),
                    new ServiceDeliveryDescriptor(3668759L, LocalDate.of(2021, 7, 2), 5),
                    new ServiceDeliveryDescriptor(3668767L, LocalDate.of(2001, 7, 4), 5),
                    new ServiceDeliveryDescriptor(3668760L, LocalDate.of(2021, 7, 2), 5),
                    new ServiceDeliveryDescriptor(3323323L, LocalDate.of(2021, 7, 2), 5),
                    new ServiceDeliveryDescriptor(3323324L, LocalDate.of(2021, 7, 2), 5),
                    new ServiceDeliveryDescriptor(4001447L, DATE_4, 5),
                    new ServiceDeliveryDescriptor(4001469L, DATE_4, 5),
                    new ServiceDeliveryDescriptor(4001494L, DATE_4, 5),
                    new ServiceDeliveryDescriptor(4449036L, DATE_4, 5),
                    new ServiceDeliveryDescriptor(4001425L, DATE_4, 5)
                )
            ),
            Arguments.of(
                "Для SORTING_CENTER берем дату с следующего MOVEMENT сегмента",
                "json/route/with_sc_with_logistic_date_in_next_segment.json",
                List.of(
                    new ServiceDeliveryDescriptor(SERVICE_ID_1_1, DATE_3, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_1_2, DATE_3, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_1_3, DATE_4, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_1_4, DATE_4, 1),
                    new ServiceDeliveryDescriptor(2675523L, DATE_4, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_2, LocalDate.of(2001, 7, 4), 1),
                    new ServiceDeliveryDescriptor(3584126L, LocalDate.of(2001, 7, 4), 1),
                    new ServiceDeliveryDescriptor(3584127L, LocalDate.of(2001, 7, 4), 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_3_1, DATE_4, 1),
                    new ServiceDeliveryDescriptor(5672382L, LocalDate.of(2001, 7, 4), 1),
                    new ServiceDeliveryDescriptor(5672381L, DATE_4, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_3_2, DATE_5, 1),
                    new ServiceDeliveryDescriptor(5672256L, DATE_5, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_3_3, DATE_6, 1)
                )
            ),
            Arguments.of(
                "Logistic_date нет в MOVEMENT, берем его start_date",
                "json/route/with_sc_without_logistic_date_in_next_segment.json",
                List.of(
                    new ServiceDeliveryDescriptor(SERVICE_ID_2, LocalDate.of(2001, 7, 4), 1),
                    new ServiceDeliveryDescriptor(3584126L, LocalDate.of(2001, 7, 4), 1),
                    new ServiceDeliveryDescriptor(3584127L, LocalDate.of(2001, 7, 4), 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_3_1, DATE_4, 1),
                    new ServiceDeliveryDescriptor(5672382L, LocalDate.of(2001, 7, 4), 1),
                    new ServiceDeliveryDescriptor(5672381L, DATE_4, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_3_2, DATE_5, 1),
                    new ServiceDeliveryDescriptor(5672256L, DATE_5, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_3_3, DATE_6, 1)
                )
            ),
            Arguments.of(
                "Нет MOVEMENT сегмента следом за SC, берем logistic_date из собственных сервисов ",
                "json/route/with_sc_without_next_movement_segment.json",
                List.of(
                    new ServiceDeliveryDescriptor(SERVICE_ID_1_1, DATE_3, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_1_2, DATE_3, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_1_3, DATE_4, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_1_4, DATE_4, 1),
                    new ServiceDeliveryDescriptor(2675523L, DATE_4, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_2, DATE_4, 1),
                    new ServiceDeliveryDescriptor(3584126L, DATE_4, 1),
                    new ServiceDeliveryDescriptor(3584127L, DATE_4, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_3_2, DATE_5, 1),
                    new ServiceDeliveryDescriptor(5672256L, DATE_5, 1),
                    new ServiceDeliveryDescriptor(SERVICE_ID_3_3, DATE_6, 1)
                )
            )
        );
    }

    @Test
    void shouldGetZeroItemsCount_whenNoItems() {
        var actualDescriptors = serviceDeliveryDescriptorExtractor.extract(
            combinatorRoute("json/route/no_items.json")
        );

        softly.assertThat(actualDescriptors)
            .containsExactlyInAnyOrder(
                new ServiceDeliveryDescriptor(SERVICE_ID_1_1, DATE_4, 0, 0)
            );
    }

    @Test
    void shouldUseDefaultFactor_whenOfferHasNoCargoTypes() {
        doReturn(1.0).when(partnerCargoTypeFactorService).getMaxFactor(any(), any());

        var actualDescriptors = serviceDeliveryDescriptorExtractor.extract(
            combinatorRoute("json/route/offer_without_cargo_types.json")
        );

        softly.assertThat(actualDescriptors)
            .containsExactlyInAnyOrder(
                new ServiceDeliveryDescriptor(SERVICE_ID_1_1, DATE_4, 1, 1)
            );
    }

    @Test
    void shouldUseMaxCargoTypeFactor_whenOfferHasCargoTypes() {
        doReturn(5.1).when(partnerCargoTypeFactorService).getMaxFactor(PARTNER_ID_1, ALL_CARGO_TYPES);
        var actualDescriptors = serviceDeliveryDescriptorExtractor.extract(
            combinatorRoute("json/route/offer_with_cargo_types.json")
        );

        softly.assertThat(actualDescriptors)
            .containsExactlyInAnyOrder(
                new ServiceDeliveryDescriptor(SERVICE_ID_1_1, DATE_4, 1, 6)
            );
        verify(partnerCargoTypeFactorService).getMaxFactor(eq(PARTNER_ID_1), any());
    }

    @Test
    void shouldGetCorrectItemCountWithFactor_whenComplexOrderWithDifferentItemsQuantityAndCargoTypes() {
        doReturn(1.0).when(partnerCargoTypeFactorService).getMaxFactor(eq(PARTNER_ID_1), any());
        doReturn(1.5).when(partnerCargoTypeFactorService).getMaxFactor(PARTNER_ID_1, CARGO_TYPE_1);
        doReturn(5.1).when(partnerCargoTypeFactorService).getMaxFactor(PARTNER_ID_1, CARGO_TYPES_2_3);

        var actualDescriptors = serviceDeliveryDescriptorExtractor.extract(
            combinatorRoute("json/route/complex_order_with_different_items_quantity_and_cargo_types.json")
        );

        softly.assertThat(actualDescriptors)
            .containsExactlyInAnyOrder(
                new ServiceDeliveryDescriptor(SERVICE_ID_1_1, DATE_4, 8, 14),
                new ServiceDeliveryDescriptor(SERVICE_ID_1_2, DATE_4, 8, 14),
                new ServiceDeliveryDescriptor(SERVICE_ID_1_3, DATE_4, 8, 14)
            );
        verify(partnerCargoTypeFactorService, times(3)).getMaxFactor(PARTNER_ID_1, CARGO_TYPES_2_3);
        verify(partnerCargoTypeFactorService, times(3)).getMaxFactor(PARTNER_ID_1, CARGO_TYPE_1);
        verifyNoMoreInteractions(partnerCargoTypeFactorService);
    }

    @Test
    void shouldGetCorrectItemCountWithFactor_whenComplexOrderWithDifferentItemsQuantityAndCargoTypes_2() {
        doReturn(1.0).when(partnerCargoTypeFactorService).getMaxFactor(anyLong(), any());
        doReturn(1.3).when(partnerCargoTypeFactorService).getMaxFactor(PARTNER_ID_1, CARGO_TYPES_2_3);
        doReturn(3.0).when(partnerCargoTypeFactorService).getMaxFactor(PARTNER_ID_2, CARGO_TYPES_2_3);
        doReturn(10.0).when(partnerCargoTypeFactorService).getMaxFactor(PARTNER_ID_3, CARGO_TYPE_1);
        doReturn(30.0).when(partnerCargoTypeFactorService).getMaxFactor(PARTNER_ID_3, CARGO_TYPES_2_3);
        var actualDescriptors = serviceDeliveryDescriptorExtractor.extract(
            combinatorRoute("json/route/complex_order_with_different_items_quantity_and_cargo_types_2.json")
        );

        int quantity1 = 1;
        int quantity2 = 2;
        int quantity3 = 5;
        int expectedIemCountWithFactor1 = quantity1 * 2 + quantity2 * 1 + quantity3 * 1;
        int expectedIemCountWithFactor2 = quantity1 * 3 + quantity2 * 1 + quantity3 * 1;
        int expectedIemCountWithFactor3 = quantity1 * 30 + quantity2 * 10 + quantity3 * 1;
        softly.assertThat(actualDescriptors)
            .containsExactlyInAnyOrder(
                new ServiceDeliveryDescriptor(SERVICE_ID_1_1, DATE_3, 8, expectedIemCountWithFactor1),
                new ServiceDeliveryDescriptor(SERVICE_ID_1_4, DATE_4, 8, expectedIemCountWithFactor1),
                new ServiceDeliveryDescriptor(SERVICE_ID_2, DATE_4, 8, expectedIemCountWithFactor2),
                new ServiceDeliveryDescriptor(SERVICE_ID_3_1, DATE_4, 8, expectedIemCountWithFactor3),
                new ServiceDeliveryDescriptor(SERVICE_ID_3_2, DATE_5, 8, expectedIemCountWithFactor3),
                new ServiceDeliveryDescriptor(SERVICE_ID_3_3, DATE_6, 8, expectedIemCountWithFactor3)
            );
        verify(partnerCargoTypeFactorService, times(6)).getMaxFactor(eq(PARTNER_ID_1), any());
        verify(partnerCargoTypeFactorService, times(3)).getMaxFactor(eq(PARTNER_ID_2), any());
        verify(partnerCargoTypeFactorService, times(9)).getMaxFactor(eq(PARTNER_ID_3), any());
    }

    @SneakyThrows
    @Nonnull
    private DeliveryRoute deliveryRoute(String jsonPath) {
        return MAPPER.readValue(extractFileContent(jsonPath), DeliveryRoute.class);
    }

    @Nonnull
    private SimpleCombinatorRoute combinatorRoute(DeliveryRoute deliveryRoute) {
        return new SimpleCombinatorRoute(deliveryRoute, null);
    }

    @SneakyThrows
    @Nonnull
    private SimpleCombinatorRoute combinatorRoute(String jsonPath) {
        return MAPPER.readValue(extractFileContent(jsonPath), SimpleCombinatorRoute.class);
    }
}
