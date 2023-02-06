package ru.yandex.market.mbo.mdm.common.masterdata.services.iris;

import java.util.Arrays;
import java.util.Collections;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.application.monitoring.MonitoringUnit;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class IrisEventProcessorMonitoringTest extends MdmBaseDbTestClass {

    @Autowired
    private IrisEventProcessor irisEventProcessor;
    @Autowired
    private StorageKeyValueService keyValueService;
    @Autowired
    private ComplexMonitoring complexMonitoring;

    private static final String MONITORING_NAME = "IrisEventProcessor monitoring";
    private MonitoringUnit monitoringUnit;
    private EnhancedRandom random;
    private static final int SEED = 343215;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandomBuilder(SEED)
            .randomize(CompressionCodec.class, (Randomizer<CompressionCodec>) () -> CompressionCodec.RAW)
            .build();
        monitoringUnit = complexMonitoring.getOrCreateUnit(MONITORING_NAME);
    }

    @Test
    public void whenDataIsInvalidShouldSetMonitoringToCrit() {
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(new ShopSkuKey(1, "test"),
            MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(1.0, 1.5, 2.0, 0.001, null, null, 0L));
        MessageMeta messageMeta = random.nextObject(MessageMeta.class);

        byte[] unCorrectBytes = Arrays.copyOf(item.toByteArray(), item.toByteArray().length + 2);
        MessageData messageData = new MessageData(unCorrectBytes, 0, messageMeta);
        MessageBatch messageBatch = new MessageBatch("topic", 0, Collections.singletonList(messageData));

        //Отправляем некорректные данные
        irisEventProcessor.process(messageBatch);
        MonitoringStatus status = monitoringUnit.getStatus();
        Assertions.assertThat(status).isEqualTo(MonitoringStatus.CRITICAL);
        boolean isMonitoringInManualMode = keyValueService.getBool(MdmProperties.IRIS_LB_READER_MANUAL_MONITORING_MODE,
            false);
        Assertions.assertThat(isMonitoringInManualMode).isTrue();

        //Отправляем корректные данные
        messageData = new MessageData(item.toByteArray(), 0, messageMeta);
        messageBatch = new MessageBatch("topic", 0, Collections.singletonList(messageData));
        irisEventProcessor.process(messageBatch);
        status = monitoringUnit.getStatus();
        Assertions.assertThat(status).isEqualTo(MonitoringStatus.CRITICAL);
        isMonitoringInManualMode = keyValueService.getBool(MdmProperties.IRIS_LB_READER_MANUAL_MONITORING_MODE,
            false);
        Assertions.assertThat(isMonitoringInManualMode).isTrue();

        //Отключаем мануальный режим и отправляем корреткные данные
        keyValueService.putValue(MdmProperties.IRIS_LB_READER_MANUAL_MONITORING_MODE, false);
        irisEventProcessor.process(messageBatch);
        status = monitoringUnit.getStatus();
        Assertions.assertThat(status).isEqualTo(MonitoringStatus.OK);
    }
}
