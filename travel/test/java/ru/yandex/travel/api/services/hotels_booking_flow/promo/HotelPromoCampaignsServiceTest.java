package ru.yandex.travel.api.services.hotels_booking_flow.promo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.google.protobuf.UInt32Value;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.api.services.hotels_booking_flow.CheckParamsProvider;
import ru.yandex.travel.api.services.hotels_booking_flow.CheckParamsRequest;
import ru.yandex.travel.api.services.hotels_booking_flow.promo.HotelPromoCampaignsServiceProperties.Taxi2020PromoCampaignProperties;
import ru.yandex.travel.api.services.promo.YandexPlusService;
import ru.yandex.travel.commons.experiments.ExperimentDataProvider;
import ru.yandex.travel.commons.http.CommonHttpHeaders;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.hotels.models.booking_flow.promo.WhiteLabelPromoCampaign;
import ru.yandex.travel.hotels.models.booking_flow.promo.YandexEdaPromoCampaign;
import ru.yandex.travel.hotels.models.booking_flow.promo.YandexPlusPromoCampaign;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.hotels.proto.EWhiteLabelEligibility;
import ru.yandex.travel.hotels.proto.EYandexEdaEligibility;
import ru.yandex.travel.hotels.proto.EYandexPlusEligibility;
import ru.yandex.travel.hotels.proto.PromoServiceV1Grpc.PromoServiceV1FutureStub;
import ru.yandex.travel.hotels.proto.TDeterminePromosForOfferRsp;
import ru.yandex.travel.hotels.proto.TWhiteLabelPoints;
import ru.yandex.travel.hotels.proto.TWhiteLabelPointsLinguistics;
import ru.yandex.travel.hotels.proto.TWhiteLabelStatus;
import ru.yandex.travel.hotels.proto.TYandexEda2022Status;
import ru.yandex.travel.hotels.proto.TYandexEdaPromoInfo;
import ru.yandex.travel.hotels.proto.TYandexPlusStatus;
import ru.yandex.travel.hotels.services.promoservice.PromoServiceClient;
import ru.yandex.travel.hotels.services.promoservice.PromoServiceClientFactory;
import ru.yandex.travel.hotels.services.promoservice.PromoServiceClientImpl;
import ru.yandex.travel.testing.time.SettableClock;
import ru.yandex.travel.white_label.proto.EWhiteLabelPartnerId;
import ru.yandex.travel.white_label.proto.EWhiteLabelPointsType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.commons.concurrent.FutureUtils.joinCompleted;

public class HotelPromoCampaignsServiceTest {
    private HotelPromoCampaignsService service;
    private SettableClock clock;
    private PromoServiceV1FutureStub promoService;

