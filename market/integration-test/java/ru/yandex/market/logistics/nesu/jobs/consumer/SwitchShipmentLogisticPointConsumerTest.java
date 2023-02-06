package ru.yandex.market.logistics.nesu.jobs.consumer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;
import ru.yandex.market.logistics.nesu.controller.warehouse.AbstractSwitchShipmentLogisticPointTest;
import ru.yandex.market.logistics.nesu.jobs.model.SwitchShipmentLogisticPointPayload;
import ru.yandex.market.logistics.nesu.jobs.producer.SendNotificationToShopProducer;
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DatabaseSetup({
    "/service/shop/prepare_database.xml",
    "/repository/validation/default_validation_settings.xml",
    "/jobs/consumer/switch_shipment_logistic_point/shop_partner_settings.xml",
    "/jobs/consumer/switch_shipment_logistic_point/logistic_point_availability.xml",
})
@DisplayName("Переключение партнера на СЦ")
public class SwitchShipmentLogisticPointConsumerTest extends AbstractSwitchShipmentLogisticPointTest {

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private SwitchShipmentLogisticPointConsumer switchShipmentLogisticPointConsumer;

    @Autowired
    private SendNotificationToShopProducer sendNotificationToShopProducer;

    @RegisterExtension
    final BackLogCaptor backLogCaptor = new BackLogCaptor();

    @BeforeEach
    void setup() {
        clock.setFixed(NOW, ZoneId.systemDefault());
        mockGetPartnerRelation();
        mockSearchAvailableShipmentOptions();
        mockSetPartnerRelation();
        mockBannerRemoving();
        doNothing().when(sendNotificationToShopProducer).produceTask(anyInt(), anyLong(), anyLong(), anyString());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, sendNotificationToShopProducer, mbiApiClient);
    }

    @Test
    @DisplayName("Успешное переключение партнера на СЦ")
    @DatabaseSetup(value = "/service/shop/remove_market_id_3.xml", type = DatabaseOperation.UPDATE)
    void switchShipmentLogisticPointSuccess() {
        switchShipmentLogisticPointConsumer.execute(getTask());
        verifySwitchLogisticPoint();

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "Generated request CpaPartnerInterfaceRelationRequest("
                    + "shipmentType=IMPORT, "
                    + "capacityValue=50, "
                    + "cutoffTime=10:00, "
                    + "handlingTime=0, "
                    + "toPartnerLogisticsPointId=5101, "
                    + "shipmentScheduleDayIds=[3], "
                    + "partnerSchedules=null, "
                    + "useElectronicReceptionTransferAct=null, "
                    + "handlingTimeExpressMinutes=40, "
                    + "expressReturnSortingCenterId=null"
                    + ") "
                    + "to create new partner relation"
            );

        ArgumentCaptor<String> xmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendNotificationToShopProducer).produceTask(
            eq(1625113398),
            eq(SHOP_ID),
            eq(PARTNER_ID),
            xmlCaptor.capture()
        );
        softly.assertThat(xmlCaptor.getValue())
            .isXmlEqualTo(extractFileContent(
                "jobs/consumer/switch_shipment_logistic_point/notification_data.xml"
            ));
    }

    @Test
    @DisplayName("Ошибка при переключении партнера на СЦ. Не найдено доступных вариантов отгрузки для СЦ")
    void switchShipmentLogisticPointErrorAvailableShipmentOptionsNotFound() {
        mockNotFoundPoint();
        mockNotFoundPartners();
        when(lmsClient.searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnerId(PARTNER_ID)
                .build()
        )).thenReturn(List.of(createPartnerRelation(ShipmentType.WITHDRAW)));

        switchShipmentLogisticPointConsumer.execute(getTask());

        verifyLmsScNotFound();
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "ShipmentOptionsNotFoundException: Available shipment options with shipment type WITHDRAW " +
                    "not found for partner 1001"
            );
    }

    @Test
    @DisplayName("Ошибка при переключении партнера на СЦ. Не найдено СЦ в регионе")
    void switchShipmentLogisticPointErrorSortingCenterInRegionNotFound() {
        mockNotFoundPoint();
        mockNotFoundPartners();
        switchShipmentLogisticPointConsumer.execute(getTask());

        verifyLmsScNotFound();
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("No sc has been found in region with id=1");
    }

    @Nonnull
    private Task<SwitchShipmentLogisticPointPayload> getTask() {
        return new Task<>(
            new QueueShardId("queueShardId"),
            new SwitchShipmentLogisticPointPayload(
                "requestId",
                PARTNER_ID,
                SHOP_ID,
                "Самый лучший склад"
            ),
            3,
            ZonedDateTime.now(),
            "traceInfo",
            "actor"
        );
    }
}
