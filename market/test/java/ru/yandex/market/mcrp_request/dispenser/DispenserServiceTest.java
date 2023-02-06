package ru.yandex.market.mcrp_request.dispenser;

import java.util.Collections;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.mcrp_request.DAO.MDBResources;
import ru.yandex.market.mcrp_request.DAO.Request;
import ru.yandex.market.mcrp_request.DAO.RequestResourcesData;
import ru.yandex.market.mcrp_request.DAO.YTResources;
import ru.yandex.market.mcrp_request.clients.AbcApiClient;
import ru.yandex.market.mcrp_request.clients.dispenser.DispenserFeignClient;
import ru.yandex.market.mcrp_request.clients.dispenser.Projects;
import ru.yandex.market.mcrp_request.clients.dispenser.QuotaRequest;
import ru.yandex.market.mcrp_request.clients.dispenser.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.matches;

public class DispenserServiceTest {

    DispenserFeignClient client = Mockito.mock(DispenserFeignClient.class);
    AbcApiClient abcApiClient = Mockito.mock(AbcApiClient.class);
    DispenserService dispenserService = new DispenserService(client, abcApiClient, "token", 44, "2021-09");

    {
        Mockito.when(client.createQuotaRequests(any(QuotaRequest.class), matches("token")))
                .thenReturn(new Response());
        Mockito.when(client.getProjects(any()))
                .thenReturn(Mockito.mock(Projects.class));
    }

    @Test
    public void ytConverterTest() {
        YTResources.Builder ytResourceBuilder = new YTResources.Builder();

        ytResourceBuilder.setAccounts("Hahn", 1, "fakehahnaccount1");
        ytResourceBuilder.setCPUCores("Hahn", 0, (float) 8.1);
        ytResourceBuilder.setCPUCores("Hahn", 1, 40);
        ytResourceBuilder.setSSDGb("Hahn", 0, 80);

        ytResourceBuilder.setAccounts("Seneca-SAS", 0, "fakesenecasasaccount");
        ytResourceBuilder.setCPUCores("Seneca-SAS", 0, 12);

        Request.Builder requestBuilder = new Request.Builder(
                "new-age-preorder",
                "fake_abc_service_slug",
                "TEST",
                "nobody",
                RequestResourcesData.fromRequestResources(dispenserService.getDeadline(), ytResourceBuilder.build()), Collections.singletonList("nobody")
        );
        dispenserService.createABCTicket(requestBuilder.build());
        ArgumentCaptor<QuotaRequest> captor = ArgumentCaptor.forClass(QuotaRequest.class);
        Mockito.verify(client).createQuotaRequests(captor.capture(), matches("token"));
        QuotaRequest quotaReq = captor.getValue();
        assertEquals("RESOURCE_PREORDER", quotaReq.getType());
        assertEquals("market", quotaReq.getProjectKey());
        assertEquals(4, quotaReq.getChanges().length);
    }

    @Test
    public void mdbConverterTest() {
        MDBResources.Builder mdbResourceBuilder = new MDBResources.Builder();

        mdbResourceBuilder.setCPUCores("PGAAS", 0, 15);
        mdbResourceBuilder.setRAMGb("PGAAS", 0, 2048);
        mdbResourceBuilder.setHDDGb("PGAAS", 0, 500);

        Request.Builder requestBuilder = new Request.Builder(
                "new-age-preorder",
                "fake_abc_service_slug",
                "TEST",
                "nobody",
                RequestResourcesData.fromRequestResources(dispenserService.getDeadline(), mdbResourceBuilder.build()), Collections.singletonList("nobody")
        );
        dispenserService.createABCTicket(requestBuilder.build());
        ArgumentCaptor<QuotaRequest> captor = ArgumentCaptor.forClass(QuotaRequest.class);
        Mockito.verify(client).createQuotaRequests(captor.capture(), matches("token"));
        QuotaRequest quotaReq = captor.getValue();
        assertEquals("RESOURCE_PREORDER", quotaReq.getType());
        assertEquals("market", quotaReq.getProjectKey());
        assertEquals(9, quotaReq.getChanges().length);
    }
}
