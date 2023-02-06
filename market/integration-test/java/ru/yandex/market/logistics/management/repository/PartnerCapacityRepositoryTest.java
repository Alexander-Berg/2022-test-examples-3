package ru.yandex.market.logistics.management.repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.PartnerAndCapacityDto;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.service.client.PartnerCapacityDayOffService;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
public class PartnerCapacityRepositoryTest extends AbstractContextualTest {

    private static final int ONE_DAY = 1;
    private static final int TWO_DAYS = 2;
    private static final int THREE_DAYS = 3;
    private static final int FIVE_DAYS = 5;
    private static final PartnerType TYPE_SUPPLIER = PartnerType.SUPPLIER;
    private static final CapacityService CAPACITY_SERVICE_SHIPMENT = CapacityService.SHIPMENT;
    private static final LocalDate OCTOBER_10 = LocalDate.of(2020, 10, 10);
    private static final LocalDate OCTOBER_11 = LocalDate.of(2020, 10, 11);

    @Autowired
    private PartnerCapacityRepository partnerCapacityRepository;
    @Autowired
    private PartnerCapacityDayOffService partnerCapacityDayOffService;

    @Test
    @Sql("/data/repository/partnerCapacity/partner_with_different_capacities.sql")
    void findNoCapacitiesByDayOffDate() {
        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                ONE_DAY,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_10,
                OCTOBER_10.plusDays(ONE_DAY)
            );

        softly.assertThat(result)
            .isNotNull()
            .as("Must find no partners")
            .isEmpty();
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner_with_different_capacities.sql")
    void findPartnerAndCapacityByDayOffDate() {
        addDayOff(12L, OCTOBER_10);

        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                ONE_DAY,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_10,
                OCTOBER_10.plusDays(ONE_DAY)
            );

