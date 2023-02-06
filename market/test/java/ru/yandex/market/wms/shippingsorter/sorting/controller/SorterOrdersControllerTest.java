package ru.yandex.market.wms.shippingsorter.sorting.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import ru.yandex.market.wms.common.model.dto.TransportUnitId;
import ru.yandex.market.wms.common.model.dto.TransportUnitLocation;
import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.model.enums.TransportUnitStatus;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.common.spring.servicebus.model.request.CreateSorterOrderRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.response.VendorApiResponse;
import ru.yandex.market.wms.common.spring.servicebus.vendor.VendorProvider;
import ru.yandex.market.wms.core.base.dto.DimensionDto;
import ru.yandex.market.wms.core.base.dto.LocationType;
import ru.yandex.market.wms.core.base.dto.ServiceType;
import ru.yandex.market.wms.core.base.request.BoxInfoRequest;
import ru.yandex.market.wms.core.base.request.MoveBalanceRequest;
import ru.yandex.market.wms.core.base.request.TransportUnitTrackingRequest;
import ru.yandex.market.wms.core.base.response.BoxDimensionsResponse;
import ru.yandex.market.wms.core.base.response.BoxInfoResponse;
import ru.yandex.market.wms.core.base.response.BoxStatusResponse;
import ru.yandex.market.wms.core.base.response.Carrier;
import ru.yandex.market.wms.core.base.response.MoveBalanceResponse;
import ru.yandex.market.wms.core.base.response.OperationDay;
import ru.yandex.market.wms.core.base.response.TransportUnitTrackingResponse;
import ru.yandex.market.wms.core.client.CoreClient;
import ru.yandex.market.wms.shared.libs.configproperties.dao.GlobalConfigurationDao;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxId;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxInfo;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxStatus;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.PackStationId;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.request.SorterOrderRequest;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.async.consumers.MoveBalanceConsumer;
import ru.yandex.market.wms.shippingsorter.sorting.dto.ConfigDto;
import ru.yandex.market.wms.shippingsorter.sorting.utils.JsonAssertUtils;

import static com.github.springtestdbunit.annotation.DatabaseOperation.CLEAN_INSERT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.math.BigDecimal.TEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;
import static ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants.MOVE_CONTAINER;

@Import(ShippingSorterSecurityTestConfiguration.class)
@RequiredArgsConstructor
public class SorterOrdersControllerTest extends IntegrationTest {

    private static final BigDecimal TEN_THOUSANDS = BigDecimal.valueOf(10000);

    @MockBean
    @Autowired
    private ServicebusClient servicebusClient;

    @MockBean
    @Autowired
    @Qualifier("configPropertyPostgreSqlDao")
    private GlobalConfigurationDao configPropertyPostgreSqlDao;

    @MockBean
    @Autowired
    private CoreClient coreClient;

    @SpyBean
    @Autowired
    private JmsTemplate defaultJmsTemplate;

    @Autowired
    MoveBalanceConsumer moveBalanceConsumer;

    @BeforeEach
    public void reset() {
        Mockito.reset(servicebusClient);
        Mockito.reset(coreClient);
        Mockito.reset(defaultJmsTemplate);
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/successful-creation/after.xml",
            assertionMode = NON_STRICT
    )
    void shouldSucceedCreateSorterOrder() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        CreateSorterOrderRequest request = mockCreateSorterOrder("PACK-TAB03");

        assertApiCallOk(
                "sorting/controller/sorter-order-management/successful-creation/request.json",
                "sorting/controller/sorter-order-management/successful-creation/response.json",
                post("/sorting/sorter-orders")
        );

