package ru.yandex.market.core.delivery.balance;

import java.time.Clock;

import com.google.common.collect.ImmutableMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.api.cpa.yam.dao.PrepayRequestDao;
import ru.yandex.market.api.cpa.yam.dao.impl.PrepayRequestDaoImpl;
import ru.yandex.market.core.database.EmbeddedPostgresConfig;
import ru.yandex.market.core.delivery.service.billing.DeliveryBalanceOrderDao;
import ru.yandex.market.core.delivery.service.billing.DeliveryBalanceOrderService;
import ru.yandex.market.core.orginfo.OrganizationInfoService;
import ru.yandex.market.core.orginfo.model.OrganizationInfo;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.program.ProgramSwitchOver;
import ru.yandex.market.core.salesnotes.SalesNotesService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@Import(EmbeddedPostgresConfig.class)
public class DeliveryBalanceOrderServiceTestConfig {
    public static final Long SHOP_ID = 774L;
    private static final String OGRN = "5999988881234";
    private static final long ORG_ID = 789L;
    private static final String REG_NUMBER = "109341";
    private static final String FACT_ADDR = "Pushkina";
    private static final String INFO_URL = "http://info.url";

    private static OrganizationInfo prepareTestOrganizationInfo() {
        OrganizationInfo organizationInfo = new OrganizationInfo();
        organizationInfo.setId(ORG_ID);
        organizationInfo.setRegistrationNumber(REG_NUMBER);
        organizationInfo.setOgrn(OGRN);
        organizationInfo.setDatasourceId(SHOP_ID);
        organizationInfo.setJuridicalAddress(FACT_ADDR);
        organizationInfo.setInfoUrl(INFO_URL);
        return organizationInfo;
    }

    private static final OrganizationInfo ORGANIZATION_INFO = prepareTestOrganizationInfo();

    @Bean
    public Clock clock() {
        return mock(Clock.class);
    }

    @Bean
    OrganizationInfoService mockedOrganizationInfoService() {
        OrganizationInfoService organizationInfoService = mock(OrganizationInfoService.class);
        when(organizationInfoService.getDatasourceOrganizations(SHOP_ID))
                .thenReturn(ImmutableMap.of(ORGANIZATION_INFO.getId(), ORGANIZATION_INFO));
        return organizationInfoService;
    }

    @Bean
    SalesNotesService mockedSalesNotesService() {
        return mock(SalesNotesService.class);
    }

    @Bean
    ParamService mockedParamService() {
        return mock(ParamService.class);
    }

    @Bean
    EnvironmentService mockedEnvironmentService() {
        return mock(EnvironmentService.class);
    }

    @Bean
    ProgramSwitchOver programSwitchOver() {
        ProgramSwitchOver programSwitchOver = mock(ProgramSwitchOver.class);
        when(programSwitchOver.isProgramOldMechanicsEnabled()).thenReturn(true);
        return programSwitchOver;
    }

    @Bean
    DeliveryBalanceOrderDao deliveryBalanceOrderDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new DeliveryBalanceOrderDao(namedParameterJdbcTemplate);
    }

    @Bean
    PrepayRequestDao prepayRequestDao(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new PrepayRequestDaoImpl(namedParameterJdbcTemplate,
                PrepayRequestDaoImpl.selectCompletedRequestEnabled(mockedEnvironmentService()));
    }

    @Bean
    DeliveryBalanceOrderService DeliveryBalanceOrderService(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            DeliveryBalanceOrderDao deliveryBalanceOrderDao
    ) {
        return new DeliveryBalanceOrderService(namedParameterJdbcTemplate, deliveryBalanceOrderDao);
    }
}
