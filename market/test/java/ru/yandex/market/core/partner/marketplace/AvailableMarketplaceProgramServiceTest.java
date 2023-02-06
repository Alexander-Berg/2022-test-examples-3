package ru.yandex.market.core.partner.marketplace;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Функциональные тесты для {@link AvailableMarketplaceProgramService}
 */
public class AvailableMarketplaceProgramServiceTest extends FunctionalTest {
    @Autowired
    AvailableMarketplaceProgramService availableMarketplaceProgramService;

    @Test
    @DbUnitDataSet(
            before = "reloadCandidatesTest.before.csv",
            after = "reloadCandidatesTest.after.csv")
    void reloadCandidates() {
        availableMarketplaceProgramService.reloadPrograms(List.of(
                new AvailableMarketplaceProgramInfo.Builder()
                        .setPartnerId(1L)
                        .setIsDropship(true)
                        .setIsFulfillment(true)
                        .setExpectedOrders(120)
                        .build(),
                new AvailableMarketplaceProgramInfo.Builder()
                        .setPartnerId(2L)
                        .setIsDropship(true)
                        .setIsFulfillment(false)
                        .setExpectedOrders(10)
                        .build(),
                new AvailableMarketplaceProgramInfo.Builder()
                        .setPartnerId(3L)
                        .setIsDropship(true)
                        .setIsFulfillment(true)
                        .setExpectedOrders(0)
                        .build()
        ));
    }

    @Test
    @DbUnitDataSet(before = "getPartnerProgramInfo.before.csv")
    void getPartnerPrograms() {
        assertTrue(reflectionEquals(new AvailableMarketplaceProgramInfo.Builder()
                        .setPartnerId(1L)
                        .setIsDropship(true)
                        .setIsFulfillment(true)
                        .setExpectedOrders(10)
                        .build(),
                availableMarketplaceProgramService.getPartnerProgramInfo(1L)
        ));
        assertTrue(reflectionEquals(new AvailableMarketplaceProgramInfo.Builder()
                        .setPartnerId(2L)
                        .setIsDropship(true)
                        .setIsFulfillment(false)
                        .setExpectedOrders(11)
                        .build(),
                availableMarketplaceProgramService.getPartnerProgramInfo(2L)));
        assertTrue(reflectionEquals(new AvailableMarketplaceProgramInfo.Builder()
                        .setPartnerId(3L)
                        .setIsDropship(false)
                        .setIsFulfillment(false)
                        .setExpectedOrders(0)
                        .build(),
                availableMarketplaceProgramService.getPartnerProgramInfo(3L)));
        assertTrue(reflectionEquals(new AvailableMarketplaceProgramInfo.Builder()
                        .setPartnerId(4L)
                        .setIsDropship(false)
                        .setIsFulfillment(false)
                        .setExpectedOrders(0)
                        .build(),
                availableMarketplaceProgramService.getPartnerProgramInfo(4L)));
    }

    @Test
    @DbUnitDataSet(
            before = "updatePartnerProgramInfo.before.csv",
            after = "updatePartnerProgramInfo.after.csv")
    void updatePartnerPrograms() {
        availableMarketplaceProgramService.updatePartnerProgramInfo(1L, 2L,
                PartnerPlacementProgramType.FULFILLMENT);
        availableMarketplaceProgramService.updatePartnerProgramInfo(2L, 3L,
                PartnerPlacementProgramType.DROPSHIP);
    }

    @Test
    @DbUnitDataSet(
            before = "AvailableMarketplaceProgramServiceTest.setFeedsImportedFlag.before.csv",
            after = "AvailableMarketplaceProgramServiceTest.setFeedsImportedFlag.after.csv")
    void setFeedsImportedFlag() {
        availableMarketplaceProgramService.setFeedsImportedFlag(2L);
    }

    @Test
    @DbUnitDataSet(
            before = "AvailableMarketplaceProgramServiceTest.getImportedFeedsPartnerId.before.csv")
    void getImportedFeedsPartnerId() {
        assertEquals(1L,
                availableMarketplaceProgramService.getImportedFeedsPartnerId(2L));
        assertEquals(3L,
                availableMarketplaceProgramService.getImportedFeedsPartnerId(3L));
        assertEquals(4L,
                availableMarketplaceProgramService.getImportedFeedsPartnerId(4L));
        assertEquals(6L,
                availableMarketplaceProgramService.getImportedFeedsPartnerId(6L));
    }
}
