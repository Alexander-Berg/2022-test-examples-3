package ru.yandex.market.logistics.management.service.export.points;

import java.io.IOException;
import java.io.InputStream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.logistics.delivery.calculator.DeliveryCalculator;
import ru.yandex.market.logistics.delivery.calculator.DeliveryCalculator.PickupPointInfo;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.repository.export.dynamic.s3.MdsS3BucketClient;
import ru.yandex.market.protobuf.factories.Magics;
import ru.yandex.market.protobuf.readers.LEDelimerMessageReader;
import ru.yandex.market.protobuf.readers.MessageReader;
import ru.yandex.market.protobuf.streams.YandexSnappyInputStream;
import ru.yandex.market.protobuf.tools.MagicChecker;

import static ru.yandex.market.Magics.MagicConstants.MLPP;

@DatabaseSetup("/data/service/export/dynamic/db/before/prepare.xml")
class PickupPointsReportTest extends AbstractContextualTest {

    @Autowired
    private MdsS3BucketClient mdsClient;

    @Autowired
    private PickupPointsDynamicReportFacade pickupPointsDynamicReportFacade;

    /**
     * Тестовый сценарий:
     * <ul>
     *     <li>БД инициализируется тремя ПВЗ и одним складом. Активны только два ПВЗ</li>
     *     <li>Выполняется выгрузка ПВЗ</li>
     *     <li>Проверяется контент, переданный в mds-клиент</li>
     * </ul>
     */
    @Test
    void testExportPickupPoints() throws IOException {
        pickupPointsDynamicReportFacade.instantUpdate();

        ArgumentCaptor<String> captorPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ContentProvider> captorContent = ArgumentCaptor.forClass(ContentProvider.class);
        Mockito.verify(mdsClient).upload(captorPath.capture(), captorContent.capture());

        String path = captorPath.getValue();
        softly.assertThat(path).isEqualTo("pickup_points/active_pickup_points.pbuf.sn");

        ContentProvider content = captorContent.getValue();

        DeliveryCalculator.PickupPoints points;
        try (InputStream is = new YandexSnappyInputStream(content.getInputStream())) {

            MagicChecker.checkMagic(is, MLPP.name());
            MessageReader<DeliveryCalculator.PickupPoints> messageReader =
                new LEDelimerMessageReader<>(Magics.getParser(MLPP.name()), is);

            points = messageReader.read();
        }

        DeliveryCalculator.PickupPointInfo firstPoint = PickupPointInfo.newBuilder()
            .setId(1)
            .setPartnerId(1)
            .setCode("8530-47e5-93c5-d44320e55dc1")
            .build();

        DeliveryCalculator.PickupPointInfo secondPoint = PickupPointInfo.newBuilder()
            .setId(2)
            .setPartnerId(1)
            .setCode("8530-47e5-93c5-d44320e55dc2")
            .build();

        softly.assertThat(points.getPickupPointsList())
            .containsExactlyInAnyOrderElementsOf(ImmutableList.of(firstPoint, secondPoint));
    }
}
