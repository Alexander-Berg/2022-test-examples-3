package ru.yandex.market.partner.mvc.controller.prepay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.api.cpa.checkout.AsyncCheckouterService;
import ru.yandex.market.api.cpa.yam.converter.PartnerApplicationConverter;
import ru.yandex.market.api.cpa.yam.converter.PrepayRequestDocumentToDTOConverter;
import ru.yandex.market.api.cpa.yam.converter.PrepayRequestToDTOConverter;
import ru.yandex.market.api.cpa.yam.dto.PrepayRequestDTO;
import ru.yandex.market.api.cpa.yam.dto.PrepaymentShopInfoDTO;
import ru.yandex.market.api.cpa.yam.entity.AvailabilityStatus;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestService;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestValidatorService;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.program.ProgramSwitchOver;
import ru.yandex.market.core.program.RequestProgramInfoDao;
import ru.yandex.market.core.protocol.MockProtocolService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.partner.mvc.validation.PrepayRequestDocumentUploadValidator;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler.PartnerHttpServRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link PrepayRequestController}.
 *
 * @author avetokhin 11/05/17.
 */
public class PrepayRequestControllerTest {

    private static final long REQ_ID_1 = 1;
    private static final long REQ_ID_2 = 2;

    private static final long DATASOURCE_ID_1 = 10;
    private static final long DATASOURCE_ID_2 = 11;

    @Mock
    private PrepayRequestService prepayRequestService;

    @Mock
    private PrepayRequestValidatorService validatorService;

    @Mock
    private RequestProgramInfoDao requestProgramInfoDao;

    @Mock
    private PartnerHttpServRequest request;

    @Mock
    private PrepayRequestDocumentUploadValidator documentUploadValidator;

    @Mock
    private PrepayRequestDocumentToDTOConverter documentConverter;

    @Mock
    private ProgramSwitchOver programSwitchOver;

    @Mock
    private AsyncCheckouterService asyncCheckouterService;


    private PrepayRequestController controller;
    private final ProtocolService protocolService = new MockProtocolService();

    private static PrepayRequest request(final long id, final long datasourceId,
                                         final PartnerApplicationStatus status) {
        return request(id, datasourceId, status, PrepayType.YANDEX_MARKET);
    }

