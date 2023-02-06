package ru.yandex.market.delivery.mdbapp.integration.service;

import java.util.Optional;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.delivery.mdbapp.components.service.lms.LmsLogisticsPointClient;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.mapper.ReturnRequestMapper;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.ReturnRequestRepository;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.outlets.Outlet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.DS_OUTLET_CODE;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.MARKET_PVZ_SUBTYPE_IDS;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.PARTNER_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.PICKUP_POINT_LMS_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.PICKUP_POINT_MBI_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_DS_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID_STR;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_REQUEST_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.marketPvzPartnerResponse;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.nonMarketPvzPartnerResponse;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.pickupPoint;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.pvzLogisticsPointResponse;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnDelivery;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequest;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.setJpaIds;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.terminalLogisticsPointResponse;

@RunWith(MockitoJUnitRunner.class)
public class ReturnRequestServiceTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Mock
    LmsLogisticsPointClient logisticsPointClient;
    @Mock
    MbiApiClient mbiApiClient;
    @Mock
    PickupPointService pickupPointService;
    @Mock
    ReturnRequestMapper returnRequestMapper;
    @Mock
    ReturnRequestRepository returnRequestRepository;

    @InjectMocks
    ReturnRequestService service;

    @Before
    public void setUp() {
        service.setMarketPvzSubtypeIds(MARKET_PVZ_SUBTYPE_IDS);
    }

    @Test
    public void shouldSave() {
        // given:
        when(returnRequestRepository.save(any(ReturnRequest.class)))
            .then(setJpaIds());

        // when:
        final ReturnRequest actual = service.save(returnRequest(null));

        // then:
        softly.assertThat(actual).isEqualToComparingFieldByField(returnRequest(RETURN_REQUEST_ID));
        verify(returnRequestRepository).save(any(ReturnRequest.class));
        verifyZeroInteractions(mbiApiClient);
        verifyZeroInteractions(logisticsPointClient);
        verifyZeroInteractions(pickupPointService);
    }

    @Test
    public void findByReturnId__shouldReturnReturnRequest() {
        // given:
        when(returnRequestRepository.findByReturnId(RETURN_ID_STR))
            .thenReturn(Optional.of(returnRequest(RETURN_REQUEST_ID)));

        // when:
        final Optional<ReturnRequest> actual = service.findByReturnId(RETURN_ID_STR);

        // then:
        softly.assertThat(actual).isPresent();
        softly.assertThat(actual.get()).isEqualToComparingFieldByField(returnRequest(RETURN_REQUEST_ID));
        verify(returnRequestRepository).findByReturnId(any(String.class));
        verifyZeroInteractions(mbiApiClient);
        verifyZeroInteractions(logisticsPointClient);
        verifyZeroInteractions(pickupPointService);
    }

    @Test
    public void findByReturnId__shouldReturnEmpty() {
        // given:
        when(returnRequestRepository.findByReturnId(RETURN_ID_STR))
            .thenReturn(Optional.empty());

        // when:
        final Optional<ReturnRequest> actual = service.findByReturnId(RETURN_ID_STR);

        // then:
        softly.assertThat(actual).isEmpty();
        verify(returnRequestRepository).findByReturnId(any(String.class));
        verifyZeroInteractions(mbiApiClient);
        verifyZeroInteractions(logisticsPointClient);
        verifyZeroInteractions(pickupPointService);
    }

    @Test
    public void addReturnRequestToPickupPoint__shouldDoNothing_whenReturnDeliveryHasNoOutletId() {
        // given:
        final ReturnRequest returnRequest = new ReturnRequest();
        final ReturnDelivery returnDelivery = returnDelivery();
        returnDelivery.setOutletId(null);

        // when:
        service.addReturnRequestToPickupPoint(returnRequest, returnDelivery);

        // then:
        softly.assertThat(returnRequest.getPickupPoint()).isNull();
        verifyZeroInteractions(mbiApiClient);
        verifyZeroInteractions(logisticsPointClient);
        verifyZeroInteractions(pickupPointService);
        verifyZeroInteractions(returnRequestRepository);
    }

    @Test(expected = NullPointerException.class)
    public void addReturnRequestToPickupPoint__shouldDoNothingAndThrowNpe_whenReturnDeliveryHasNoDeliveryServiceId() {
        // given:
        final ReturnRequest returnRequest = new ReturnRequest();
        final ReturnDelivery returnDelivery = returnDelivery();
        returnDelivery.setDeliveryServiceId(null);

        when(mbiApiClient.getOutlet(PICKUP_POINT_MBI_ID, false))
            .thenReturn(outlet());

        // when:
        service.addReturnRequestToPickupPoint(returnRequest, returnDelivery);

        // then:
        softly.assertThat(returnRequest.getPickupPoint()).isNull();
        verify(mbiApiClient).getOutlet(anyLong(), anyBoolean());
        verifyZeroInteractions(logisticsPointClient);
        verifyZeroInteractions(pickupPointService);
        verifyZeroInteractions(returnRequestRepository);
    }

    @Test
    public void addReturnRequestToPickupPoint__shouldAddExistingPickupPointByMbi() {
        // given:
        final ReturnRequest returnRequest = returnRequest(RETURN_REQUEST_ID);
        final ReturnDelivery returnDelivery = returnDelivery();

        doReturn(outlet())
            .when(mbiApiClient)
            .getOutlet(anyLong(), anyBoolean());
        doReturn(pvzLogisticsPointResponse())
            .when(logisticsPointClient)
            .getPickupPoint(anyLong(), anyString());
        doReturn(Optional.of(marketPvzPartnerResponse()))
            .when(logisticsPointClient)
            .getPartner(anyLong());
        doReturn(pickupPoint())
            .when(pickupPointService)
            .getOrCreate(any(LogisticsPointResponse.class));

        // when:
        service.addReturnRequestToPickupPoint(returnRequest, returnDelivery);

        // then:
        softly.assertThat(returnRequest.getPickupPoint()).isEqualTo(pickupPoint());

        verify(mbiApiClient).getOutlet(PICKUP_POINT_MBI_ID, false);
        verify(logisticsPointClient).getPickupPoint(RETURN_DS_ID, DS_OUTLET_CODE);
        verify(logisticsPointClient).getPartner(PARTNER_ID);
        verifyZeroInteractions(logisticsPointClient);
        verify(pickupPointService).getOrCreate(any(LogisticsPointResponse.class));
        verifyZeroInteractions(returnRequestRepository);
    }

    @Test
    public void addReturnRequestToPickupPoint__shouldAddExistingPickupPointByLms() {
        // given:
        final ReturnRequest returnRequest = returnRequest(RETURN_REQUEST_ID);
        final ReturnDelivery returnDelivery = returnDelivery();
        returnDelivery.setOutletId(PICKUP_POINT_LMS_ID);

        doReturn(null)
            .when(mbiApiClient)
            .getOutlet(anyLong(), anyBoolean());
        doReturn(pvzLogisticsPointResponse())
            .when(logisticsPointClient)
            .getLogisticsPoint(anyLong());
        doReturn(Optional.of(marketPvzPartnerResponse()))
            .when(logisticsPointClient)
            .getPartner(anyLong());
        doReturn(pickupPoint())
            .when(pickupPointService)
            .getOrCreate(any(LogisticsPointResponse.class));

        // when:
        service.addReturnRequestToPickupPoint(returnRequest, returnDelivery);

        // then:
        softly.assertThat(returnRequest.getPickupPoint()).isEqualTo(pickupPoint());

        verify(mbiApiClient).getOutlet(PICKUP_POINT_LMS_ID, false);
        verify(logisticsPointClient).getLogisticsPoint(PICKUP_POINT_LMS_ID);
        verify(logisticsPointClient).getPartner(PARTNER_ID);
        verifyZeroInteractions(logisticsPointClient);
        verify(pickupPointService).getOrCreate(any(LogisticsPointResponse.class));
    }

    @Test
    public void addReturnRequestToPickupPoint__shouldDoNothing_whenNoLogisticsPointFromLms() {
        // given:
        final ReturnRequest returnRequest = returnRequest(RETURN_REQUEST_ID);
        final ReturnDelivery returnDelivery = returnDelivery();
        returnDelivery.setOutletId(PICKUP_POINT_LMS_ID);

        when(mbiApiClient.getOutlet(PICKUP_POINT_LMS_ID, false))
            .thenReturn(null);
        when(logisticsPointClient.getLogisticsPoint(PICKUP_POINT_LMS_ID))
            .thenReturn(null);

        // when:
        service.addReturnRequestToPickupPoint(returnRequest, returnDelivery);

        // then:
        softly.assertThat(returnRequest.getPickupPoint()).isNull();
        verify(mbiApiClient).getOutlet(anyLong(), anyBoolean());
        verify(logisticsPointClient).getLogisticsPoint(anyLong());
        verifyZeroInteractions(logisticsPointClient);
        verifyZeroInteractions(pickupPointService);
    }

    @Test
    public void addReturnRequestToPickupPoint__shouldNotAddPickupPoint_whenLogisticsPointIsNotPvz() {
        // given:
        final ReturnRequest returnRequest = returnRequest(RETURN_REQUEST_ID);
        final ReturnDelivery returnDelivery = returnDelivery();

        when(mbiApiClient.getOutlet(PICKUP_POINT_MBI_ID, false))
            .thenReturn(outlet());
        when(logisticsPointClient.getPickupPoint(RETURN_DS_ID, DS_OUTLET_CODE))
            .thenReturn(terminalLogisticsPointResponse());

        // when:
        service.addReturnRequestToPickupPoint(returnRequest, returnDelivery);

        // then:
        softly.assertThat(returnRequest.getPickupPoint()).isNull();
        verify(mbiApiClient).getOutlet(anyLong(), anyBoolean());
        verify(logisticsPointClient).getPickupPoint(anyLong(), anyString());
        verifyZeroInteractions(logisticsPointClient);
        verifyZeroInteractions(pickupPointService);
    }

    @Test
    public void addReturnRequestToPickupPoint__shouldNotAddPickupPoint_whenLogisticPointIsNotMarketPvz() {
        // given:
        final ReturnRequest returnRequest = returnRequest(RETURN_REQUEST_ID);
        final ReturnDelivery returnDelivery = returnDelivery();

        when(mbiApiClient.getOutlet(PICKUP_POINT_MBI_ID, false))
            .thenReturn(outlet());
        when(logisticsPointClient.getPickupPoint(RETURN_DS_ID, DS_OUTLET_CODE))
            .thenReturn(pvzLogisticsPointResponse());
        when(logisticsPointClient.getPartner(PARTNER_ID))
            .thenReturn(Optional.of(nonMarketPvzPartnerResponse()));

        // when:
        service.addReturnRequestToPickupPoint(returnRequest, returnDelivery);

        // then:
        softly.assertThat(returnRequest.getPickupPoint()).isNull();
        verify(mbiApiClient).getOutlet(anyLong(), anyBoolean());
        verify(logisticsPointClient).getPickupPoint(anyLong(), anyString());
        verify(logisticsPointClient).getPartner(anyLong());
        verifyZeroInteractions(logisticsPointClient);
        verifyZeroInteractions(pickupPointService);
    }

    private Outlet outlet() {
        return new Outlet(5324L, null, null, DS_OUTLET_CODE, null, null, null, null, null, null);
    }
}