        Mockito.verify(servicebusClient).createSorterOrder(request);
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/successful-recreation/after.xml",
            assertionMode = NON_STRICT
    )
    void shouldSucceedRecreateSorterOrder() throws Exception {
        mockGetBoxInfo();
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        CreateSorterOrderRequest request = mockCreateSorterOrder("ESORTEXIT");

        var trackingRequest = new TransportUnitTrackingRequest(
                "P123456789",
                List.of(TransportUnitStatus.ERROR_NOORDER),
                null
        );
        when(coreClient.getTracking(trackingRequest)).thenReturn(new TransportUnitTrackingResponse(List.of()));
        when(coreClient.getBoxDimensions("P123456789")).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(BigDecimal.valueOf(1001), TEN, TEN, TEN, Instant.now().minusSeconds(10))
        )));
        mockBalanceMovement();

        assertApiCallOk(
                "sorting/controller/sorter-order-management/successful-recreation/request.json",
                "sorting/controller/sorter-order-management/successful-recreation/response.json",
                post("/sorting/sorter-orders/recreate")
        );

        verify(servicebusClient).createSorterOrder(request);
        verify(defaultJmsTemplate).convertAndSend(eq(MOVE_CONTAINER), any(MoveBalanceRequest.class), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase(value =
            "/sorting/controller/sorter-order-management/successful-recreation-no-measured-dimensions/after.xml",
            assertionMode = NON_STRICT
    )
    void shouldSucceedRecreateSorterOrderWithoutMeasuredDimensions() throws Exception {
        mockGetBoxInfo();
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        CreateSorterOrderRequest request = mockCreateSorterOrder("ESORTEXIT");

        var trackingRequest = new TransportUnitTrackingRequest(
                "P123456789",
                List.of(TransportUnitStatus.ERROR_NOORDER),
                null
        );
        when(coreClient.getTracking(trackingRequest)).thenReturn(new TransportUnitTrackingResponse(List.of()));
        when(coreClient.getBoxDimensions("P123456789")).thenReturn(new BoxDimensionsResponse(List.of()));
        mockBalanceMovement();

        assertApiCallOk(
                "sorting/controller/sorter-order-management/successful-recreation-no-measured-dimensions/request.json",
                "sorting/controller/sorter-order-management/successful-recreation-no-measured-dimensions/response.json",
                post("/sorting/sorter-orders/recreate")
        );

        verify(servicebusClient).createSorterOrder(request);
        verify(defaultJmsTemplate).convertAndSend(eq(MOVE_CONTAINER), any(MoveBalanceRequest.class), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/without-box-info-from-packing/before.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/without-box-info-from-packing/after.xml",
            assertionMode = NON_STRICT
    )
    void shouldSucceedCreateSorterOrderWithoutBoxInfo_fromCore() throws Exception {
        mockGetBoxInfo();
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        final CreateSorterOrderRequest request = CreateSorterOrderRequest.builder()
                .transportUnitId(TransportUnitId.of("P123456789"))
                .sourceLocation(TransportUnitLocation.builder().id("PACK-TAB03").build())
                .targetLocation(TransportUnitLocation.builder().id("SR1_ch-01").build())
                .alternateTarget(TransportUnitLocation.builder().id("SR1_ch-01").build())
                .errorTarget(TransportUnitLocation.builder().id("SR1_ch-03").build())
                .weightMin(951)
                .weightMax(1051)
                .force(true)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        Mockito.when(servicebusClient.createSorterOrder(request))
                .thenReturn(VendorApiResponse.builder().code("200").build());

        assertApiCallOk(
                "sorting/controller/sorter-order-management/without-box-info-from-packing/request.json",
                "sorting/controller/sorter-order-management/successful-creation/response.json",
                post("/sorting/sorter-orders")
        );

        Mockito.verify(servicebusClient).createSorterOrder(request);
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/box-info-from-table/before.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/box-info-from-table/after.xml",
            assertionMode = NON_STRICT
    )
    void shouldSucceedCreateSorterOrderWithoutBoxInfo_fromBoxInfoTable() throws Exception {
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        final CreateSorterOrderRequest request = CreateSorterOrderRequest.builder()
                .transportUnitId(TransportUnitId.of("P123456789"))
                .sourceLocation(TransportUnitLocation.builder().id("PACK-TAB03").build())
                .targetLocation(TransportUnitLocation.builder().id("SR1_ch-01").build())
                .alternateTarget(TransportUnitLocation.builder().id("SR1_ch-01").build())
                .errorTarget(TransportUnitLocation.builder().id("SR1_ch-03").build())
                .weightMin(951)
                .weightMax(1051)
                .force(true)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        Mockito.when(servicebusClient.createSorterOrder(request))
                .thenReturn(VendorApiResponse.builder().code("200").build());

        assertApiCallOk(
                "sorting/controller/sorter-order-management/box-info-from-table/request.json",
                "sorting/controller/sorter-order-management/successful-creation/response.json",
                post("/sorting/sorter-orders")
        );

        Mockito.verify(servicebusClient).createSorterOrder(request);
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @DatabaseSetup("/sorting/controller/sorter-order-management/successful-creation-with-warn/before.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/successful-creation-with-warn/after.xml",
            assertionMode = NON_STRICT
    )
    void shouldSucceedToCreateSorterOrderWhenFailedAttemptsExceedThreshold() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        final CreateSorterOrderRequest request = CreateSorterOrderRequest.builder()
                .transportUnitId(TransportUnitId.of("P123456789"))
                .sourceLocation(TransportUnitLocation.builder().id("PACK-TAB03").build())
                .targetLocation(TransportUnitLocation.builder().id("SR1_ch-01").build())
                .alternateTarget(TransportUnitLocation.builder().id("SR1_ch-01").build())
                .errorTarget(TransportUnitLocation.builder().id("SR1_ch-03").build())
                .weightMin(7601)
                .weightMax(8401)
                .force(true)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        Mockito.when(servicebusClient.createSorterOrder(request))
                .thenReturn(VendorApiResponse.builder().code("200").build());

        assertApiCallOk(
                "sorting/controller/sorter-order-management/successful-creation-with-warn/request.json",
                "sorting/controller/sorter-order-management/successful-creation-with-warn/response.json",
                post("/sorting/sorter-orders")
        );

        Mockito.verify(servicebusClient).createSorterOrder(request);
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @DatabaseSetup("/sorting/controller/sorter-order-management/successful-recreation-with-warn/before.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/successful-recreation-with-warn/after.xml",
            assertionMode = NON_STRICT
    )
    void shouldSucceedToRecreateSorterOrderWhenFailedAttemptsExceedThreshold() throws Exception {
        mockGetBoxInfo();
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        final CreateSorterOrderRequest request = CreateSorterOrderRequest.builder()
                .transportUnitId(TransportUnitId.of("P123456789"))
                .sourceLocation(TransportUnitLocation.builder().id("ESORTEXIT").build())
                .targetLocation(TransportUnitLocation.builder().id("SR1_ch-01").build())
                .alternateTarget(TransportUnitLocation.builder().id("SR1_ch-01").build())
                .errorTarget(TransportUnitLocation.builder().id("SR1_ch-03").build())
                .weightMin(951)
                .weightMax(1051)
                .force(true)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        Mockito.when(servicebusClient.createSorterOrder(request))
                .thenReturn(VendorApiResponse.builder().code("200").build());

        var trackingRequest = new TransportUnitTrackingRequest(
                "P123456789",
                List.of(TransportUnitStatus.ERROR_NOORDER),
                Instant.parse("2020-04-01T12:28:56.789Z")
        );
        when(coreClient.getTracking(trackingRequest)).thenReturn(new TransportUnitTrackingResponse(List.of()));
        when(coreClient.getBoxDimensions("P123456789")).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(BigDecimal.valueOf(1001), TEN, TEN, TEN, Instant.now().minusSeconds(10))
        )));
        mockBalanceMovement();

        assertApiCallOk(
                "sorting/controller/sorter-order-management/successful-recreation-with-warn/request.json",
                "sorting/controller/sorter-order-management/successful-recreation-with-warn/response.json",
                post("/sorting/sorter-orders/recreate")
        );

        verify(servicebusClient).createSorterOrder(request);
        verify(defaultJmsTemplate).convertAndSend(eq(MOVE_CONTAINER), any(MoveBalanceRequest.class), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @DatabaseSetup("/sorting/controller/sorter-order-management/existing-order/before.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/existing-order/after.xml",
            assertionMode = NON_STRICT
    )
    public void shouldForceCreateOrderIfOrderAlreadyCreated() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );
        mockOkApiResponse();
        assertApiCallOk(
                "sorting/controller/sorter-order-management/existing-order/request.json",
                "sorting/controller/sorter-order-management/existing-order/response.json",
                post("/sorting/sorter-orders")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @DatabaseSetup("/sorting/controller/sorter-order-management/recreation-existing-order/before.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/recreation-existing-order/after.xml",
            assertionMode = NON_STRICT
    )
    public void shouldForceRecreateOrderIfOrderAlreadyCreated() throws Exception {
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );
        mockOkApiResponse();

        var trackingRequest = new TransportUnitTrackingRequest(
                "P123456789",
                List.of(TransportUnitStatus.ERROR_NOORDER),
                Instant.parse("2020-04-01T12:28:56.789Z")
        );
        when(coreClient.getTracking(trackingRequest)).thenReturn(new TransportUnitTrackingResponse(List.of()));
        when(coreClient.getBoxDimensions("P123456789")).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(BigDecimal.valueOf(1001), TEN, TEN, TEN, Instant.now().minusSeconds(10))
        )));
        mockBalanceMovement();

        assertApiCallOk(
                "sorting/controller/sorter-order-management/recreation-existing-order/request.json",
                "sorting/controller/sorter-order-management/recreation-existing-order/response.json",
                post("/sorting/sorter-orders/recreate")
        );

        verify(defaultJmsTemplate).convertAndSend(eq(MOVE_CONTAINER), any(MoveBalanceRequest.class), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @DatabaseSetup("/sorting/controller/sorter-order-management/successful-creation-in-completed-status/before.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/successful-creation-in-completed-status/after.xml",
            assertionMode = NON_STRICT
    )
    public void shouldSuccessCreateOrderIfOrderAlreadyExistsInCompletedStatus() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );
        mockOkApiResponse();
        String req = "sorting/controller/sorter-order-management/successful-creation-in-completed-status/request.json";
        String res = "sorting/controller/sorter-order-management/successful-creation-in-completed-status/response.json";

        assertApiCallOk(
                req,
                res,
                post("/sorting/sorter-orders")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @DatabaseSetup("/sorting/controller/sorter-order-management/successful-recreation-in-completed-status/before.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/successful-recreation-in-completed-status/after.xml",
            assertionMode = NON_STRICT
    )
    public void shouldSuccessRecreateOrderIfOrderAlreadyExistsInCompletedStatus() throws Exception {
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );
        mockOkApiResponse();
        var req = "sorting/controller/sorter-order-management/successful-recreation-in-completed-status/request.json";
        var res = "sorting/controller/sorter-order-management/successful-recreation-in-completed-status/response.json";

        var trackingRequest = new TransportUnitTrackingRequest(
                "P123456789",
                List.of(TransportUnitStatus.ERROR_NOORDER),
                Instant.parse("2020-04-01T12:32:56.789Z")
        );
        when(coreClient.getTracking(trackingRequest)).thenReturn(new TransportUnitTrackingResponse(List.of()));
        when(coreClient.getBoxDimensions("P123456789")).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(BigDecimal.valueOf(1001), TEN, TEN, TEN, Instant.now().minusSeconds(10))
        )));
        mockNoBalanceMovement();

        assertApiCallOk(
                req,
                res,
                post("/sorting/sorter-orders/recreate")
        );

        verify(defaultJmsTemplate).convertAndSend(eq(MOVE_CONTAINER), any(MoveBalanceRequest.class), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/test-internal-error/after.xml",
            assertionMode = NON_STRICT
    )
    public void testInternalError() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );
        Mockito.when(servicebusClient.createSorterOrder(any(CreateSorterOrderRequest.class))).thenThrow(
                new WebClientResponseException(500, "Internal error occurred.", HttpHeaders.EMPTY, null, null)
        );

        assertApiCallFail(
                "sorting/controller/sorter-order-management/internal-error/request.json",
                "sorting/controller/sorter-order-management/internal-error/response.json",
                post("/sorting/sorter-orders")
        );
    }

    @Test
    @DatabaseSetup(value = "/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/recreation-with-internal-error/after.xml",
            assertionMode = NON_STRICT
    )
    public void testRecreationInternalError() throws Exception {
        mockGetBoxInfo();
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );
        Mockito.when(servicebusClient.createSorterOrder(any(CreateSorterOrderRequest.class))).thenThrow(
                new WebClientResponseException(500, "Internal error occurred.", HttpHeaders.EMPTY, null, null)
        );
        var trackingRequest = new TransportUnitTrackingRequest(
                "P123456789",
                List.of(TransportUnitStatus.ERROR_NOORDER),
                null
        );
        when(coreClient.getTracking(trackingRequest)).thenReturn(new TransportUnitTrackingResponse(List.of()));
        when(coreClient.getBoxDimensions("P123456789")).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(BigDecimal.valueOf(1001), TEN, TEN, TEN, Instant.now().minusSeconds(10))
        )));

        assertApiCallOk(
                "sorting/controller/sorter-order-management/recreation-with-internal-error/request.json",
                "sorting/controller/sorter-order-management/recreation-with-internal-error/response.json",
                post("/sorting/sorter-orders/recreate")
        );

        verify(defaultJmsTemplate, never()).convertAndSend(eq(MOVE_CONTAINER), any(MoveBalanceRequest.class), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/test-internal-error/after.xml",
            assertionMode = NON_STRICT
    )
    public void testNon200CodeError() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );
        Mockito.when(servicebusClient.createSorterOrder(any(CreateSorterOrderRequest.class)))
                .thenReturn(VendorApiResponse.builder()
                        .code("401")
                        .message("Unauthorized")
                        .build());

        assertApiCallFail(
                "sorting/controller/sorter-order-management/vendor-integration-faults/request.json",
                "sorting/controller/sorter-order-management/vendor-integration-faults/" +
                        "non-200-status-code-response.json",
                post("/sorting/sorter-orders")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/test-internal-error/after.xml",
            assertionMode = NON_STRICT
    )
    public void testNullVendorApiResponse() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );
        Mockito.when(servicebusClient.createSorterOrder(any(CreateSorterOrderRequest.class)))
                .thenReturn(null);

        assertApiCallFail(
                "sorting/controller/sorter-order-management/vendor-integration-faults/request.json",
                "sorting/controller/sorter-order-management/vendor-integration-faults/null-response.json",
                post("/sorting/sorter-orders")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/test-internal-error/after.xml",
            assertionMode = NON_STRICT
    )
    public void testEmptyStatusCodeInApiResponse() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );
        Mockito.when(servicebusClient.createSorterOrder(any(CreateSorterOrderRequest.class)))
                .thenReturn(VendorApiResponse.builder()
                        .message("")
                        .build());

        assertApiCallFail(
                "sorting/controller/sorter-order-management/vendor-integration-faults/request.json",
                "sorting/controller/sorter-order-management/vendor-integration-faults/empty-status-code-response.json",
                post("/sorting/sorter-orders")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-order-management/weight-exceeded-error/after.xml")
    public void testTotalWeightExceededError() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("7.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        assertApiCallOk(
                "sorting/controller/sorter-order-management/weight-exceeded-error/request.json",
                "sorting/controller/sorter-order-management/weight-exceeded-error/response.json",
                post("/sorting/sorter-orders/")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/recreation-weight-exceeded-error/before.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-order-management/recreation-weight-exceeded-error/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testRecreationTotalWeightExceededError() throws Exception {
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("7000")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        var request = new TransportUnitTrackingRequest(
                "P123456789",
                List.of(TransportUnitStatus.ERROR_NOORDER),
                null
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of()));
        when(coreClient.getBoxDimensions("P123456789")).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.now().minusSeconds(10))
        )));

        assertApiCallOk(
                "sorting/controller/sorter-order-management/recreation-weight-exceeded-error/request.json",
                "sorting/controller/sorter-order-management/recreation-weight-exceeded-error/response.json",
                post("/sorting/sorter-orders/recreate")
        );

        verify(defaultJmsTemplate, never()).convertAndSend(eq(MOVE_CONTAINER), any(MoveBalanceRequest.class), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-order-management/length-exceeded-error/after.xml")
    public void testMaxLengthExceededError() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("3")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        assertApiCallOk(
                "sorting/controller/sorter-order-management/length-exceeded-error/request.json",
                "sorting/controller/sorter-order-management/length-exceeded-error/response.json",
                post("/sorting/sorter-orders")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/recreation-length-exceeded-error/before.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-order-management/recreation-length-exceeded-error/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testRecreationMaxLengthExceededError() throws Exception {
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("3")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        var request = new TransportUnitTrackingRequest(
                "P123456789",
                List.of(TransportUnitStatus.ERROR_NOORDER),
                null
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of()));
        when(coreClient.getBoxDimensions("P123456789")).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.now().minusSeconds(10))
        )));

        assertApiCallOk(
                "sorting/controller/sorter-order-management/recreation-length-exceeded-error/request.json",
                "sorting/controller/sorter-order-management/recreation-length-exceeded-error/response.json",
                post("/sorting/sorter-orders/recreate")
        );

        verify(defaultJmsTemplate, never()).convertAndSend(eq(MOVE_CONTAINER), any(MoveBalanceRequest.class), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/recreation-from-pack-station-error/before.xml")
    @ExpectedDatabase("/sorting/controller/sorter-order-management/recreation-from-pack-station-error/after.xml")
    public void testRecreationErrorWhenSourceLocationIsPackStation() throws Exception {
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        assertApiCallBadRequest(
                "sorting/controller/sorter-order-management/recreation-from-pack-station-error/request.json",
                "sorting/controller/sorter-order-management/recreation-from-pack-station-error/response.json",
                post("/sorting/sorter-orders/recreate")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/recreation-not-from-nok-sorter-exit-error/before.xml")
    @ExpectedDatabase("/sorting/controller/sorter-order-management/recreation-not-from-nok-sorter-exit-error/after.xml")
    public void testRecreationErrorWhenSourceLocationIsNotNokSorterExit() throws Exception {
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        assertApiCallBadRequest(
                "sorting/controller/sorter-order-management/recreation-not-from-nok-sorter-exit-error/request.json",
                "sorting/controller/sorter-order-management/recreation-not-from-nok-sorter-exit-error/response.json",
                post("/sorting/sorter-orders/recreate")
        );
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/recreation-last-order-from-nok-to-nok/before.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/recreation-last-order-from-nok-to-nok/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testRecreationWhenLastSorterOrderWasFromNokToNok() throws Exception {
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );
        mockOkApiResponse();

        when(coreClient.getBoxDimensions("P123456789")).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.now().minusSeconds(10))
        )));
        mockNoBalanceMovement();

        assertApiCallOk(
                "sorting/controller/sorter-order-management/recreation-last-order-from-nok-to-nok/request.json",
                "sorting/controller/sorter-order-management/recreation-last-order-from-nok-to-nok/response.json",
                post("/sorting/sorter-orders/recreate")
        );

        verify(defaultJmsTemplate).convertAndSend(eq(MOVE_CONTAINER), any(MoveBalanceRequest.class), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/recreation-order-created-to-nok/before.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-order-management/recreation-order-created-to-nok/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testRecreationWhenSorterOrderCreatedToNok() throws Exception {
        mockGetBoxStatus(false);
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );
        mockOkApiResponse();

        when(coreClient.getBoxDimensions("P123456789")).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.now().minusSeconds(10))
        )));
        mockNoBalanceMovement();

        assertApiCallOk(
                "sorting/controller/sorter-order-management/recreation-order-created-to-nok/request.json",
                "sorting/controller/sorter-order-management/recreation-order-created-to-nok/response.json",
                post("/sorting/sorter-orders/recreate")
        );

        verify(defaultJmsTemplate).convertAndSend(eq(MOVE_CONTAINER), any(MoveBalanceRequest.class), any());
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-orders/common.xml")
    void testParameterLessRequest() throws Exception {
        ResultActions result = mockMvc.perform(get("/sorting/sorter-orders")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sorting/controller/sorter-orders/parameterless-request/response.json")));
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/common.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-orders/common.xml")
    void testSequentialPageLoad() throws Exception {
        String cursor = "";
        for (int i = 0; i < 3; i++) {
            ResultActions result = mockMvc.perform(get("/sorting/sorter-orders")
                    .param("cursor", cursor)
                    .param("sort", "status")
                    .param("order", "desc")
                    .param("limit", "4")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(content().json(getFileContent(String.format(
                            "sorting/controller/sorter-orders/sequential-page-load/response-%s.json", i))));

            JSONObject obj = new JSONObject(result.andReturn().getResponse().getContentAsString());
            assertions.assertThat(obj).isNotNull();

            cursor = obj.getJSONObject("cursor").getString("value");
        }
        assertions.assertThat(cursor).isEmpty();
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-orders/common.xml")
    void testSequentialPageLoadWithFilterByFromLoc() throws Exception {
        String cursor = "";
        for (int i = 0; i < 2; i++) {
            ResultActions result = mockMvc.perform(get("/sorting/sorter-orders")
                    .param("cursor", cursor)
                    .param("sort", "status")
                    .param("order", "desc")
                    .param("filter", "fromLoc==FF-01")
                    .param("limit", "2")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(content().json(getFileContent(String.format(
                            "sorting/controller/sorter-orders/sequential-page-load-filter-by-from-loc/response-%s" +
                                    ".json", i)), true));

            JSONObject obj = new JSONObject(result.andReturn().getResponse().getContentAsString());
            assertions.assertThat(obj).isNotNull();

            cursor = obj.getJSONObject("cursor").getString("value");
        }
        assertions.assertThat(cursor).isEmpty();
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-orders/common.xml")
    void testFilterByAssigneeAndCurrentLoc() throws Exception {
        ResultActions result = mockMvc.perform(get("/sorting/sorter-orders")
                .param("filter", "assignee==\"USER 1\";currentLoc==TMP-03")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent(
                        "sorting/controller/sorter-orders/filter-by-assignee-and-current-loc/response.json"),
                        true));
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/sort-by-edit-date-cursor/before.xml")
    @ExpectedDatabase("/sorting/controller/sorter-orders/sort-by-edit-date-cursor/before.xml")
    void testSortByEditDateWithCursor() throws Exception {
        ResultActions result = mockMvc.perform(get("/sorting/sorter-orders")
                .param("order", "DESC")
                .param("sort", "editDate")
                .param("limit", "35")
                .param("cursor", "PMRHG33SORSXET3SMRSXES3FPERDUIRTGYRCYITDOVZHG33SIZUWK3DEEI5HWITOMFWW" +
                        "KIR2EJSWI2LUIRQXIZJCFQRHMYLMOVSSEORCGIYDEMJNGEYC2MBUKQZDEORVGE5DIOJOHA4TQNRWGIRH27I=")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sorting/controller/sorter-orders/sort-by-edit-date-cursor/response.json"), true));
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/sort-by-edit-date-cursor/before.xml")
    @ExpectedDatabase("/sorting/controller/sorter-orders/sort-by-edit-date-cursor/before.xml")
    void testSortByBoxId() throws Exception {
        ResultActions result = mockMvc.perform(get("/sorting/sorter-orders")
                .param("order", "DESC")
                .param("sort", "boxId")
                .param("limit", "35")
                .contentType(MediaType.APPLICATION_JSON));
        System.out.println(result.andReturn().getResponse().getContentAsString());
        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sorting/controller/sorter-orders/sort-by-boxid/response.json"), true));
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-orders/common.xml")
    void testFilterByEditDate() throws Exception {
        ResultActions result = mockMvc.perform(get("/sorting/sorter-orders")
                .param("filter", "editDate=ge='2021-10-01 11:30';editDate=le='2021-10-03 14:00'")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sorting/controller/sorter-orders/filter-by-editdate/response.json"), true));
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-orders/common.xml")
    void testFilterByToLocAndCurrentLoc() throws Exception {
        ResultActions result = mockMvc.perform(get("/sorting/sorter-orders")
                .param("filter", "toLoc==EX-01;currentLoc==TMP-03")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sorting/controller/sorter-orders/filter-by-to-loc-and-current-loc/response.json"), true));
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-orders/common.xml")
    void testFilterByToBoxId() throws Exception {
        ResultActions result = mockMvc.perform(get("/sorting/sorter-orders")
                .param("sort", "editDate")
                .param("order", "desc")
                .param("filter", "boxId==U-02")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sorting/controller/sorter-orders/filter-by-box-id/response.json")));
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-orders/common.xml")
    void testSortByEditDate() throws Exception {
        ResultActions result = mockMvc.perform(get("/sorting/sorter-orders")
                .param("sort", "editDate")
                .param("order", "desc")
                .param("limit", "3")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "sorting/controller/sorter-orders/sort-by-edit-date/response.json"), true));
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/get-sorter-order-happy/db_setup.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-orders/get-sorter-order-happy/db_setup.xml")
    public void getSorterOrderHappyPath() throws Exception {
        mockMvc.perform(get("/sorting/sorter-orders/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("sorting/controller/sorter-orders/get-sorter-order-happy/response.json"),
                        true
                ))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/get-sorter-order-happy/db_setup.xml")
    @ExpectedDatabase("/sorting/controller/sorter-orders/get-sorter-order-happy/db_setup.xml")
    public void getSorterOrderWrongId() throws Exception {
        mockMvc.perform(get("/sorting/sorter-orders/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/cancel-sorter-order-happy/before.xml")
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-orders/cancel-sorter-order-happy/after.xml",
            assertionMode = NON_STRICT
    )
    public void cancelSorterOrderHappyPath() throws Exception {
        mockOkApiResponse();
        mockMvc.perform(delete("/sorting/sorter-orders/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-orders/cancel-sorter-order-happy/before.xml")
    @ExpectedDatabase("/sorting/controller/sorter-orders/cancel-sorter-order-happy/before.xml")
    public void cancelSorterOrderWrongId() throws Exception {
        mockMvc.perform(delete("/sorting/sorter-orders/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/creation-already-shipped-box/immutable-state.xml")
    @ExpectedDatabase("/sorting/controller/sorter-order-management/creation-already-shipped-box/after.xml")
    public void shouldNotCreateSorterOrderIfBoxAlreadyShipped() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("3")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );

        mockMvc.perform(post("/sorting/sorter-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/sorter-order-management/creation-already-shipped-box/request.json")
                ))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent(
                                "sorting/controller/sorter-order-management/creation-already-shipped-box/response.json"
                        ),
                        true
                ))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/recreation-already-shipped-box/before.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-order-management/recreation-already-shipped-box/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldNotRecreateSorterOrderIfBoxAlreadyShipped() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );
        mockGetBoxStatus(true);
        var request = new TransportUnitTrackingRequest(
                "P123456789",
                List.of(TransportUnitStatus.ERROR_NOORDER),
                null
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of()));
        when(coreClient.getBoxDimensions("P123456789")).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(BigDecimal.valueOf(1001), TEN, TEN, TEN, Instant.now().minusSeconds(10))
        )));

        mockMvc.perform(post("/sorting/sorter-orders/recreate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/sorter-order-management/recreation-already-shipped-box/request.json")
                ))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent(
                            "sorting/controller/sorter-order-management/recreation-already-shipped-box/response.json"
                        ),
                        true))
                .andReturn();

        verify(defaultJmsTemplate, never()).convertAndSend(eq(MOVE_CONTAINER), any(MoveBalanceRequest.class), any());
    }

    @Test
    @DatabaseSetup(value = "/sorting/controller/sorter-orders-async/before.xml", type = CLEAN_INSERT)
    @ExpectedDatabase(value = "/sorting/controller/sorter-orders-async/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void shouldCreateSorterOrderIfAsyncPostEnable() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("15")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .asyncShippingSorterOn("1")
                        .build()
        );

        assertApiCallOk(
                "sorting/controller/sorter-orders-async/request.json",
                "sorting/controller/sorter-orders-async/response.json",
                post("/sorting/sorter-orders")
        );

        // Задержка, чтобы sorterorder успел создаться в асинхронном режиме и ExpectedDatabase выполнился.
        Thread.sleep(1000);
    }

    @Test
    @DatabaseSetup(value = "/sorting/controller/sorter-orders-async/before-3-similar-box-id.xml", type = CLEAN_INSERT)
    @ExpectedDatabase(
            value = "/sorting/controller/sorter-orders-async/after-3-similar-box-id.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void shouldCreateSorterOrderIfAsyncPostEnableWhenAttemptsMoreThenPossible() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("3")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .asyncShippingSorterOn("1")
                        .build()
        );

        assertApiCallOk(
                "sorting/controller/sorter-orders-async/request.json",
                "sorting/controller/sorter-orders-async/response-3-similar-box-id.json",
                post("/sorting/sorter-orders")
        );

        // Задержка, чтобы sorterorder успел создаться в асинхронном режиме и ExpectedDatabase выполнился.
        Thread.sleep(1000);
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-order-management/common.xml")
    public void emptyRequestReturnsBadRequestTest() throws Exception {
        mockMvc.perform(post("/sorting/sorter-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/sorter-order-management/sorter-orders-bad-request/empty/request.json"
                )))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-order-management/common.xml")
    public void emptyRequestForRecreationReturnsBadRequestTest() throws Exception {
        mockMvc.perform(post("/sorting/sorter-orders/recreate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/sorter-order-management/sorter-orders-bad-request/empty/request.json"
                )))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sorting/controller/sorter-order-management/common.xml")
    @ExpectedDatabase("/sorting/controller/sorter-order-management/common.xml")
    public void requestWithoutZoneReturnsBadRequestTest() throws Exception {
        mockMvc.perform(post("/sorting/sorter-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "sorting/controller/sorter-order-management/sorter-orders-bad-request/no-zone/" +
                                "request.json")
                ))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    /*
     * Тест на проверку валидности баркода коробки.
     * Изменения формата должны быть согласованы со складом и командой автоматизации.
     * */
    @Test
    @DatabaseSetup(value = "/sorting/controller/sorter-order-management/common.xml", type = CLEAN_INSERT)
    @ExpectedDatabase(value = "/sorting/controller/sorter-order-management/request-validation-for-create/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testBarcodeValidation() throws Exception {
        mockConfig(
                ConfigDto.builder()
                        .maxCube("100")
                        .maxWeight("30000.0")
                        .maxWidth("60")
                        .maxLength("30")
                        .maxHeight("20")
                        .minWeight("50")
                        .orderWarnThreshold("5")
                        .weightMaxDeviation("1000")
                        .weightMinDeviation("1000")
                        .build()
        );
        mockOkApiResponse();

        ObjectMapper mapper = new ObjectMapper();

        String boxId = "P";
        Set<Integer> validLengths = Set.of(10, 14, 20);

        for (int k = 0; k < 2; k++) {
            for (int i = 0; i <= 9; i++) {
                boxId += i;

                SorterOrderRequest request = SorterOrderRequest.builder()
                        .boxId(BoxId.of(boxId))
                        .packStationId(PackStationId.of("UPACK_" + (k * 10 + i)))
                        .boxInfo(
                                BoxInfo.builder()
                                        .boxWeight(1001)
                                        .boxWidth(new BigDecimal("10.0"))
                                        .boxHeight(new BigDecimal("15.0"))
                                        .boxLength(new BigDecimal("3.0"))
                                        .carrierCode("123456")
                                        .carrierName("rier")
                                        .operationDayId(18262L)
                                        .boxStatus(
                                                BoxStatus.builder()
                                                        .isBoxDropped(false)
                                                        .isBoxLoaded(false)
                                                        .isBoxShipped(false)
                                                        .build()
                                        )
                                        .build()
                        )
                        .zone("SSORT_ZONE")
                        .build();

                ResultActions result = mockMvc.perform(post("/sorting/sorter-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)));

                if (validLengths.contains(boxId.length())) {
                    result.andExpect(status().isOk());
                } else {
                    result.andExpect(status().isBadRequest());
                    JsonAssertUtils.assertFileNonExtensibleEquals(
                            "sorting/controller/sorter-orders/response-when-bad-box-id.json",
                            result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
                }
            }
        }
    }

    private void assertApiCallOk(String requestFile, String responseFile, MockHttpServletRequestBuilder request)
            throws Exception {
        assertApiCall(requestFile, responseFile, request, status().isOk());
    }

    private void assertApiCallFail(String requestFile, String responseFile, MockHttpServletRequestBuilder request)
            throws Exception {
        assertApiCall(requestFile, responseFile, request, status().isInternalServerError());
    }

    private void assertApiCallBadRequest(String requestFile, String responseFile, MockHttpServletRequestBuilder request)
            throws Exception {
        assertApiCall(requestFile, responseFile, request, status().isBadRequest());
    }

    private void assertApiCall(
            String requestFile,
            String responseFile,
            MockHttpServletRequestBuilder request,
            ResultMatcher statusCodeMatcher
    ) throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFile)))
                .andExpect(statusCodeMatcher)
                .andReturn();
        if (responseFile != null) {
            JsonAssertUtils.assertFileNonExtensibleEquals(responseFile,
                    mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8), List.of("content.id"));
        }
    }

    private void mockGetBoxInfo() {
        Carrier carrier = new Carrier("123456", "rier");
        OperationDay operationDay = new OperationDay(18262);
        BoxInfoResponse boxInfoResponse = new BoxInfoResponse(
                new BigDecimal("1.001"),
                new BigDecimal("10.0"),
                new BigDecimal("15.0"),
                new BigDecimal("3.0"),
                carrier,
                operationDay
        );

        Mockito.when(coreClient.getBoxInfo(any(BoxInfoRequest.class))).thenReturn(boxInfoResponse);
    }

    private void mockGetBoxStatus(boolean isBoxShipped) {
        BoxStatusResponse boxStatusResponse = new BoxStatusResponse(
                false, false, isBoxShipped
        );

        Mockito.when(coreClient.getBoxStatus(anyString())).thenReturn(boxStatusResponse);
    }

    private void mockBalanceMovement() {
        doAnswer((Answer<Void>) invocation -> {
            MoveBalanceResponse response = new MoveBalanceResponse(
                    "flowId",
                    ServiceType.SORTER,
                    1585733696000L,
                    new MoveBalanceResponse.BalanceData(
                            "userId",
                            "P123456789",
                            new MoveBalanceResponse.Location("ESORTEXIT", LocationType.SHIPSORTEXIT)));
            moveBalanceConsumer.receiveNotification(response, null);
            return null;
        }).when(defaultJmsTemplate).convertAndSend(anyString(), any(), any());
    }

    private void mockNoBalanceMovement() {
        doAnswer((Answer<Void>) invocation -> null).when(defaultJmsTemplate).convertAndSend(anyString(), any(), any());
    }

    private void mockOkApiResponse() {
        VendorApiResponse apiResponse = VendorApiResponse.builder().code("200").message("OK").build();

        Mockito.when(servicebusClient.createSorterOrder(any())).thenReturn(apiResponse);
    }

    private CreateSorterOrderRequest mockCreateSorterOrder(String sourceLocation) {
        final CreateSorterOrderRequest request = CreateSorterOrderRequest.builder()
                .transportUnitId(TransportUnitId.of("P123456789"))
                .sourceLocation(TransportUnitLocation.builder().id(sourceLocation).build())
                .targetLocation(TransportUnitLocation.builder().id("SR1_ch-01").build())
                .alternateTarget(TransportUnitLocation.builder().id("SR1_ch-01").build())
                .errorTarget(TransportUnitLocation.builder().id("SR1_ch-03").build())
                .weightMin(951)
                .weightMax(1051)
                .force(true)
                .vendorProvider(VendorProvider.SCHAEFER)
                .build();

        Mockito.when(servicebusClient.createSorterOrder(request))
                .thenReturn(VendorApiResponse.builder().code("200").build());

        return request;
    }

    private void mockConfig(ConfigDto dto) {
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_WEIGHT_GRAMS"))
                .thenReturn(dto.getMaxWeight());
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_WIDTH"))
                .thenReturn(dto.getMaxWidth());
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_LENGTH"))
                .thenReturn(dto.getMaxLength());
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_HEIGHT"))
                .thenReturn(dto.getMaxHeight());
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MIN_WEIGHT_GRAMS"))
                .thenReturn(dto.getMinWeight());
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("WEIGHT_LIMIT_FOR_ROUND_GRAMS"))
                .thenReturn("31000");
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("CREATE_ORDER_WARN_THRESHOLD"))
                .thenReturn(dto.getOrderWarnThreshold());
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("WEIGHTMAXDEVIATION"))
                .thenReturn(dto.getWeightMaxDeviation());
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("WEIGHTMINDEVIATION"))
                .thenReturn(dto.getWeightMinDeviation());
        Mockito.when(configPropertyPostgreSqlDao.getStringConfigValue("YM_ASYNC_SHIPPING_SORTER_ON"))
                .thenReturn(dto.getAsyncShippingSorterOn());
    }

}
