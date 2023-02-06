package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.curator.framework.CuratorFramework;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import steps.LocationSteps;
import steps.logisticsPointSteps.LogisticPointSteps;
import steps.outletSteps.OutletSteps;

import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.moderation.OutletStatus;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.curator.managers.ZkEventIdManager;
import ru.yandex.market.delivery.mdbapp.components.geo.GeoInfo;
import ru.yandex.market.delivery.mdbapp.components.service.crm.client.OrderCommands;
import ru.yandex.market.delivery.mdbapp.components.service.lms.LmsLogisticsPointClient;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.OrderEventsFailoverRepository;
import ru.yandex.market.delivery.mdbapp.util.GeoTestUtils;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.outlets.Outlet;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.OutletInfoDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.PagedOutletsDTO;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;

@RunWith(Parameterized.class)
@DirtiesContext
public class OrderEventsPollerIntegrationTest extends AllMockContextualTest {

    @Autowired
    private List<OrderEventsPoller> pollers;

    @Autowired
    private GeoInfo geoInfo;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    private MockRestServiceServer checkouterMockServer;

    @Parameter
    public String eventFilePath;

    @Parameter(1)
    public boolean orderWasLostHasBeenCalled;

    @Autowired
    private OrderCommands orderCommands;

    @Autowired
    private LmsLogisticsPointClient lmsLogisticsPointClient;

    @Autowired
    private CuratorFramework curator;

    @Autowired
    private OrderEventsFailoverRepository failoverRepository;

    @Before
    public void setUp() throws Exception {
        checkouterMockServer = MockRestServiceServer.bindTo(checkouterRestTemplate).ignoreExpectOrder(true).build();

        prepareMockServer(
            checkouterMockServer,
            4,
            "/shop-shipments/",
            "/data/controller/response/shop-shipment-response.json",
            HttpMethod.GET
        );

        prepareMockServer(
            checkouterMockServer,
            5,
            "/orders/2397856",
            "/data/controller/response/checkouter_order_response.json",
            HttpMethod.GET
        );

        prepareMockServer(
            checkouterMockServer,
            1,
            "/orders/2106833/edit-options",
            "/data/controller/response/delivery_options_response.json",
            HttpMethod.POST
        );

        prepareMockServer(
            checkouterMockServer,
            1,
            "/orders/2106833/edit",
            "/data/controller/response/delivery_options_response.json",
            HttpMethod.POST
        );

        prepareMockServer(
            checkouterMockServer,
            3,
            "/orders/events",
            "/data/events/" + eventFilePath,
            HttpMethod.GET
        );

        steps.PartnerInfoSteps.getPartnerInfoDTO(123L);
        doReturn(steps.PartnerInfoSteps.getPartnerInfoDTO(123)).when(mbiApiClient).getPartnerInfo(123L);
        Outlet outlet = OutletSteps.getDefaultOutlet("5252757", "100500", 213L);
        mockGetOutletv2(outlet);
        doReturn(outlet)
            .when(mbiApiClient).getOutlet(anyLong(), anyBoolean());

        for (String c : curator.getChildren().forPath(ZkEventIdManager.LAST_EVENT_ID_PATH_BASE)) {
            curator.delete().forPath(ZkEventIdManager.LAST_EVENT_ID_PATH_BASE + "/" + c);
        }
        curator.delete().forPath(ZkEventIdManager.LAST_EVENT_ID_PATH_BASE);

        failoverRepository.deleteAll();
    }

    // FIXME test[7] ERROR: null value in column "sorting_center_id" violates not-null constraint
    //  Detail: Failing row contains (4470085, D0000189318, 1003937, null, null, 2019-10-31 11:12:44.535, null).
    @Test
    public void eventWasProcessedCorrectly() throws Exception {

        when(lmsLogisticsPointClient.getWarehousesByPartnerId(131L))
            .thenReturn(LogisticPointSteps.getWarehouseResponse());
        when(geoInfo.getLocation(anyInt())).thenReturn(LocationSteps.getLocation());
        when(geoInfo.getRegionTree()).thenReturn(GeoTestUtils.buildRegionTree());

        pollers.get(8).poll();

        if (orderWasLostHasBeenCalled) {
            Mockito.verify(orderCommands).orderWasLost(Mockito.anyLong());
        }

        verify(fulfillmentClient, never()).getOrder(any(ResourceId.class), any(Partner.class));

        checkouterMockServer.verify();
    }

    @Parameters(name = "{0} {1} {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {"not-valid.json", false},
            {"parcel-cancellation-request.json", false},
            {"order_was_lost.json", true},
            {"checkpoint_changed_events.json", false},
            {"checkpoint_changed_events_POST_order.json", false},
            {"checkpoint_changed_events_gold_order.json", false},
            {"checkpoint_changed_events_dropship_order.json", false},
            {"checkpoint_changed_events_crossdock_order.json", false},
            {"parcel_boxes_changed.json", false},
            {"blue_cross_dock_order_create.json", false}, //переехали на lgw, dsm не дергаем
            {"blue_cross_dock_order_create_register.json", false}
        });
    }

    private void prepareMockServer(
        MockRestServiceServer mockServer,
        int timesTo,
        String urlPath,
        String responseFilePath,
        HttpMethod httpMethod
    ) throws IOException {
        mockServer.expect(
                ExpectedCount.between(0, timesTo),
                MockRestRequestMatchers.requestTo(StringContains.containsString(urlPath))
            )
            .andExpect(method(httpMethod))
            .andRespond(
                MockRestResponseCreators.withSuccess(
                    getBody(responseFilePath),
                    MediaType.APPLICATION_JSON_UTF8
                )
            );
    }

    private String getBody(String filePath) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(filePath);
        return IOUtils.toString(Objects.requireNonNull(inputStream), UTF_8);
    }

    private void mockGetOutletv2(Outlet outlet) {
        OutletInfo outletInfo = new OutletInfo(
            5252757,
            123L,
            OutletType.MIXED,
            outlet.getName(),
            false,
            outlet.getDeliveryServiceOutletId()
        );
        outletInfo.setGeoInfo(null);
        OutletInfoDTO outletInfoDTO = new OutletInfoDTO(outletInfo);

        PagedOutletsDTO pagedOutletsDTO = new PagedOutletsDTO(
            new ru.yandex.market.api.pager.Pager(), List.of(outletInfoDTO));

        doReturn(pagedOutletsDTO).when(mbiApiClient).getOutletsV2(
            anyLong(),
            anyLong(),
            anyString(),
            any(OutletType.class),
            anyBoolean(),
            any(OutletStatus.class),
            anyString(),
            anyInt(),
            anyInt()
        );

    }
}
