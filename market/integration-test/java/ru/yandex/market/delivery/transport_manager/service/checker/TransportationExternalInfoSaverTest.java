package ru.yandex.market.delivery.transport_manager.service.checker;

import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMetadataMapper;
import ru.yandex.market.delivery.transport_manager.service.checker.dto.EnrichedTransportation;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

@DatabaseSetup("/repository/distribution_unit_center/transportations_to_dc_and_ff.xml")
@DbUnitConfiguration(
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"},
    dataSetLoader = ReplacementDataSetLoader.class
)
class TransportationExternalInfoSaverTest  extends AbstractContextualTest {

    @Autowired
    private TransportationExternalInfoSaver saver;

    @Autowired
    private TransportationMapper transportationMapper;

    @Autowired
    private TransportationMetadataMapper metadataMapper;

    @Test
    void testSaveFakePoint() {
        var t = transportationMapper.getById(11L);
        saver.save(buildEnrichedTransportation(t));

        var metadata = metadataMapper.getLogisticsPointForUnit(112L);
        softly.assertThat(metadata.getLogisticsPointId()).isEqualTo(0L);
        softly.assertThat(metadata.getExternalId()).isNotEmpty();
    }

    private EnrichedTransportation buildEnrichedTransportation(Transportation transportation) {
        return new EnrichedTransportation()
            .setTransportation(transportation)
            .setEnabledMethods(Set.of())
            .setPartnerInfos(Map.of())
            .setInboundLogisticPoint(
                LogisticsPointResponse.newBuilder()
                    .id(10000000007L)
                    .partnerId(402L)
                    .address(Address.newBuilder().build())
                    .build()
            )
            .setLegalInfos(Map.of());
    }
}
