package ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import steps.LocationSteps;
import steps.logisticsPointSteps.LogisticPointSteps;
import steps.outletSteps.OutletSteps;

import ru.yandex.market.delivery.mdbapp.components.geo.GeoInfo;
import ru.yandex.market.delivery.mdbapp.components.service.lms.LmsLogisticsPointClient;
import ru.yandex.market.delivery.mdbapp.integration.converter.LmsWarehouseToInletConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.OutletToLogisticsPointConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.mbi.MbiAddressToAddressConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.mbi.MbiGeoInfoToGeoInfoConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.mbi.MbiPhoneNumberToPhoneNumberConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.mbi.MbiScheduleLinesToScheduleLinesConverter;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPointPair;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReturnInletFetcherTest {

    private static final Long FF_ID = 123L;

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Rule
    public ExpectedException expectException = ExpectedException.none();

    @Mock
    private LmsLogisticsPointClient lmsLogisticsPointClient;

    @Mock
    private MbiApiClient mbiApiClient;

    @Mock
    private GeoInfo geoInfo;

    private ReturnInletFetcher fetcher;

    @Before
    public void setUp() {
        MbiGeoInfoToGeoInfoConverter mbiGeoInfoConverter = new MbiGeoInfoToGeoInfoConverter();
        MbiScheduleLinesToScheduleLinesConverter mbiScheduleLinesConverter =
            new MbiScheduleLinesToScheduleLinesConverter();
        MbiAddressToAddressConverter mbiAddressConverter = new MbiAddressToAddressConverter();
        MbiPhoneNumberToPhoneNumberConverter mbiPhoneNumberConverter = new MbiPhoneNumberToPhoneNumberConverter();

        OutletToLogisticsPointConverter logisticsPointConverter = new OutletToLogisticsPointConverter(
            mbiAddressConverter,
            mbiGeoInfoConverter,
            mbiPhoneNumberConverter,
            mbiScheduleLinesConverter
        );

        LmsWarehouseToInletConverter lmsWarehouseToOutletConverter = new LmsWarehouseToInletConverter(geoInfo);
        fetcher = new ReturnInletFetcher(
            lmsLogisticsPointClient,
            mbiApiClient,
            lmsWarehouseToOutletConverter,
            logisticsPointConverter
        );
    }

    @Test
    public void testWarehouseFoundInLms() throws IOException {
        when(geoInfo.getLocation(anyInt())).thenReturn(LocationSteps.getLocation());
        when(lmsLogisticsPointClient.getPartner(FF_ID))
            .thenReturn(Optional.of(PartnerResponse.newBuilder().marketId(100L).build()));
        when(lmsLogisticsPointClient.getWarehousesByPartnerId(FF_ID))
            .thenReturn(LogisticPointSteps.getWarehouseResponse());

        LogisticsPointPair logisticsPointPair = fetcher.doFetch(FF_ID);

        verify(lmsLogisticsPointClient, times(1)).getWarehousesByPartnerId(FF_ID);
        verify(mbiApiClient, never()).getInletsV2(FF_ID);
        verify(lmsLogisticsPointClient).getPartner(FF_ID);

        softly.assertThat(logisticsPointPair.getLogisticsPoint()).as("Should properly fetch and convert")
            .isEqualTo(LogisticPointSteps.getDefaultInlet());
    }

    @Test
    public void testPartnerNotFoundInLms() {
        when(lmsLogisticsPointClient.getPartner(FF_ID)).thenReturn(Optional.empty());


        assertNull(fetcher.doFetch(FF_ID));
    }

    @Test
    public void tesPartnerWithoutMarketIdFoundInLms() {
        when(lmsLogisticsPointClient.getPartner(FF_ID)).thenReturn(Optional.of(PartnerResponse.newBuilder().build()));

        assertNull(fetcher.doFetch(FF_ID));
    }

    @Test
    public void testWarehouseFoundInMbi() {
        when(lmsLogisticsPointClient.getPartner(FF_ID))
            .thenReturn(Optional.of(PartnerResponse.newBuilder().marketId(100L).build()));
        when(lmsLogisticsPointClient.getWarehousesByPartnerId(FF_ID)).thenReturn(Collections.emptyList());
        when(mbiApiClient.getInletsV2(FF_ID)).thenReturn(Collections.singletonList(
            OutletSteps.getDefaultOutlet()
        ));

        LogisticsPointPair logisticsPointPair = fetcher.doFetch(FF_ID);

        verify(lmsLogisticsPointClient, times(1)).getWarehousesByPartnerId(FF_ID);
        verify(mbiApiClient, times(1)).getInletsV2(FF_ID);
        verify(lmsLogisticsPointClient).getPartner(FF_ID);

        softly.assertThat(logisticsPointPair.getLogisticsPoint()).as("Should properly fetch and convert")
            .isEqualTo(LogisticPointSteps.getDefaultOutlet());
    }
}
