package ru.yandex.travel.api.services.hotels_booking_flow;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.travel.api.config.hotels.HotelOfferConfigurationProperties;
import ru.yandex.travel.api.exceptions.HotelInfoNotFoundException;
import ru.yandex.travel.api.exceptions.InvalidTravelTokenException;
import ru.yandex.travel.api.services.hotels_booking_flow.promo.HotelPromoCampaignsService;
import ru.yandex.travel.api.services.orders.Meters;
import ru.yandex.travel.api.services.orders.user_info.UserInfoGrpcService;
import ru.yandex.travel.commons.health.HealthCheckedSupplier;
import ru.yandex.travel.commons.messaging.KeyValueStorage;
import ru.yandex.travel.hotels.common.token.TokenException;
import ru.yandex.travel.hotels.models.booking_flow.HotelInfo;
import ru.yandex.travel.hotels.models.booking_flow.LegalInfo;
import ru.yandex.travel.hotels.models.booking_flow.Offer;
import ru.yandex.travel.hotels.models.booking_flow.OfferState;
import ru.yandex.travel.hotels.models.booking_flow.RateStatus;
import ru.yandex.travel.hotels.models.booking_flow.promo.Mir2020PromoCampaign;
import ru.yandex.travel.hotels.models.booking_flow.promo.PromoCampaignsInfo;
import ru.yandex.travel.hotels.models.booking_flow.promo.Taxi2020PromoCampaign;
import ru.yandex.travel.hotels.models.booking_flow.promo.YandexEdaPromoCampaign;
import ru.yandex.travel.hotels.models.booking_flow.promo.YandexPlusPromoCampaign;
import ru.yandex.travel.hotels.proto.EMirEligibility;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.hotels.proto.EYandexEdaEligibility;
import ru.yandex.travel.hotels.proto.TDeterminePromosForOfferRsp;
import ru.yandex.travel.hotels.proto.TOfferData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("FieldMayBeFinal")
@Slf4j
public class OfferServiceTests extends BaseBookingFlowTest {
    public static final String MALFORMED_TOKEN = "malformedToken";
    public static final String EXPIRED_TOKEN = "expiredToken";
    public static final String MISSING_TOKEN = "missingToken";
    public static final String MISSING_GEO = "missingGeo";
    public static final String FAILED_GEO = "failedGeo";

    private PartnerDispatcher partnerDispatcher = mock(PartnerDispatcher.class);
    private Meters meters = mock(Meters.class, Mockito.RETURNS_DEEP_STUBS);
    private KeyValueStorage storage = mock(KeyValueStorage.class);
    private GeoSearchHotelContentService hotelContentService = mock(GeoSearchHotelContentService.class,
            Mockito.RETURNS_DEEP_STUBS);
    private ChecksumService checksumService = new ChecksumService();
    private HotelPromoCampaignsService promoCampaignsService = mock(HotelPromoCampaignsService.class);
    private OfferService offerService = new OfferService(partnerDispatcher, tokenCodec, meters);
    private UserInfoGrpcService userInfoGrpcService = mock(UserInfoGrpcService.class);


