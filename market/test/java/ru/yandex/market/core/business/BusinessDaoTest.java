package ru.yandex.market.core.business;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.db.SortingInfo;
import ru.yandex.common.util.db.SortingOrder;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.core.contact.model.Contact;
import ru.yandex.market.core.partner.placement.GeneralPlacementType;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Тесты на {@link BusinessDao}.
 */
@DbUnitDataSet(before = "BusinessDaoTest.before.csv")
class BusinessDaoTest extends FunctionalTest {

    @Autowired
    private BusinessDao businessDao;

    @Autowired
    private BalanceContactService balanceContactService;

    @Test
    void getBusiness() {
        var business = businessDao.getBusiness(505L);
        assertEquals(505L, business.getId());
        assertEquals("бизнес 1", business.getName());
        assertEquals(10L, business.getCampaignId());
    }

    @Test
    @DbUnitDataSet(after = "BusinessDaoTest.create.after.csv")
    void createBusiness() {
        var businessName = "test_business";
        businessDao.createBusiness(new BusinessInfo(12, businessName));
    }

    @Test
    @DbUnitDataSet(before = "BusinessDaoTest.findByDate.before.csv")
    void searchByDateTest() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -15);
        Set<Long> businessIds = businessDao.searchBusinessIdsByCreationDate(cal.getTime(), new Date());
        //не должны вернуть 1000, тк он создавался 20 дней назад
        assertEquals(Set.of(505L, 506L, 507L, 508L, 509L, 1001L, 1002L, 1003L, 1004L, 13L), businessIds);
    }

    @Test
    @DbUnitDataSet(after = "BusinessDaoTestAddBusinessService.after.csv")
    void createBusinessServiceLink() {
        var businessId = 507L;
        var serviceId = 3L;
        businessDao.createBusinessServiceLink(businessId, serviceId, MarketServiceType.SHOP);
    }

    private static Stream<Arguments> regNumTestArgs() {
        return Stream.of(
                Arguments.of("111", 505L, 100L),
                Arguments.of("222", 506L, 200L),
                Arguments.of("333", 507L, 300L),
                Arguments.of("444", null, 300L),
                Arguments.of("555", null, 400L),
                Arguments.of("111", null, 500L),
                Arguments.of("666", 508L, 600L)
        );
    }

    @ParameterizedTest
    @MethodSource("regNumTestArgs")
    void testGetBusinessByRegNum(String regNum, Long businessId, Long clientId) {
        BusinessInfo business = businessDao.getBusinessByRegNumAndClientId(regNum, clientId);
        if (businessId == null) {
            assertNull(business);
        } else {
            assertEquals(businessId, business.getId());
        }
    }

    @ParameterizedTest
    @MethodSource("partnersArgsEmpty")
    void testGetBusinessIdsByPartnerIdsEmpty(List<Long> partnerIds) {
        List<BusinessPartnerInfo> businessPartnerInfoList
                = businessDao.getBusinessIdsByPartnerIds(new HashSet<>(partnerIds));
        assertTrue(businessPartnerInfoList.isEmpty());
    }

    private static Stream<Arguments> partnersArgsEmpty() {
        return Stream.of(
                Arguments.of(List.of()),
                Arguments.of(List.of(10L, 11L))
        );
    }

    @Test
    void searchBusinessTest() {
        final List<BusinessInfo> businesses = businessDao.searchBusinesses("1",
                new SortingInfo<>("ID", SortingOrder.ASC), 0, 10);
        assertThat(businesses, notNullValue());
        assertThat(businesses, hasSize(3));

        assertThat(businesses.get(0).getId(), equalTo(13L));
        assertThat(businesses.get(0).getName(), equalTo("бизнес 13"));

        assertThat(businesses.get(1).getId(), equalTo(505L));
        assertThat(businesses.get(1).getName(), equalTo("бизнес 1"));

        assertThat(businesses.get(2).getId(), equalTo(506L));
        assertThat(businesses.get(2).getName(), equalTo("бизнес 1"));
    }

    @Test
    @DbUnitDataSet(before = "BusinessDaoTest.searchForAgencies.before.csv")
    void searchBusinessForAgencyTest() {
        Contact contact = new Contact();
        contact.setId(1);
        when(balanceContactService.getClientIdByUid(1)).thenReturn(100011L);
        final List<BusinessInfo> businesses = businessDao.searchBusinesses(contact, 100011L, "", false,
                SeekSliceRequest.firstN(100));
        assertThat(businesses, notNullValue());
        assertThat(businesses, hasSize(1));

        assertThat(businesses.get(0).getId(), equalTo(13L));
        assertThat(businesses.get(0).getName(), equalTo("бизнес 13"));
    }

    @ParameterizedTest
    @MethodSource("partnersArgs")
    void testGetBusinessIdsByPartnerIds(List<Long> partnerIds, List<BusinessPartnerInfo> expectedBusinessId) {
        List<BusinessPartnerInfo> businessPartnerInfoList
                = businessDao.getBusinessIdsByPartnerIds(new HashSet<>(partnerIds));
        businessPartnerInfoList.sort(Comparator.comparingLong(BusinessPartnerInfo::getBusinessId));
        assertEquals(expectedBusinessId, businessPartnerInfoList);
    }

    @Test
    void testGetPartnerIdsByBusiness() {
        assertThat(businessDao.getPartnerIdsByBusiness(505L), containsInAnyOrder(1L));
        assertThat(businessDao.getPartnerIdsByBusiness(509L), containsInAnyOrder(7L, 8L, 9L));
    }

    @Test
    void testGetBusinessUserServiceCount() {
        long businessId = 13L;
        Contact contact = new Contact();
        contact.setId(1);
        Map<Long, Map<GeneralPlacementType, Integer>> businessUserServiceCount
                = businessDao.getBusinessUserServiceCount(contact, null, List.of(businessId));
        Map<GeneralPlacementType, Integer> generalPlacementTypeIntegerMap = businessUserServiceCount.get(businessId);
        long totalServicesCount =
                generalPlacementTypeIntegerMap.values().stream().mapToInt(val -> val).sum();
        Assertions.assertEquals(6, totalServicesCount);
        Assertions.assertTrue(generalPlacementTypeIntegerMap
                .keySet()
                .containsAll(List.of(GeneralPlacementType.values())));
    }

    @Test
    void testGetMarketplaces() {
        assertThat(businessDao.getMarketplaces(13L), containsInAnyOrder(15L, 14L, 17L, 16L));
    }

    private static Stream<Arguments> partnersArgs() {
        return Stream.of(
                Arguments.of(List.of(1L, 2L), List.of(
                        new BusinessPartnerInfo(1L, 505L, MarketServiceType.SHOP),
                        new BusinessPartnerInfo(2L, 506L, MarketServiceType.SHOP)
                )),
                Arguments.of(List.of(4L), List.of(
                        new BusinessPartnerInfo(4L, 507L, MarketServiceType.SHOP)))
        );
    }
}
