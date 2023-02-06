package ru.yandex.market.delivery.tracker;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.delivery.tracker.dao.repository.ConsumerDao;
import ru.yandex.market.delivery.tracker.dao.repository.DeliveryTrackDao;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.exception.InvalidTrackerRequestException;
import ru.yandex.market.delivery.tracker.service.tracking.DeliveryTrackService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryTrackServiceTest extends AbstractContextualTest {

    private static final String TRACK_CODE_1 = "TRACK_CODE_1";
    private static final long DS_1_ID = 101;
    private static final long CONSUMER_ID = 10;
    private static final String ORDER_1_ID = "ORDER_1";
    private static final String DIFF_ORDER_ID = "DIFF_ORDER_ID";
    private static final long PIM_PAY_ID = 999;

    @Autowired
    private Clock clock;

    @Autowired
    private DeliveryTrackService deliveryTrackService;

    @Autowired
    private ConsumerDao consumerDao;

    @Autowired
    private DeliveryTrackDao deliveryTrackDao;

    @Test
    @DatabaseSetup("/database/states/ds_exist.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_track_registered.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testConcurrentRegistration() throws InterruptedException {
        CountDownLatch startCountDown = new CountDownLatch(1);
        CountDownLatch stopCountDown = new CountDownLatch(2);

        AtomicReference<DuplicateKeyException> failure1 = new AtomicReference<>();
        AtomicReference<DuplicateKeyException> failure2 = new AtomicReference<>();

        createRegisterTrackThread(startCountDown, stopCountDown, failure1).start();
        createRegisterTrackThread(startCountDown, stopCountDown, failure2).start();

        startCountDown.countDown();
        stopCountDown.await(5, TimeUnit.SECONDS);

        assertions().assertThat(Arrays.asList(failure1, failure2))
            .extracting(AtomicReference::get)
            .filteredOn(Objects::nonNull)
            .as("There's got to be exact one Exception to be thrown")
            .hasSize(1)
            .as("Exception has to be DuplicateKeyException")
            .hasOnlyElementsOfType(DuplicateKeyException.class);
    }

    @Test
    @DatabaseSetup("/database/states/three_track_registered.xml")
    @ExpectedDatabase(
        value = "/database/expected/three_track_registered_updated_next_request_ts.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testInstantRequest() {
        Set<String> trackerIds = new HashSet<>(Arrays.asList("ORDER_1", "ORDER_2", "ORDER_5"));
        deliveryTrackService.instantRequestDeliveryTrack(trackerIds);
    }

    @Test
    @DatabaseSetup("/database/states/three_track_registered.xml")
    @ExpectedDatabase(
        value = "/database/expected/three_track_registered_updated_last_orders_status_request_ts.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateLastStatusRequestDate() {
        List<DeliveryTrackMeta> trackMetas = Arrays.asList(
            createDeliveryTrackMeta(1L),
            createDeliveryTrackMeta(2L),
            createDeliveryTrackMeta(5L)
        );
        Date lastStatusRequestDate = Date.from(Instant.now(clock));

        List<DeliveryTrackMeta> actualTrackMetas = deliveryTrackService.updateLastStatusRequestDate(
            trackMetas,
            lastStatusRequestDate
        );

        assertions().assertThat(actualTrackMetas)
            .as("Expected to return the same tracks")
            .isEqualTo(trackMetas)
            .extracting(DeliveryTrackMeta::getLastStatusRequestDate)
            .as("Expected lastStatusRequestDate to be set in tracks")
            .allMatch(lastStatusRequestDate::equals);
    }

    @Test
    @DatabaseSetup("/database/states/single_track_registered.xml")
    void testRegisterExistingWithDifferentOrder() {
        assertions().assertThatThrownBy(this::registerExistingWithDifferentOrder)
            .as("Exception has to be InvalidTrackerRequestException")
            .isInstanceOf(InvalidTrackerRequestException.class);
    }

    @Test
    @DatabaseSetup("/database/states/external_order_ds.xml")
    void testRegisterExternalOrderTrack() {
        DeliveryTrackMeta deliveryTrackMeta = registerExternalOrderTrack();

        assertions().assertThat(deliveryTrackMeta)
            .extracting(DeliveryTrackMeta::getDeliveryServiceId)
            .isEqualTo(PIM_PAY_ID);
        assertions().assertThat(deliveryTrackMeta)
            .extracting(DeliveryTrackMeta::getExternalDeliveryServiceId)
            .isEqualTo(DS_1_ID);
    }

    @Test
    @DatabaseSetup("/database/states/external_order_single_track.xml")
    void testRegisterExternalOrderTrackExists() {
        DeliveryTrackMeta deliveryTrackMeta = registerExternalOrderTrack();
        assertions().assertThat(deliveryTrackMeta)
            .extracting(DeliveryTrackMeta::getId)
            .isEqualTo(1L);
    }

    @Test
    @DatabaseSetup("/database/states/external_order_ds.xml")
    @ExpectedDatabase(
        value = "/database/expected/single_track_external_order.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testConcurrentExternalOrderRegistration() throws InterruptedException {
        CountDownLatch startCountDown = new CountDownLatch(1);
        CountDownLatch stopCountDown = new CountDownLatch(2);

        AtomicReference<DuplicateKeyException> failure1 = new AtomicReference<>();
        AtomicReference<DuplicateKeyException> failure2 = new AtomicReference<>();

        createRegisterExternalOrderTrackThread(startCountDown, stopCountDown, failure1).start();
        createRegisterExternalOrderTrackThread(startCountDown, stopCountDown, failure2).start();

        startCountDown.countDown();
        stopCountDown.await(5, TimeUnit.SECONDS);

        // one of the above methods could throw an error
        // but also can return track created by concurrent method
        // later on we'll check that only one track was actually
        // created and consumer state is consistent
        assertions().assertThat(Arrays.asList(failure1, failure2))
            .extracting(AtomicReference::get)
            .filteredOn(Objects::nonNull)
            .as("There's got to be exact one Exception to be thrown")
            .hasSizeLessThanOrEqualTo(1)
            .as("Exception has to be DuplicateKeyException")
            .hasOnlyElementsOfType(DuplicateKeyException.class);

        List<DeliveryTrackMeta> tracks = Stream.concat(
            Optional.ofNullable(deliveryTrackDao.getDeliveryTrackMeta(1)).stream(),
            Optional.ofNullable(deliveryTrackDao.getDeliveryTrackMeta(2)).stream()
        ).collect(Collectors.toList());

        var consumers = consumerDao.getTrackConsumers(Set.of(1L, 2L));
        assertEquals(1, tracks.size(), "Only one track should be created");
        assertEquals(1, consumers.size(), "Only one track consumer should present");
        assertEquals(tracks.get(0).getId(), consumers.get(0).getTrackId());
    }

    private DeliveryTrackMeta createDeliveryTrackMeta(long id) {
        DeliveryTrackMeta trackMeta = new DeliveryTrackMeta();
        trackMeta.setId(id);

        return trackMeta;
    }

    private Thread createRegisterTrackThread(
        CountDownLatch startLatch,
        CountDownLatch finishLatch,
        AtomicReference<DuplicateKeyException> exceptionHolder
    ) {
        return new Thread(() -> {
            try {
                startLatch.await();
                deliveryTrackService.registerDeliveryTrack(
                    TRACK_CODE_1,
                    DS_1_ID,
                    CONSUMER_ID,
                    ORDER_1_ID,
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 12, 31),
                    DeliveryType.DELIVERY,
                    true,
                    EntityType.ORDER,
                    ApiVersion.DS,
                    null
                );
            } catch (DuplicateKeyException e) {
                exceptionHolder.set(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                finishLatch.countDown();
            }
        });
    }

    void registerExistingWithDifferentOrder() {
        deliveryTrackService.registerDeliveryTrack(
            TRACK_CODE_1,
            DS_1_ID,
            CONSUMER_ID,
            DIFF_ORDER_ID,
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 12, 31),
            DeliveryType.DELIVERY,
            true,
            EntityType.ORDER,
            null,
            null
        );
    }

    DeliveryTrackMeta registerExternalOrderTrack() {
        return deliveryTrackService.registerDeliveryTrack(
            TRACK_CODE_1,
            DS_1_ID,
            CONSUMER_ID,
            DIFF_ORDER_ID,
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 12, 31),
            DeliveryType.DELIVERY,
            true,
            EntityType.EXTERNAL_ORDER,
            null,
            null
        );
    }

    private Thread createRegisterExternalOrderTrackThread(
        CountDownLatch startLatch,
        CountDownLatch finishLatch,
        AtomicReference<DuplicateKeyException> exceptionHolder
    ) {
        return new Thread(() -> {
            try {
                startLatch.await();
                registerExternalOrderTrack();
            } catch (DuplicateKeyException e) {
                exceptionHolder.set(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                finishLatch.countDown();
            }
        });
    }
}
