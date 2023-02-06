package ru.yandex.market.delivery.transport_manager.service.external.lgw;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.provider.service.transportation.volume.DefaultVolumeStrategy;
import ru.yandex.market.delivery.transport_manager.provider.service.transportation.volume.ReturnDropoffVolumeStrategy;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;

public class VolumeStrategyTest extends AbstractContextualTest {
    @Autowired
    private DefaultVolumeStrategy defaultStrategy;
    @Autowired
    private ReturnDropoffVolumeStrategy dropoffVolumeStrategy;
    @Autowired
    private TransportationMapper mapper;

    @Test
    @DatabaseSetup({
        "/repository/transportation/multiple_transportations_deps.xml",
        "/repository/transportation/multiple_transportations.xml",
        "/repository/transportation/metadata.xml",
        "/repository/transportation/multuple_transportations_logistics_points.xml",
        "/repository/register/register_with_volume.xml"
    })
    void testOutboundRegisterVolume() {
        Integer volume = defaultStrategy.calculateVolume(mapper.getById(1));
        softly.assertThat(volume).isEqualTo(111000);
    }

    @Test
    @DatabaseSetup("/repository/transportation/return_dropoff_with_volume.xml")
    void testReturnDropoffVolume() {
        mockProperty(TmPropertyKey.DROPOFF_ORDER_AVERAGE_VOLUME_IN_CCM, 800);
        Integer volume = dropoffVolumeStrategy.calculateVolume(mapper.getById(1));
        softly.assertThat(volume).isEqualTo(4000);
    }

    @Test
    @DatabaseSetup("/repository/transportation/return_dropoff_with_volume.xml")
    @DatabaseSetup("/repository/transportation/return_dropoff_orders.xml")
    void testReturnDropoffVolumeWithRealBarcodes() {
        mockProperty(TmPropertyKey.DROPOFF_ORDER_AVERAGE_VOLUME_IN_CCM, 800);
        Integer volume = dropoffVolumeStrategy.calculateVolume(mapper.getById(1));
        softly.assertThat(volume).isEqualTo(4102);
    }
}
