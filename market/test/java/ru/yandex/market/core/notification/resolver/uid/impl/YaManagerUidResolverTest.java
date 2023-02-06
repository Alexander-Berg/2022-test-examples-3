package ru.yandex.market.core.notification.resolver.uid.impl;

import java.util.Collection;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.ThrowsException;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.agency.Agency;
import ru.yandex.market.core.agency.AgencyService;
import ru.yandex.market.core.campaign.IdsResolver;
import ru.yandex.market.core.marketmanager.MarketManagerService;
import ru.yandex.market.core.notification.context.IShopNotificationContext;
import ru.yandex.market.core.passport.model.ManagerInfo;
import ru.yandex.market.core.staff.Employee;
import ru.yandex.market.core.staff.EmployeeGroup;


public class YaManagerUidResolverTest extends FunctionalTest {
    private static final ThrowsException DEFAULT_ANSWER = new ThrowsException(new RuntimeException());
    private static final long SHOP_ID = 774;
    private static final long CAMPAIGN_ID = 10774;
    private static final String AGENCY_EMAIL = "agency@domain.com";
    private static final String MANAGER_EMAIL = "manager@domain.com";
    private static final Agency AGENCY = new Agency(123, "testAgency", 456, AGENCY_EMAIL);
    private static final ManagerInfo MANAGER = ManagerInfo.createFrom(new Employee.Builder()
            .setUid(123)
            .setLogin("testLogin")
            .setName("testName")
            .setEmail(MANAGER_EMAIL)
            .setPassportEmail(MANAGER_EMAIL)
            .setGroup(EmployeeGroup.MANAGER)
            .setSubstituteId(345)
            .build());

    private static MarketManagerService getMockDatasourceManagerService(boolean returnManager) {
        MarketManagerService datasourceManagerService = Mockito.mock(MarketManagerService.class, DEFAULT_ANSWER);
        if (returnManager) {
            Mockito.doReturn(MANAGER).when(datasourceManagerService).getPartnerManager(SHOP_ID);
        }
        return datasourceManagerService;
    }

    private static IShopNotificationContext getMockNotificationContext() {
        IShopNotificationContext notificationContext = Mockito.mock(IShopNotificationContext.class, DEFAULT_ANSWER);
        Mockito.doReturn(SHOP_ID).when(notificationContext).getShopId();
        return notificationContext;
    }

    private static AgencyService getMockAgencyService(boolean returnAgency, boolean returnNull) {
        AgencyService agencyService = Mockito.mock(AgencyService.class, DEFAULT_ANSWER);
        if (returnAgency) {
            Agency returnValue = returnNull ? null : AGENCY;
            Mockito.doReturn(returnValue).when(agencyService).getCampaignAgency(CAMPAIGN_ID);
        }
        return agencyService;
    }

    private static IdsResolver getMockIdsResolver() {
        IdsResolver idsResolver = Mockito.mock(IdsResolver.class, DEFAULT_ANSWER);
        Mockito.doReturn(CAMPAIGN_ID).when(idsResolver).getCampaignId(SHOP_ID);
        return idsResolver;
    }

    @Test
    public void ignoreAgencyStrategyTest() {
        YaManagerUidResolver resolver = new YaManagerUidResolver(true);
        resolver.setAgencyService(getMockAgencyService(false, false));
        resolver.setIdsResolver(getMockIdsResolver());
        resolver.setMarketManagerService(getMockDatasourceManagerService(true));

        Collection<Long> uids = resolver.resolveUids("testAlias", getMockNotificationContext());
        MatcherAssert.assertThat(uids.size(), Matchers.equalTo(1));
        MatcherAssert.assertThat(uids, Matchers.contains(123L));
    }

    @Test
    public void agencyStrategyTest() {
        YaManagerUidResolver resolver = new YaManagerUidResolver(false);
        resolver.setAgencyService(getMockAgencyService(true, false));
        resolver.setIdsResolver(getMockIdsResolver());
        resolver.setMarketManagerService(getMockDatasourceManagerService(false));

        Collection<Long> uids = resolver.resolveUids("testAlias", getMockNotificationContext());
        MatcherAssert.assertThat(uids.size(), Matchers.equalTo(1));
        MatcherAssert.assertThat(uids, Matchers.contains(456L));
    }

    @Test
    public void campaignStrategyTest() {
        YaManagerUidResolver resolver = new YaManagerUidResolver(false);
        resolver.setAgencyService(getMockAgencyService(true, true));
        resolver.setIdsResolver(getMockIdsResolver());
        resolver.setMarketManagerService(getMockDatasourceManagerService(true));

        Collection<Long> uids = resolver.resolveUids("testAlias", getMockNotificationContext());
        MatcherAssert.assertThat(uids.size(), Matchers.equalTo(1));
        MatcherAssert.assertThat(uids, Matchers.contains(123L));
    }
}
