package ru.yandex.travel.api.services.hotels_booking_flow;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.hotels.common.PartnerConfigService;
import ru.yandex.travel.hotels.geosearch.GeoSearchParser;
import ru.yandex.travel.hotels.geosearch.GeoSearchService;
import ru.yandex.travel.hotels.geosearch.model.GeoSearchRsp;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.hotels.proto.TPartner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeoSearchHotelContentServiceTest extends BaseBookingFlowTest {
    private ObjectMapper mapper = new ObjectMapper();
    private GeoSearchParser parser = new GeoSearchParser(mapper);
    private GeoSearchService geoSearchService = mock(GeoSearchService.class);
    private PartnerConfigService partnerConfigService = mock(PartnerConfigService.class);
    private GeoSearchHotelContentService service = new GeoSearchHotelContentService(geoSearchService, new GeoSearchHotelContentServiceProperties(),  partnerConfigService);
    private GeoSearchRsp validResponse;
    private GeoSearchRsp emptyResponse;

    @SuppressWarnings("UnstableApiUsage")
    @Before
    public void prepare() throws IOException {
        String validProto = Resources.toString(Resources.getResource("geo-response.pb.txt"), Charset.forName("UTF-8"));
        String emptyProto = Resources.toString(Resources.getResource("geo-response-empty.pb.txt"),
                Charset.forName("UTF-8"));
        validResponse = parser.parseTextProtoResponse(validProto);
        emptyResponse = parser.parseTextProtoResponse(emptyProto);

    }

    @Test
    public void testSuccess() throws ExecutionException, InterruptedException {
        when(geoSearchService.query(any())).thenReturn(CompletableFuture.completedFuture(validResponse));
        when(partnerConfigService.getByKey(EPartnerId.PI_EXPEDIA)).thenReturn(TPartner.newBuilder().setCode("ytravel_expedia").build());
        BookingFlowContext context = prepareContext("any");
        context.setDecodedToken(mockedTokenResponse("any"));
        var futures = service.getFutures(context);
        assertThat(futures.getHotelInfoFuture()).isCompleted();
        assertThat(futures.getHotelLegalItemFuture()).isCompleted();
        var hotelInfo = futures.getHotelInfoFuture().get();
        var hotelLegal = futures.getHotelLegalItemFuture().get();
        assertThat(hotelInfo).isEqualTo(mockedGeoHotelInfo());
        assertThat(hotelLegal).isEqualTo(mockedGeoLegalItem());
    }

    @Test
    public void testEmptyResponse() throws ExecutionException, InterruptedException, IOException {
        when(geoSearchService.query(any())).thenReturn(CompletableFuture.completedFuture(emptyResponse));
        when(partnerConfigService.getByKey(EPartnerId.PI_EXPEDIA)).thenReturn(TPartner.newBuilder().setCode("ytravel_expedia").build());
        BookingFlowContext context = prepareContext("any");
        context.setDecodedToken(mockedTokenResponse("any"));
        var futures = service.getFutures(context);
        assertThat(futures.getHotelInfoFuture()).isCompleted();
        assertThat(futures.getHotelInfoFuture().get()).isNull();
        assertThat(futures.getHotelLegalItemFuture()).isCompleted();
        assertThat(futures.getHotelLegalItemFuture().get()).isNull();
    }

    @Test
    public void testException() {
        when(geoSearchService.query(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException(
                "Unable to get geo content")));
        BookingFlowContext context = prepareContext("any");
        context.setDecodedToken(mockedTokenResponse("any"));
        var futures = service.getFutures(context);
        assertThat(futures.getHotelInfoFuture()).isCompletedExceptionally();
        assertThat(futures.getHotelLegalItemFuture()).isCompletedExceptionally();
    }
}
