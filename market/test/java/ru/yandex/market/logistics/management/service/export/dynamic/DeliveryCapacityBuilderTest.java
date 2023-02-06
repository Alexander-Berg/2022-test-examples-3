package ru.yandex.market.logistics.management.service.export.dynamic;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerCapacityDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.WarehouseDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;

class DeliveryCapacityBuilderTest extends AbstractDynamicBuilderTest {

    private static final String FILE_PATH = PATH_PREFIX + "capacity/";
    private static final String CAPACITY_POSTFIX = "_capacity.json";
    private static final String DAYS_POSTFIX_POSTFIX = "_day_set.json";

    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource({
        "arguments1",
        "arguments2",
        "arguments3",
        "arguments4",
    })
    void testCapacities(
        @SuppressWarnings("unused") String name,
        List<PartnerRelationDto> dayOffPartners,
        Map<Long, Set<PartnerCapacityDto>> dayOffPartnerCapacities,
        String nameTemplate
    ) {
        Mockito.when(partnerRelationRepository.findAllForDynamic(any(), anySet(), anySet(), any()))
            .thenReturn(dayOffPartners).thenReturn(Collections.emptyList());

        Logistics.MetaInfo metaInfo = buildReport();

        softly.assertThat(metaInfo).as("Fulfillments and delivery services are equal")
            .hasSameFFsAndDssAs(getRelationPath(nameTemplate));

        softly.assertThat(metaInfo).as("Fulfillments and delivery services are equal")
            .hasSameDaySetAs(getDayOffPath(nameTemplate));
    }

    @ParameterizedTest(name = "{index} : {0}")
    @MethodSource({
        "whToWhArguments",
    })
    void testWhToWhCapacities(
        @SuppressWarnings("unused") String name,
        List<PartnerRelationDto> dayOffPartners,
        Map<Long, Set<PartnerCapacityDto>> dayOffPartnerCapacities,
        String nameTemplate
    ) {
        Mockito.when(partnerRelationRepository.findAllForDynamic(any(), anySet(), anySet(), any()))
            .thenReturn(dayOffPartners).thenReturn(Collections.emptyList());

        Logistics.MetaInfo metaInfo = buildReport();

        softly.assertThat(metaInfo).as("Fulfillments and delivery services are equal")
            .hasSameWhsToWhsAs(getRelationPath(nameTemplate));

        softly.assertThat(metaInfo).as("Fulfillments and delivery services are equal")
            .hasSameDaySetAs(getDayOffPath(nameTemplate));
    }

