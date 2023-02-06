package ru.yandex.market.logistics.nesu.jobs.processor;

import java.time.ZoneId;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.consumer.UpdateShopPickupPointTariffConsumer;
import ru.yandex.market.logistics.nesu.jobs.model.ShopPickupPointMetaIdPayload;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.shop.PickupPointDeliveryRuleCreateRequest;
import ru.yandex.market.logistics.tarifficator.model.dto.shop.PickupPointDeliveryRuleResponse;
import ru.yandex.market.logistics.tarifficator.model.dto.shop.PickupPointDeliveryRuleUpdateRequest;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.PickupPointDeliveryRuleStatus;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.money.common.dbqueue.api.TaskExecutionResult.Type.FINISH;

@DisplayName("Проверка создания правила доставки ПВЗ магазина в Тариффикаторе")
@DatabaseSetup("/jobs/processor/shop_pickup_point_tariff/update/before.xml")
class UpdateShopPickupPointTariffProcessorTest extends AbstractContextualTest {

    @Autowired
    private UpdateShopPickupPointTariffConsumer updateShopPickupPointTariffConsumer;

    @Autowired
    private TarifficatorClient tarifficatorClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(tarifficatorClient);
    }

    @Test
    @DisplayName("Успешное обновление правила доставки ПВЗ магазина в Тариффикаторе")
    void update() {
        when(tarifficatorClient.updatePickupPointDeliveryRule(eq(1L), safeRefEq(getUpdateRequest())))
            .thenReturn(new PickupPointDeliveryRuleResponse().setId(1L));

        softly.assertThat(updateShopPickupPointTariffConsumer.execute(getTask()).getActionType())
            .isEqualTo(FINISH);

        verify(tarifficatorClient).updatePickupPointDeliveryRule(eq(1L), safeRefEq(getUpdateRequest()));
    }

    @Nonnull
    private PickupPointDeliveryRuleUpdateRequest getUpdateRequest() {
        return new PickupPointDeliveryRuleCreateRequest()
            .setStatus(PickupPointDeliveryRuleStatus.ACTIVE)
            .setDaysFrom(3)
            .setDaysTo(4)
            .setPickupPointType("PICKUP_POINT")
            .setOrderBeforeHour(13);
    }

    @Nonnull
    private Task<ShopPickupPointMetaIdPayload> getTask() {
        return new Task<>(
            new QueueShardId("1"),
            getShopPickupPointMetaIdPayload(),
            1,
            clock.instant().atZone(ZoneId.systemDefault()),
            null,
            null
        );
    }

    @Nonnull
    private ShopPickupPointMetaIdPayload getShopPickupPointMetaIdPayload() {
        return new ShopPickupPointMetaIdPayload("123", 1);
    }

}