    @Before
    public void init() {
        clock = new SettableClock();
        HotelPromoCampaignsServiceProperties properties = HotelPromoCampaignsServiceProperties.builder()
                .taxi2020(Taxi2020PromoCampaignProperties.builder()
                        .startsAt(Instant.parse("2020-08-16T21:00:00Z"))
                        .endsAt(Instant.parse("2020-11-17T21:00:00Z"))
                        .minPriceCurrency("RUB")
                        .minPriceAmount(BigDecimal.valueOf(5000))
                        .maxCheckInDate(LocalDate.parse("2021-03-31"))
                        .build())
                .yandexPlusEnabled(true)
                .yandexPlusWithdrawEnabled(true)
                .build();

        PromoServiceClientFactory promoServiceClientFactory = Mockito.mock(PromoServiceClientFactory.class);
        promoService = Mockito.mock(PromoServiceV1FutureStub.class);
        PromoServiceClient promoServiceClient = new PromoServiceClientImpl(promoServiceClientFactory, clock);
        YandexPlusService yandexPlusService = Mockito.mock(YandexPlusService.class);
        ExperimentDataProvider experimentDataProvider = new ExperimentDataProvider();
        service = new HotelPromoCampaignsService(properties, clock, promoServiceClient, yandexPlusService, experimentDataProvider);

        when(promoServiceClientFactory.createFutureStub()).thenReturn(promoService);
        when(yandexPlusService.isCurrencySupported(any())).thenAnswer(Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void getCommonPromoCampaignsInfo_failed() {
        // imitating EXPIRED offers (price == null)
        CheckParamsProvider params = checkParams("2021-01-01", 10_000);
        when(promoService.determinePromosForOffer(any()))
                .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        CompletableFuture<TDeterminePromosForOfferRsp> commonCampaigns = service.getCommonPromoCampaignsInfo(params, Collections.emptyList(), new CommonHttpHeaders(Map.of()));
        assertThatThrownBy(() -> joinCompleted(commonCampaigns))
                .isExactlyInstanceOf(CompletionException.class)
                .hasMessageContaining("StatusRuntimeException: UNAVAILABLE")
                .hasRootCauseExactlyInstanceOf(StatusRuntimeException.class);
    }

    @Test
    public void getTaxi2020Campaign() {
        clock.setCurrentTime(Instant.parse("2020-08-18T17:10:00Z"));
        assertThat(service.getTaxi2020Campaign(checkParams("2021-02-21", 7000)).isEligible()).isTrue();

        // price or check-in mismatch
        assertThat(service.getTaxi2020Campaign(checkParams("2021-02-21", 4999)).isEligible()).isFalse();
        assertThat(service.getTaxi2020Campaign(checkParams("2021-04-01", 7000)).isEligible()).isFalse();
        // last check-in day
        assertThat(service.getTaxi2020Campaign(checkParams("2021-03-31", 7000)).isEligible()).isTrue();

        // the promo has ended
        clock.setCurrentTime(Instant.parse("2020-11-18T21:10:00Z"));
        assertThat(service.getTaxi2020Campaign(checkParams("2021-02-21", 7000)).isEligible()).isFalse();
    }

    @Test
    public void getYandexPlusCampaign_ok() {
        YandexPlusPromoCampaign plusPromo = service.getYandexPlusCampaign(
                getPlusCommonCampaigns(1_000), checkParams("2021-01-01", 10_000), null);

        assertThat(plusPromo).isNotNull();
        assertThat(plusPromo.getEligible()).isTrue();
        assertThat(plusPromo.getPoints()).isEqualTo(1_000);
        assertThat(plusPromo.getWithdrawPoints()).isNull();
    }

    @Test
    public void getYandexPlusCampaign_withdrawPoints() {
        TDeterminePromosForOfferRsp plusCommonCampaigns = getPlusCommonCampaigns(1_000);
        YandexPlusPromoCampaign plus0 = service.getYandexPlusCampaign(
                plusCommonCampaigns, checkParams("2021-01-01", 10_000), 0);
        assertThat(plus0.getWithdrawPoints()).isNull();

        YandexPlusPromoCampaign plus500 = service.getYandexPlusCampaign(
                plusCommonCampaigns, checkParams("2021-01-01", 10_000), 500);
        assertThat(plus500.getWithdrawPoints()).isEqualTo(500);

        YandexPlusPromoCampaign plus200k = service.getYandexPlusCampaign(
                plusCommonCampaigns, checkParams("2021-01-01", 10_000), 200_000);
        assertThat(plus200k.getWithdrawPoints()).isEqualTo(9_999);

        YandexPlusPromoCampaign plusMinus100 = service.getYandexPlusCampaign(
                plusCommonCampaigns, checkParams("2021-01-01", 10_000), -100);
        assertThat(plusMinus100.getWithdrawPoints()).isNull();

        YandexPlusPromoCampaign plusOrder1r = service.getYandexPlusCampaign(
                plusCommonCampaigns, checkParams("2021-01-01", 1), 1_000);
        assertThat(plusOrder1r.getWithdrawPoints()).isNull();

        YandexPlusPromoCampaign plusOrder2r = service.getYandexPlusCampaign(
                plusCommonCampaigns, checkParams("2021-01-01", 2), 1_000);
        assertThat(plusOrder2r.getWithdrawPoints()).isEqualTo(1);
    }

    @Test
    public void getYandexEdaCampaign_ok() {
        YandexEdaPromoCampaign edaPromo = service.getYandexEdaCampaign(getEdaCommonCampaigns());

        assertThat(edaPromo).isNotNull();
        assertThat(edaPromo.getEligible()).isEqualTo(EYandexEdaEligibility.YEE_ELIGIBLE);
        assertThat(edaPromo.getData().getNumberOfPromocodes()).isEqualTo(1);
        assertThat(edaPromo.getData().getPromocodeCost()).isEqualTo(Money.of(100, "RUB"));
        assertThat(edaPromo.getData().getFirstSendDate()).isEqualTo("2022-02-26");
        assertThat(edaPromo.getData().getLastSendDate()).isEqualTo("2022-02-27");
    }

    @Test
    public void getYandexEdaCampaign_promo_disabled() {
        YandexEdaPromoCampaign yandexEdaPromoCampaign = service.getYandexEdaCampaign(
                TDeterminePromosForOfferRsp.newBuilder()
                        .setYandexEda2022Status(TYandexEda2022Status.newBuilder()
                                .setEligibility(EYandexEdaEligibility.YEE_PROMO_DISABLED)
                                .build())
                        .build());

        assertThat(yandexEdaPromoCampaign).isNotNull();
        assertThat(yandexEdaPromoCampaign.getEligible()).isEqualTo(EYandexEdaEligibility.YEE_PROMO_DISABLED);
        assertThat(yandexEdaPromoCampaign.getData()).isEqualTo(null);
    }

    @Test
    public void getWhiteLabelCampaign() {
        WhiteLabelPromoCampaign whiteLabelPromo = service.getWhiteLabelCampaign(getWhiteLabelCommonCampaigns());

        assertThat(whiteLabelPromo).isNotNull();
        assertThat(whiteLabelPromo.getEligible()).isEqualTo(EWhiteLabelEligibility.WLE_ELIGIBLE);
        assertThat(whiteLabelPromo.getPartnerId()).isEqualTo(EWhiteLabelPartnerId.WL_S7);
        assertThat(whiteLabelPromo.getPoints().getAmount()).isEqualTo(200);
        assertThat(whiteLabelPromo.getPoints().getPointsType()).isEqualTo(EWhiteLabelPointsType.WLP_S7);
        assertThat(whiteLabelPromo.getPointsLinguistics().getNameForNumeralNominative()).isEqualTo("миль");
    }

    private TDeterminePromosForOfferRsp getPlusCommonCampaigns(int points) {
        return TDeterminePromosForOfferRsp.newBuilder()
                .setPlus(TYandexPlusStatus.newBuilder()
                        .setEligibility(points > 0 ?
                                EYandexPlusEligibility.YPE_ELIGIBLE:
                                EYandexPlusEligibility.YPE_PROMO_DISABLED)
                        .setPoints(UInt32Value.newBuilder()
                                .setValue(points)
                                .build())
                        .build())
                .build();
    }

    private TDeterminePromosForOfferRsp getEdaCommonCampaigns() {
        return TDeterminePromosForOfferRsp.newBuilder()
                .setYandexEda2022Status(TYandexEda2022Status.newBuilder()
                        .setEligibility(EYandexEdaEligibility.YEE_ELIGIBLE)
                        .setPromoInfo(TYandexEdaPromoInfo.newBuilder()
                                .setPromoCodeCount(1)
                                .setPromoCodeNominal(100)
                                .setFirstDate("2022-02-26")
                                .setLastDate("2022-02-27")
                                .build())
                        .build())
                .build();
    }

    public CheckParamsProvider checkParams(String date, int sum) {
        return new CheckParamsRequest(
                Money.of(BigDecimal.valueOf(sum), ProtoCurrencyUnit.RUB),
                Money.of(BigDecimal.valueOf(sum), ProtoCurrencyUnit.RUB),
                Money.of(BigDecimal.valueOf(sum), ProtoCurrencyUnit.RUB),
                LocalDate.parse(date), LocalDate.parse(date).plusDays(2),
                EPartnerId.PI_TRAVELLINE, "100", "345678", false, "0.0.0.0");
    }

    private TDeterminePromosForOfferRsp getWhiteLabelCommonCampaigns() {
        return TDeterminePromosForOfferRsp.newBuilder()
                .setWhiteLabelStatus(TWhiteLabelStatus.newBuilder()
                        .setEligibility(EWhiteLabelEligibility.WLE_ELIGIBLE)
                        .setPartnerId(EWhiteLabelPartnerId.WL_S7)
                        .setPoints(TWhiteLabelPoints.newBuilder()
                                .setAmount(200)
                                .setPointsType(EWhiteLabelPointsType.WLP_S7)
                                .build())
                        .setPointsLinguistics(TWhiteLabelPointsLinguistics.newBuilder()
                                .setNameForNumeralNominative("миль")
                                .build())
                        .build())
                .build();
    }
}
