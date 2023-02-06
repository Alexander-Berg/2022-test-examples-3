package ru.yandex.market.core.marketmanager;

import java.util.Collection;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.balance.BalanceConstants;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.marketmanager.db.DbMarketManagerService;
import ru.yandex.market.core.marketmanager.db.ManagerChannel;
import ru.yandex.market.core.passport.model.ManagerInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.core.marketmanager.MarketManagerService.MARKET_SUPPORT_MANAGER_UID;
@DbUnitDataSet(before = "DbMarketManagerServiceTest.before.csv")
class DbMarketManagerServiceTest extends FunctionalTest {

    @Autowired
    private DbMarketManagerService service;

    @Autowired
    private BalanceService balanceService;

    @Test
    @DbUnitDataSet(before = "DbMarketManagerServiceTestLink.before.csv",
            after = "DbMarketManagerServiceTestLink.after.csv")
    void testLinkChannelToManager() {
        service.linkChannelToManager(4, 1, 100500);
    }

    @Test
    @DbUnitDataSet(before = "DbMarketManagerServiceTestUnlink.before.csv",
            after = "DbMarketManagerServiceUnlink.after.csv")
    void testUnlinkChannelFromManager() {
        service.unlinkChannelFromManager(3, 100500);
    }

    @Test
    @DbUnitDataSet(before = "DbMarketManagerServiceTestGet.before.csv")
    void testGetManagersWithChannel() {
        Collection<ManagerChannel> managersWithChannel = service.getManagersWithChannel();
        assertThat(managersWithChannel, Matchers.hasSize(3));
    }

    @Test
    @DbUnitDataSet(before = "DbMarketManagerServiceTestGetManager.before.csv")
    void getPartnerManager() {
        // Менеджер по-умолчанию
        assertThat(service.getPartnerManager(1).getId(), equalTo(MARKET_SUPPORT_MANAGER_UID));

        // Явно указанный менеджер
        assertThat(service.getPartnerManager(2).getId(), equalTo(1L));

        // Индустриальный менеджер, не должен светиться
        assertThat(service.getPartnerManager(3L).getId(), equalTo(-2L));

        // Менеджеры магазинов
        assertThat(service.getPartnerManager(4).getId(), equalTo(2L));
        assertThat(service.getPartnerManager(5).getId(), equalTo(-1L));

        // Менеджер по умолчанию для партнера в DaaS
        assertThat(service.getPartnerManager(8).getId(),
                equalTo(BalanceConstants.DEFAULT_DAAS_MANAGER_UID));

    }

    @Test
    @DbUnitDataSet(before = "DbMarketManagerServiceTestGetPartnerOrManagerTest.before.csv")
    void getPartnerOrManagerTest() {
        var answer = service.getAgencyOrManager(1);
        assertThat(answer.getId(), equalTo(1L));
        assertThat(answer.getManagerType(), equalTo(ManagerInfo.ManagerType.AGENCY));
        assertThat(answer.getName(), equalTo("agencyName"));
        Mockito.verifyNoInteractions(balanceService);
    }

}