    private static String getByteString(String string) {
        Formatter formatter = new Formatter();
        for (byte b : string.getBytes()) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    @Before
    public void prepare() {
        TestProvider testPartner = new TestProvider();
        testPartner.storageSupplier = new HealthCheckedSupplier<>(storage, "test_kv_storage");
        testPartner.meters = meters;
        testPartner.geoSearchHotelContentService = hotelContentService;
        testPartner.offerConfig = new OfferServiceConfigurationProperties();
        testPartner.offerConfig.setTokenTtl(Duration.of(2, ChronoUnit.HOURS));
        testPartner.checksumService = checksumService;
        testPartner.promoCampaignsService = promoCampaignsService;
        testPartner.paymentScheduleService = mock(PaymentScheduleService.class);
        testPartner.offerProperties = new HotelOfferConfigurationProperties();
        testPartner.offerProperties.setPartnersWithAllGuests(new ArrayList<>());
        testPartner.userInfoGrpcService = userInfoGrpcService;

        OfferServiceConfigurationProperties.LegalData yandexLegalData =
                new OfferServiceConfigurationProperties.LegalData();
        yandexLegalData.setName("ООО Яндекс");
        yandexLegalData.setAddress("Льва Тослтого что-то там");
        yandexLegalData.setOgrn("4242");
        yandexLegalData.setWorkingHours("Да постоянно");

        testPartner.offerConfig.setYandexLegalData(yandexLegalData);
        when(hotelContentService.getFutures(any())).thenAnswer(this::mockGeoResponses);
        when(partnerDispatcher.get(any())).thenReturn(testPartner);
        when(tokenCodec.decode(MALFORMED_TOKEN)).thenThrow(new TokenException("Unable to decrypt token", null));
        when(storage.get(anyString(), eq(TOfferData.class))).thenAnswer(this::mockStorageResponse);
        when(storage.isHealthy()).thenReturn(true);
        YandexPlusPromoCampaign yandexPlusPromoCampaign = YandexPlusPromoCampaign.builder()
                .eligible(false)
                .points(100)
                .withdrawPoints(100)
                .build();
        when(promoCampaignsService.getCommonPromoCampaignsInfo(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(TDeterminePromosForOfferRsp.newBuilder().build()));
        when(promoCampaignsService.getYandexPlusCampaign(any(), any(), any()))
                .thenReturn(yandexPlusPromoCampaign);
        YandexEdaPromoCampaign yandexEdaPromoCampaign = YandexEdaPromoCampaign.builder()
                .eligible(EYandexEdaEligibility.YEE_ELIGIBLE)
                .data(YandexEdaPromoCampaign.YandexEdaPromocodePayload.builder()
                        .numberOfPromocodes(1)
                        .promocodeCost(Money.of(100, "RUB"))
                        .firstSendDate("2022-03-01")
                        .lastSendDate("2022-03-02")
                        .build())
                .build();
        when(promoCampaignsService.getYandexEdaCampaign(any()))
                .thenReturn(yandexEdaPromoCampaign);
        when(promoCampaignsService.calculatePromoCampaignsInfo(any(), any(), any(), any()))
                .thenReturn(PromoCampaignsInfo.builder()
                        .taxi2020(new Taxi2020PromoCampaign(true))
                        .mir2020(Mir2020PromoCampaign.builder().eligibility(EMirEligibility.ME_WRONG_BOOKING_DATE).build())
                        .yandexPlus(yandexPlusPromoCampaign)
                        .yandexEda(yandexEdaPromoCampaign)
                        .build());
        when(userInfoGrpcService.getUserExistingOrderTypes(any()))
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    }

    private CompletableFuture<TOfferData> mockStorageResponse(InvocationOnMock invocationOnMock) {
        String token = invocationOnMock.getArgument(0);
        if (getByteString(EXPIRED_TOKEN).equals(token) || getByteString(MISSING_TOKEN).equals(token)) {
            return CompletableFuture.completedFuture(null);
        } else {
            return CompletableFuture.completedFuture(TOfferData.newBuilder().build());
        }
    }

    private GeoSearchHotelContentService.GeoSearchFutures mockGeoResponses(InvocationOnMock invocationOnMock) {
        BookingFlowContext context = invocationOnMock.getArgument(0);
        return new GeoSearchHotelContentService.GeoSearchFutures() {
            @Override
            public CompletableFuture<HotelInfo> getHotelInfoFuture() {
                switch (context.getDecodedToken().getOfferId()) {
                    case MISSING_GEO:
                        return CompletableFuture.completedFuture(null);
                    case FAILED_GEO:
                        return CompletableFuture.failedFuture(new RuntimeException("Failed in Geo"));
                    default:
                        return CompletableFuture.completedFuture(mockedGeoHotelInfo());
                }

            }

            @Override
            public CompletableFuture<LegalInfo.LegalInfoItem> getHotelLegalItemFuture() {
                switch (context.getDecodedToken().getOfferId()) {
                    case MISSING_GEO:
                        return CompletableFuture.completedFuture(null);
                    case FAILED_GEO:
                        return CompletableFuture.failedFuture(new RuntimeException("Failed in Geo"));
                    default:
                        return CompletableFuture.completedFuture(mockedGeoLegalItem());
                }
            }
        };
    }

    private CompletableFuture<Offer> getOffer(BookingFlowContext context) {
        return getOffer(context, 100);
    }

    private CompletableFuture<Offer> getOffer(BookingFlowContext context, Integer userPlusBalance) {
        return offerService.getOffer(context, userPlusBalance);
    }

    /**
     * Tests a successful getOffer scenario
     * <p>
     * All the fields are set, offer is usable for createOrder requests
     */
    @Test
    public void testSuccessfulOffer() throws ExecutionException, InterruptedException, JsonProcessingException {
        var context = prepareContext("someCorrectOffer");
        var offerFuture = getOffer(context);
        assertThat(offerFuture).isCompleted();
        var offer = offerFuture.get();
        assertThat(offer.getHotelInfo()).isNotNull();
        assertThat(offer.getPartnerHotelInfo()).isNotNull();
        assertThat(offer.getRefundRules()).isNotNull();
        assertThat(offer.getRateInfo()).isNotNull();
        assertThat(offer.getRateInfo().getStatus()).isEqualTo(RateStatus.CONFIRMED);
        assertThat(offer.getRateInfo().getTotalRate()).isNotNull();
        assertThat(offer.getRateInfo().getBaseRate()).isNotNull();
        assertThat(offer.getRateInfo().getBaseRateBreakdown()).isNotNull();
        assertThat(offer.getRateInfo().getTaxesAndFees()).isNotNull();
        assertThat(offer.getRoomInfo()).isNotNull();
        assertThat(offer.getStayInfo()).isNotNull();
        assertThat(offer.getLegalInfo()).isNotNull();
        assertThat(offer.getLegalInfo().getHotel()).isNotNull();
        assertThat(offer.getLegalInfo().getYandex()).isNotNull();
        assertThat(offer.getLegalInfo().getPartner()).isNotNull();
        assertThat(offer.getMetaInfo()).isNotNull();
        assertThat(offer.getMetaInfo().getSearch()).isNotNull();
        assertThat(offer.getMetaInfo().getCheckSum()).isNotNull();
        assertThat(offer.getPromoCampaignsInfo()).isNotNull();
        assertThat(offer.checkOfferState()).isEqualTo(OfferState.READY);
        assertThat(offer.isAllGuestsRequired()).isNotNull();

        var serializedOffer = TestHelper.createMapper().writeValueAsString(offer);
        log.info(serializedOffer);

        var anotherOffer = getOffer(prepareContext("someCorrectOffer")).get();
        assertThat(offer.getMetaInfo().getCheckSum()).isEqualTo(anotherOffer.getMetaInfo().getCheckSum());
    }


    /**
     * Tests a malformed-token case.
     * <p>
     * This is the only scenario when getOffer method fails (i.e. returns an exceptionally completed future.
     */
    @Test
    public void testMalformedToken() {
        var context = prepareContext(MALFORMED_TOKEN);
        var offerFuture = getOffer(context);
        assertThat(offerFuture).hasFailedWithThrowableThat().isInstanceOf(InvalidTravelTokenException.class);
        verify(meters.getOfferMeters(BookingFlowContext.Stage.GET_OFFER).getMalformedToken(), times(1)).increment();
    }

    /**
     * Tests an expired token case.
     * <p>
     * In this case offer data cannot be fetched from local storage, thus causing some partner-specific futures to fail
     * (this is not necessary true for all partners: some partners may not rely on local storage for some of their
     * futures, but the test partner is written to imitate EAN's behavior - to fail on all futures if the offer data is
     * missing).
     * This results in rate, cancellation info, partner legal etc to be null. However the offer itself is returned, with
     * GeoSearch-provided hotel info, data about the search etc.
     */
    @Test
    public void testExpiredToken() throws ExecutionException, InterruptedException {

        var context = prepareContext(EXPIRED_TOKEN);
        var offerFuture = getOffer(context);
        assertThat(offerFuture).isCompleted();
        var offer = offerFuture.get();
        assertThat(offer.getHotelInfo()).isNotNull();
        assertThat(offer.getPartnerHotelInfo()).isNull();
        assertThat(offer.getRefundRules()).isNull();
        assertThat(offer.getRateInfo()).isNull();
        assertThat(offer.getRoomInfo()).isNull();
        assertThat(offer.getStayInfo()).isNull();
        assertThat(offer.getLegalInfo()).isNotNull();
        assertThat(offer.getLegalInfo().getHotel()).isNotNull();
        assertThat(offer.getLegalInfo().getYandex()).isNotNull();
        assertThat(offer.getLegalInfo().getPartner()).isNull();
        assertThat(offer.getMetaInfo()).isNotNull();
        assertThat(offer.getMetaInfo().getSearch()).isNotNull();
        assertThat(offer.getMetaInfo().getCheckSum()).isNull();
        assertThat(offer.isAllGuestsRequired()).isNotNull();
        assertThat(offer.checkOfferState()).isEqualTo(OfferState.MISSING_DATA);
        verify(meters.getOfferMeters(BookingFlowContext.Stage.GET_OFFER, EPartnerId.PI_EXPEDIA).getExpiredToken(),
                times(1)).increment();
        verify(meters.getOfferMeters(BookingFlowContext.Stage.GET_OFFER, EPartnerId.PI_EXPEDIA).getMissingToken(),
                times(0)).increment();
    }

    /**
     * Tests an missing token case.
     * <p>
     * Same as testExpiredToken, but the token is generated much younger, thus causing different meters to trigger.
     */
    @Test
    public void testMissingToken() throws ExecutionException, InterruptedException {

        var context = prepareContext(MISSING_TOKEN);
        var offerFuture = getOffer(context);
        assertThat(offerFuture).isCompleted();
        var offer = offerFuture.get();
        assertThat(offer.getHotelInfo()).isNotNull();
        assertThat(offer.getPartnerHotelInfo()).isNull();
        assertThat(offer.getRefundRules()).isNull();
        assertThat(offer.getRateInfo()).isNull();
        assertThat(offer.getRoomInfo()).isNull();
        assertThat(offer.getStayInfo()).isNull();
        assertThat(offer.getLegalInfo()).isNotNull();
        assertThat(offer.getLegalInfo().getHotel()).isNotNull();
        assertThat(offer.getLegalInfo().getYandex()).isNotNull();
        assertThat(offer.getLegalInfo().getPartner()).isNull();
        assertThat(offer.getMetaInfo()).isNotNull();
        assertThat(offer.getMetaInfo().getSearch()).isNotNull();
        assertThat(offer.getMetaInfo().getCheckSum()).isNull();
        assertThat(offer.isAllGuestsRequired()).isNotNull();
        assertThat(offer.checkOfferState()).isEqualTo(OfferState.MISSING_DATA);
        verify(meters.getOfferMeters(BookingFlowContext.Stage.GET_OFFER, EPartnerId.PI_EXPEDIA).getExpiredToken(),
                times(0)).increment();
        verify(meters.getOfferMeters(BookingFlowContext.Stage.GET_OFFER, EPartnerId.PI_EXPEDIA).getMissingToken(),
                times(1)).increment();
    }

    private void checkHotelInfoNotFoundExceptionThrown(BookingFlowContext context) throws InterruptedException {
        try {
            getOffer(context).get();
        } catch (java.util.concurrent.ExecutionException ex) {
            assertThat(ex.getCause().toString()).isEqualTo(
                    (new HotelInfoNotFoundException("", "Can't get hotel info from geosearch")).toString());
            return;
        }
        throw new AssertionError("HotelInfoNotFoundException must be raised");
    }

    /**
     * Tests an Empty geoResponse case.
     * <p>
     * In this case GeoSearch service is unable to find the hotel by its partner id - and returns a response without
     * hotel snippets. This results GeoSearchHotelContentService to return null-vlued futures, which leads to null
     * in HotelInfo fields and hotel-specific legal data. All other attributes are inplace.
     */
    @Test
    public void testEmptyGeoResponse() throws InterruptedException {
        var context = prepareContext(MISSING_GEO);
        checkHotelInfoNotFoundExceptionThrown(context);
    }


    /**
     * Tests a Failed geoResponse case.
     * <p>
     * Same as testEmptyGeoResponse, but instead of returning empty geoResponse this scenario simulates an exception
     * occurred while interacting with geoSearch
     */
    @Test
    public void testFailedGeoResponse() throws InterruptedException {
        var context = prepareContext(FAILED_GEO);
        checkHotelInfoNotFoundExceptionThrown(context);
    }

    /**
     * Tests a failed partner's content response case
     * <p>
     * In this case partner client fails to complete all its futures related to hotel static information. This imitates
     * EAN's failure of PropertyContent future.
     * This leads to partnerHotelInfo, roomInfo and stayInfo to be null in resulting offer
     */
    @Test
    public void testFailedPartnerContent() throws ExecutionException, InterruptedException {
        var offerFuture = getOffer(prepareContext(TestProvider.TEST_CONTENT_NOT_FOUND));
        assertThat(offerFuture).isCompleted();
        var offer = offerFuture.get();
        assertThat(offer.getHotelInfo()).isNotNull();
        assertThat(offer.getPartnerHotelInfo()).isNull();
        assertThat(offer.getRefundRules()).isNotNull();
        assertThat(offer.getRateInfo()).isNotNull();
        assertThat(offer.getRoomInfo()).isNull();
        assertThat(offer.getStayInfo()).isNull();
        assertThat(offer.getLegalInfo()).isNotNull();
        assertThat(offer.getLegalInfo().getHotel()).isNotNull();
        assertThat(offer.getLegalInfo().getYandex()).isNotNull();
        assertThat(offer.getLegalInfo().getPartner()).isNotNull();
        assertThat(offer.getMetaInfo()).isNotNull();
        assertThat(offer.getMetaInfo().getSearch()).isNotNull();
        assertThat(offer.getMetaInfo().getCheckSum()).isNull();
        assertThat(offer.isAllGuestsRequired()).isNotNull();
        assertThat(offer.checkOfferState()).isEqualTo(OfferState.MISSING_DATA);
    }

    /**
     * Tests a failed partner's price check response case
     * <p>
     * In this case partner client fails to complete all its futures related to hotel price information. This imitates
     * EAN's failure of PriceCheck future.
     * This leads to rateInfo and cancellationInfo to be null in resulting offer
     */
    @Test
    public void testFailedPartnerPrice() throws ExecutionException, InterruptedException {
        var offerFuture = getOffer(prepareContext(TestProvider.TEST_PRICE_NOT_FOUND));
        assertThat(offerFuture).isCompleted();
        var offer = offerFuture.get();
        assertThat(offer.getHotelInfo()).isNotNull();
        assertThat(offer.getPartnerHotelInfo()).isNotNull();
        assertThat(offer.getRefundRules()).isNull();
        assertThat(offer.getRateInfo()).isNull();
        assertThat(offer.getRoomInfo()).isNotNull();
        assertThat(offer.getStayInfo()).isNotNull();
        assertThat(offer.getLegalInfo()).isNotNull();
        assertThat(offer.getLegalInfo().getHotel()).isNotNull();
        assertThat(offer.getLegalInfo().getYandex()).isNotNull();
        assertThat(offer.getLegalInfo().getPartner()).isNotNull();
        assertThat(offer.getMetaInfo()).isNotNull();
        assertThat(offer.getMetaInfo().getSearch()).isNotNull();
        assertThat(offer.getMetaInfo().getCheckSum()).isNull();
        assertThat(offer.isAllGuestsRequired()).isNotNull();
        assertThat(offer.checkOfferState()).isEqualTo(OfferState.MISSING_DATA);
    }


    /**
     * Tests a case when partner's price check responds with a price mismatch
     * <p>
     * In this case all futures complete successfully and all the offer fields are set,
     * however the resulting offer should indicate a price mismatch information and should be in "conflict" state
     * (later mappable to HTTP 409 status)
     */
    @Test
    public void testPriceMismatch() throws ExecutionException, InterruptedException {
        var offerFuture = getOffer(prepareContext(TestProvider.TEST_PRICE_MISMATCH));
        assertThat(offerFuture).isCompleted();
        var offer = offerFuture.get();
        assertThat(offer.getHotelInfo()).isNotNull();
        assertThat(offer.getPartnerHotelInfo()).isNotNull();
        assertThat(offer.getRefundRules()).isNotNull();
        assertThat(offer.getRateInfo()).isNotNull();
        assertThat(offer.getRateInfo().getStatus()).isEqualTo(RateStatus.PRICE_MISMATCH);
        assertThat(offer.getRateInfo().getTotalRate()).isNotNull();
        assertThat(offer.getRateInfo().getBaseRate()).isNotNull();
        assertThat(offer.getRateInfo().getBaseRateBreakdown()).isNotNull();
        assertThat(offer.getRateInfo().getTaxesAndFees()).isNotNull();
        assertThat(offer.getRoomInfo()).isNotNull();
        assertThat(offer.getStayInfo()).isNotNull();
        assertThat(offer.getLegalInfo()).isNotNull();
        assertThat(offer.getLegalInfo().getHotel()).isNotNull();
        assertThat(offer.getLegalInfo().getYandex()).isNotNull();
        assertThat(offer.getLegalInfo().getPartner()).isNotNull();
        assertThat(offer.getMetaInfo()).isNotNull();
        assertThat(offer.getMetaInfo().getSearch()).isNotNull();
        assertThat(offer.getMetaInfo().getCheckSum()).isNotNull();
        assertThat(offer.isAllGuestsRequired()).isNotNull();
        assertThat(offer.checkOfferState()).isEqualTo(OfferState.PRICE_CONFLICT);
    }

    /**
     * Tests a сase when partner's price check responds with a sold-out
     * <p>
     * In this case all futures complete successfully and all the offer fields are set,
     * however the resulting offer should not contain any price info, and should indicate a price mismatch information
     * and should be in "conflict" state (later mappable to HTTP 409 status)
     */
    @Test
    public void testSoldOut() throws ExecutionException, InterruptedException {
        var offerFuture = getOffer(prepareContext(TestProvider.TEST_SOLD_OUT));
        assertThat(offerFuture).isCompleted();
        var offer = offerFuture.get();
        assertThat(offer.getHotelInfo()).isNotNull();
        assertThat(offer.getPartnerHotelInfo()).isNotNull();
        assertThat(offer.getRefundRules()).isNotNull();
        assertThat(offer.getRateInfo()).isNotNull();
        assertThat(offer.getRateInfo().getStatus()).isEqualTo(RateStatus.SOLD_OUT);
        assertThat(offer.getRateInfo().getTotalRate()).isNull();
        assertThat(offer.getRateInfo().getBaseRate()).isNull();
        assertThat(offer.getRateInfo().getBaseRateBreakdown()).isNull();
        assertThat(offer.getRateInfo().getTaxesAndFees()).isNull();
        assertThat(offer.getRoomInfo()).isNotNull();
        assertThat(offer.getStayInfo()).isNotNull();
        assertThat(offer.getLegalInfo()).isNotNull();
        assertThat(offer.getLegalInfo().getHotel()).isNotNull();
        assertThat(offer.getLegalInfo().getYandex()).isNotNull();
        assertThat(offer.getLegalInfo().getPartner()).isNotNull();
        assertThat(offer.getMetaInfo()).isNotNull();
        assertThat(offer.getMetaInfo().getSearch()).isNotNull();
        assertThat(offer.getMetaInfo().getCheckSum()).isNull();
        assertThat(offer.isAllGuestsRequired()).isNotNull();
        assertThat(offer.checkOfferState()).isEqualTo(OfferState.PRICE_CONFLICT);
    }
}
