package ru.yandex.travel.api.services.orders.train;

import java.time.Duration;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.api.config.common.EncryptionConfigurationProperties;
import ru.yandex.travel.api.infrastucture.ApiTokenEncrypter;
import ru.yandex.travel.api.models.train.WarningMessageCode;
import ru.yandex.travel.api.services.orders.TrainDictionaryMapService;
import ru.yandex.travel.api.services.orders.TrainModelMapService;
import ru.yandex.travel.api.services.orders.TrainOrderStatusMappingService;
import ru.yandex.travel.api.services.orders.TrainOrdersServiceProperties;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.train.model.InsuranceStatus;
import ru.yandex.travel.train.model.RoutePolicy;
import ru.yandex.travel.train.model.TrainReservation;
import ru.yandex.travel.train.partners.im.model.ImBlankStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class TrainModelMapServiceTest {
    private TrainModelMapService trainModelMapService;
    private TrainOrdersServiceProperties trainOrdersServiceProperties;

    @Before
    public void setUp() {
        trainOrdersServiceProperties = new TrainOrdersServiceProperties();
        trainOrdersServiceProperties.setElectronicRegistrationChangeWarningTime(Duration.ofMinutes(15));
        trainOrdersServiceProperties.setInsuranceAutoReturnWarningTime(Duration.ofMinutes(10));
        trainOrdersServiceProperties.setAfterDepartureMaxWarningTime(Duration.ofDays(10));
        var trainDictionaryMapService = new TrainDictionaryMapService(
                MockTrainDictionaryHelper.trainSettlementDataProvider(),
                MockTrainDictionaryHelper.trainStationDataProvider(),
                MockTrainDictionaryHelper.trainStationCodeDataProvider(),
                MockTrainDictionaryHelper.trainTimeZoneDataProvider(),
                MockTrainDictionaryHelper.trainStationExpressAliasDataProvider());
        trainModelMapService = new TrainModelMapService(trainDictionaryMapService,
                new TrainOrderStatusMappingService(), trainOrdersServiceProperties,
                new ApiTokenEncrypter(new EncryptionConfigurationProperties()));
    }

    @Test
    public void testWarningTicketsTakenAway() {
        TrainReservation payload = TrainTestHelpers.createTrainReservation(EOrderItemState.IS_CONFIRMED);
        payload.getPassengers().get(0).getTicket().setImBlankStatus(ImBlankStatus.STRICT_BOARDING_PASS);
        var warnings = trainModelMapService.getOrderWarnings(payload, Instant.now(), true);
        assertThat(warnings.size()).isEqualTo(1);
        assertThat(warnings.get(0).getCode()).isEqualTo(WarningMessageCode.TICKETS_TAKEN_AWAY);
    }

    @Test
    public void testWarningElectronicRegistrationExpired() {
        TrainReservation payload = TrainTestHelpers.createTrainReservation(EOrderItemState.IS_CONFIRMED);
        payload.getPassengers().get(0).getTicket().setImBlankStatus(ImBlankStatus.REMOTE_CHECK_IN);
        var departure = Instant.now().plus(Duration.ofMinutes(999));
        var expiresAt = Instant.now().minus(Duration.ofMinutes(9));
        payload.setDepartureTime(departure);
        payload.getPassengers().get(0).getTicket().setCanChangeElectronicRegistrationTill(expiresAt);
        payload.getPassengers().get(0).getTicket().setCanReturnTill(expiresAt);
        var warnings = trainModelMapService.getOrderWarnings(payload, null, true);
        assertThat(warnings.size()).isEqualTo(1);
        assertThat(warnings.get(0).getCode()).isEqualTo(WarningMessageCode.ELECTRONIC_REGISTRATION_EXPIRED);
        assertThat(warnings.get(0).getFrom()).isEqualTo(expiresAt);
        assertThat(warnings.get(0).getTo()).isEqualTo(expiresAt.plus(Duration.ofHours(1)));
    }

    @Test
    public void testWarningElectronicRegistrationAlmostExpired() {
        TrainReservation payload = TrainTestHelpers.createTrainReservation(EOrderItemState.IS_CONFIRMED);
        payload.getPassengers().get(0).getTicket().setImBlankStatus(ImBlankStatus.REMOTE_CHECK_IN);
        payload.setRoutePolicy(RoutePolicy.INTERNATIONAL);
        var departure = Instant.now().plus(Duration.ofMinutes(999));
        var expiresAt = Instant.now().plus(Duration.ofMinutes(9));
        payload.setDepartureTime(departure);
        payload.getPassengers().get(0).getTicket().setCanChangeElectronicRegistrationTill(expiresAt);
        payload.getPassengers().get(0).getTicket().setCanReturnTill(expiresAt);
        var warnings = trainModelMapService.getOrderWarnings(payload, null, true);
        assertThat(warnings.size()).isEqualTo(1);
        assertThat(warnings.get(0).getCode()).isEqualTo(WarningMessageCode.ELECTRONIC_REGISTRATION_ALMOST_EXPIRED);
        assertThat(warnings.get(0).getFrom()).isEqualTo(
                expiresAt.minus(trainOrdersServiceProperties.getElectronicRegistrationChangeWarningTime()));
        assertThat(warnings.get(0).getTo()).isEqualTo(expiresAt);
    }

    @Test
    public void testWarningElectronicRegistrationAlmostSixHoursToDeparture() {
        TrainReservation payload = TrainTestHelpers.createTrainReservation(EOrderItemState.IS_CONFIRMED);
        payload.getPassengers().get(0).getTicket().setImBlankStatus(ImBlankStatus.REMOTE_CHECK_IN);
        payload.setRoutePolicy(RoutePolicy.INTERNATIONAL);
        var departure = Instant.now().plus(Duration.ofMinutes(8 + 6 * 60));
        var expiresAt = Instant.now().plus(Duration.ofMinutes(9));
        payload.setDepartureTime(departure);
        payload.getPassengers().get(0).getTicket().setCanChangeElectronicRegistrationTill(expiresAt);
        payload.getPassengers().get(0).getTicket().setCanReturnTill(expiresAt);
        var warnings = trainModelMapService.getOrderWarnings(payload, null, true);
        assertThat(warnings.size()).isEqualTo(1);
        assertThat(warnings.get(0).getCode()).isEqualTo(WarningMessageCode.ALMOST_SIX_HOURS_TO_DEPARTURE);
        assertThat(warnings.get(0).getFrom()).isEqualTo(departure.minus(Duration.ofHours(6))
                .minus(trainOrdersServiceProperties.getElectronicRegistrationChangeWarningTime()));
        assertThat(warnings.get(0).getTo()).isEqualTo(departure.minus(Duration.ofHours(6)));
    }

    @Test
    public void testWarningElectronicRegistrationLessThanSixHoursToDeparture() {
        TrainReservation payload = TrainTestHelpers.createTrainReservation(EOrderItemState.IS_CONFIRMED);
        payload.getPassengers().get(0).getTicket().setImBlankStatus(ImBlankStatus.REMOTE_CHECK_IN);
        payload.setRoutePolicy(RoutePolicy.INTERNATIONAL);
        var departure = Instant.now().plus(Duration.ofMinutes(99));
        var expiresAt = Instant.now().plus(Duration.ofMinutes(9));
        payload.setDepartureTime(departure);
        payload.getPassengers().get(0).getTicket().setCanChangeElectronicRegistrationTill(expiresAt);
        payload.getPassengers().get(0).getTicket().setCanReturnTill(expiresAt);
        var warnings = trainModelMapService.getOrderWarnings(payload, null, true);
        assertThat(warnings.size()).isEqualTo(1);
        assertThat(warnings.get(0).getCode()).isEqualTo(WarningMessageCode.LESS_THEN_SIX_HOURS_TO_DEPARTURE);
        assertThat(warnings.get(0).getFrom()).isEqualTo(departure.minus(Duration.ofHours(6)));
        assertThat(warnings.get(0).getTo()).isEqualTo(departure.plus(Duration.ofDays(10)));
    }

    @Test
    public void testWarningTrainLeftStartStation() {
        TrainReservation payload = TrainTestHelpers.createTrainReservation(EOrderItemState.IS_CONFIRMED);
        payload.getPassengers().get(0).getTicket().setImBlankStatus(ImBlankStatus.REMOTE_CHECK_IN);
        var departure = Instant.now().minus(Duration.ofMinutes(9));
        var expiresAt = Instant.now().minus(Duration.ofMinutes(99));
        payload.setDepartureTime(departure);
        payload.getPassengers().get(0).getTicket().setCanChangeElectronicRegistrationTill(expiresAt);
        payload.getPassengers().get(0).getTicket().setCanReturnTill(expiresAt);
        var warnings = trainModelMapService.getOrderWarnings(payload, null, true);
        assertThat(warnings.size()).isEqualTo(1);
        assertThat(warnings.get(0).getCode()).isEqualTo(WarningMessageCode.TRAIN_LEFT_START_STATION);
        assertThat(warnings.get(0).getFrom()).isEqualTo(expiresAt.plus(Duration.ofHours(1)));
        assertThat(warnings.get(0).getTo()).isEqualTo(departure.plus(Duration.ofDays(10)));
    }

    @Test
    public void testWarningAlmostSixHoursToDeparture() {
        TrainReservation payload = TrainTestHelpers.createTrainReservation(EOrderItemState.IS_CONFIRMED, 2);
        payload.getPassengers().get(0).getTicket().setImBlankStatus(ImBlankStatus.NO_REMOTE_CHECK_IN);
        payload.setRoutePolicy(RoutePolicy.INTERNATIONAL);
        var departure = Instant.now().plus(Duration.ofMinutes(9 + 6 * 60));
        payload.setDepartureTime(departure);
        var warnings = trainModelMapService.getOrderWarnings(payload, null, true);
        assertThat(warnings.size()).isEqualTo(1);
        assertThat(warnings.get(0).getCode()).isEqualTo(WarningMessageCode.ALMOST_SIX_HOURS_TO_DEPARTURE);
        assertThat(warnings.get(0).getFrom()).isEqualTo(departure.minus(Duration.ofHours(6))
                .minus(trainOrdersServiceProperties.getElectronicRegistrationChangeWarningTime()));
        assertThat(warnings.get(0).getTo()).isEqualTo(departure.minus(Duration.ofHours(6)));
    }

    @Test
    public void testWarningLessThanSixHoursToDeparture() {
        TrainReservation payload = TrainTestHelpers.createTrainReservation(EOrderItemState.IS_CONFIRMED, 2);
        payload.getPassengers().get(0).getTicket().setImBlankStatus(ImBlankStatus.NO_REMOTE_CHECK_IN);
        payload.setRoutePolicy(RoutePolicy.INTERNATIONAL);
        var departure = Instant.now().plus(Duration.ofMinutes(9));
        payload.setDepartureTime(departure);
        var warnings = trainModelMapService.getOrderWarnings(payload, null, true);
        assertThat(warnings.size()).isEqualTo(1);
        assertThat(warnings.get(0).getCode()).isEqualTo(WarningMessageCode.LESS_THEN_SIX_HOURS_TO_DEPARTURE);
        assertThat(warnings.get(0).getFrom()).isEqualTo(departure.minus(Duration.ofHours(6)));
        assertThat(warnings.get(0).getTo()).isEqualTo(departure.plus(Duration.ofDays(10)));
    }

    @Test
    public void testWarningTrainLeftDepartureStation() {
        TrainReservation payload = TrainTestHelpers.createTrainReservation(EOrderItemState.IS_CONFIRMED, 2);
        payload.getPassengers().get(0).getTicket().setImBlankStatus(ImBlankStatus.NO_REMOTE_CHECK_IN);
        var departure = Instant.now().minus(Duration.ofMinutes(9));
        var expiresAt = Instant.now().minus(Duration.ofMinutes(99));
        payload.setDepartureTime(departure);
        payload.getPassengers().get(0).getTicket().setCanChangeElectronicRegistrationTill(expiresAt);
        payload.getPassengers().get(0).getTicket().setCanReturnTill(expiresAt);
        var warnings = trainModelMapService.getOrderWarnings(payload, null, true);
        assertThat(warnings.size()).isEqualTo(1);
        assertThat(warnings.get(0).getCode()).isEqualTo(WarningMessageCode.TRAIN_LEFT_DEPARTURE_STATION);
        assertThat(warnings.get(0).getFrom()).isEqualTo(departure);
        assertThat(warnings.get(0).getTo()).isEqualTo(departure.plus(Duration.ofDays(10)));
    }

    @Test
    public void testWarningTrainAlmostLeftDepartureStation() {
        TrainReservation payload = TrainTestHelpers.createTrainReservation(EOrderItemState.IS_CONFIRMED, 2);
        payload.getPassengers().get(0).getTicket().setImBlankStatus(ImBlankStatus.NO_REMOTE_CHECK_IN);
        var departure = Instant.now().plus(Duration.ofMinutes(9));
        var expiresAt = Instant.now().minus(Duration.ofMinutes(9));
        payload.setDepartureTime(departure);
        payload.getPassengers().get(0).getTicket().setCanChangeElectronicRegistrationTill(expiresAt);
        payload.getPassengers().get(0).getTicket().setCanReturnTill(expiresAt);
        var warnings = trainModelMapService.getOrderWarnings(payload, null, true);
        assertThat(warnings.size()).isEqualTo(1);
        assertThat(warnings.get(0).getCode()).isEqualTo(WarningMessageCode.TRAIN_ALMOST_LEFT_DEPARTURE_STATION);
        assertThat(warnings.get(0).getFrom()).isEqualTo(
                departure.minus(trainOrdersServiceProperties.getElectronicRegistrationChangeWarningTime()));
        assertThat(warnings.get(0).getTo()).isEqualTo(departure);
    }

    @Test
    public void testWarningInsuranceAutoReturn() {
        TrainReservation payload = TrainTestHelpers.createTrainReservation(EOrderItemState.IS_CONFIRMED);
        payload.setInsuranceStatus(InsuranceStatus.AUTO_RETURN);
        var confirmedAt = Instant.now().minus(Duration.ofMinutes(9));
        var warnings = trainModelMapService.getOrderWarnings(payload, confirmedAt, true);
        assertThat(warnings.size()).isEqualTo(1);
        assertThat(warnings.get(0).getCode()).isEqualTo(WarningMessageCode.INSURANCE_AUTO_RETURN);
        assertThat(warnings.get(0).getTo()).isEqualTo(
                confirmedAt.plus(trainOrdersServiceProperties.getInsuranceAutoReturnWarningTime()));
    }
}
