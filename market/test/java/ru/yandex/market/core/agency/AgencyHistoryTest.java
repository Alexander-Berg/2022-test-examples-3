package ru.yandex.market.core.agency;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.campaign.CampaignService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class AgencyHistoryTest extends FunctionalTest {
    @Autowired
    private AgencyService agencyService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private BalanceService balanceService;

    @Test
    @DbUnitDataSet(before = "agencyHistoryTest.before.csv", after = "agencyHistoryTest.after.csv")
    void testAddSubclientAndDumpShopAgency() {
        agencyService.addSubclient(1L, 100L, 332L, 774L, 10774L);
    }

    @Test
    @DbUnitDataSet(before = "agencyHistoryTest.before.csv", after = "dumpAgencyHistoryTest.after.csv")
    void testDumpShopAgency() {
        agencyService.dumpShopAgencyHistory(100L, 332L, 774L, 10774L);
    }


    @Test
    @DbUnitDataSet(before = "agencyHistoryTest.before.csv", after = "agencyHistoryTestAdd.after.csv")
    void testDumpShopAgencyWhenAddClient() {
        ClientInfo clientInfo = new ClientInfo(301L, ClientType.OAO, false, 101L);
        when(balanceService.getClient(eq(301L))).thenReturn(clientInfo);
        campaignService.assignClient(10775L, 301L, 1L, new ArrayList<>(), 2L);
    }

}