    private static PrepayRequest request(final long id, final long datasourceId, final PartnerApplicationStatus status,
                                         final PrepayType prepayType) {
        return new PrepayRequest(id, prepayType, status, datasourceId);
    }

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(request.getDatasourceId()).thenReturn(DATASOURCE_ID_1);
        PrepayRequestToDTOConverter requestConverter = new PrepayRequestToDTOConverter(
                validatorService, new PrepayRequestDocumentToDTOConverter(), new PartnerApplicationConverter()
        );
        controller = new PrepayRequestController(
                protocolService,
                prepayRequestService,
                requestConverter,
                documentUploadValidator,
                documentConverter,
                requestProgramInfoDao,
                programSwitchOver,
                asyncCheckouterService
        );
    }

    @Test
    public void getPrepayInfoByShopNotAvailable() {
        initStatusMapResponse(AvailabilityStatus.NOT_AVAILABLE);

        final PrepaymentShopInfoDTO response = controller.getPrepayInfoByShop(request);

        assertThat(response, notNullValue());
        assertThat(response.getAvailabilityStatus(), equalTo(AvailabilityStatus.NOT_AVAILABLE));
        assertThat(response.getPrepayRequests(), emptyCollectionOf(PrepayRequestDTO.class));
    }

    @Test
    public void getPrepayInfoByShopAppliedGrouped() {
        initStatusMapResponse(AvailabilityStatus.APPLIED);
        final Map<Long, List<PrepayRequest>> requestsByShop = new HashMap<>();
        requestsByShop.put(REQ_ID_1, asList(
                request(REQ_ID_1, DATASOURCE_ID_1, PartnerApplicationStatus.INIT),
                request(REQ_ID_1, DATASOURCE_ID_2, PartnerApplicationStatus.INIT)
        ));
        requestsByShop.put(REQ_ID_2, singletonList(
                request(REQ_ID_2, DATASOURCE_ID_1, PartnerApplicationStatus.FROZEN)
        ));

        when(prepayRequestService.findActiveRequestsByShop(DATASOURCE_ID_1)).thenReturn(requestsByShop);

        final PrepaymentShopInfoDTO response = controller.getPrepayInfoByShop(request);

        checkStatus(response, AvailabilityStatus.APPLIED);
        final List<PrepayRequestDTO> prepayRequests = response.getPrepayRequests();
        assertThat(prepayRequests, notNullValue());
        assertThat(prepayRequests, hasSize(2));

        checkRequestDTO(prepayRequests.get(0), PartnerApplicationStatus.INIT, asList(DATASOURCE_ID_1, DATASOURCE_ID_2));
        checkRequestDTO(prepayRequests.get(1), PartnerApplicationStatus.FROZEN, singletonList(DATASOURCE_ID_1));
    }

    @Test
    public void getPrepayInfoByShopAppliedNotGrouped() {
        initStatusMapResponse(AvailabilityStatus.APPLIED);
        final Map<Long, List<PrepayRequest>> requestsByShop = new HashMap<>();
        requestsByShop.put(REQ_ID_1, asList(
                request(REQ_ID_1, DATASOURCE_ID_1, PartnerApplicationStatus.COMPLETED),
                request(REQ_ID_1, DATASOURCE_ID_2, PartnerApplicationStatus.FROZEN)
        ));
        when(prepayRequestService.findActiveRequestsByShop(DATASOURCE_ID_1)).thenReturn(requestsByShop);

        final PrepaymentShopInfoDTO response = controller.getPrepayInfoByShop(request);

        checkStatus(response, AvailabilityStatus.APPLIED);
        final List<PrepayRequestDTO> prepayRequests = response.getPrepayRequests();
        assertThat(prepayRequests, notNullValue());
        assertThat(prepayRequests, hasSize(1));
        checkRequestDTO(prepayRequests.get(0), PartnerApplicationStatus.COMPLETED, singletonList(DATASOURCE_ID_1));
    }

    @Test
    public void getPrepayInfoByShopAppliedYMInconsistent() {
        initStatusMapResponse(AvailabilityStatus.APPLIED);
        // Старые ЯДовские заявки в неконсистентном состоянии.
        final Map<Long, List<PrepayRequest>> requestsByShop = new HashMap<>();
        requestsByShop.put(REQ_ID_1, asList(
                request(REQ_ID_1, DATASOURCE_ID_1, PartnerApplicationStatus.INIT, PrepayType.YANDEX_MONEY),
                request(REQ_ID_1, DATASOURCE_ID_2, PartnerApplicationStatus.COMPLETED, PrepayType.YANDEX_MONEY)
        ));

        when(prepayRequestService.findActiveRequestsByShop(DATASOURCE_ID_1)).thenReturn(requestsByShop);

        final PrepaymentShopInfoDTO response = controller.getPrepayInfoByShop(request);

        checkStatus(response, AvailabilityStatus.APPLIED);
        final List<PrepayRequestDTO> prepayRequests = response.getPrepayRequests();
        assertThat(prepayRequests, notNullValue());
        assertThat(prepayRequests, hasSize(1));

        checkRequestDTO(prepayRequests.get(0), PartnerApplicationStatus.INIT, singletonList(DATASOURCE_ID_1));
    }

    @Test
    public void getPrepayInfoByShopAvailable() {
        initStatusMapResponse(AvailabilityStatus.AVAILABLE);
        final Map<Long, List<PrepayRequest>> requestsByShop = new HashMap<>();
        when(prepayRequestService.findActiveRequestsByShop(DATASOURCE_ID_1)).thenReturn(requestsByShop);

        final PrepaymentShopInfoDTO response = controller.getPrepayInfoByShop(request);

        checkStatus(response, AvailabilityStatus.AVAILABLE);
        assertThat(response.getPrepayRequests(), emptyCollectionOf(PrepayRequestDTO.class));
    }

    @Test
    public void getRequestJasper() {
        when(programSwitchOver.isProgramOldMechanicsEnabled()).thenReturn(true);
        String requestJasper = controller.getRequestJasper();
        assertThat(requestJasper, equalTo("prepay-request.jasper"));

        when(programSwitchOver.isProgramOldMechanicsEnabled()).thenReturn(false);
        requestJasper = controller.getRequestJasper();
        assertThat(requestJasper, equalTo("programs-request.jasper"));
    }

    private void checkRequestDTO(final PrepayRequestDTO requestDTO, final PartnerApplicationStatus status,
                                 final List<Long> datasourceIds) {
        assertThat(requestDTO.getStatus(), equalTo(status));
        assertThat(requestDTO.getDatasourceIds(), equalTo(datasourceIds));
    }

    private void checkStatus(final PrepaymentShopInfoDTO response, final AvailabilityStatus status) {
        assertThat(response, notNullValue());
        assertThat(response.getAvailabilityStatus(), equalTo(status));
    }

    private void initStatusMapResponse(final AvailabilityStatus status) {
        final Map<Long, AvailabilityStatus> availabilityStatusMap = new HashMap<>();
        availabilityStatusMap.put(DATASOURCE_ID_1, status);
        when(prepayRequestService.getAvailabilityStatuses(any())).thenReturn(availabilityStatusMap);
    }
}