    @Nonnull
    private static Stream<Arguments> arguments1() {
        List<PartnerRelationDto> p4 = createAndMockDayOffPartner(
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.SHIPMENT)
                    .setType(CapacityType.REGULAR),
                new PartnerCapacityDto()
                    .setServiceType(CapacityService.INBOUND)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 5)))
            ), Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR),

                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(213)
                    .setType(CapacityType.REGULAR)
                    .setServiceType(CapacityService.DELIVERY)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 5)))
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 11, 6)))
            ));


        List<PartnerRelationDto> p5 = createAndMockDayOffPartner(
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.SHIPMENT)
                    .setType(CapacityType.REGULAR)
            ), Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR),

                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(213)
                    .setType(CapacityType.RESERVE)
                    .setServiceType(CapacityService.DELIVERY)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 5)))
            ));

        return Stream.of(
            Arguments.of(
                "Day off on regular DS",
                p4,
                capacities(p4),
                "4"),

            Arguments.of(
                "Day off on reserve DS",
                p5,
                capacities(p5),
                "5")
        );
    }

    @Nonnull
    private static Stream<Arguments> arguments2() {
        List<PartnerRelationDto> p8 = createAndMockDayOffPartner(
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.SHIPMENT)
                    .setType(CapacityType.REGULAR),
                new PartnerCapacityDto()
                    .setServiceType(CapacityService.INBOUND)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 5)))
            ), Set.of(

                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(213)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.RESERVE)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 6)))
            ));
        List<PartnerRelationDto> p10 = createAndMockDayOffPartner(
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.SHIPMENT)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 6)))
            ), Set.of(

                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 7))),

                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(213)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .setDeliveryType(DeliveryType.COURIER)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 7)))
            ));
        List<PartnerRelationDto> p11 = createAndMockDayOffPartner(
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.SHIPMENT)
                    .setType(CapacityType.REGULAR)
            ), Set.of(

                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR),

                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(2)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .setDeliveryType(DeliveryType.COURIER)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, (LocalDate.of(2018, 10, 7)))
                    )));
        return Stream.of(
            Arguments.of(
                "Day off set if only reserve capacity",
                p8,
                capacities(p8),
                "8"),

            Arguments.of(
                "Regular capacity with specified type",
                p10,
                capacities(p10),
                "10"),

            Arguments.of(
                "Regular capacity with specified type",
                p11,
                capacities(p11),
                "11")
        );
    }

    @Nonnull
    private static Stream<Arguments> arguments3() {
        List<PartnerRelationDto> p12 = createAndMockDayOffPartner(
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.SHIPMENT)
                    .setType(CapacityType.REGULAR),
                new PartnerCapacityDto()
                    .setServiceType(CapacityService.INBOUND)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 5)))
            ), Set.of(

                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 7))),

                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(2)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 8)))
            ));
        List<PartnerRelationDto> p13 = createAndMockDayOffPartner(
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.SHIPMENT)
                    .setType(CapacityType.REGULAR)
            ), Set.of(

                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 7))),

                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(2)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 7)))
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 8)))
            ));
        List<PartnerRelationDto> p14 = createAndMockDayOffPartner(
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.SHIPMENT)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 6)))
            ), Set.of(

                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 7))),

                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(2)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 7)))
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 8)))
            ));
        List<PartnerRelationDto> p15 = createAndMockDayOffPartner(
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.SHIPMENT)
                    .setType(CapacityType.REGULAR)
            ),
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(4)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 7))),
                new PartnerCapacityDto()
                    .setLocationFrom(213)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 7))),
                new PartnerCapacityDto()
                    .setLocationFrom(2)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 7)))
            )
        );
        return Stream.of(
            Arguments.of(
                "Regular of higher region today and lower next day",
                p12,
                capacities(p12),
                "12"),

            Arguments.of(
                "Regular of higher region today and lower today and the next day",
                p13,
                capacities(p13),
                "13"),

            Arguments.of(
                "Lower region added to fulfillment days off",
                p14,
                capacities(p14),
                "14"),

            // Баг https://st.yandex-team.ru/DELIVERY-16386
            Arguments.of(
                "DS same regular capacities with different locationFrom",
                p15,
                capacities(p15),
                "15")
        );
    }

    @Nonnull
    private static Stream<Arguments> arguments4() {
        List<PartnerRelationDto> p15 = createAndMockDayOffPartner(
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.SHIPMENT)
                    .setType(CapacityType.REGULAR),
                new PartnerCapacityDto()
                    .setServiceType(CapacityService.INBOUND)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 5)))
            ),
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(4)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.RESERVE)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 7))),
                new PartnerCapacityDto()
                    .setLocationFrom(213)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.RESERVE)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 7))),
                new PartnerCapacityDto()
                    .setLocationFrom(2)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.RESERVE)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 7)))
            )
        );
        List<PartnerRelationDto> p17 = createAndMockDayOffPartner(
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.SHIPMENT)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 10))),
                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(1)
                    .setServiceType(CapacityService.SHIPMENT)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 11)))
            ),
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(225)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .setDeliveryType(DeliveryType.PICKUP)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 12))),
                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(213)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 13))),
                new PartnerCapacityDto()
                    .setLocationFrom(213)
                    .setLocationTo(17)
                    .setDeliveryType(DeliveryType.COURIER)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .setDay(LocalDate.of(2018, 10, 14))
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 14))),
                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(2)
                    .setServiceType(CapacityService.DELIVERY)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 15)))
            )
        );
        return Stream.of(
            Arguments.of(
                "DS same reserve capacities with different locationFrom",
                p15,
                capacities(p15),
                "15")
            /* todo DELIVERY-23425
            Arguments.of(
                "DS and FF child regions duplicate days from parent regions",
                p17,
                capacities(p17),
                "17")
            */
        );
    }

    private static Stream<Arguments> whToWhArguments() {
        List<PartnerRelationDto> p16 = createAndMockDayOffCrossdockRelation(
            Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.SHIPMENT)
                    .setType(CapacityType.REGULAR)
            ), Set.of(
                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(225)
                    .setServiceType(CapacityService.INBOUND)
                    .setType(CapacityType.REGULAR)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 5))),
                new PartnerCapacityDto()
                    .setLocationFrom(1)
                    .setLocationTo(225)
                    .setType(CapacityType.REGULAR)
                    .setServiceType(CapacityService.SHIPMENT)
                    .addPartnerCapacityDayOff(
                        new PartnerCapacityDto.DayOff(null, LocalDate.of(2018, 10, 6)))
            ));
        return Stream.of(
            Arguments.of(
                "Crossdock capacity supplier with ff",
                p16,
                capacities(p16),
                "16")
        );
    }

    @Nonnull
    private static List<PartnerRelationDto> createAndMockDayOffPartner(
        Set<PartnerCapacityDto> ffCapacities,
        Set<PartnerCapacityDto> dsCapacities
    ) {
        List<PartnerRelationDto> partnerRelations = createPartnerRelations();

        PartnerDto resultPartner = partnerRelations.stream().flatMap(relation -> Stream.of(relation.getFromPartner(),
            relation.getToPartner()))
            .filter(partner -> partner.getPartnerType() == PartnerType.DELIVERY)
            .min(Comparator.comparing(PartnerDto::getId))
            .orElseThrow(AssertionError::new);

        PartnerDto resultFfPartner = partnerRelations.stream().flatMap(relation -> Stream.of(relation.getFromPartner(),
            relation.getToPartner()))
            .filter(partner -> partner.getPartnerType() == PartnerType.FULFILLMENT)
            .min(Comparator.comparing(PartnerDto::getId))
            .orElseThrow(AssertionError::new);
        resultPartner.addCapacities(dsCapacities);
        resultFfPartner.addCapacities(ffCapacities);
        return partnerRelations;
    }

    @Nonnull
    private static List<PartnerRelationDto> createAndMockDayOffCrossdockRelation(
        Set<PartnerCapacityDto> supplierCapacities,
        Set<PartnerCapacityDto> ffCapacities
    ) {
        List<WarehouseDto> warehouses = createFulfillments();
        PartnerRelationDto partnerRelation = new PartnerRelationDto()
            .setId(1L)
            .setFromPartner(warehouses.get(0).addCapacities(supplierCapacities))
            .setToPartner(warehouses.get(1).addCapacities(ffCapacities))
            .setHandlingTime(10)
            .setEnabled(true);

        return Collections.singletonList(partnerRelation);
    }

    @Nonnull
    private static String getRelationPath(String nameTemplate) {
        return FILE_PATH + nameTemplate + CAPACITY_POSTFIX;
    }

    @Nonnull
    private static String getDayOffPath(String nameTemplate) {
        return FILE_PATH + nameTemplate + DAYS_POSTFIX_POSTFIX;
    }


    private static Map<Long, Set<PartnerCapacityDto>> capacities(List<PartnerRelationDto> relations) {
        HashMap<Long, Set<PartnerCapacityDto>> capacities = new HashMap<>();
        for (PartnerRelationDto rel : relations) {
            addCapacitites(capacities, rel.getFromPartner());
            addCapacitites(capacities, rel.getToPartner());
        }
        return capacities;
    }

    private static void addCapacitites(HashMap<Long, Set<PartnerCapacityDto>> capacities, PartnerDto partner) {
        for (PartnerCapacityDto c : partner.getCapacities()) {
            capacities.compute(partner.getId(), (pId, val) -> {
                if (val == null) {
                    val = new HashSet<>();
                }
                val.add(c);
                return val;
            });
        }
    }
}