        softly.assertThat(result.size())
            .as("Must find exactly 1 partner")
            .isEqualTo(1);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerId)
            .as("Must find correct partner id")
            .containsExactlyInAnyOrder(1L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerName)
            .as("Must find correct partner name")
            .containsExactlyInAnyOrder("CrossDock1");

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityId)
            .as("Must find correct capacity id")
            .containsExactlyInAnyOrder(12L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityValue)
            .as("Must find correct capacity value")
            .containsExactlyInAnyOrder(100L);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner_with_different_capacities.sql")
    void findPartnerAndCapacityByDayOffDateRegardlessOfCountingType() {
        addDayOff(13L, OCTOBER_10);

        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                ONE_DAY,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_10,
                OCTOBER_10.plusDays(ONE_DAY)
            );

        softly.assertThat(result.size())
            .as("Must find exactly 1 partner")
            .isEqualTo(1);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerId)
            .as("Must find correct partner id")
            .containsExactlyInAnyOrder(1L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerName)
            .as("Must find correct partner name")
            .containsExactlyInAnyOrder("CrossDock1");

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityId)
            .as("Must find correct capacity id")
            .containsExactlyInAnyOrder(13L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityValue)
            .as("Must find correct capacity value")
            .containsExactlyInAnyOrder(100L);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner_with_different_capacities.sql")
    void findPartnerAndCapacityByDayOffDateRegardlessOfLocation() {
        addDayOff(14L, OCTOBER_10);

        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                ONE_DAY,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_10,
                OCTOBER_10.plusDays(ONE_DAY)
            );

        softly.assertThat(result.size())
            .as("Must find exactly 1 partner")
            .isEqualTo(1);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerId)
            .as("Must find correct partner id")
            .containsExactlyInAnyOrder(1L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerName)
            .as("Must find correct partner name")
            .containsExactlyInAnyOrder("CrossDock1");

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityId)
            .as("Must find correct capacity id")
            .containsExactlyInAnyOrder(14L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityValue)
            .as("Must find correct capacity value")
            .containsExactlyInAnyOrder(100L);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner_with_different_capacities.sql")
    void findPartnerAndCapacityByDayOffDateRegardlessOfValue() {
        addDayOff(15L, OCTOBER_10);

        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                ONE_DAY,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_10,
                OCTOBER_10.plusDays(ONE_DAY)
            );

        softly.assertThat(result.size())
            .as("Must find exactly 1 partner")
            .isEqualTo(1);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerId)
            .as("Must find correct partner id")
            .containsExactlyInAnyOrder(1L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerName)
            .as("Must find correct partner name")
            .containsExactlyInAnyOrder("CrossDock1");

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityId)
            .as("Must find correct capacity id")
            .containsExactlyInAnyOrder(15L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityValue)
            .as("Must find correct capacity value")
            .containsExactlyInAnyOrder(200L);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner_with_different_capacities.sql")
    void findPartnerAndCapacityByDayOffDateRegardlessOfCapacityType() {
        addDayOff(16L, OCTOBER_10);

        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                ONE_DAY,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_10,
                OCTOBER_10.plusDays(ONE_DAY)
            );

        softly.assertThat(result.size())
            .as("Must find exactly 1 partner")
            .isEqualTo(1);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerId)
            .as("Must find correct partner id")
            .containsExactlyInAnyOrder(1L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerName)
            .as("Must find correct partner name")
            .containsExactlyInAnyOrder("CrossDock1");

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityId)
            .as("Must find correct capacity id")
            .containsExactlyInAnyOrder(16L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityValue)
            .as("Must find correct capacity value")
            .containsExactlyInAnyOrder(200L);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner_with_different_capacities.sql")
    void findPartnerAndCapacityByDayOffDateRegardlessOfPlatform() {
        addDayOff(17L, OCTOBER_10);

        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                ONE_DAY,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_10,
                OCTOBER_10.plusDays(ONE_DAY)
            );

        softly.assertThat(result.size())
            .as("Must find exactly 1 partner")
            .isEqualTo(1);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerId)
            .as("Must find correct partner id")
            .containsExactlyInAnyOrder(1L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerName)
            .as("Must find correct partner name")
            .containsExactlyInAnyOrder("CrossDock1");

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityId)
            .as("Must find correct capacity id")
            .containsExactlyInAnyOrder(17L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityValue)
            .as("Must find correct capacity value")
            .containsExactlyInAnyOrder(100L);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner_with_different_capacities.sql")
    void findPartnerAndCapacityByDayOffDateRegardlessOfDeliveryType() {
        addDayOff(18L, OCTOBER_10);

        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                ONE_DAY,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_10,
                OCTOBER_10.plusDays(ONE_DAY)
            );

        softly.assertThat(result.size())
            .as("Must find exactly 1 partner")
            .isEqualTo(1);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerId)
            .as("Must find correct partner id")
            .containsExactlyInAnyOrder(1L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerName)
            .as("Must find correct partner name")
            .containsExactlyInAnyOrder("CrossDock1");

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityId)
            .as("Must find correct capacity id")
            .containsExactlyInAnyOrder(18L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityValue)
            .as("Must find correct capacity value")
            .containsExactlyInAnyOrder(100L);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner_with_duplicate_dayoffs.sql")
    void findOnePartnerAndCapacityByDayOffDateWithoutDuplicate() {
        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                ONE_DAY,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_10,
                OCTOBER_10.plusDays(ONE_DAY)
            );

        softly.assertThat(result.size())
            .as("Must find exactly 1 partner")
            .isEqualTo(1);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerId)
            .as("Must find correct partner id")
            .containsExactlyInAnyOrder(1L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerName)
            .as("Must find correct partner name")
            .containsExactlyInAnyOrder("CrossDock1");

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityId)
            .as("Must find correct capacity id")
            .containsExactlyInAnyOrder(10L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityValue)
            .as("Must find correct capacity value")
            .containsExactlyInAnyOrder(100L);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner_with_duplicate_capacities_in_one_platform.sql")
    void findOnePartnerAndCapacityByDayOffDateWithoutCapacitiesDuplicate() {
        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                ONE_DAY,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_10,
                OCTOBER_10.plusDays(ONE_DAY)
            );

        softly.assertThat(result.size())
            .as("Must find exactly 1 partner")
            .isEqualTo(1);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerId)
            .as("Must find correct partner id")
            .containsExactly(1L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerName)
            .as("Must find correct partner name")
            .containsExactly("CrossDock1");

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityId)
            .as("Must find one of capacities with dayoff")
            .containsAnyElementsOf(Arrays.asList(10L, 11L, 12L, 13L, 14L, 15L));

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityValue)
            .as("Must find one of capacities with dayoff")
            .containsAnyElementsOf(Arrays.asList(100L, 200L, 300L, 400L, 500L, 600L));
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partners_with_capacities_in_different_platforms.sql")
    void findPartnersAndCapacityWithDayOffsInOnePlatform() {
        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                TWO_DAYS,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_10,
                OCTOBER_10.plusDays(TWO_DAYS)
            );

        softly.assertThat(result.size())
            .as("Must find exactly 1 partner")
            .isEqualTo(1);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerId)
            .as("Must only find partner with 2 dayoffs in one platform")
            .containsExactly(2L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerName)
            .as("Must find correct partner name")
            .containsExactly("CrossDock2");

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityId)
            .as("Must find capacity of the first dayoff")
            .containsExactly(12L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityValue)
            .as("Must find capacity value of the first dayoff")
            .containsExactly(300L);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner_with_consecutive_dayoffs_in_multiple_platforms.sql")
    void findPartnerAndCapacitiesMultipleTimesFromDifferentPlatforms() {
        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                THREE_DAYS,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_10,
                OCTOBER_10.plusDays(THREE_DAYS)
            );

        softly.assertThat(result.size())
            .as("Must find exactly 2 records")
            .isEqualTo(2);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerId)
            .as("Must find the same partner 2 times")
            .containsExactly(1L, 1L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerName)
            .as("Must find correct partner name")
            .containsExactly("CrossDock1", "CrossDock1");

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityId)
            .as("Must find capacity of the first dayoff in each platform")
            .containsExactlyInAnyOrder(10L, 12L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityValue)
            .as("Must find capacity value of the first dayoff")
            .containsExactlyInAnyOrder(100L, 300L);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partner_with_many_capacities_and_dayoffs.sql")
    void findPartnerAndAnyCapacityOfEarliestDayOffs() {
        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                THREE_DAYS,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_10,
                OCTOBER_10.plusDays(THREE_DAYS)
            );

        softly.assertThat(result.size())
            .as("Must find exactly 1 record")
            .isEqualTo(1);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerId)
            .as("Must find correct partner id")
            .containsExactly(1L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerName)
            .as("Must find correct partner name")
            .containsExactly("CrossDock1");

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityId)
            .as("Must find any capacity of the earliest dayoffs")
            .containsAnyOf(13L, 14L, 15L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityValue)
            .as("Must find any capacity value of the earliest dayoffs")
            .containsAnyOf(400L, 500L, 600L);
    }

    @Test
    @Sql("/data/repository/partnerCapacity/partners_with_many_capacities_with_many_dayoffs_in_many_platforms.sql")
    void findCorrectPartnersAndCapacitiesFromMultiplePlatforms() {
        List<PartnerAndCapacityDto> result = partnerCapacityRepository
            .findAllPartnersWithCapacityOfPartnerTypeWithDayOffsInPeriod(
                FIVE_DAYS,
                TYPE_SUPPLIER,
                CAPACITY_SERVICE_SHIPMENT,
                OCTOBER_11,
                OCTOBER_11.plusDays(FIVE_DAYS)
            );

        softly.assertThat(result.size())
            .as("Must find exactly 5 records")
            .isEqualTo(5);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerId)
            .as("Must find correct partners")
            .containsExactlyInAnyOrder(2L, 5L, 5L, 5L, 7L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getPartnerName)
            .as("Must find correct partner names")
            .containsExactlyInAnyOrder("CrossDock2", "CrossDock4", "CrossDock4", "CrossDock4", "CrossDock6");

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityId)
            .as("Must find all capacities of the earliest dayoffs for each partner in resultSet")
            .containsExactlyInAnyOrder(12L, 17L, 18L, 19L, 21L);

        softly.assertThat(result)
            .extracting(PartnerAndCapacityDto::getCapacityValue)
            .as("Must find all capacity values of the earliest dayoffs for each partner in resultSet")
            .containsAnyOf(300L, 800L, 900L, 1000L, 1200L);
    }

    void addDayOff(Long capacityId, LocalDate date) {
        partnerCapacityDayOffService.createCapacityDayOff(capacityId, date);
    }
}
