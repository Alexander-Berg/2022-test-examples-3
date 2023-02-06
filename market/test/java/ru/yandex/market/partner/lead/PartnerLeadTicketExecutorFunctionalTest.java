package ru.yandex.market.partner.lead;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.onboarding.lead.PartnerLeadService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для {@link PartnerLeadTicketExecutor} и {@link PartnerLeadCleanExecutor}.
 */
public class PartnerLeadTicketExecutorFunctionalTest extends FunctionalTest {

    @Autowired
    private PartnerLeadService partnerLeadService;

    @Autowired
    private OperatorWindowPartnerService operatorWindowPartnerService;

    @Autowired
    private EnvironmentService environmentService;

    private OperatorWindowRestClient operatorWindowRestClient = mock(OperatorWindowRestClient.class);

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadTicketExecutorFunctionalTest.testJob.before.csv",
            after = "PartnerLeadTicketExecutorFunctionalTest.testJob.after.csv"
    )
    void testJob() {
        when(operatorWindowRestClient.addTicket(any())).thenReturn("ticket@111");

        OperatorWindowService operatorWindowService = new OperatorWindowService(
                partnerLeadService,
                operatorWindowRestClient,
                operatorWindowPartnerService);
        PartnerLeadTicketExecutor partnerLeadTicketExecutor =
                new PartnerLeadTicketExecutor(operatorWindowService, environmentService);
        partnerLeadTicketExecutor.doJob(null);
        verify(operatorWindowRestClient, times(1)).addTicket(any());
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadTicketExecutorFunctionalTest.testJobUpdate.before.csv",
            after = "PartnerLeadTicketExecutorFunctionalTest.testJobUpdate.after.csv"
    )
    void testJobUpdate() {
        when(operatorWindowRestClient.updateTicket(any())).thenReturn("ticket@12");

        OperatorWindowService operatorWindowService = new OperatorWindowService(
                partnerLeadService,
                operatorWindowRestClient,
                operatorWindowPartnerService);
        PartnerLeadTicketExecutor partnerLeadTicketExecutor =
                new PartnerLeadTicketExecutor(operatorWindowService, environmentService);
        partnerLeadTicketExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadTicketExecutorFunctionalTest.testJobPartnersCreate.before.csv",
            after = "PartnerLeadTicketExecutorFunctionalTest.testJobPartnersCreate.after.csv"
    )
    void testJobPartnersCreate() {
        when(operatorWindowRestClient.updateTicket(any())).thenReturn("ticket@12");
        when(operatorWindowRestClient.findEntityGid("orderSupplierType",
                "code", "THIRD_PARTY")).thenReturn("third_party");
        when(operatorWindowRestClient.findEntityGid("orderSupplierType",
                "code", "THIRD_PARTY")).thenReturn("third_party");
        when(operatorWindowRestClient.findEntities("distributionType")).thenReturn(
                List.of(new OperatorWindowSearchEntityDTO("adv", "ADV"),
                        new OperatorWindowSearchEntityDTO("fby", "FBY"),
                        new OperatorWindowSearchEntityDTO("fby+", "FBY+"),
                        new OperatorWindowSearchEntityDTO("fbs", "FBS"),
                        new OperatorWindowSearchEntityDTO("dbs", "DBS"))
        );
        when(operatorWindowRestClient.createShop(any())).thenReturn("account@1");
        OperatorWindowService operatorWindowService = new OperatorWindowService(
                partnerLeadService,
                operatorWindowRestClient,
                operatorWindowPartnerService);
        PartnerLeadTicketExecutor partnerLeadTicketExecutor =
                new PartnerLeadTicketExecutor(operatorWindowService, environmentService);
        partnerLeadTicketExecutor.doJob(null);
        verify(operatorWindowRestClient, times(1)).createShop(any());
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadTicketExecutorFunctionalTest.testCleanJob.before.csv",
            after = "PartnerLeadTicketExecutorFunctionalTest.testCleanJob.after.csv"
    )
    void testCleanJob() {
        PartnerLeadCleanExecutor partnerLeadCleanExecutor = new PartnerLeadCleanExecutor(partnerLeadService);
        partnerLeadCleanExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerLeadTicketExecutorFunctionalTest.testCleanJobEnv.before.csv",
            after = "PartnerLeadTicketExecutorFunctionalTest.testCleanJobEnv.after.csv"
    )
    void testCleanJobEnv() {
        PartnerLeadCleanExecutor partnerLeadCleanExecutor = new PartnerLeadCleanExecutor(partnerLeadService);
        partnerLeadCleanExecutor.doJob(null);
    }
}
