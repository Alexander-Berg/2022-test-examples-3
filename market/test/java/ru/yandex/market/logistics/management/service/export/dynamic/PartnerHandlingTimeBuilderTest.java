package ru.yandex.market.logistics.management.service.export.dynamic;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.DeliveryDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.LogisticsPointDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerHandlingTimeDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.WarehouseDto;
import ru.yandex.market.logistics.management.util.UnitTestUtil;

class PartnerHandlingTimeBuilderTest extends AbstractDynamicBuilderTest {

    @Override
    @BeforeEach
    public void setUp() {
        capacityPrepareService = new CapacityPrepareService(
            new CapacityTreeProcessorService(new RegionHelper(UnitTestUtil.getRegionTree()))
        );
        factory = new DeliveryCapacityBuilderFactory(regionService, capacityPrepareService, capacityMergeService);
        initBuilder();
    }

    @ParameterizedTest(name = "{index} {0}")
    @ArgumentsSource(TestArgumentsProvider.class)
    void mapWarehouseAndDelivery(String testName,
                                 DeliveryDto delivery,
                                 WarehouseDto warehouse,
                                 String expectedDeliveryJsonPath,
                                 String expectedFulfillmentJsonPath) {
        mockServices(
            Collections.singletonList(delivery),
            Collections.singletonList(warehouse),
            createPlatformClients()
        );

        Logistics.MetaInfo metaInfo = buildReport();

        softly.assertThat(metaInfo).as("Delivery should be equal")
            .hasSameDSsAs(expectedDeliveryJsonPath);

        softly.assertThat(metaInfo).as("Fulfillment should be equal")
            .hasSameFFsAs(expectedFulfillmentJsonPath);
    }

    static class TestArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.arguments(
                    "No handling times",
                    createDelivery(1),
                    createFulfillment(2),
                    "data/mds/partner_handling_time/delivery_without_handling_times.json",
                    "data/mds/partner_handling_time/fulfillment_without_handling_times.json"
                ),
                Arguments.arguments(
                    "With handling times",
                    createDelivery(10,
                        createPartnerHandlingTime(10, 20, 0),
                        createPartnerHandlingTime(10, 30, 60000000000L),
                        createPartnerHandlingTime(20, 10, 7200000000000L)
                    ),
                    createFulfillment(20,
                        createPartnerHandlingTime(20, 20, 0),
                        createPartnerHandlingTime(21, 21, 70000000000L),
                        createPartnerHandlingTime(21, 20, 10000000000000L)
                    ),
                    "data/mds/partner_handling_time/delivery_with_handling_times.json",
                    "data/mds/partner_handling_time/fulfillment_with_handling_times.json"
                )
            );
        }

        private DeliveryDto createDelivery(long id, PartnerHandlingTimeDto... partnerHandlingTimes) {
            return (DeliveryDto) new DeliveryDto()
                .setId(id)
                .setPartnerType(PartnerType.DELIVERY)
                .setLocationId(100)
                .setStatus(PartnerStatus.ACTIVE)
                .addPartnerHandlingTimes(Arrays.asList(partnerHandlingTimes));
        }

        private WarehouseDto createFulfillment(long id, PartnerHandlingTimeDto... partnerHandlingTimes) {
            WarehouseDto partner = (WarehouseDto) new WarehouseDto()
                .setId(id)
                .setPartnerType(PartnerType.FULFILLMENT)
                .setLocationId(100)
                .setStatus(PartnerStatus.ACTIVE)
                .addPartnerHandlingTimes(Arrays.asList(partnerHandlingTimes));
            addLogisticPoint(partner);


            return partner;
        }

        private void addLogisticPoint(PartnerDto partner) {
            partner.addActiveWarehouse(new LogisticsPointDto().setLocationId(300));
        }

        private PartnerHandlingTimeDto createPartnerHandlingTime(int locationFrom, int locationTo, long handlingTime) {
            return new PartnerHandlingTimeDto()
                .setLocationFrom(locationFrom)
                .setLocationTo(locationTo)
                .setHandlingTime(Duration.ofNanos(handlingTime));
        }
    }
}
