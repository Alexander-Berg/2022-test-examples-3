package ru.yandex.market.partner.notification.api;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypeDTO;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypesRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypesResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerWithPlacementProgramTypeDTO;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.environment.EnvironmentService;
import ru.yandex.mj.generated.client.self.api.SenderApiClient;
import ru.yandex.mj.generated.client.self.model.DestinationDTO;
import ru.yandex.mj.generated.client.self.model.SendNotificationRequestJson;
import ru.yandex.mj.generated.client.self.model.SendNotificationResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class SenderApiServiceTest extends AbstractFunctionalTest {

    @Autowired
    SenderApiClient senderApiClient;

    @Autowired
    EnvironmentService environmentService;

    @Autowired
    MbiOpenApiClient mbiOpenApiClient;

    @Test
    void sendNotificationJson() throws Exception {
        environmentService.addValues("context.debug.templates", Set.of());
        SendNotificationResponse expected = new SendNotificationResponse().groupId(1L);

        mockMbiClient();

        ObjectMapper mapper = new ObjectMapper();
        InputStream is = getClass().getResourceAsStream("sender.data.json");
        Object data = mapper.readValue(is, Object.class);
        SendNotificationRequestJson requestJson = new SendNotificationRequestJson()
                .typeId(1627032762L)
                .destination(new DestinationDTO().shopId(1L))
                .data(data)
                .renderOnly(false);


        SendNotificationResponse actual =
                senderApiClient.sendNotificationJson(requestJson).scheduleResponse().get().body();
        assertThat(actual, equalTo(expected));
    }

    private void mockMbiClient() {
        PartnerPlacementProgramTypesRequest request = new PartnerPlacementProgramTypesRequest().partnerIds(List.of(1L));
        PartnerPlacementProgramTypesResponse programs = new PartnerPlacementProgramTypesResponse()
                .programTypes(List.of(new PartnerWithPlacementProgramTypeDTO()
                        .partnerId(1L)
                        .programTypes(List.of(PartnerPlacementProgramTypeDTO.DROPSHIP))));
        Mockito.when(mbiOpenApiClient.providePartnerPlacementProgramTypes(
                Mockito.eq(request))).thenReturn(programs);
    }
}
