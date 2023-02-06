package ru.yandex.market.core.periodic_survey.service.scheduler;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DbUnitDataSet(before = "NetPromoterScorePartnerUsersProviderTest.before.csv")
class NetPromoterScorePartnerUsersProviderTest extends FunctionalTest {
    @Autowired
    NetPromoterScorePartnerUsersProvider partnerUsersProvider;

    @Test
    void emptyPartnersList() {
        assertTrue(partnerUsersProvider.getPartnerToUsersMap(List.of()).isEmpty());
    }

    @Test
    void partnerWithShopAdmin() {
        var partnerToUsers = partnerUsersProvider.getPartnerToUsersMap(List.of(101L));

        assertThat(partnerToUsers, equalTo(Map.of(101L, Set.of(1010L))));
    }

    @Test
    void partnerWithShopOperator() {
        var partnerToUsers = partnerUsersProvider.getPartnerToUsersMap(List.of(102L));

        assertTrue(partnerToUsers.isEmpty());
    }

    @Test
    void partnerWithAllRolesContact() {
        var partnerToUsers = partnerUsersProvider.getPartnerToUsersMap(List.of(104L));

        assertThat(partnerToUsers, equalTo(Map.of(104L, Set.of(1040L))));
    }

    @Test
    void partnerWithMultipleUsers() {
        var partnerToUsers = partnerUsersProvider.getPartnerToUsersMap(List.of(103L));

        assertThat(partnerToUsers, equalTo(Map.of(103L, Set.of(1031L, 1035L, 1036L))));
    }

    @Test
    void multiplePartners() {
        var partnerToUsers = partnerUsersProvider.getPartnerToUsersMap(List.of(101L, 102L, 103L, 104L));

        assertThat(partnerToUsers, equalTo(Map.of(
                101L, Set.of(1010L),
                103L, Set.of(1031L, 1035L, 1036L),
                104L, Set.of(1040L)
        )));
    }
}
