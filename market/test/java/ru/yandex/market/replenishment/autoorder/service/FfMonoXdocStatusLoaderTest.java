package ru.yandex.market.replenishment.autoorder.service;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTOContainer;
import ru.yandex.market.ff.client.dto.ShopRequestFilterDTO;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class FfMonoXdocStatusLoaderTest extends FunctionalTest {

    @Autowired
    private FfMonoXdocStatusLoader loader;

    @Test
    @DbUnitDataSet(before = "FfMonoXdocStatusLoaderTest.load.before.csv",
        after = "FfMonoXdocStatusLoaderTest.load.after.csv")
    public void loadTest() {
        final FulfillmentWorkflowClientApi mockedClientApi = Mockito.mock(FulfillmentWorkflowClientApi.class);
        Mockito.doAnswer(invocation -> {
            final ShopRequestFilterDTO request = invocation.getArgument(0);

            assertThat(request.getRequestIds(), containsInAnyOrder("1","2","3","4"));

            final ShopRequestDTO responseRequest1 = new ShopRequestDTO();
            responseRequest1.setId(1L);
            responseRequest1.setStatus(RequestStatus.ACCEPTED_BY_SERVICE);
            final ShopRequestDTO responseRequest2 = new ShopRequestDTO();
            responseRequest2.setId(2L);
            responseRequest2.setStatus(RequestStatus.INVALID);
            final ShopRequestDTOContainer response = new ShopRequestDTOContainer();
            response.addRequest(responseRequest1);
            response.addRequest(responseRequest2);
            return response;
        }).when(mockedClientApi).getRequests(Mockito.any());
        ReflectionTestUtils.setField(loader, "ffApi", mockedClientApi);
        loader.load();
    }

}
