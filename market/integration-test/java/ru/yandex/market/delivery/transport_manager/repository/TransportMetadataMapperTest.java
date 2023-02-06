package ru.yandex.market.delivery.transport_manager.repository;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportMetadata;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportMetadataMapper;

class TransportMetadataMapperTest extends AbstractContextualTest {

    @Autowired
    private TransportMetadataMapper transportMetadataMapper;

    private static final TransportMetadata FIRST = new TransportMetadata()
        .setExternalId(1L)
        .setPartnerId(1L)
        .setLogisticPointFromId(1L)
        .setLogisticPointToId(2L)
        .setDuration(Duration.ofMinutes(10))
        .setPalletCount(10)
        .setPrice(1000L);

    private static final TransportMetadata SECOND = new TransportMetadata()
        .setExternalId(2L)
        .setPartnerId(2L)
        .setLogisticPointFromId(1L)
        .setLogisticPointToId(3L)
        .setDuration(Duration.ofHours(3))
        .setPalletCount(20)
        .setPrice(2000L);

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2021-02-24T14:00:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transport_metadata.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_transport_metadata_deletion.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteByLogisticPoints() {
        transportMetadataMapper.deleteAbsentByLogisticPoints(1L, 2L, List.of());
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_transport_metadata_insertion.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insert() {
        transportMetadataMapper.insert(List.of(FIRST, SECOND));
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation_task/transportations.xml",
        "/repository/transportation_task/transport_metadata.xml",
        "/repository/transportation_task/rejected_transport_reject_3.xml"
    })
    void testCorrectMapping() {
        TransportMetadata result = new TransportMetadata()
            .setPalletCount(10)
            .setPrice(1000L)
            .setDuration(Duration.ofHours(3))
            .setPartnerId(1L)
            .setExternalId(1L)
            .setLogisticPointFromId(1L)
            .setLogisticPointToId(2L);

        TransportMetadata transportMetadata = transportMetadataMapper.getForTransportations(List.of(1L)).get(0);

        assertThatModelEquals(result, transportMetadata);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation_task/transportation_tasks.xml",
        "/repository/transportation_task/transport_metadata.xml"
    })
    void getForTransportationTask() {
        List<Long> transportMetadataIds =
            transportMetadataMapper.getForTransportationTask(1L).stream()
                .map(TransportMetadata::getExternalId).collect(Collectors.toList());
        softly.assertThat(transportMetadataIds).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation_task/transportations.xml",
        "/repository/transportation_task/transport_metadata.xml",
        "/repository/transportation_task/rejected_transport_all_fit.xml",
    })
    void getForTransportationAllFit() {
        List<Long> transportMetadataIds =
            transportMetadataMapper.getForTransportations(List.of(1L)).stream()
                .map(TransportMetadata::getExternalId)
                .collect(Collectors.toList());

        softly.assertThat(transportMetadataIds).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation_task/transportations.xml",
        "/repository/transportation_task/transport_metadata.xml",
        "/repository/transportation_task/rejected_transport_reject_3.xml"
    })
    void getForTransportationReject() {
        Set<Long> transportMetadataIds =
            transportMetadataMapper.getForTransportations(List.of(1L)).stream()
                .map(TransportMetadata::getExternalId)
                .collect(Collectors.toSet());

        softly.assertThat(transportMetadataIds).containsExactly(1L);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/transportation_task/transportations.xml",
        "/repository/transportation_task/transport_metadata.xml",
        "/repository/transportation_task/rejected_transport_all_fit.xml",
    })
    void getForTransportationEmpty() {
        Set<Long> transportMetadataIds =
            transportMetadataMapper.getForTransportations(List.of()).stream()
                .map(TransportMetadata::getExternalId)
                .collect(Collectors.toSet());

        softly.assertThat(transportMetadataIds).isEmpty();
    }

    @Test
    @DatabaseSetup(value = "/repository/transportation_task/transport_with_logs.xml")
    void getByMovementId() {
        TransportMetadata transportMetadata = transportMetadataMapper.getByMovementId(1L);
        softly.assertThat(transportMetadata).isEqualTo(SECOND);
    }

    @Test
    @DatabaseSetup(value = "/repository/transportation_task/transport_metadata.xml")
    void getMaxTransportByRoute() {
        Integer maxPalletCount = transportMetadataMapper.getMaxTransportByRoute(1L, 2L);
        softly.assertThat(maxPalletCount).isEqualTo(10);
    }

    @Test
    @DatabaseSetup(value = "/repository/transportation_task/transport_metadata.xml")
    void getMaxTransportByRouteNull() {
        Integer maxPalletCount = transportMetadataMapper.getMaxTransportByRoute(3L, 4L);
        softly.assertThat(maxPalletCount).isNull();
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/transport_metadata.xml")
    void getByIds() {
        List<TransportMetadata> transport = transportMetadataMapper.getByIds(Set.of(2L, 4L));

        TransportMetadata first =
            new TransportMetadata()
                .setExternalId(2L)
                .setPartnerId(2L)
                .setLogisticPointFromId(1L)
                .setLogisticPointToId(3L)
                .setDuration(Duration.ofHours(3))
                .setPrice(2000L)
                .setPalletCount(20);

        TransportMetadata second =
            new TransportMetadata()
                .setExternalId(4L)
                .setPartnerId(4L)
                .setLogisticPointFromId(2L)
                .setLogisticPointToId(1L)
                .setDuration(Duration.ofHours(3))
                .setPrice(1000L)
                .setPalletCount(50);

        assertContainsExactlyInAnyOrder(transport, first, second);
    }
}
