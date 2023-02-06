package ru.yandex.market.logistics.iris.service.mdm.publish;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.model.DimensionsDTO;
import ru.yandex.market.logistics.iris.model.ShelfLifeMeasurementDTO;
import ru.yandex.market.logistics.iris.service.logbroker.producer.LogBrokerPushService;
import ru.yandex.market.logistics.iris.service.mdm.publish.measurement.MeasurementPublishService;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyBooleanKey;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

public class MeasurementPublishServiceImplTest extends AbstractContextualTest {

    private final String WAREHOUSE_ID = "111";

    @MockBean
    private LogBrokerPushService logBrokerPushService;

    @Autowired
    private MeasurementPublishService publishService;

    @Captor
    private ArgumentCaptor<byte[]> eventsCaptor;

    @SpyBean
    private SystemPropertyService systemPropertyService;

    @Test
    public void shouldSuccessPushToLogBroker() {
        publishService.publish(createDimensionsData(), WAREHOUSE_ID);

        Mockito.verify(logBrokerPushService, times(1)).push(eventsCaptor.capture());
        assertions().assertThat(eventsCaptor.getValue().length > 0).isTrue();
    }

    @Test
    public void shouldSuccessPushToLogBrokerShelfLife() {
        doReturn(true).when(systemPropertyService)
                .getBooleanProperty(SystemPropertyBooleanKey.ENABLE_NEW_SHELF_LIFE_FEATURE);

        publishService.publishShelfLifes(createShelfLifeData(), WAREHOUSE_ID);

        Mockito.verify(logBrokerPushService, times(1)).push(eventsCaptor.capture());
        assertions().assertThat(eventsCaptor.getValue().length > 0).isTrue();
    }

    @Test
    public void shouldNotPushMessageToLogBrokerIfDataEmpty() {
        publishService.publish(ImmutableMap.of(), WAREHOUSE_ID);

        Mockito.verify(logBrokerPushService, times(0)).push(any());
    }

    @Test
    public void shouldNotPushMessageToLogBrokerIfDimensionsNull() {
        publishService.publish(Collections.singletonMap(ItemIdentifier.of("1", "sku1"), null), WAREHOUSE_ID);

        Mockito.verify(logBrokerPushService, times(0)).push(any());
    }

    @Test
    public void shouldNotPushMessageToLogBrokerIfWarehouseIdNull() {
        publishService.publish(createDimensionsData(), null);

        Mockito.verify(logBrokerPushService, times(0)).push(any());
    }

    private Map<ItemIdentifier, DimensionsDTO> createDimensionsData() {
        return ImmutableMap.of(
                ItemIdentifier.of("1", "sku1"), DimensionsDTO.builder()
                        .setWidth(toBigDecimal(110, 3))
                        .setHeight(toBigDecimal(210, 3))
                        .setLength(toBigDecimal(310, 3))
                        .setWeightGross(toBigDecimal(1110, 3))
                        .build(),
                ItemIdentifier.of("1", "sku2"), DimensionsDTO.builder()
                        .setHeight(toBigDecimal(510, 3))
                        .setLength(toBigDecimal(610, 3))
                        .build());
    }

    private Map<ItemIdentifier, ShelfLifeMeasurementDTO> createShelfLifeData() {
        return ImmutableMap.of(
                ItemIdentifier.of("1", "sku1"), ShelfLifeMeasurementDTO.builder()
                        .setOperatorId("john_doe_666")
                        .setShelfLife(14)
                        .build(),
                ItemIdentifier.of("1", "sku2"), ShelfLifeMeasurementDTO.builder()
                        .setOperatorId("john_doe_666")
                        .setShelfLife(28)
                        .build());
    }
}
