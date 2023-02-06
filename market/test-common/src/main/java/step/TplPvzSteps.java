package step;

import javax.annotation.Nonnull;

import client.TplPvzClient;
import dto.responses.tpl.pvz.PvzOrderDto;
import dto.responses.tpl.pvz.PvzReturnDto;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

@Slf4j
public class TplPvzSteps {
    private static final TplPvzClient TPL_PVZ_CLIENT = new TplPvzClient();

    @Nonnull
    @Step("Проверить код заказа в ПВЗ")
    public PvzOrderDto verifyCodeForPvzOrder(String pvzId, String id, String code) {
        return Retrier.clientRetry(() -> {
                PvzOrderDto pvzOrderDto = TPL_PVZ_CLIENT.verifyCodeForPvzOrder(pvzId, id, code);
                Assertions.assertEquals(
                    true,
                    pvzOrderDto.getVerificationCode().getAccepted(),
                    "Код верификации в заказе в ЛОМе и в ПВЗ не совпадает"
                );
                return pvzOrderDto;
            }
        );
    }

    @Step("Приёмка возврата в ПВЗ")
    public void receiveReturn(Long pvzId, Long id) {
        Retrier.clientRetry(() -> {
            PvzReturnDto returnDto = TPL_PVZ_CLIENT.receiveReturn(pvzId, id);
            Assertions.assertEquals(
                "RECEIVED",
                returnDto.getStatus(),
                "Неподходящий статус возврата в ПВЗ"
            );
        });
    }

    @Step("Приемка заказа в ПВЗ")
    public void receiveOrderInPvz(Long pvzId, String id) {
        Retrier.clientRetry(() -> {
            TPL_PVZ_CLIENT.receiveOrder(pvzId, id);
            Assertions.assertEquals(
                "ARRIVED_TO_PICKUP_POINT",
                getOrder(pvzId, id).getStatus(),
                "Неподходящий статус возврата в ПВЗ"
            );
        });
    }

    @Step("Получаем заказ в ПВЗ")
    public PvzOrderDto getOrder(Long pvzId, String orderId) {
        return TPL_PVZ_CLIENT.getOrder(pvzId, orderId).getOrder();
    }
}
