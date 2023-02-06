package ru.yandex.market.delivery.mdbapp.integration.router;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import steps.ParcelSteps;
import steps.UpdateLastMileSteps;
import steps.orderSteps.ChangeRequestSteps;
import steps.orderSteps.OrderEventSteps;

import ru.yandex.common.util.region.CustomRegionAttribute;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryLastMileChangeRequestPayload;
import ru.yandex.market.delivery.mdbapp.components.geo.GeoInfo;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.AddressDto;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.PossibleOrderChangeRepository;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.enums.UpdateRequestStatus;
import ru.yandex.market.delivery.mdbapp.integration.converter.AddressConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.ChangeRequestStatusConverter;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.LocationFetcher;
import ru.yandex.market.delivery.mdbapp.integration.payload.change.request.ChangeRequestInternal;
import ru.yandex.market.delivery.mdbapp.integration.payload.change.request.ChangeRequestTypeInternal;
import ru.yandex.market.delivery.mdbapp.integration.payload.change.request.LastMileChangeRequestFactory;
import ru.yandex.market.delivery.mdbapp.integration.payload.change.request.LastMileChangeRequestInternal;
import ru.yandex.market.delivery.mdbapp.integration.service.PersonalDataService;
import ru.yandex.market.personal.PersonalClient;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.delivery.mdbapp.integration.router.ChangeRequestsRouter.CHANNEL_PROCESS_CHANGE_REQUEST;

@RunWith(Parameterized.class)
public class ChangeRequestsRouterTest {

    private ChangeRequestsRouter router = new ChangeRequestsRouter(mock(PossibleOrderChangeRepository.class));

    @Parameterized.Parameter
    public ChangeRequestInternal changeRequestInternal;

    @Parameterized.Parameter(1)
    public String channel;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
            {
                OrderEventSteps.createInternalCancelChangeRequest(),
                CHANNEL_PROCESS_CHANGE_REQUEST
            }
        });
    }

    @Test
    public void routeTest() {
        assertEquals("Route to correct channel", channel, router.route(changeRequestInternal));
    }

    @Test
    public void routeChangeLastMileTest() {
        ChangeRequest changeRequest = createChangeRequest();
        RegionService regionService = mock(RegionService.class);
        LastMileChangeRequestFactory factory = createFactory(regionService);

        mockRegionService(regionService);

        LastMileChangeRequestInternal requestInternal = factory.create(
            OrderEventSteps.createChangeRequestCreatedEvent(List.of(changeRequest)),
            changeRequest
        );
        assertEquals(
            "Route to correct channel",
            CHANNEL_PROCESS_CHANGE_REQUEST,
            router.route(requestInternal)
        );
        assertLastMileChangeRequestFields(requestInternal);
    }

    private void assertLastMileChangeRequestFields(LastMileChangeRequestInternal requestInternal) {
        assertEquals(requestInternal.getType(), ChangeRequestTypeInternal.UPDATE_LAST_MILE);
        assertEquals(requestInternal.getStatus(), UpdateRequestStatus.PROCESSING);
        AddressDto payloadAddress = requestInternal.getQueuePayload().getAddress();
        assertEquals(payloadAddress.getCountry(), "Country");
        assertEquals(payloadAddress.getFederalDistrict(), "FederalDistrict");
        assertEquals(payloadAddress.getRegion(), "Region");
        assertEquals(payloadAddress.getSubRegion(), "Subregion");
        assertEquals(payloadAddress.getGeoId().intValue(), 123);
    }

    @Nonnull
    private ChangeRequest createChangeRequest() {
        DeliveryLastMileChangeRequestPayload payload = new DeliveryLastMileChangeRequestPayload();
        payload.setAddress(UpdateLastMileSteps.createAddress());
        payload.setRoute(new ObjectMapper().valueToTree(ParcelSteps.ROUTE));
        return ChangeRequestSteps.createChangeRequest(payload);
    }

    @Nonnull
    private LastMileChangeRequestFactory createFactory(RegionService regionService) {
        GeoInfo geoInfo = new GeoInfo(regionService);
        LocationFetcher locationFetcher = new LocationFetcher(geoInfo);
        return new LastMileChangeRequestFactory(
            new ChangeRequestStatusConverter(),
            new AddressConverter(),
            new PersonalDataService(
                new FeatureProperties().setFillPersonalDataValuesWithDefaultData(true),
                Mockito.mock(PersonalClient.class),
                new ru.yandex.market.logistics.personal.converter.AddressConverter()
            ),
            locationFetcher
        );
    }

    private void mockRegionService(RegionService regionService) {
        RegionTree regionTree = mock(RegionTree.class);
        Region continent = new Region(123, "Region", RegionType.CONTINENT, null);
        continent.setCustomAttributeValue(
            CustomRegionAttribute.TIMEZONE_OFFSET,
            Integer.toString(ZoneOffset.ofHours(3).getTotalSeconds())
        );
        doReturn(regionTree).when(regionService).getRegionTree();
        doReturn(continent).when(regionTree).getRegion(123);
        Region country = new Region(123, "Country", RegionType.COUNTRY, null);
        Region federalDistrict = new Region(123, "FederalDistrict", RegionType.COUNTRY_DISTRICT, null);
        Region region = new Region(123, "Region", RegionType.REGION, null);
        Region locality = new Region(123, "Locality", RegionType.VILLAGE, null);
        Region subRegion = new Region(123, "Subregion", RegionType.SUBJECT_FEDERATION_DISTRICT, null);
        doReturn(country).when(regionTree).getTypedParent(123, RegionType.COUNTRY);
        doReturn(federalDistrict).when(regionTree).getTypedParent(123, RegionType.COUNTRY_DISTRICT);
        doReturn(region).when(regionTree).getTypedParent(123, RegionType.SUBJECT_FEDERATION);
        doReturn(locality).when(regionTree).getTypedParent(123, RegionType.VILLAGE);
        doReturn(subRegion).when(regionTree).getTypedParent(123, RegionType.SUBJECT_FEDERATION_DISTRICT);
    }
}
