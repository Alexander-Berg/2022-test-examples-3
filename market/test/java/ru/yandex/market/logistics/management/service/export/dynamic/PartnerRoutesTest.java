package ru.yandex.market.logistics.management.service.export.dynamic;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mockito;

import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.DeliveryDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRouteDto;

import static org.mockito.ArgumentMatchers.any;

class PartnerRoutesTest extends AbstractDynamicBuilderTest {

    private static final String FILE_PATH = PATH_PREFIX + "partner_routes/";
    private static final String REGION_TO_REGION_POSTFIX = "__region_to_region.json";


    @BeforeEach
    void initMocks() {
        builder = new ReportDynamicBuilder(
            validationService,
            partnerService,
            platformClientService,
            deliveryDistributorParamsRepository,
            jdbcDeliveryRepository,
            jdbcWarehouseRepository,
            partnerPlatformClientRepository,
            partnerRelationRepository,
            dynamicLogService,
            CLOCK_MOCK,
            calendarService,
            factory,
            logisticsPointService,
            transactionTemplate
        );
        builder.setDepth(15);
        builder.setDateOffset(0);
    }

    @ParameterizedTest(name = "{index} : {1}")
    @ArgumentsSource(TestArgumentsProvider.class)
    void testPartnerRoutes(List<DeliveryDto> deliveries, String jsonNameTemplate) {

        Mockito.when(jdbcDeliveryRepository.findAll(any(), any())).thenReturn(deliveries);

        Logistics.MetaInfo metaInfo = buildReport();

        String path = FILE_PATH + jsonNameTemplate + "%s";

        softly.assertThat(metaInfo).as("Delivery services are equal")
            .hasSameDSsAs(String.format(path, REGION_TO_REGION_POSTFIX));

        softly.assertThat(metaInfo).as("Days sets are equal")
            .hasSameDaySetAs(String.format(path, DAYS_SET_POSTFIX));
    }


    private static List<PartnerDto> createDelivery(Set<PartnerRouteDto> partnerRoutes) {
        PartnerDto delivery = newDelivery();

        delivery.addPartnerRoutes(partnerRoutes);

        return Collections.singletonList(delivery);
    }

    private static DeliveryDto newDelivery() {
        return (DeliveryDto) new DeliveryDto()
            .setName("Delivery10")
            .setRating(1)
            .setId(10L);
    }

    private static PartnerRouteDto newRoute(int locationFrom, int locationTo, int workingDays) {
        return new PartnerRouteDto()
            .setLocationFrom(locationFrom)
            .setLocationTo(locationTo)
            .setScheduleDays(IntStream.range(1, workingDays + 1).boxed().collect(Collectors.toSet()));
    }


    private static class TestArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.arguments(
                    createDelivery(Collections.emptySet()),
                    "empty_routes"
                ),
                Arguments.arguments(
                    createDelivery(ImmutableSet.of(
                        newRoute(1, 2, 7))),
                    "one_route_no_holidays"
                ),
                Arguments.arguments(
                    createDelivery(ImmutableSet.of(
                        newRoute(1, 2, 0))),
                    "one_route_all_holidays"
                ),
                Arguments.arguments(
                    createDelivery(ImmutableSet.of(
                        newRoute(1, 2, 6),
                        newRoute(3, 4, 6))),
                    "two_routes_same_holidays"
                ),
                Arguments.arguments(
                    createDelivery(ImmutableSet.of(
                        newRoute(1, 2, 5),
                        newRoute(3, 4, 6),
                        newRoute(5, 6, 7))),
                    "three_routes_different_holidays"
                )
            );
        }

    }
}
