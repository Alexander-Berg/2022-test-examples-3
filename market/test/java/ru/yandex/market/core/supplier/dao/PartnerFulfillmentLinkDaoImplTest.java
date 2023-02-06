package ru.yandex.market.core.supplier.dao;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.supplier.model.PartnerFulfillmentLink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.core.delivery.DeliveryServiceType.CROSSDOCK;
import static ru.yandex.market.core.delivery.DeliveryServiceType.DROPSHIP;

/**
 * Тесты для {@link PartnerFulfillmentLinkDaoImpl}
 */
@DbUnitDataSet(before = "PartnerFulfillmentLinkDaoImplTest.before.csv")
class PartnerFulfillmentLinkDaoImplTest extends FunctionalTest {

    @Autowired
    private PartnerFulfillmentLinkDao partnerFulfillmentLinkDao;

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(774L, 1, 1, new PartnerFulfillmentLinkWithType(new PartnerFulfillmentLink(774L, 145L, null), CampaignType.SHOP)),
                Arguments.of(1000L, 1, 1, new PartnerFulfillmentLinkWithType(new PartnerFulfillmentLink(1000L, 145L, 100L), CampaignType.SUPPLIER))
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void findByPartnerExtended(long partnerId, int expectedPartnerCount,
                               int expectedFeedsCount,
                               PartnerFulfillmentLinkWithType partnerFulfillmentLink) {

        Map<PartnerFulfillmentLinkWithType, List<Long>> partnerExtended =
                partnerFulfillmentLinkDao.findByPartnersExtended(Collections.singletonList(partnerId));

        assertEquals(expectedPartnerCount, partnerExtended.size());
        assertEquals(expectedFeedsCount, partnerExtended.get(partnerFulfillmentLink).size());
    }

    @Test
    void findByUnknownPartnerExtended() {
        Map<PartnerFulfillmentLinkWithType, List<Long>> partnerExtended =
                partnerFulfillmentLinkDao.findByPartnersExtended(Collections.singletonList(20L));
        assertTrue(partnerExtended.isEmpty());
    }

    @Test
    void getServiceToPartner() {
        Map<Long, Set<Long>> servicesToPartner =
                partnerFulfillmentLinkDao.getDeliveryServiceToPartner(Set.of(DROPSHIP, CROSSDOCK));

        assertTrue(servicesToPartner.containsKey(1005L));
        assertTrue(servicesToPartner.containsKey(1006L));

        assertEquals(servicesToPartner.get(1005L).stream().findFirst().get(), 45785L);
        assertEquals(servicesToPartner.get(1006L).stream().findFirst().get(), 65152L);
    }
}
