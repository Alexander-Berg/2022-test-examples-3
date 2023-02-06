package ru.yandex.market.billing.checkout;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.DATE_2017_01_01;
import static ru.yandex.market.billing.checkout.GetOrderEventsStrategyCommon.buildPartiallyMockedStrategy;

/**
 * Обработка события {@link HistoryEventType#ITEMS_UPDATED}.
 */
@DbUnitDataSet(before = {"db/datasource.csv", "db/environmentZeroDelay.csv"})
public class GetOrderEventsStrategy_UpdateItemsEvent_Test extends FunctionalTest implements ResourceUtilitiesMixin {

    private static final String RESOURCE_PREFIX = "resources/checkouter_response/events/items_update/";
    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    private ObjectMapper objectMapper;
    @Autowired
    private EventProcessorSupportFactory eventProcessorSupportFactory;
    private GetOrderEventsStrategy strategy;

    @BeforeEach
    void before() {
        strategy = buildPartiallyMockedStrategy(eventProcessorSupportFactory.createSupport());
    }

    /**
     * Тест обновления состава необиленного заказа.
     * <p>
     * Подготовка данных для теста содержится:
     * - в виде среза таблиц
     * - в виде подготовки события от Ч через {@link RestTemplate} + json. orderBefore содержит пустое items, так как
     * его содержимое не используется логике - items before поднимаются из базы.
     * Не все поля из таблиц используются в тесте, но инициализация в json и представление в таблицах максимально
     * синхронизированно для удобства восприятия.
     * <p>
     * До обработки события:
     * - состав заказа имеет непустое множество позиций
     * <p>
     * После обработки события:
     * - состав заказов в cpa_order_item меняется(удаляются позиции,изменяются счетчики и ПЕРЕСЧИТЫВАЮТСЯ значения комиссии в полях FEE_UE,FEE_NET_UE)
     * - в таблице cpa_order ПЕРЕСЧИТЫВАЕТСЯ общая сумма заказа (поля ITEMS_TOTAL,ITEMS_TOTAL_UE).
     * - в таблице cpa_order ПЕРЕСЧИТЫВАЮТСЯ данные о комиссии (поля FEE_SUM,FEE_CORRECT)
     * - итоговая комисиия в поле cpa_order.FEE_SUM(и производных от него) соответствует(с учетом множителей) сумме
     * комиссий cpa_order_item.FEE_UE (и FEE_NET_UE) по всем позициям.
     * - скидка не применяется, так как тип оплаты {@link PaymentMethod#CASH_ON_DELIVERY}.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateItems_WhenNotBilled_ChangeFee_ChangeTotal_PaidWithCashOnDelivery.before.csv",
            after = "db/GetOrderEventsStrategy_updateItems_WhenNotBilled_ChangeFee_ChangeTotal_PaidWithCashOnDelivery.after.csv"
    )
    void test_process_UpdateOrderItems_should_updateOrderFeeValues_when_orderNotInBilledStatusAndPaidWithCashOnDelivery() throws IOException {
        List<OrderHistoryEvent> events = events(
                "processingToProcessing-with-" +
                        "itemsCountDecrementWithDelete-paymentCashOnDelivery.json"
        );

        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Тест обновления состава необиленного заказа.
     * <p>
     * Подготовка данных для теста содержится:
     * - в виде среза таблиц
     * - в виде подготовки события от Ч через {@link RestTemplate} + json. orderBefore содержит пустое items, так как
     * его содержимое не используется логике - items before поднимаются из базы.
     * Не все поля из таблиц используются в тесте, но инициализация в json и представление в таблицах максимально
     * синхронизированно для удобства восприятия.
     * <p>
     * До обработки события:
     * - состав заказа имеет непустое множество позиций
     * <p>
     * После обработки события:
     * - состав заказов в cpa_order_item меняется(удаляются позиции,изменяются счетчики и ПЕРЕСЧИТЫВАЮТСЯ значения комиссии в полях FEE_UE,FEE_NET_UE)
     * - в таблице cpa_order ПЕРЕСЧИТЫВАЕТСЯ общая сумма заказа (поля ITEMS_TOTAL,ITEMS_TOTAL_UE).
     * - в таблице cpa_order ПЕРЕСЧИТЫВАЮТСЯ данные о комиссии (поля FEE_SUM,FEE_CORRECT)
     * - итоговая комисиия в поле cpa_order.FEE_SUM(и производных от него) соответствует(с учетом множителей) сумме
     * комиссий cpa_order_item.FEE_UE (и FEE_NET_UE) по всем позициям.
     * - применяется скидка, так как тип оплаты {@link PaymentMethod#YANDEX}.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateItems_WhenNotBilled_ChangeFee_ChangeTotal_PaidWithYandex.before.csv",
            after = "db/GetOrderEventsStrategy_updateItems_WhenNotBilled_ChangeFee_ChangeTotal_PaidWithYandex.after.csv"
    )
    void test_process_UpdateOrderItems_should_updateOrderFeeValues_when_orderNotInBilledStatusAndPaidWithYandex() throws IOException {
        List<OrderHistoryEvent> events = events(
                "processingToProcessing-with-itemsCountDecrementWithDelete-paymentYandex.json"
        );

        strategy.process(events, DATE_2017_01_01);
    }

    /**
     * Даже без изменений в составе должно проходить обновление полей заказа.
     * Это полезно например вот в каком случае: Ч при заборе ивентов, в items, возвращает всегда актуалное значение.
     * То есть если пропустить события создания, перевота pending->processing,items_update и тд, то при последующем
     * заборе во всех orderAfter->orderBefore будет один и тот же состав items соотествующий последнему состоянию.
     * Потому переход items_update будет с равными orderBefore.items и orderAfter.items ->, но будет содержать
     * обновление полей для заказа (cpa_order).
     * И чтобы результат был консистенным, надо обновлять ползя заказа в любом случае.
     * <p>
     * В тесте json содержит подобный переход. orderBefore содержит пустое items, так как его содержимое не
     * используется логике - items before поднимаются из базы.
     * Содержимое items, одинаковое, но меняется itemsTotal.
     * Данные в полях fee* и items_total_* в таблице *before, не имеют значения.
     */
    @Test
    @DbUnitDataSet(
            before = "db/GetOrderEventsStrategy_updateItems_WhenNotBilled_WhenNoItemsChanged.before.csv",
            after = "db/GetOrderEventsStrategy_updateItems_WhenNotBilled_WhenNoItemsChanged.after.csv"
    )
    void test_process_UpdateOrderItems_should_updateAnyOrderValues_when_itemsHasNoChangeButOrderChangedAndOrderNotBilled()
            throws IOException {
        List<OrderHistoryEvent> events = events(
                "processingToProcessing-with-noItemsChange-" +
                        "hasOrderChange-imitateLostPreviousEvents.json");

        strategy.process(events, DATE_2017_01_01);
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public String getResourcePrefix() {
        return RESOURCE_PREFIX;
    }
}
